package com.kelin.apkUpdater.dialog

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.kelin.apkUpdater.ApkUpdater
import com.kelin.apkUpdater.R
import kotlinx.android.synthetic.main.dialog_kelin_apk_updater_def_update.*

/**
 * **描述:** 更新弹窗提示。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/26 3:17 PM
 *
 * **版本:** v 1.0.0
 */
class DefaultUpdateDialog(private val updater: ApkUpdater, @StyleRes private val style: Int = R.style.KelinApkUpdaterUpdateDialog) : DialogFragment(), ApkUpdateDialog {

    private var isForceUpdate = false
    private var isDismissed = false
    private var versionName: CharSequence? = null
    private var messageTitle: CharSequence? = null
    private var message: CharSequence? = null
    private var hasNetworkErrorStatus = false

    init {
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_kelin_apk_updater_def_update, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivKelinApkUpdaterUpdateDialogDismiss.visibility = if (isForceUpdate) View.GONE else View.VISIBLE
        tvKelinApkUpdaterVersion.text = versionName
        tvKelinApkUpdaterTitle.text = messageTitle
        tvKelinApkUpdaterUpdateContent.text = message

        tvKelinApkUpdaterSure.apply {
            text = "立即更新"
            setOnClickListener {
                setOnClickListener(null)
                if (!isForceUpdate) {
                    dismiss()
                } else {
                    text = "正在下载..."
                }
                updater.setCheckHandlerResult(true)
            }
        }

        ivKelinApkUpdaterUpdateDialogDismiss.setOnClickListener {
            dismiss()
            updater.setCheckHandlerResult(false)
        }
        view.post {
            pbKelinApkUpdaterProgress.progress = 0
        }
    }

    override fun getTheme(): Int {
        return style
    }

    override fun dismiss() {
        super.dismiss()
        isDismissed = true
    }

    override fun show(activity: Activity, version: String?, messageTitle: CharSequence?, message: CharSequence?, isForce: Boolean) {
        isDismissed = false
        isForceUpdate = isForce
        versionName = version
        this.messageTitle = messageTitle
        this.message = message
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity.isDestroyed
        } else {
            false
        }.also { destroyed ->
            if (!destroyed) {
                if (activity is FragmentActivity) {
                    show(activity.supportFragmentManager, javaClass.name)
                } else {
                    throw IllegalStateException("Only support Androidx!")
                }
            }
        }
    }

    override fun onNetworkError() {
        hasNetworkErrorStatus = true
        tvKelinApkUpdaterSure?.text = "网络已断开，等待恢复..."
    }

    override fun onProgress(total: Long, current: Long, percentage: Int) {
        if (!isDismissed) {
            if (hasNetworkErrorStatus) {
                hasNetworkErrorStatus = false
                tvKelinApkUpdaterSure.text = "正在下载..."
            }
            pbKelinApkUpdaterProgress.progress = percentage
        }
    }
}