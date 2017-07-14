package com.kelin.apkUpdater.dialog;

import android.text.TextUtils;

import com.kelin.apkUpdater.R;

/**
 * 描述 下载更新时对话框的配置信息。
 * 创建人 kelin
 * 创建时间 2017/7/5  下午5:23
 * 版本 v 1.0.0
 */

public class DownloadDialogParams extends DialogParams {

    /**
     * 获取对话框的图标。
     *
     * @return 返回对话框图标的资源ID。
     */
    @Override
    public int getIcon() {
        return icon == 0 ? R.drawable.com_cheng_shi_ic_download_green : icon;
    }

    /**
     * 获取对话框的标题。
     *
     * @return 返回你要设置的标题。
     */
    @Override
    public CharSequence getTitle() {
        return TextUtils.isEmpty(title) ? "下载更新" : title;
    }

    /**
     * 获取对话框的内容。
     *
     * @return 返回要显示的内容。
     */
    @Override
    public CharSequence getMessage() {
        return TextUtils.isEmpty(msg) ? "拼命下载中，请您耐心等候……" : msg;
    }
}
