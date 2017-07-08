package com.chengshi.apkUpdater.callback;

/**
 * 描述 用来监听 {@link com.chengshi.apkUpdater.dialog.DefaultDialog} 的事件。
 * 创建人 kelin
 * 创建时间 2016/10/12  上午11:07
 * 包名 com.chengshi.downloader.dialog
 */

public abstract class DialogListener {

    /**
     * 当用户点击了取消按钮,或通过其他方式销毁了{@link com.chengshi.apkUpdater.dialog.DefaultDialog}后回调的方法。
     *
     * @param isSure 是否是通过点击确认按钮后销毁的。<code color="blue">true</code>表示是,
     *                 <code color="blue">false</code>则表示不是。
     */
    public abstract void onDialogDismiss(boolean isSure);
}
