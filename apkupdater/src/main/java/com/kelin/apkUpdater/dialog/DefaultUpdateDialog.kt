package com.kelin.apkUpdater.dialog

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
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
open class DefaultUpdateDialog(protected val updater: ApkUpdater, @StyleRes private val style: Int = R.style.KelinApkUpdaterUpdateDialog) : DialogFragment(), ApkUpdateDialog {

    private var mIsForceUpdate = false
    private var mIsDismissed = false
    private var mVersionName: CharSequence? = null
    private var mMessageTitle: CharSequence? = null
    private var mMessage: CharSequence? = null
    private var mHasNetworkErrorStatus = false

    init {
        isCancelable = false
    }

    @get: LayoutRes
    protected open val contentLayoutRes: Int
        get() = R.layout.dialog_kelin_apk_updater_def_update

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(contentLayoutRes, container)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onInitView(mIsForceUpdate, mVersionName, mMessageTitle, mMessage)
    }

    override fun getTheme(): Int {
        return style
    }

    @CallSuper
    override fun dismiss() {
        super.dismiss()
        mIsDismissed = true
    }

    @CallSuper
    override fun show(activity: Activity, version: String?, messageTitle: CharSequence?, message: CharSequence?, isForce: Boolean) {
        mIsDismissed = false
        mIsForceUpdate = isForce
        mVersionName = version
        this.mMessageTitle = messageTitle
        this.mMessage = message
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity.isDestroyed
        } else {
            false
        }.also { destroyed ->
            if (!destroyed) {
                if (activity is FragmentActivity) {
                    onShow(activity)
                } else {
                    throw IllegalStateException("Only support Androidx!")
                }
            }
        }
    }

    @CallSuper
    override fun onNetworkError() {
        mHasNetworkErrorStatus = true
        onShowNetworkError()
    }

    final override fun onProgress(total: Long, current: Long, percentage: Int) {
        if (!mIsDismissed) {
            if (mHasNetworkErrorStatus) {
                mHasNetworkErrorStatus = false
                onShowNetWorkAvailable()
            }
            onShowProgress(percentage)
        }
    }

    protected open fun onInitView(isForceUpdate: Boolean, versionName: CharSequence?, messageTitle: CharSequence?, message: CharSequence?) {
        ivKelinApkUpdaterUpdateDialogDismiss.visibility = if (isForceUpdate) View.GONE else View.VISIBLE
        tvKelinApkUpdaterVersion.text = versionName
        tvKelinApkUpdaterTitle.text = messageTitle
        tvKelinApkUpdaterUpdateContent.text = message

        tvKelinApkUpdaterSure.apply {
            text = "立即更新"
            setOnClickListener {
                setOnClickListener(null)
                if (!mIsForceUpdate) {
                    Toast.makeText(context, "正在下载，请稍后……", Toast.LENGTH_SHORT).show()
                    dismiss()
                    onUpdateButtonClick()
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
        pbKelinApkUpdaterProgress.progress = 0  //TODO 测试这里不用post有没有问题
    }

    protected open fun onUpdateButtonClick() {

    }

    protected open fun onShow(activity: FragmentActivity) {
        show(activity.supportFragmentManager, javaClass.name)
    }

    protected open fun onShowNetworkError() {
        tvKelinApkUpdaterSure?.text = "网络已断开，等待恢复..."
    }

    protected open fun onShowNetWorkAvailable() {
        tvKelinApkUpdaterSure.text = "正在下载..."
    }

    protected open fun onShowProgress(percentage: Int) {
        pbKelinApkUpdaterProgress.progress = percentage
    }
}