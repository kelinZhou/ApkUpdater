package com.chengshi.apkUpdater.dialog;

import android.support.annotation.DrawableRes;
import android.text.TextUtils;

/**
 * 描述 对话框的配置信息
 * 创建人 kelin
 * 创建时间 2017/7/5  下午4:35
 * 版本 v 1.0.0
 */

abstract class DialogConfig {

    @DrawableRes
    protected int icon;
    protected CharSequence title;
    CharSequence msg;

    private boolean isForceUpdate;

    public void setForceUpdate(boolean forceUpdate) {
        isForceUpdate = forceUpdate;
    }

    /**
     * 是否强制更新。
     * @return true表示强制更新，false则不是。
     */
    boolean isForceUpdate() {
        return isForceUpdate;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setTitle(CharSequence title) {
        if (TextUtils.isEmpty(title)) {
            return;
        }
        this.title = title;
    }

    public void setMsg(CharSequence msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        this.msg = msg;
    }

    /**
     * 获取对话框的图标。
     * @return 返回对话框图标的资源ID。
     */
    @DrawableRes
    public abstract int getIcon();

    /**
     * 获取对话框的标题。
     * @return 返回你要设置的标题。
     */
    public abstract CharSequence getTitle();

    /**
     * 获取对话框的内容。
     * @return 返回要显示的内容。
     */
    public abstract CharSequence getMessage();
}
