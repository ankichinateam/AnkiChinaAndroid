/*
 *  Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.utils;

import android.os.Build;

import com.ichi2.anki.BuildConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class OKHttpUtil {

    public interface MyCallBack {

        void onFailure(Call call, IOException e);

        void onResponse(Call call, String token, Object arg1, Response response) throws IOException;
    }


    public static void get(String url, String token, Object arg1, MyCallBack callback) {
        Timber.i("start fetch url:%s，has token ：%s", url, token);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30,TimeUnit.SECONDS)
                .callTimeout(30,TimeUnit.SECONDS).build();
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("x-app-version", BuildConfig.VERSION_NAME)
                .addHeader("x-os", "android " + Build.VERSION.SDK_INT)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(call, e);
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {

//                Timber.i("http get result:%s", response.body().string());
                callback.onResponse(call, token, arg1, response);
            }
        });
    }


    public static void put(String url, String token, Object arg1, MyCallBack callback) {
        Timber.i("start fetch url（put):%s，has token ：%s", url, token);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30,TimeUnit.SECONDS)
                .callTimeout(30,TimeUnit.SECONDS).build();
        final Request request = new Request.Builder()
                .url(url)
                .put(new FormBody.Builder().build())
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("x-app-version", BuildConfig.VERSION_NAME)
                .addHeader("x-os", "android " + Build.VERSION.SDK_INT)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(call, e);
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {

//                Timber.i("http get result:%s", response.body().string());
                callback.onResponse(call, token, arg1, response);
            }
        });
    }


    public static void post(String url, RequestBody body, String token, Object arg1, MyCallBack callback) {
        Timber.i("start fetch url（post):%s，has token ：%s", url, token);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30,TimeUnit.SECONDS)
                .callTimeout(30,TimeUnit.SECONDS).build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("x-app-version", BuildConfig.VERSION_NAME)
                .addHeader("x-os", "android " + Build.VERSION.SDK_INT)
                .post(body)
                .build();

        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    callback.onFailure(call, e);
                }
            }


            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (callback != null) {
                    callback.onResponse(call, token, arg1, response);
                }
            }

        });
    }


}
