package com.chengshi.apkUpdater.dialog;

import android.text.TextUtils;

import com.chengshi.apkUpdater.R;

/**
 * 描述 检查更新时对话框的配置信息。
 * 创建人 kelin
 * 创建时间 2017/7/5  下午4:51
 * 版本 v 1.0.0
 */

public class InformDialogConfig extends DialogConfig {

    /**
     * 获取对话框的图标。
     *
     * @return 返回对话框图标的资源ID。
     */
    @Override
    public int getIcon() {
        return icon == 0 ? R.drawable.com_cheng_shi_ic_cloud_download_green : icon;
    }

    /**
     * 获取对话框的标题。
     *
     * @return 返回你要设置的标题。
     */
    @Override
    public CharSequence getTitle() {
        return TextUtils.isEmpty(title) ? "检测到新的版本" : title;
    }

    /**
     * 获取对话框的内容。
     *
     * @return 返回要显示的内容。
     */
    @Override
    public CharSequence getMessage() {
        return TextUtils.isEmpty(msg) ? "是否现在更新？" : msg;
    }
}
