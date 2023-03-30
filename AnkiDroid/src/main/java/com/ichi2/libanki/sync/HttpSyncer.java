/***************************************************************************************
 * Copyright (c) 2012 Norbert Nagold <norbert.nagold@gmail.com>                         *
 * Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>                          *
 * Copyright (c) 2019 Mike Hardy <github@mikehardy.net>                                 *
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

package com.ichi2.libanki.sync;


import android.content.SharedPreferences;
import android.net.Uri;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.exception.NoEnoughServerSpaceException;
import com.ichi2.anki.exception.UnknownHttpResponseException;
import com.ichi2.anki.web.CustomSyncServer;
import com.ichi2.async.Connection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Utils;
import com.ichi2.utils.VersionUtils;

import org.apache.http.entity.AbstractHttpEntity;
import com.ichi2.utils.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLException;

import androidx.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

import static com.ichi2.libanki.Consts.ANKI_CHINA_BASE;
import static com.ichi2.libanki.Consts.API_VERSION;

/**
 * # HTTP syncing tools
 * Calling code should catch the following codes:
 * - 501: client needs upgrade
 * - 502: ankiweb down
 * - 503/504: server too busy
 */
@SuppressWarnings( {"PMD.AvoidThrowingRawExceptionTypes", "PMD.NPathComplexity"})
public class HttpSyncer {

    private static final String BOUNDARY = "Anki-sync-boundary";
    private static final MediaType ANKI_POST_TYPE = MediaType.get("multipart/form-data; boundary=" + BOUNDARY);

    public static final String ANKIWEB_STATUS_OK = "OK";

    public volatile long bytesSent = 0;
    public volatile long bytesReceived = 0;
    public volatile long mNextSendS = 1024;
    public volatile long mNextSendR = 1024;

    /**
     * Synchronization.
     */

    protected String mHKey;
    protected String mSKey;
    protected Connection mCon;
    protected Map<String, Object> mPostVars;
    private volatile OkHttpClient mHttpClient;
    private final HostNum mHostNum;

    public HttpSyncer(String hkey, Connection con, HostNum hostNum) {
        mHKey = hkey;
        mSKey = Utils.checksum(Float.toString(new Random().nextFloat())).substring(0, 8);
        mCon = con;
        mPostVars = new HashMap<>();
        mHostNum = hostNum;
    }

    private OkHttpClient.Builder getHttpClientBuilder() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> chain.proceed(
                        chain.request()
                                .newBuilder()
                                .header("User-Agent", "AnkiDroid-" + VersionUtils.getPkgVersionNameFake())
                                .build()
                ));
        Tls12SocketFactory.enableTls12OnPreLollipop(clientBuilder)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .cache(null)
                .connectTimeout(Connection.CONN_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Connection.CONN_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Connection.CONN_TIMEOUT, TimeUnit.SECONDS);
        return clientBuilder;
    }

    private OkHttpClient getHttpClient() {
        if (this.mHttpClient != null) {
            return mHttpClient;
        }
        return setupHttpClient();
    }

    //PERF: Thread safety isn't required for the current implementation
    private synchronized OkHttpClient setupHttpClient() {
        if (mHttpClient != null) {
            return mHttpClient;
        }
        mHttpClient = getHttpClientBuilder().build();
        return mHttpClient;
    }


    public void assertOk(Response resp) throws UnknownHttpResponseException {
        // Throw RuntimeException if HTTP error
        if (resp == null) {
            throw new UnknownHttpResponseException("Null HttpResponse", -2);
        }
        int resultCode = resp.code();
        if (!(resultCode == 200 || resultCode == 403)) {
            String reason = resp.toString();
            Timber.d("UnknownHttpResponseException: %d，%s",resultCode,reason);
            throw new UnknownHttpResponseException(reason, resultCode);
        }
    }

    /** Note: Return value must be closed */
    public Response req(String method) throws UnknownHttpResponseException {
        return req(method, null);
    }

    /** Note: Return value must be closed */
    public Response req(String method, InputStream fobj) throws UnknownHttpResponseException {
        return req(method, fobj, 6);
    }

    /** Note: Return value must be closed */
    public Response req(String method, int comp, InputStream fobj) throws UnknownHttpResponseException {
        return req(method, fobj, comp);
    }

    /** Note: Return value must be closed */
    public Response req(String method, InputStream fobj, int comp) throws UnknownHttpResponseException {
        return req(method, fobj, comp, null);
    }
