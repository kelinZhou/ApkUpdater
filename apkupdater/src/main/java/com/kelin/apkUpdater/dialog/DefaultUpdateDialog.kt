package com.kelin.apkUpdater.dialog

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.kelin.apkUpdater.ApkUpdater
import com.kelin.apkUpdater.UpdateType
import com.kelin.apkupdater.R

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

    override val isDismissed: Boolean
        get() = mIsDismissed

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

    protected open val contentLayoutRes: Int
        get() = R.layout.dialog_kelin_apk_updater_def_update

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(contentLayoutRes, container)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onInitView(view, mVersionName, mUpdateTitle, mUpdateContent, mIsAutoCheck)
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
        if (!activity.isDestroyed) {
            if (activity is FragmentActivity) {
                onShow(activity)
            } else {
                throw IllegalStateException("Only support FragmentActivity!")
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

    protected open fun onInitView(rootView: View, versionName: CharSequence?, updateTitle: CharSequence?, updateContent: CharSequence?, isAutoCheck: Boolean) {
        rootView.findViewById<View>(R.id.tvKelinApkUpdaterSkipThisVersion).apply {
            visibility = if (isWeak && isAutoCheck) View.VISIBLE else View.GONE
            setOnClickListener {
                dismiss()
                updater.skipThisVersion()
            }
        }
        rootView.findViewById<View>(R.id.ivKelinApkUpdaterUpdateDialogDismiss).apply {
            visibility = if (isForce) View.INVISIBLE else View.VISIBLE
            setOnClickListener {
                dismiss()
                updater.setCheckHandlerResult(false)
            }
        }
        rootView.findViewById<TextView>(R.id.tvKelinApkUpdaterVersion).text = versionName
        rootView.findViewById<TextView>(R.id.tvKelinApkUpdaterTitle).text = updateTitle
        rootView.findViewById<TextView>(R.id.tvKelinApkUpdaterUpdateContent).text = updateContent

        rootView.findViewById<TextView>(R.id.tvKelinApkUpdaterSure).apply {
            text = if (isForce) getString(R.string.kelin_apk_updater_update_now) else getString(R.string.kelin_apk_updater_update_in_background)
            setOnClickListener {
                setOnClickListener(null)
                if (!isForce) {
                    Toast.makeText(context, getString(R.string.kelin_apk_updater_update_in_background_tip), Toast.LENGTH_SHORT).show()
                    dismiss()
                    onUpgradingInTheBackground()
                } else {
                    text = getString(R.string.kelin_apk_updater_downloading)
                }
                updater.setCheckHandlerResult(true)
            }
        }

        rootView.findViewById<ProgressBar>(R.id.pbKelinApkUpdaterProgress).progress = 0
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
        view?.findViewById<TextView>(R.id.tvKelinApkUpdaterSure)?.text = if (available) getString(R.string.kelin_apk_updater_downloading) else getString(R.string.kelin_apk_updater_network_error_waiting)
    }

    protected open fun onShowProgress(percentage: Int) {
        view?.findViewById<ProgressBar>(R.id.pbKelinApkUpdaterProgress)?.progress = percentage
    }
}