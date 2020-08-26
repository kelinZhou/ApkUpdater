package com.kelin.apkUpdater.dialog

import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import android.text.TextUtils
import com.kelin.apkUpdater.R

/**
 * 描述 对话框的配置信息
 * 创建人 kelin
 * 创建时间 2017/7/5  下午4:35
 * 版本 v 1.0.0
 */
abstract class DialogParams(@DrawableRes val icon: Int = 0, var title: CharSequence? = null, var message: CharSequence? = null) {
    /**
     * 是否强制更新。
     * @return true表示强制更新，false则不是。
     */
    var isForceUpdate = false

    companion object {
        @StyleRes
        var style = 0

        val informDialogParams by lazy { object : DialogParams(R.drawable.com_kelin_apkupdater_ic_cloud_download_green, "检测到新的版本", "是否现在更新？") {} }

        val downloadDialogParams by lazy { object : DialogParams(R.drawable.com_kelin_apkupdater_ic_download_green, "下载更新", "拼命下载中，请您耐心等候……") {} }
    }
}