private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    /** Note: Return value must be closed */
    private Response req(String method, InputStream fobj, int comp, JSONObject registerData) throws UnknownHttpResponseException {
        File tmpFileBuffer = null;
        try {
            String bdry = "--" + BOUNDARY;
            StringWriter buf = new StringWriter();
            // post vars
            mPostVars.put("c", comp != 0 ? 1 : 0);
            for (String key : mPostVars.keySet()) {
                buf.write(bdry + "\r\n");
                buf.write(String.format(Locale.US, "Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n", key,
                        mPostVars.get(key)));
            }
            tmpFileBuffer = File.createTempFile("syncer", ".tmp", new File(AnkiDroidApp.getCacheStorageDirectory()));
            FileOutputStream fos = new FileOutputStream(tmpFileBuffer);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            GZIPOutputStream tgt;
            // payload as raw data or json
            if (fobj != null) {
                // header
                buf.write(bdry + "\r\n");
                buf.write("Content-Disposition: form-data; name=\"data\"; filename=\"data\"\r\nContent-Type: application/octet-stream\r\n\r\n");
                buf.close();
                bos.write(buf.toString().getBytes("UTF-8"));
                // write file into buffer, optionally compressing
                int len;
                int totalLen = 0;
                BufferedInputStream bfobj = new BufferedInputStream(fobj);
                byte[] chunk = new byte[65536];
                if (comp != 0) {
                    tgt = new GZIPOutputStream(bos);
                    while ((len = bfobj.read(chunk)) >= 0) {
                        tgt.write(chunk, 0, len);
                        totalLen+=len;

                    }
                    tgt.close();
                    bos = new BufferedOutputStream(new FileOutputStream(tmpFileBuffer, true));
                } else {
                    while ((len = bfobj.read(chunk)) >= 0) {
                        bos.write(chunk, 0, len);
                        totalLen+=len;
                    }
                }
                Timber.d("Write common data length: %d",totalLen);//������Ⱦ�����ͨ���ݳ���
                Timber.d("Write common data length2: %d",tmpFileBuffer.length());//������Ⱦ�����ͨ���ݳ���
                bos.write(("\r\n" + bdry + "--\r\n").getBytes("UTF-8"));
            } else {
                buf.close();
                bos.write(buf.toString().getBytes("UTF-8"));
                bos.write((bdry + "--\r\n").getBytes("UTF-8"));
            }
            bos.flush();
            bos.close();
            // connection headers

            String url = Uri.parse(syncURL()).buildUpon().appendPath(method).toString();

            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(parseUrl(url));
            Timber.d("request url: %s",url);

            requestBuilder.post(new CountingFileRequestBody(tmpFileBuffer, ANKI_POST_TYPE.toString(), num -> {
                bytesSent += num;
                publishProgress();
            }));
            Request httpPost = requestBuilder.build();

            try {
                OkHttpClient httpClient = getHttpClient();
                Response httpResponse = httpClient.newCall(httpPost).execute();

                // we assume badAuthRaises flag from Anki Desktop always False
                // so just throw new RuntimeException if response code not 200 or 403
                Timber.d("TLSVersion in use is: %s",
                        (httpResponse.handshake() != null ? httpResponse.handshake().tlsVersion() : "unknown"));


                assertOk(httpResponse);
                return httpResponse;
            } catch (SSLException e) {
                Timber.e(e, "SSLException while building HttpClient");
                throw new RuntimeException("SSLException while building HttpClient", e);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Timber.e(e, "BasicHttpSyncer.sync: IOException");
            throw new RuntimeException(e);
        } finally {
            if (tmpFileBuffer != null && tmpFileBuffer.exists()) {
                tmpFileBuffer.delete();
            }
        }
    }
 public Response reqPost( String json,String method,String token) throws UnknownHttpResponseException {
        try {
            // connection headers
            String url = ANKI_CHINA_BASE+ API_VERSION  + method;
            Timber.d("final url: %s",
                    url);
            RequestBody body = RequestBody.create(JSON, json);
            Request.Builder builder=new Request.Builder().url(url).post(body)  .addHeader("User-Agent", "AnkiDroid-" + VersionUtils.getPkgVersionNameFake())
                    .addHeader("Accept",   "application/json" )
                    .addHeader("x-device-id",   Utils.UUID )
                    .addHeader("Content-Type",   "application/json" );
            if(token!=null){
                builder  .addHeader("Authorization",  "Bearer "+ token );
            }
            Request httpPost=builder.build();

            try {
                OkHttpClient httpClient =new OkHttpClient();
                return httpClient.newCall(httpPost).execute();
            } catch (SSLException e) {
                Timber.e(e, "SSLException while building HttpClient");
                throw new RuntimeException("SSLException while building HttpClient");
            }
        } catch (UnsupportedEncodingException  e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Timber.e(e, "BasicHttpSyncer.sync: IOException");
            throw new RuntimeException(e);
        }
    }
    public Response reqGet( String param,String method,String token) throws UnknownHttpResponseException {
        try {
            // connection headers
            String url = ANKI_CHINA_BASE +API_VERSION  + method +param;
            Timber.d("final url: %s",
                    url);
            Request.Builder builder=new Request.Builder().url(url)  .addHeader("User-Agent", "AnkiDroid-" + VersionUtils.getPkgVersionNameFake())  .addHeader("x-device-id",   Utils.UUID ).addHeader("Accept",   "application/json" ).addHeader("Content-Type",   "application/json" ).addHeader("x-os",   "Android" );
            if(token!=null){
                builder  .addHeader("Authorization",   "Bearer "+token );
            }
            Request httpRequest= builder.build();

            try {
                OkHttpClient httpClient =new OkHttpClient();
                return httpClient.newCall(httpRequest).execute();
            } catch (SSLException e) {
                Timber.e(e, "SSLException while building HttpClient");
                throw new RuntimeException("SSLException while building HttpClient");
            }
        } catch (UnsupportedEncodingException  e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Timber.e(e, "BasicHttpSyncer.sync: IOException");
            throw new RuntimeException(e);
        }
    }
    public Response reqPUT( String param,String method,String token) throws UnknownHttpResponseException {
        try {
            // connection headers
            String url = ANKI_CHINA_BASE +API_VERSION  + method ;
            Timber.d("final url: %s",
                    url);
            RequestBody body = RequestBody.create(JSON, param);
            Request.Builder builder=new Request.Builder().url(url).put(body) .addHeader("User-Agent", "AnkiDroid-" + VersionUtils.getPkgVersionNameFake())  .addHeader("x-device-id",   Utils.UUID ).addHeader("Accept",   "application/json" ).addHeader("Content-Type",   "application/json" );
            if(token!=null){
                builder  .addHeader("Authorization",   "Bearer "+token );
            }
            Request httpRequest= builder.build();

            try {
                OkHttpClient httpClient =new OkHttpClient();
                return httpClient.newCall(httpRequest).execute();
            } catch (SSLException e) {
                Timber.e(e, "SSLException while building HttpClient");
                throw new RuntimeException("SSLException while building HttpClient");
            }
        } catch (UnsupportedEncodingException  e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Timber.e(e, "BasicHttpSyncer.sync: IOException");
            throw new RuntimeException(e);
        }
    }

    private HttpUrl parseUrl(String url) {
        // #5843 - show better exception if the URL is invalid
        try {
            return HttpUrl.get(url);
        } catch (IllegalArgumentException ex) {
            if (isUsingCustomSyncServer(AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance()))) {
                throw new CustomSyncServerUrlException(url, ex);
            } else {
                throw ex;
            }
        }
    }



    // Could be replaced by Compat copy method if that method took listener for bytesReceived/publishProgress()
    public void writeToFile(InputStream source, String destination) throws IOException {
        File file = new File(destination);
        OutputStream output = null;
        try {
            file.createNewFile();
            output = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buf = new byte[Utils.CHUNK_SIZE];
            int len;
            long bytesReadBandwidth = 0L;
            long lastTime = System.currentTimeMillis();
            while ((len = source.read(buf)) >= 0) {
                output.write(buf, 0, len);
                bytesReceived += len;
                bytesReadBandwidth += len;
                if (bytesReadBandwidth >= Consts.DOWNLOAD_LIMIT_BANDWIDTH_BYTE&&Consts.loginAnkiChina()) {
                    // 检查下载到限制带宽的时间，这个时间小于1秒，这1秒剩下的时间就不读数据了，干耗着
                    long offsetTime = System.currentTimeMillis() - lastTime;
                    if (offsetTime < 1000L) {
                        try {
                            if (offsetTime < 0) offsetTime = 0;
                            Thread.sleep(1000L - offsetTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    lastTime = System.currentTimeMillis();
                    bytesReadBandwidth = 0;
                }
                publishProgress();
            }
        } catch (IOException e) {
            if (file.exists()) {
                // Don't keep the file if something went wrong. It'll be corrupt.
                file.delete();
            }
            // Re-throw so we know what the error was.
            throw e;
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }


    public String stream2String(InputStream stream, int maxSize) {
        BufferedReader rd;
        try {
            rd = new BufferedReader(new InputStreamReader(stream, "UTF-8"), maxSize == -1 ? 4096 : Math.min(4096,
                    maxSize));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = rd.readLine()) != null && (maxSize == -1 || sb.length() < maxSize)) {
                sb.append(line);
                bytesReceived += line.length();
                publishProgress();
            }
            rd.close();
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void publishProgress() {
//        Timber.d("Publishing progress");
        if (mCon != null && (mNextSendR <= bytesReceived || mNextSendS <= bytesSent)) {
            long bR = bytesReceived;
            long bS = bytesSent;
//            Timber.d("Current progress: %d, %d", bytesReceived, bytesSent);
            mNextSendR = (bR / 1024 + 1) * 1024;
            mNextSendS = (bS / 1024 + 1) * 1024;
            mCon.publishProgress(0, bS, bR);
        }
    }


    public Response hostKey(String arg1, String arg2) throws UnknownHttpResponseException {
        return null;
    }
    public Response sendCommonPost(String method,String body,String token) throws UnknownHttpResponseException {
        return null;
    }
    public Response sendCommonGet(String method,String param,String token) throws UnknownHttpResponseException {
        return null;
    }
    public Response sendCommonPUT(String method,String param,String token) throws UnknownHttpResponseException {
        return null;
    }

    public JSONObject applyChanges(JSONObject kw) throws UnknownHttpResponseException {
        return null;
    }


    public JSONObject start(JSONObject kw) throws UnknownHttpResponseException {
        return null;
    }


    public JSONObject chunk() throws UnknownHttpResponseException {
        return null;
    }


    public long finish() throws UnknownHttpResponseException {
        return 0;
    }


    public void abort() throws UnknownHttpResponseException {
        // do nothing
    }


    public Response meta() throws UnknownHttpResponseException {
        return null;
    }


    public Object[] download() throws UnknownHttpResponseException {
        return null;
    }


    public Object[] upload(long restSpace) throws UnknownHttpResponseException, NoEnoughServerSpaceException {
        return null;
    }


    public JSONObject sanityCheck2(JSONObject client) throws UnknownHttpResponseException {
        return null;
    }


    public void applyChunk(JSONObject sech) throws UnknownHttpResponseException {
        // do nothing
    }


    public class ProgressByteEntity extends AbstractHttpEntity {

        private InputStream mInputStream;
        private long mLength;


        public ProgressByteEntity(File file) {
            super();
            mLength = file.length();
            try {
                mInputStream = new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }


        @Override
        public void writeTo(OutputStream outstream) throws IOException {
            try {
                byte[] tmp = new byte[4096];
                int len;
                while ((len = mInputStream.read(tmp)) != -1) {
                    outstream.write(tmp, 0, len);
                    bytesSent += len;
                    publishProgress();
                }
                outstream.flush();
            } finally {
                mInputStream.close();
            }
        }


        @Override
        public InputStream getContent() throws IllegalStateException {
            return mInputStream;
        }


        @Override
        public long getContentLength() {
            return mLength;
        }


        @Override
        public boolean isRepeatable() {
            return false;
        }


        @Override
        public boolean isStreaming() {
            return false;
        }
    }


    public static ByteArrayInputStream getInputStream(String string) {
        try {
            return new ByteArrayInputStream(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Timber.e(e, "HttpSyncer: error on getting bytes from string");
            return null;
        }
    }


    public String syncURL() {
        // Allow user to specify custom sync server
        SharedPreferences userPreferences = AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance());
        if (isUsingCustomSyncServer(userPreferences)) {
            String syncBaseString = CustomSyncServer.getSyncBaseUrl(userPreferences);
            if (syncBaseString == null) {
                return getDefaultAnkiWebUrl();
            }
            return Uri.parse(syncBaseString).buildUpon().appendPath(getUrlPrefix()).toString() + "/";
        }
        // Usual case
        return getDefaultAnkiWebUrl();
    }

    protected String getUrlPrefix() {
        return "sync";
    }

    protected Integer getHostNum() {
        return mHostNum.getHostNum();
    }

    protected boolean isUsingCustomSyncServer(@Nullable SharedPreferences userPreferences) {
        return userPreferences != null && CustomSyncServer.isEnabled(userPreferences);
    }

    protected String getDefaultAnkiWebUrl() {
        String hostNumAsStringFormat = "";
        Integer hostNum = getHostNum();
        if (hostNum != null) {
            hostNumAsStringFormat = hostNum.toString();
        }
        Timber.d("now is using server:%s", Consts.LOGIN_SERVER);
        return Consts.loginAnkiWeb()?String.format(Consts.SYNC_BASE, hostNumAsStringFormat) + getUrlPrefix() + "/":Consts.SYNC_BASE_CHINA + getUrlPrefix() + "/";
    }
}


