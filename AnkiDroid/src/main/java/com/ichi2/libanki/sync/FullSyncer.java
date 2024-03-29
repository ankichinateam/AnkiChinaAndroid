/***************************************************************************************
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

package com.ichi2.libanki.sync;

import android.database.sqlite.SQLiteDatabaseCorruptException;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.anki.R;
import com.ichi2.anki.exception.NoEnoughServerSpaceException;
import com.ichi2.anki.exception.UnknownHttpResponseException;
import com.ichi2.async.Connection;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.DB;
import com.ichi2.libanki.Utils;
import com.ichi2.utils.FileUtil;
import com.ichi2.utils.VersionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

@SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes","PMD.NPathComplexity"})
public class FullSyncer extends HttpSyncer {

    private Collection mCol;
    private Connection mCon;


    public FullSyncer(Collection col, String hkey, Connection con, HostNum hostNum) {
        super(hkey, con, hostNum);
        mPostVars = new HashMap<>();
        mPostVars.put("k", hkey);
        mPostVars.put("v",
                String.format(Locale.US, "anki,%s,%s", VersionUtils.getPkgVersionNameFake(), Utils.platDesc()));
        mCol = col;
        mCon = con;
    }

    @Override
    public Object[] download() throws UnknownHttpResponseException {
        InputStream cont;
        ResponseBody body = null;
        try {
            Response ret = super.req("download");
            if (ret == null || ret.body() == null) {
                return null;
            }
            body = ret.body();
            cont = body.byteStream();
        } catch (IllegalArgumentException e1) {
            if (body != null) {
                body.close();
            }
            throw new RuntimeException(e1);
        }
        String path;
        if (mCol != null) {
            Timber.i("Closing collection for full sync");
            // Usual case where collection is non-null
            path = mCol.getPath();
            mCol.close();
            mCol = null;
        } else {
            // Allow for case where collection is completely unreadable
            Timber.w("Collection was unexpectedly null when doing full sync download");
            path = CollectionHelper.getCollectionPath(AnkiDroidApp.getInstance());
        }
        String tpath = path + ".tmp";
        try {
            super.writeToFile(cont, tpath);
            Timber.d("Full Sync - Downloaded temp file");
            FileInputStream fis = new FileInputStream(tpath);
            if ("upgradeRequired".equals(super.stream2String(fis, 15))) {
                Timber.w("Full Sync - 'Upgrade Required' message received");
                return new Object[]{"upgradeRequired"};
            }
        } catch (FileNotFoundException e) {
            Timber.e(e, "Failed to create temp file when downloading collection.");
            throw new RuntimeException(e);
        } catch (IOException e) {
            Timber.e(e, "Full sync failed to download collection.");
            return new Object[] { "sdAccessError" };
        } finally {
            body.close();
        }

        // check the received file is ok
        mCon.publishProgress(R.string.sync_check_download_file);
        DB tempDb = null;
        try {
            tempDb = new DB(tpath);
            if (!"ok".equalsIgnoreCase(tempDb.queryString("PRAGMA integrity_check"))) {
                Timber.e("Full sync - downloaded file corrupt");
                return new Object[] { "remoteDbError" };
            }
        } catch (SQLiteDatabaseCorruptException e) {
            Timber.e("Full sync - downloaded file corrupt");
            return new Object[] { "remoteDbError" };
        } finally {
            if (tempDb != null) {
                tempDb.close();
            }
        }
        Timber.d("Full Sync: Downloaded file was not corrupt");
        // overwrite existing collection
        File newFile = new File(tpath);
        if (newFile.renameTo(new File(path))) {
            Timber.i("Full Sync Success: Overwritten collection with downloaded file");
            return new Object[] { "success" };
        } else {
            Timber.w("Full Sync: Error overwriting collection with downloaded file");
            return new Object[] { "overwriteError" };
        }
    }


    @Override
    public Object[] upload(long restSpace) throws UnknownHttpResponseException, NoEnoughServerSpaceException {
        // make sure it's ok before we try to upload
        mCon.publishProgress(R.string.sync_check_upload_file);
        if (!"ok".equalsIgnoreCase(mCol.getDb().queryString("PRAGMA integrity_check"))) {
            return new Object[] { "dbError" };
        }
        if (!mCol.basicCheck()) {
            return new Object[] { "dbError" };
        }
        // apply some adjustments, then upload
        mCol.beforeUpload();
        String filePath = mCol.getPath();
        double totalSize = FileUtil.getFileOrFilesSize(filePath, FileUtil.SIZETYPE_B);
        Timber.i("full sync path size:%f", totalSize);
        if (restSpace < totalSize&& Consts.loginAnkiChina()) {
            throw new NoEnoughServerSpaceException(restSpace, (long) totalSize);
        }
        Response ret;
        mCon.publishProgress(R.string.sync_uploading_message);
        try {
            ret = super.req("upload", new FileInputStream(filePath));
            if (ret == null || ret.body() == null) {
                return null;
            }
            int status = ret.code();
            if (status != 200) {
                // error occurred
                return new Object[] { "error", status, ret.message() };
            } else {
                return new Object[] { ret.body().string() };
            }
        } catch (IllegalStateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
