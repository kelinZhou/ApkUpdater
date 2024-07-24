package com.kelin.apkUpdater

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.kelin.apkUpdater.DownloadService.DownloadBinder
import com.kelin.apkUpdater.UpdateHelper.clearDownloadFailedCount
import com.kelin.apkUpdater.UpdateHelper.downloadFailedCountPlus
import com.kelin.apkUpdater.UpdateHelper.getApkPathFromSp
import com.kelin.apkUpdater.UpdateHelper.getApkVersionCodeFromSp
import com.kelin.apkUpdater.UpdateHelper.getCurrentVersionCode
import com.kelin.apkUpdater.UpdateHelper.getCurrentVersionName
import com.kelin.apkUpdater.UpdateHelper.getDownloadFailedCount
import com.kelin.apkUpdater.UpdateHelper.getFileSignature
import com.kelin.apkUpdater.UpdateHelper.installApk
import com.kelin.apkUpdater.UpdateHelper.getUpdateType
import com.kelin.apkUpdater.UpdateHelper.putApkVersionCode2Sp
import com.kelin.apkUpdater.UpdateHelper.removeOldApk
import com.kelin.apkUpdater.callback.CompleteUpdateCallback
import com.kelin.apkUpdater.callback.DownloadProgressCallback
import com.kelin.apkUpdater.callback.IUpdateCallback
import com.kelin.apkUpdater.dialog.ApkUpdateDialog
import com.kelin.apkUpdater.dialog.DefaultUpdateDialog
import com.kelin.apkUpdater.util.NetWorkStateUtil
import com.kelin.apkUpdater.util.NetworkStateChangedListener
import com.kelin.apkupdater.R
import com.kelin.okpermission.OkPermission
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 描述 用来更新APK的核心类。
 * 创建人 kelin
 * 创建时间 2017/3/13  下午2:32
 * 版本 v 1.0.0
 */
