package com.kelin.apkUpdater.dialog

import android.app.Activity
import com.kelin.apkUpdater.ApkUpdater
import com.kelin.apkUpdater.UpdateType

/**
 * **描述:** 安装包升级弹窗。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/26 5:04 PM
 *
 * **版本:** v 1.0.0
 */
interface ApkUpdateDialog {
    /**
     * 当需要显示检查更新提示对话框的时候调用。你需要在这里进行检查更新提示对话框的显示。
     * 在用户做出相应的操作后，你应当调用[ApkUpdater.setCheckHandlerResult]方法进行下一步的操作。
     *
     * @param activity 弹窗必须依赖于Activity。
     * @param version 当前可更新的版本名称。
     * @param updateTitle 更新标题。
     * @param updateContent 更新内容。
     * @param updateType 更新类型。
     * @param isAutoCheck 是否是自动更新。
     * @see ApkUpdater.setCheckHandlerResult
     */
    fun show(activity: Activity, version: String?, updateTitle: CharSequence?, updateContent: CharSequence?, updateType: UpdateType, isAutoCheck:Boolean)


    /**
     * 下载进度更新的时候调用。如果您是自定义的UI交互的话您需要覆盖此方法，并在此方法中做更新进度的操作。
     * 您还需要在进度完成后进行销毁dialog的操作。
     *
     * @param total      文件总大小(字节)。
     * @param current    当前的进度(字节)。
     * @param percentage 当前下载进度的百分比。
     */
    fun onProgress(total: Long, current: Long, percentage: Int)

    /**
     * 网络错误时调用，如果在下载的过程中出现了网络连接断开等错误，而你希望把该错误告知给用户，那么你就需要重写该方法，并做出相应的处理。
     */
    fun onNetworkError()

    /**
     * 结束弹窗。
     */
    fun dismiss()
}