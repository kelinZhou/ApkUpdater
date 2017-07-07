package com.chengshi.apkUpdater.callback;

/**
 * 描述 服务被解绑的监听。
 * 创建人 kelin
 * 创建时间 2017/3/15  下午6:00
 * 版本 v 1.0.0
 */

public interface ServiceUnBindListener {

    /**
     * 当服务被解绑的时候回调。
     */
    void onUnBind();
}
