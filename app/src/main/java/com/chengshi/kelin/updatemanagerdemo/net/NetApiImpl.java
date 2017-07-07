package com.chengshi.kelin.updatemanagerdemo.net;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.chengshi.kelin.updatemanagerdemo.UpdateModel;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;

/**
 * 描述 获取APK更新接口的实现类。
 * 创建人 kelin
 * 创建时间 2017/3/14  上午11:18
 * 版本 v 1.0.0
 */

public class NetApiImpl implements NetApi {

    private final NetApi mNetApi;
    private final String mChildPath;
    private static final String PATHNAME_SEPARATOR = "/";

    public NetApiImpl(@NonNull String urlPath) {
        if (TextUtils.isEmpty(urlPath) || urlPath.matches("/+")) {
            throw new NullPointerException("urlPath must not empty or “/////......”!");
        }
        urlPath = replacePathnameSeparator(urlPath);
        int lastIndexOf = urlPath.lastIndexOf(PATHNAME_SEPARATOR) + 1;
        String baseUrl = urlPath.substring(0, lastIndexOf);
        mChildPath = urlPath.substring(lastIndexOf);
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(baseUrl)
                .build();
        mNetApi = retrofit.create(NetApi.class);
    }

    private String replacePathnameSeparator(String urlPath) {
        if (urlPath.endsWith(PATHNAME_SEPARATOR)) {
            return replacePathnameSeparator(urlPath.substring(0, urlPath.length() - 1));
        } else {
            return urlPath;
        }
    }

    public Observable<UpdateModel> getApkUpdateInfo() {
        return getApkUpdateInfo(mChildPath);
    }

    @Override
    public final Observable<UpdateModel> getApkUpdateInfo(String path) {
        return mNetApi.getApkUpdateInfo(path);
    }
}
