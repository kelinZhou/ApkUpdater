package com.kelin.apkUpdater.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.kelin.apkUpdater.ActivityStackManager.stackTopActivity
import com.kelin.apkUpdater.R
import java.util.*

/**
 * 描述 更新信息对话框。
 * 创建人 kelin
 * 创建时间 2017/7/5  下午3:59
 * 版本 v 1.0.0
 */
class DefaultDialog {

    private var mDialog: AlertDialog? = null
    private var mNetWorkUnusableDialog: AlertDialog? = null
    private var mWiFiUnusableDialog: AlertDialog? = null
    private var mMD5FailedDialog: AlertDialog? = null
    private var mProgressBar: ProgressBar? = null
    private var mPercentageView: TextView? = null
    private val mOnClickListener by lazy { DialogClickListener() }
    private var mConfig: DialogParams? = null
    //为了让进度条走完才销毁。
    private val mAction = Runnable {
        dismiss(mDialog)
        mDialog = null
    }

    /**
     * 显示对话框。
     */
    fun show(config: DialogParams) {
        show(config, null)
    }

    /**
     * 显示对话框。
     *
     * @param listener 对话框的监听。
     */
    @SuppressLint("InflateParams")
    fun show(config: DialogParams, listener: DialogListener?) {
        val curActivity = stackTopActivity
        if (curActivity != null) {
            dismiss(mWiFiUnusableDialog)
            mWiFiUnusableDialog = null
            dismiss(mNetWorkUnusableDialog)
            mNetWorkUnusableDialog = null
            if (mDialog == null || config !== mConfig) {
                mConfig = config
                //构建AlertDialog。
                val builder = AlertDialog.Builder(curActivity, DialogParams.style)
                builder.setCancelable(false)
                if (config == DialogParams.downloadDialogParams) {
                    val contentView = LayoutInflater.from(curActivity).inflate(R.layout.com_kelin_apkupdater_layout_progress_layout, null)
                    mProgressBar = contentView.findViewById<View>(R.id.progress) as ProgressBar
                    val drawableRes = if (ContextCompat.getColor(mProgressBar!!.context, R.color.colorPrimary) == Color.WHITE) {
                        R.drawable.com_kelin_apkupdater_shape_progressbar_mini_default
                    } else {
                        R.drawable.com_kelin_apkupdater_shape_progressbar_mini
                    }
                    mProgressBar!!.progressDrawable = ContextCompat.getDrawable(mProgressBar!!.context, drawableRes)
                    mPercentageView = contentView.findViewById<View>(R.id.tv_percentage) as TextView
                    builder.setView(contentView)
                    if (!config.isForceUpdate) {
                        builder.setPositiveButton("悄悄的下载") { dialog, which ->
                            dismiss(mDialog)
                            mDialog = null
                            listener!!.onDialogDismiss(false)
                        }
                    }
                } else {
                    mOnClickListener.setListener(listener)
                    builder.setPositiveButton("立刻安装", mOnClickListener) //设置确定按钮
                    if (!config.isForceUpdate) {
                        builder.setNegativeButton("稍候安装", mOnClickListener) //如果不是强制更新则设置取消按钮
                    }
                }
                builder.setIcon(config.icon) //设置图标
                        .setTitle(config.title) //设置标题
                        .setMessage(config.message) //设置内容
                mDialog = builder.create()
            }
            mDialog!!.show()
        } else {
            NullPointerException("the curActivity is Null!").printStackTrace()
        }
    }

    fun updateDownLoadsProgress(percentage: Int) {
        mProgressBar!!.progress = percentage
        mPercentageView!!.text = String.format(Locale.CHINA, "%d %%", percentage)
        if (percentage == mProgressBar!!.max) {
            mProgressBar!!.post(mAction)
        }
    }

    fun showWiFiUnusableDialog(listener: DialogListener?) {
        val curActivity = stackTopActivity
        if (curActivity != null) {
            dismiss(mNetWorkUnusableDialog)
            mNetWorkUnusableDialog = null
            dismiss(mDialog)
            mDialog = null
            mOnClickListener.setListener(listener)
            if (mWiFiUnusableDialog == null) {
                mWiFiUnusableDialog = AlertDialog.Builder(curActivity, DialogParams.style)
                        .setCancelable(false)
                        .setTitle("提示：")
                        .setMessage("当前为非WiFi网络，是否继续下载？")
                        .setPositiveButton("继续下载", mOnClickListener)
                        .setNegativeButton("稍后下载", mOnClickListener)
                        .create()
            }
            mWiFiUnusableDialog!!.show()
        }
    }

    fun dismissAll() {
        dismiss(mDialog)
        mDialog = null
        dismiss(mNetWorkUnusableDialog)
        mNetWorkUnusableDialog = null
        dismiss(mWiFiUnusableDialog)
        mWiFiUnusableDialog = null
    }

    private fun dismiss(dialog: AlertDialog?) {
        if (dialog != null && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun showNetWorkUnusableDialog(listener: DialogListener?) {
        dismiss(mWiFiUnusableDialog)
        mWiFiUnusableDialog = null
        dismiss(mDialog)
        mDialog = null
        mOnClickListener.setListener(listener)
        stackTopActivity.also {
            if (it != null) {
                if (mNetWorkUnusableDialog == null) {
                    mNetWorkUnusableDialog = AlertDialog.Builder(it, DialogParams.style)
                            .setCancelable(false)
                            .setTitle("提示：")
                            .setMessage("网络连接已经断开，请稍后再试。")
                            .setNegativeButton("确定", mOnClickListener)
                            .create()
                }
                mNetWorkUnusableDialog!!.show()
            }
        }
    }

    fun showCheckMD5FailedDialog(listener: DialogListener?) {
        dismiss(mDialog)
        mDialog = null
        mOnClickListener.setListener(listener)
        stackTopActivity.also {
            if (it != null) {
                if (mMD5FailedDialog == null) {
                    mMD5FailedDialog = AlertDialog.Builder(it, DialogParams.style)
                            .setCancelable(false)
                            .setTitle("提示：")
                            .setMessage("下载失败，请尝试切换您的网络环境后再试~")
                            .setNegativeButton("确定") { dialog, which ->
                                mMD5FailedDialog!!.dismiss()
                                mMD5FailedDialog = null
                                mOnClickListener.onClick(dialog, which)
                            }
                            .create()
                }
                mMD5FailedDialog!!.show()
            }
        }
    }

    private inner class DialogClickListener : DialogInterface.OnClickListener {
        private var mListener: DialogListener? = null
        fun setListener(listener: DialogListener?) {
            mListener = listener
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            mListener?.onDialogDismiss(which == DialogInterface.BUTTON_POSITIVE)
        }
    }

    interface DialogListener {
        /**
         * 当用户点击了取消按钮,或通过其他方式销毁了[com.kelin.apkUpdater.dialog.DefaultDialog]后回调的方法。
         *
         * @param isSure 是否是通过点击确认按钮后销毁的。`true`表示是,
         * `false`则表示不是。
         */
        fun onDialogDismiss(isSure: Boolean)
    }
}