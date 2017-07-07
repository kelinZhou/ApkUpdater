package com.chengshi.kelin.updatemanagerdemo.net;

import com.chengshi.kelin.updatemanagerdemo.UpdateModel;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/**
 * 描述 获取apk更新信息的接口。
 * 创建人 kelin
 * 创建时间 2017/3/14  上午11:13
 * 版本 v 1.0.0
 */

interface NetApi {

    @GET("{path}")
    Observable<UpdateModel> getApkUpdateInfo(@Path("path") String path);
}