class ApkUpdater private constructor(
    private val dialogGenerator: ((updater: ApkUpdater) -> ApkUpdateDialog)?,
    private var mCallback: IUpdateCallback?
) {

    companion object {
        private const val TAG = "ApkUpdater"
        internal var fileProvider: String = ""
        internal var currentAppVersion: Long = 0

        /**
         * 初始化，用于初始化ApkUpdater库，您需要在Application的onCreate方法中调用，否则在升级时有可能无法弹窗。
         * @param context 需要Context对象(Application的Context即可)。
         * @param currentVersion 当前App的版本号，如果不传则通过系统Api获取(可能不准确)，建议传入。
         * @param fileProvider 用于适配Android7.0的文件管理。
         */
        fun init(context: Context, currentVersion: Long?, fileProvider: String = context.applicationContext.packageName + ".fileProvider") {
            ActivityStackManager.initUpdater(context)
            NetWorkStateUtil.init(context, false)
            this.currentAppVersion = currentVersion ?: 0
            this.fileProvider = fileProvider
        }
    }

    private var isBindService = false
    private var mServiceIntent: Intent? = null
    private var mIsLoaded = false
    private var mUpdateInfo: UpdateInfo? = null
    private var mHaveNewVersion = false
    private var mIsChecked = false
    private var mAutoInstall = true
    private val mApplicationContext: Context = ActivityStackManager.applicationContext
    private var mIsAutoCheck = false

    private val connectChangedListener: NetworkStateChangedListener = { connected ->
        if (!isBindService) {
            if (connected) {
                startDownload()
            } else {
                onTipNetworkError()
            }
        }
    }

    private val dialog: ApkUpdateDialog by lazy {
        dialogGenerator?.invoke(this) ?: DefaultUpdateDialog(this)
    }

    private val enabledDialog: ApkUpdateDialog?
        get() = dialog.takeIf { !it.isDismissed }

    /**
     * 更新的进度监听。
     */
    private val mOnProgressListener by lazy { OnLoadProgressListener() }

    /**
     * 获取一个不为空的UpdateInfo，如果为空则抛出异常。
     */
    private val requireUpdateInfo: UpdateInfo
        get() = mUpdateInfo ?: throw NullPointerException("The UpdateInfo must not be null!")

    /**
     * 当前的版本名称。
     */
    private val localVersionName by lazy { getCurrentVersionName(mApplicationContext) }

    /**
     * 当前的版本号。
     */
    private val localVersionCode by lazy { getCurrentVersionCode(mApplicationContext) }

    /**
     * 判断当前版本是否是强制更新。
     */
    private val updateType: UpdateType
        get() = if (mUpdateInfo != null) getUpdateType(requireUpdateInfo, mApplicationContext) else UpdateType.UPDATE_WEAK

    //接口回调，下载进度
    private val serviceConnection: ServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                (service as? DownloadBinder)?.service?.apply {
                    setOnProgressListener(mOnProgressListener)
                    setServiceUnBindListener { isBindService = false }
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
    }

    /**
     * 移除监听。如果不希望再监听后续的回调，则可以调用该方法。
     */
    fun removeCallback() {
        mCallback = null
    }

    /**
     * 处理网络异常。
     */
    private fun onTipNetworkError() {
        Log.i(TAG, "网络错误，下载暂停。")
        enabledDialog.also {
            if (it == null) {
                if (updateType != UpdateType.UPDATE_FORCE) {
                    ActivityStackManager.stackTopActivity?.also { ctx -> Toast.makeText(ctx, ctx.getString(R.string.kelin_apk_updater_network_error_tip), Toast.LENGTH_SHORT).show() }
                }
            } else {
                it.onNetworkError()
            }
        }
    }

    private fun stopService() { //判断是否真的下载完成进行安装了，以及是否注册绑定过服务
        if (isBindService) {
            mApplicationContext.unbindService(serviceConnection)
            isBindService = false
        }
        mApplicationContext.stopService(mServiceIntent)
    }

    /**
     * 检查更新。如果当前
     *
     * @param updateInfo  更新信息对象。
     * @param autoCheck 是否是自动检测更新。
     * @param autoInstall 是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。
     */
    @JvmOverloads
    fun check(updateInfo: UpdateInfo, autoCheck: Boolean = true, autoInstall: Boolean = true): Boolean {
        return if (mUpdateInfo == null) {
            require(!(!autoInstall && mCallback == null)) { "Because you neither set up to monitor installed automatically, so the check update is pointless." }
            val haveNewVersion = updateInfo.versionCode.let {
                if (autoCheck) { //只有自动更新时才检测是否跳过版本
                    it != UpdateHelper.getSkippedVersion(mApplicationContext) && it > localVersionCode
                } else {
                    it > localVersionCode
                }
            }
            if (!haveNewVersion) {
                mCallback?.apply {
                    onSuccess(autoCheck, false, localVersionName, UpdateType.UPDATE_WEAK)
                    onCompleted()
                }
            } else {
                if (TextUtils.isEmpty(updateInfo.downLoadsUrl)) {
                    mCallback?.apply {
                        (this as? CompleteUpdateCallback)?.onDownloadFailed()
                        onFiled(autoCheck, isCanceled = false, haveNewVersion = true, curVersionName = localVersionName, checkMD5failedCount = 0, updateType = updateType)
                        onCompleted()
                    }
                } else {
                    mUpdateInfo = updateInfo
                    mIsAutoCheck = autoCheck
                    mHaveNewVersion = true
                    mAutoInstall = autoInstall
                    mIsChecked = true
                    //如果这个条件满足说明上一次没有安装。有因为即使上一次没有安装最新的版本也有可能超出了上一次下载的版本，所以要在这里判断。
                    val apkPath = getApkPathFromSp(mApplicationContext)
                    if (getApkVersionCodeFromSp(mApplicationContext) == updateInfo.versionCode && getApkPathFromSp(mApplicationContext).endsWith(".apk", true) && File(apkPath).exists()) {
                        mIsLoaded = true
                    } else {
                        removeOldApk(mApplicationContext)
                    }
                    onShowUpdateInformDialog()
                }
            }
            true
        } else {
            false
        }
    }

    private fun onShowUpdateInformDialog() {
        ActivityStackManager.watchStackTopActivity(true) { activity ->
            requireUpdateInfo.also {
                dialog.show(activity, it.versionName, it.updateMessageTitle, it.updateMessage, updateType, mIsAutoCheck)
            }
            true
        }
    }

    /**
     * 设置检查更新的对话框的操作结果。如果你没有关闭默认的对话框使用自定义对话框的话请不要手动调用该方法。
     *
     * @param isContinue 是否继续，如果继续则说明统一更新，否则就是不统一更新。
     */
    fun setCheckHandlerResult(isContinue: Boolean) {
        if (!isBindService) { //防止重复调用，如果服务已经启动了就不在生效。
            check(!(dialog !is DefaultUpdateDialog || !mIsChecked)) {
                //如果不是自定义UI交互或没有使用API提供的check方法检测更新的话不允许调用该方法。
                "Because of your dialog is not custom, so you can't call the method."
            }
            respondCheckHandlerResult(isContinue)
        }
    }

    /**
     * 跳过当前版本。自动更新时当前版本不在提示。
     */
    fun skipThisVersion() {
        mUpdateInfo?.versionCode?.also {
            UpdateHelper.setSkippedVersion(mApplicationContext, it)
        }
        mCallback?.apply {
            (this as? CompleteUpdateCallback)?.onDownloadCancelled()
            onFiled(mIsAutoCheck, true, mHaveNewVersion, localVersionName, 0, updateType)
            onCompleted()
            mUpdateInfo = null
        }
    }

    /**
     * 响应检查更新的对话框的操作结果。如果你没有关闭默认的对话框使用自定义对话框的话请不要手动调用该方法。
     *
     * @param isContinue 是否继续，如果继续则说明同意更新，否则就是不同意更新。
     */
    private fun respondCheckHandlerResult(isContinue: Boolean) {
        if (isContinue) {
            val apkFile = File(getApkPathFromSp(mApplicationContext))
            if (mIsLoaded && checkFileSignature(apkFile)) {
                enabledDialog?.dismiss()
                handlerDownloadSuccess(apkFile)
            } else {
                if (apkFile.exists()) {
                    removeOldApk(mApplicationContext)
                }
                if (checkCanDownloadable()) {
                    startDownload()
                } else {
                    onTipNetworkError()
                }
                NetWorkStateUtil.addNetworkStateChangedListener(connectChangedListener)
            }
        } else {
            mCallback?.apply {
                (this as? CompleteUpdateCallback)?.onDownloadCancelled()
                onFiled(mIsAutoCheck, true, mHaveNewVersion, localVersionName, 0, updateType)
                onCompleted()
                mUpdateInfo = null
            }
        }
    }

    private fun handlerDownloadSuccess(apkFile: File) {
        if (mAutoInstall) {
            Log.i(TAG, "Apk下载成功。")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ActivityStackManager.watchStackTopActivity(true) {
                    OkPermission.with(it)
                        .addDefaultPermissions(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                        .checkAndApply { granted, _ ->
                            if (granted) {
                                onInstallApk(apkFile)
                            } else {
                                mCallback?.apply {
                                    (this as? CompleteUpdateCallback)?.onDownloadSuccess(apkFile, true)
                                    onFiled(mIsAutoCheck, isCanceled = true, haveNewVersion = true, curVersionName = localVersionName, checkMD5failedCount = 0, updateType = updateType)
                                    onCompleted()
                                    mUpdateInfo = null
                                }
                            }
                        }
                    true
                }
            } else {
                onInstallApk(apkFile)
            }
        } else {
            mCallback?.apply {
                (this as? CompleteUpdateCallback)?.onDownloadSuccess(apkFile, true)
                onSuccess(mIsAutoCheck, true, localVersionName, updateType)
                onCompleted()
                mUpdateInfo = null
            }
        }
    }

    private fun onInstallApk(apkFile: File?) {
        Log.w(TAG, "开始安装Apk:${apkFile?.absolutePath}")
        val installApk = installApk(mApplicationContext, apkFile)
        if (!installApk) {
            mCallback?.apply {
                (this as? CompleteUpdateCallback)?.onInstallFailed()
                onFiled(mIsAutoCheck, isCanceled = false, haveNewVersion = true, curVersionName = localVersionName, checkMD5failedCount = 0, updateType = updateType)
                onCompleted()
                mUpdateInfo = null
            }
        } //如果installApk为true则不需要回调了，因为安装成功必定会杀死进程。杀掉进程后回调已经没有意义了。
    }

    private fun checkCanDownloadable(): Boolean {
        return NetWorkStateUtil.isNetworkAvailable
    }

    /**
     * 开始下载。
     *
     * @param updateInfo        更新信息对象。
     * @param autoInstall       是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。
     */
    fun download(updateInfo: UpdateInfo, autoInstall: Boolean = true): Boolean {
        return if (mUpdateInfo == null) {
            check(!mIsChecked) {
                //如果检查更新不是自己检查的就不能调用这个方法。
                "Because you update the action is completed, so you can't call this method."
            }
            require(!(!autoInstall && mCallback == null)) { "Because you have neither set up to monitor installed automatically, so the download is pointless." }
            if (!updateInfo.downLoadsUrl.isNullOrBlank()) {
                mAutoInstall = autoInstall
                mUpdateInfo = updateInfo
                if (checkCanDownloadable()) {
                    startDownload()
                } else {
                    onTipNetworkError()
                }
                NetWorkStateUtil.addNetworkStateChangedListener(connectChangedListener)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * 开始下载。
     */
    private fun startDownload() {
        if (!isBindService && mUpdateInfo != null) {
            Log.i(TAG, "开始下载")
            mServiceIntent = DownloadService.obtainIntent(mApplicationContext, requireUpdateInfo.downLoadsUrl!!, updateType, defaultApkName).also {
                mApplicationContext.startService(it)
                isBindService = mApplicationContext.bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    /**
     * 获取默认的Apk名称。
     *
     * @return 返回一个以 "包名+日期" 命名的Apk名称。
     */
    private val defaultApkName: String
        get() {
            val format = SimpleDateFormat("yyyy-M-d_HH-MM", Locale.CHINA)
            val formatDate = format.format(Date())
            return mApplicationContext.packageName + formatDate + ".apk"
        }

    class Builder {
        /**
         * 用来配置下载的监听回调对象。
         */
        private var callback: IUpdateCallback? = null

        /**
         * 自定义弹窗。
         */
        private var customDialogGenerator: ((updater: ApkUpdater) -> ApkUpdateDialog)? = null

        /**
         * 设置监听对象。
         *
         * @param callback 监听回调对象。
         */
        fun setCallback(callback: IUpdateCallback?): Builder {
            this.callback = callback
            return this
        }

        /**
         * 如果你希望自己创建对话框，而不使用默认提供的对话框，可以调用该方法将默认的对话框关闭。
         * 如果你关闭了默认的对话框的话就必须自己实现UI交互，并且在用户更新提示做出反应的时候调用
         * [ApkUpdater.setCheckHandlerResult] 方法。
         */
        fun setDialogGenerator(generator: (updater: ApkUpdater) -> ApkUpdateDialog): Builder {
            customDialogGenerator = generator
            return this
        }

        /**
         * 构建 [ApkUpdater] 对象。
         *
         * @return 返回一个构建好的 [ApkUpdater] 对象。
         */
        fun create(): ApkUpdater {
            return ApkUpdater(
                customDialogGenerator,
                callback
            )
        }

    }

    /**
     * 校验文件签名，如果调用者的[UpdateInfo.signature]字段不为空的字符，均认为调用者提供了正确的文件签名，那么
     * 将会对这个签名与当前apk文件的签名进行校验。
     *
     * @param apkFile 当前的apk文件。
     * @return 如果当前文件存在且正确的签名与当前文件的签名匹配或则调用者没有提供签名(不需要签名校验)则返回true， 否则返回false。
     */
    private fun checkFileSignature(apkFile: File): Boolean {
        return if (!apkFile.exists()) {
            false
        } else {
            val signatureType = requireUpdateInfo.signatureType
            val availableSignature = requireUpdateInfo.signature
            if (signatureType != null && !TextUtils.isEmpty(availableSignature)) {
                TextUtils.equals(availableSignature, getFileSignature(apkFile, signatureType))
            } else {
                true
            }
        }
    }

    private inner class OnLoadProgressListener : DownloadProgressCallback {
        override fun onStartDownLoad() {
            (mCallback as? CompleteUpdateCallback)?.onStartDownLoad()
        }

        override fun onProgress(total: Long, current: Long, percentage: Int) {
            if (percentage == 100 || total == current) {
                putApkVersionCode2Sp(mApplicationContext, requireUpdateInfo.versionCode)
            }
            (mCallback as? CompleteUpdateCallback)?.onProgress(total, current, percentage)
            //因为回调有延迟，所以这里判断一下是否有网络，如果没有网络就不在更新进度，否则明明已经提示用户没有网了，但是进度还会更新，有点诡异。
            if (NetWorkStateUtil.isNetworkAvailable) {
                enabledDialog?.onProgress(total, current, percentage)
            }
        }

        override fun onLoadSuccess(apkFile: File, isCache: Boolean) {
            enabledDialog?.dismiss()
            ActivityStackManager.stackTopActivity?.also { activity ->
                if (!checkFileSignature(apkFile)) {
                    removeOldApk(mApplicationContext)
                    downloadFailedCountPlus(mApplicationContext)
                    AlertDialog.Builder(activity)
                        .setCancelable(false)
                        .setTitle(activity.getString(R.string.kelin_apk_updater_warn))
                        .setMessage(activity.getString(R.string.kelin_apk_updater_download_failed_tip))
                        .setNegativeButton(activity.getString(R.string.kelin_apk_updater_i_known)) { _, _ ->
                            mOnProgressListener.onLoadFailed()
                        }.create().show()
                } else {
                    clearDownloadFailedCount(mApplicationContext)
                    NetWorkStateUtil.removeNetworkStateChangedListener(connectChangedListener)
                    stopService() //结束服务
                    handlerDownloadSuccess(apkFile)
                }
            }
        }

        override fun onLoadFailed() {
            NetWorkStateUtil.removeNetworkStateChangedListener(connectChangedListener)
            stopService() //结束服务
            enabledDialog?.dismiss()
            mCallback?.apply {
                (this as? CompleteUpdateCallback)?.onDownloadFailed()
                onFiled(mIsAutoCheck, false, mHaveNewVersion, localVersionName, getDownloadFailedCount(mApplicationContext), updateType)
                onCompleted()
                mUpdateInfo = null
            }
        }

        override fun onLoadPaused() {
            (mCallback as? CompleteUpdateCallback)?.onDownloadPaused()
        }

        override fun onLoadPending() {
            (mCallback as? CompleteUpdateCallback)?.onDownloadPending()
        }
    }
}