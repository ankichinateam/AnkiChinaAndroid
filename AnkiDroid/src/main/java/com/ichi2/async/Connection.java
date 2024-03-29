/****************************************************************************************
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
 * Copyright (c) 2011 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 * Copyright (c) 2012 Norbert Nagold <norbert.nagold@gmail.com>                         *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.async;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.anki.R;
import com.ichi2.anki.exception.MediaSyncException;
import com.ichi2.anki.exception.NoEnoughServerSpaceException;
import com.ichi2.anki.exception.UnknownHttpResponseException;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.sync.FullSyncer;
import com.ichi2.libanki.sync.HostNum;
import com.ichi2.libanki.sync.HttpSyncer;
import com.ichi2.libanki.sync.MediaSyncer;
import com.ichi2.libanki.sync.RemoteMediaServer;
import com.ichi2.libanki.sync.RemoteServer;
import com.ichi2.libanki.sync.Syncer;
import com.ichi2.utils.Permissions;

import com.ichi2.utils.JSONException;
import com.ichi2.utils.JSONObject;

import java.io.IOException;

import okhttp3.Response;
import timber.log.Timber;

public class Connection extends BaseAsyncTask<Connection.Payload, Object, Connection.Payload> {

    private static final int LOGIN = 0;
    private static final int SYNC = 1;

    private static final int TASK_TYPE_COMMON_POST = 2;
    private static final int TASK_TYPE_COMMON_GET = 3;
    private static final int TASK_TYPE_COMMON_PUT = 4;
    public static final int CONN_TIMEOUT = 30000;


    private static Connection sInstance;
    private TaskListener mListener;
    private static boolean sIsCancelled;
    private static boolean sIsCancellable;

    private static boolean sAllowSyncOnNoConnection;

    /**
     * Before syncing, we acquire a wake lock and then release it once the sync is complete.
     * This ensures that the device remains awake until the sync is complete. Without it,
     * the process will be paused and the sync can fail due to timing conflicts with AnkiWeb.
     */
    private final PowerManager.WakeLock mWakeLock;


    public static synchronized boolean getIsCancelled() {
        return sIsCancelled;
    }


    public Connection() {
        sIsCancelled = false;
        sIsCancellable = false;
        Context context = AnkiDroidApp.getInstance().getApplicationContext();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                AnkiDroidApp.getAppResources().getString(R.string.app_name) + ":Connection");
    }


    private static Connection launchConnectionTask(TaskListener listener, Payload data) {

        if (!isOnline()) {
            data.success = false;
            listener.onDisconnected();
            return null;
        }

        try {
            if ((sInstance != null) && (sInstance.getStatus() != AsyncTask.Status.FINISHED)) {
                sInstance.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        sInstance = new Connection();
        sInstance.mListener = listener;

        sInstance.execute(data);
        return sInstance;
    }


    public static boolean getAllowSyncOnNoConnection() {
        return sAllowSyncOnNoConnection;
    }


    public static void setAllowSyncOnNoConnection(boolean value) {
        sAllowSyncOnNoConnection = value;
    }


    /*
     * Runs on GUI thread
     */
    @Override
    protected void onCancelled() {
        super.onCancelled();
        Timber.i("Connection onCancelled() method called");
        // Sync has ended so release the wake lock
        mWakeLock.release();
        if (mListener instanceof CancellableTaskListener) {
            ((CancellableTaskListener) mListener).onCancelled();
        }
    }


    /*
     * Runs on GUI thread
     */
    @SuppressLint("WakelockTimeout")
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Acquire the wake lock before syncing to ensure CPU remains on until the sync completes.
        if (Permissions.canUseWakeLock(AnkiDroidApp.getInstance().getApplicationContext())) {
            mWakeLock.acquire();
        }
        if (mListener != null) {
            mListener.onPreExecute();
        }
    }


    /*
     * Runs on GUI thread
     */
    @Override
    protected void onPostExecute(Payload data) {
        super.onPostExecute(data);
        // Sync has ended so release the wake lock
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mListener != null) {
            mListener.onPostExecute(data);
        }
    }


    /*
     * Runs on GUI thread
     */
    @Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);
        if (mListener != null) {
            mListener.onProgressUpdate(values);
        }
    }


    public static Connection login(TaskListener listener, Payload data) {
        data.taskType = LOGIN;
        return launchConnectionTask(listener, data);
    }


    public static Connection sendCommonPost(TaskListener listener, Payload data) {
        data.taskType = TASK_TYPE_COMMON_POST;
        return launchConnectionTask(listener, data);
    }


    public static Connection sendCommonGet(TaskListener listener, Payload data) {
        data.taskType = TASK_TYPE_COMMON_GET;
        return launchConnectionTask(listener, data);
    }


    public static Connection sendCommonPUT(TaskListener listener, Payload data) {
        data.taskType = TASK_TYPE_COMMON_PUT;
        return launchConnectionTask(listener, data);
    }


    public static Connection sync(TaskListener listener, Payload data) {
        data.taskType = SYNC;
        return launchConnectionTask(listener, data);
    }


    @Override
    protected Payload doInBackground(Payload... params) {
        super.doInBackground(params);
        if (params.length != 1) {
            throw new IllegalArgumentException();
        }
        return doOneInBackground(params[0]);
    }


    private Payload doOneInBackground(Payload data) {
        switch (data.taskType) {
            case LOGIN:
                return doInBackgroundLogin(data);

            case SYNC:
                return doInBackgroundSync(data);
            case TASK_TYPE_COMMON_POST:
            case TASK_TYPE_COMMON_GET:
            case TASK_TYPE_COMMON_PUT:
                return doInBackgroundCommonRequest(data);
            default:
                return null;
        }
    }


    private Payload doInBackgroundLogin(Payload data) {
        String username = (String) data.data[0];
        String password = (String) data.data[1];
        HostNum hostNum = (HostNum) data.data[2];
        HttpSyncer server = new RemoteServer(this, null, hostNum);
        Response ret;
        try {
            ret = server.hostKey(username, password);
        } catch (UnknownHttpResponseException e) {
            data.success = false;
            data.result = new Object[] {"error", e.getResponseCode(), e.getMessage()};
            return data;
        } catch (Exception e2) {
            // Ask user to report all bugs which aren't timeout errors
            if (!timeoutOccurred(e2)) {
                AnkiDroidApp.sendExceptionReport(e2, "doInBackgroundLogin");
            }
            data.success = false;
            data.result = new Object[] {"connectionError", e2};
            return data;
        }
        String hostkey = null;
        boolean valid = false;
        if (ret != null) {
            data.returnType = ret.code();
            Timber.d("doInBackgroundLogin - response from server: %d, (%s)", data.returnType, ret.message());
            if (data.returnType == 200) {
                try {
                    JSONObject response = new JSONObject(ret.body().string());
                    hostkey = response.getString("key");
                    valid = (hostkey != null) && (hostkey.length() > 0);
                } catch (JSONException e) {
                    valid = false;
                } catch (IllegalStateException | IOException | NullPointerException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            Timber.e("doInBackgroundLogin - empty response from server");
        }
        if (valid) {
            data.success = true;
            data.data = new String[] {username, hostkey};
        } else {
            data.success = false;
        }
        return data;
    }


    private Payload doInBackgroundCommonRequest(Payload data) {
        HostNum hostNum =data.hostNum;
        HttpSyncer server = new RemoteServer(this, null,hostNum);
        Response ret;
        try {
            ret = data.RESTType == Payload.REST_TYPE_POST ? server.sendCommonPost(data.method, data.param, data.token) : data.RESTType == Payload.REST_TYPE_GET ? server.sendCommonGet(data.method, data.param, data.token) : server.sendCommonPUT(data.method, data.param, data.token);
        } catch (UnknownHttpResponseException e) {
            data.success = false;
            data.result = new Object[] {"error", e.getResponseCode(), e.getMessage()};
            return data;
        } catch (Exception e2) {
            // Ask user to report all bugs which aren't timeout errors
            if (!timeoutOccurred(e2)) {
                AnkiDroidApp.sendExceptionReport(e2, "doInBackgroundCommonRequest");
            }
            data.success = false;
            data.result = new Object[] {"connectionError"};
            return data;
        }
        int status_code = -1;
        String message = null;
        JSONObject bodyData = null;
        boolean valid = false;
        if (ret != null) {
            data.returnType = ret.code();
            Timber.d("doInBackgroundCommonRequest - response from server: %d, (%s)", data.returnType, ret.message());
            if (data.returnType == 200) {
                try {
                    String bodyStr = ret.body().string();
                    Timber.d("response body:%s", bodyStr);

                    JSONObject jo = (new JSONObject(bodyStr));
                    status_code = jo.getInt("status_code");
                    message = jo.getString("message");
                    bodyData = jo;
                    valid = status_code == 0;

                } catch (JSONException e) {
                    valid = false;
                } catch (IllegalStateException | IOException | NullPointerException e) {
//                    throw new RuntimeException(e);
                    e.printStackTrace();
                }
            }
        } else {
            Timber.e("doInBackgroundCommonRequest - empty response from server");
        }
        data.success = valid;
        data.message = message;
        data.statusCode = status_code;
        data.result = bodyData;
        return data;
    }


    private boolean timeoutOccurred(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("UnknownHostException") ||
                msg.contains("HttpHostConnectException") ||
                msg.contains("SSLException while building HttpClient") ||
                msg.contains("SocketTimeoutException") ||
                msg.contains("ClientProtocolException") ||
                msg.contains("deadline reached") ||
                msg.contains("interrupted") ||
                msg.contains("Failed to connect") ||
                msg.contains("InterruptedIOException") ||
                msg.contains("stream was reset") ||
                msg.contains("ConnectionShutdownException") ||
                msg.contains("CLEARTEXT communication") ||
                msg.contains("TimeoutException");
    }


    private Payload doInBackgroundSync(Payload data) {
        sIsCancellable = true;
        Timber.d("doInBackgroundSync()");
        // Block execution until any previous background task finishes, or timeout after 5s
        boolean ok = CollectionTask.waitToFinish(5);

        String hkey = (String) data.data[0];
        boolean media = (Boolean) data.data[1];
        String conflictResolution = (String) data.data[2];
        HostNum hostNum = (HostNum) data.data[3];
        long restServerSpace = (long) data.data[4];
        // Use safe version that catches exceptions so that full sync is still possible
        Collection col = CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());

        boolean colCorruptFullSync = false;
        if (!CollectionHelper.getInstance().colIsOpen() || !ok) {
            if (conflictResolution != null && "download".equals(conflictResolution)) {
                colCorruptFullSync = true;
            } else {
                data.success = false;
                data.result = new Object[] {"genericError"};
                return data;
            }
        }
        try {
            CollectionHelper.getInstance().lockCollection();
            HttpSyncer server = new RemoteServer(this, hkey, hostNum);
            Syncer client = new Syncer(col, server, hostNum);

            // run sync and check state
            boolean noChanges = false;
            if (conflictResolution == null) {
                Timber.i("Sync - starting sync");
                publishProgress(R.string.sync_prepare_syncing);
                Object[] ret = client.sync(this, restServerSpace);
                data.message = client.getSyncMsg();
                if (ret == null) {
                    data.success = false;
                    data.result = new Object[] {"genericError"};
                    return data;
                }
                String retCode = (String) ret[0];
                if (!"noChanges".equals(retCode) && !"success".equals(retCode)) {
                    data.success = false;
                    data.result = ret;
                    // Check if there was a sanity check error
                    if ("sanityCheckError".equals(retCode)) {
                        // Force full sync next time
                        col.modSchemaNoCheck();
                        col.save();
                    }
                    return data;
                }
                // save and note success state
                if ("noChanges".equals(retCode)) {
                    // publishProgress(R.string.sync_no_changes_message);
                    noChanges = true;
                }
                restServerSpace = client.getRestSpace();
            } else {
                try {
                    // Disable sync cancellation for full-sync
                    sIsCancellable = false;
                    server = new FullSyncer(col, hkey, this, hostNum);
                    if ("upload".equals(conflictResolution)) {
                        Timber.i("Sync - fullsync - upload collection");
                        publishProgress(R.string.sync_preparing_full_sync_message);
                        Object[] ret = server.upload(restServerSpace);
                        col.reopen();
                        if (ret == null) {
                            data.success = false;
                            data.result = new Object[] {"genericError"};
                            return data;
                        }
                        if (!ret[0].equals(HttpSyncer.ANKIWEB_STATUS_OK)) {
                            data.success = false;
                            data.result = ret;
                            return data;
                        }
                    } else if ("download".equals(conflictResolution)) {
                        Timber.i("Sync - fullsync - download collection");
                        publishProgress(R.string.sync_downloading_message);
                        Object[] ret = server.download();
                        if (ret == null) {
                            Timber.w("Sync - fullsync - unknown error");
                            data.success = false;
                            data.result = new Object[] {"genericError"};
                            return data;
                        }
                        if ("success".equals(ret[0])) {
                            data.success = true;
                            col.reopen();
                        }
                        if (!"success".equals(ret[0])) {
                            Timber.w("Sync - fullsync - download failed");
                            data.success = false;
                            data.result = ret;
                            if (!colCorruptFullSync) {
                                col.reopen();
                            }
                            return data;
                        }
                    }
                } catch (OutOfMemoryError e) {
                    AnkiDroidApp.sendExceptionReport(e, "doInBackgroundSync-fullSync");
                    data.success = false;
                    data.result = new Object[] {"OutOfMemoryError"};
                    return data;
                } catch (RuntimeException e) {
                    if (timeoutOccurred(e)) {
                        data.result = new Object[] {"connectionError", e};
                    } else if ("UserAbortedSync".equals(e.getMessage())) {
                        data.result = new Object[] {"UserAbortedSync", e};
                    } else {
                        AnkiDroidApp.sendExceptionReport(e, "doInBackgroundSync-fullSync");
                        data.result = new Object[] {"IOException", e};
                    }
                    data.success = false;
                    return data;
                }
            }

            // clear undo to avoid non syncing orphans (because undo resets usn too
            if (!noChanges) {
                col.clearUndo();
            }
            // then move on to media sync
            sIsCancellable = true;
            boolean noMediaChanges = false;
            boolean enoughServerSpace = true;
            String mediaError = null;
            if (media) {
                server = new RemoteMediaServer(col, hkey, this, hostNum);
                MediaSyncer mediaClient = new MediaSyncer(col, (RemoteMediaServer) server, this);
                String ret;
                try {
                    ret = mediaClient.sync(restServerSpace);//������ܻ���Ϊ�ϴ��ļ������ռ��С���׳��쳣���ڴ�֮ǰ��Ҫ����ʣ��ռ��С
                    Timber.e("sync media ret is null");
                    if (ret == null) {
                        mediaError = AnkiDroidApp.getAppResources().getString(R.string.sync_media_error);
                    } else {
                        if ("corruptMediaDB".equals(ret)) {
                            mediaError = AnkiDroidApp.getAppResources().getString(R.string.sync_media_db_error);
                            noMediaChanges = true;
                        }
                        if ("noChanges".equals(ret)) {
                            publishProgress(R.string.sync_media_no_changes);
                            noMediaChanges = true;
                        }
                        if ("sanityFailed".equals(ret)) {
                            mediaError = AnkiDroidApp.getAppResources().getString(R.string.sync_media_sanity_failed);
                        } else {
                            publishProgress(R.string.sync_media_success);
                        }
                    }
                } catch (RuntimeException e) {
                    if (timeoutOccurred(e)) {
                        data.result = new Object[] {"connectionError", e};
                    } else if ("UserAbortedSync".equals(e.getMessage())) {
                        data.result = new Object[] {"UserAbortedSync", e};
                    }
                    mediaError = AnkiDroidApp.getAppResources().getString(R.string.sync_media_error) + "\n\n" + e.getLocalizedMessage();
                }
            }
            if (noChanges && (!media || noMediaChanges)) {
                data.success = false;
                data.result = new Object[] {"noChanges"};
                return data;
            } else {
                data.success = true;
                data.data = new Object[] {conflictResolution, col, mediaError};
                return data;
            }
        } catch (MediaSyncException e) {
            Timber.e("Media sync rejected by server");
            data.success = false;
            data.result = new Object[] {"mediaSyncServerError", e};
            AnkiDroidApp.sendExceptionReport(e, "doInBackgroundSync");
            return data;
        } catch (UnknownHttpResponseException e) {
            Timber.e(e, "doInBackgroundSync -- unknown response code error");
            data.success = false;
            int code = e.getResponseCode();
            String msg = e.getLocalizedMessage();
            data.result = new Object[] {"error", code, msg};
            return data;
        } catch (NoEnoughServerSpaceException e) {
            Timber.e("NoEnoughServerSpaceException ");
            e.printStackTrace();
            data.success = false;
            data.result = new Object[] {"noServerSpace", e.rest, e.need};
            return data;
        } catch (Exception e) {
            // Global error catcher.
            // Try to give a human readable error, otherwise print the raw error message
            Timber.e(e, "doInBackgroundSync error");
            data.success = false;
            if (timeoutOccurred(e)) {
                data.result = new Object[] {"connectionError", e};
            } else if ("UserAbortedSync".equals(e.getMessage())) {
                data.result = new Object[] {"UserAbortedSync", e};
            } else {
                AnkiDroidApp.sendExceptionReport(e, "doInBackgroundSync");
                data.result = new Object[] {e.getLocalizedMessage(), e};
            }
            return data;
        } finally {
            Timber.i("Sync Finished - Closing Collection");
            // don't bump mod time unless we explicitly save
            if (col != null) {
                col.close(false);
            }
            CollectionHelper.getInstance().unlockCollection();
        }
    }


    public void publishProgress(int id) {
        super.publishProgress(id);
    }


    public void publishProgress(String message) {
        super.publishProgress(message);
    }


    public void publishProgress(int id, long up, long down) {
        super.publishProgress(id, up, down);
    }

    @SuppressWarnings("deprecation")
    public static boolean isOnline() {
        if (sAllowSyncOnNoConnection) {
            return true;
        }
        ConnectivityManager cm = (ConnectivityManager) AnkiDroidApp.getInstance().getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        /* NetworkInfo is deprecated in API 29 so we have to check separately for higher API Levels */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(network);
            if (networkCapabilities == null) {
                return false;
            }
            boolean isInternetSuspended = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    && !isInternetSuspended;
        } else {
            android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }


    public interface TaskListener {
        void onPreExecute();


        void onProgressUpdate(Object... values);


        void onPostExecute(Payload data);


        void onDisconnected();
    }



    public interface CancellableTaskListener extends TaskListener {
        void onCancelled();
    }



    public static class Payload {
        private int taskType;
        public Object[] data;
        public Object result;
        public boolean success;
        public int returnType;
        public int statusCode;
        public Exception exception;
        public String message;
        public String method;
        public Collection col;
        public String param;
        public String token;
        public String syncConflictResolution;
        public int RESTType;
        public HostNum hostNum;
        public static final int REST_TYPE_POST = 0;
        public static final int REST_TYPE_PUT = 1;
        public static final int REST_TYPE_GET = 2;


        public Payload(Object[] data) {
            this.data = data;
            success = true;
        }


        public Payload(String method, String param, int RESTType, String token, String syncConflictResolution,HostNum hostNum) {
//            if (RESTType == REST_TYPE_POST) {
//                this.json = param;
//            } else {
//                this.param = param;
//            }
            this.param = param;
            this.RESTType = RESTType;
            this.method = method;
            this.token = token;
            this.hostNum = hostNum;
            this.syncConflictResolution = syncConflictResolution;

        }


        public Payload(String method, String param, int RESTType,HostNum hostNum) {
            this(method, param, RESTType, null, null,hostNum);
        }


        public Payload() {
        }

    }


    public synchronized static void cancel() {
        Timber.d("Cancelled Connection task");
        sInstance.cancel(true);
        sIsCancelled = true;
    }


    public synchronized static boolean isCancellable() {
        return sIsCancellable;
    }
}
