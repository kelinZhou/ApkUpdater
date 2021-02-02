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
import com.kelin.apkUpdater.UpdateType
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

    private var mUpdateType = UpdateType.UPDATE_WEAK
    private var mIsDismissed = false
    private var mVersionName: CharSequence? = null
    private var mUpdateTitle: CharSequence? = null
    private var mUpdateContent: CharSequence? = null
    private var mIsAutoCheck = false
    private var mHasNetworkErrorStatus = false

    init {
        isCancelable = false
    }

    protected val isForce: Boolean
        get() = mUpdateType == UpdateType.UPDATE_FORCE

    protected val isWeak: Boolean
        get() = mUpdateType == UpdateType.UPDATE_WEAK

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
        onInitView(mVersionName, mUpdateTitle, mUpdateContent, mIsAutoCheck)
    }

    override fun getTheme(): Int {
        return style
    }

    @CallSuper
    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    override fun dismissAllowingStateLoss() {
        super.dismissAllowingStateLoss()
        mIsDismissed = true
    }

    @CallSuper
    override fun show(activity: Activity, version: String?, updateTitle: CharSequence?, updateContent: CharSequence?, updateType: UpdateType, isAutoCheck: Boolean) {
        mIsDismissed = false
        mUpdateType = updateType
        mVersionName = version
        mUpdateTitle = updateTitle
        mUpdateContent = updateContent
        mIsAutoCheck = isAutoCheck
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity.isDestroyed
        } else {
            false
        }.also { destroyed ->
            if (!destroyed) {
                if (activity is FragmentActivity) {
                    onShow(activity)
                } else {
                    throw IllegalStateException("Only support FragmentActivity!")
                }
            }
        }
    }

    @CallSuper
    override fun onNetworkError() {
        if (!mIsDismissed) {
            mHasNetworkErrorStatus = true
            onShowNetworkStatusChanged(false)
        }
    }

    final override fun onProgress(total: Long, current: Long, percentage: Int) {
        if (!mIsDismissed) {
            if (mHasNetworkErrorStatus) {
                mHasNetworkErrorStatus = false
                onShowNetworkStatusChanged(true)
            }
            onShowProgress(percentage)
        }
    }

    protected open fun onInitView(versionName: CharSequence?, updateTitle: CharSequence?, updateContent: CharSequence?, isAutoCheck: Boolean) {
        tvKelinApkUpdaterSkipThisVersion.apply {
            visibility = if (isWeak && isAutoCheck) View.VISIBLE else View.GONE
            setOnClickListener {
                dismiss()
                updater.skipThisVersion()
            }
        }
        ivKelinApkUpdaterUpdateDialogDismiss.apply {
            visibility = if (isForce) View.INVISIBLE else View.VISIBLE
            setOnClickListener {
                dismiss()
                updater.setCheckHandlerResult(false)
            }
        }
        tvKelinApkUpdaterVersion.text = versionName
        tvKelinApkUpdaterTitle.text = updateTitle
        tvKelinApkUpdaterUpdateContent.text = updateContent

        tvKelinApkUpdaterSure.apply {
            text = if (isForce) "立即更新" else "后台更新"
            setOnClickListener {
                setOnClickListener(null)
                if (!isForce) {
                    Toast.makeText(context, "正在后台下载，请稍后……", Toast.LENGTH_SHORT).show()
                    dismiss()
                    onUpgradingInTheBackground()
                } else {
                    text = "正在下载..."
                }
                updater.setCheckHandlerResult(true)
            }
        }

        pbKelinApkUpdaterProgress.progress = 0
    }

    /**
     * 当后台更新时调用。
     */
    protected open fun onUpgradingInTheBackground() {

    }

    protected open fun onShow(activity: FragmentActivity) {
        show(activity.supportFragmentManager, javaClass.name)
    }

    protected open fun onShowNetworkStatusChanged(available: Boolean) {
        tvKelinApkUpdaterSure?.text = if (available) "正在下载..." else "网络已断开，等待恢复..."
    }

    protected open fun onShowProgress(percentage: Int) {
        pbKelinApkUpdaterProgress.progress = percentage
    }
}