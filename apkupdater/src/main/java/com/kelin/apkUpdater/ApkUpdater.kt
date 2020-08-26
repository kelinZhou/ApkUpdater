package com.kelin.apkUpdater

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.StyleRes
import android.text.TextUtils
import android.widget.Toast
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
import com.kelin.apkUpdater.UpdateHelper.isForceUpdate
import com.kelin.apkUpdater.UpdateHelper.putApkVersionCode2Sp
import com.kelin.apkUpdater.UpdateHelper.removeOldApk
import com.kelin.apkUpdater.callback.CompleteUpdateCallback
import com.kelin.apkUpdater.callback.DialogEventCallback
import com.kelin.apkUpdater.callback.DownloadProgressCallback
import com.kelin.apkUpdater.callback.IUpdateCallback
import com.kelin.apkUpdater.dialog.DefaultDialog
import com.kelin.apkUpdater.dialog.DefaultDialog.DialogListener
import com.kelin.apkUpdater.dialog.DialogParams
import com.kelin.apkUpdater.util.NetWorkStateUtil
import com.kelin.apkUpdater.util.NetWorkStateUtil.ConnectivityChangeReceiver
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
        private var notifyCationTitle: CharSequence?,
        private var notifyCationDesc: CharSequence?,
        private val checkWiFiState: Boolean,
        noDialog: Boolean,
        private var mCallback: IUpdateCallback?,
        private val informDialogConfig: DialogParams,
        private val loadDialogConfig: DialogParams,
        private val dialogCallback: DialogEventCallback?
) {

    companion object {
        /**
         * 表示当前的状态是检查更新。
         */
        const val STATE_CHECK_UPDATE = 0x00000010
        /**
         * 表示当前的状态是无网络。
         */
        const val STATE_NETWORK_UNUSABLE = 0x00000011
        /**
         * 表示当前的状态是无WiFi。
         */
        const val STATE_WIFI_UNUSABLE = 0x00000012
        /**
         * 表示当前的状态是下载中。
         */
        const val STATE_DOWNLOAD = 0x00000013
        /**
         * 表示当前状态是校验MD5失败。
         */
        const val STATE_CHECK_MD5_FAILED = 0x00000014

        fun init(context: Context) {
            ActivityStackManager.initUpdater(context)
        }
    }

    private var isBindService = false
    private var mServiceIntent: Intent? = null
    private var mDefaultDialog: DefaultDialog? = null
    private var mIsLoaded = false
    private var mUpdateInfo: UpdateInfo? = null
    private var mHaveNewVersion = false
    private var mIsChecked = false
    private var mAutoInstall = true
    private val mNetWorkStateChangedReceiver by lazy { NetWorkStateChangedReceiver() }
    private var mOnProgressListener: OnLoadProgressListener? = null
    private var mIsProgressDialogHidden = false
    private val mDialogListener = DefaultDialogListener()
    private val mApplicationContext: Context = ActivityStackManager.applicationContext
    private var mIsAutoCheck = false

    init {
        if (!noDialog) {
            mDefaultDialog = DefaultDialog()
        }
    }

    /**
     * 当前的版本名称。
     */
    private val localVersionName by lazy { getCurrentVersionName(mApplicationContext) }
    /**
     * 当前的版本号。
     */
    private val localVersionCode by lazy { getCurrentVersionCode(mApplicationContext) }

    //接口回调，下载进度
    private val serviceConnection: ServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val binder = service as DownloadBinder
                val downloadService = binder.service
                //接口回调，下载进度
                if (mOnProgressListener == null) {
                    mOnProgressListener = OnLoadProgressListener()
                }
                downloadService.setOnProgressListener(mOnProgressListener)
                downloadService.setServiceUnBindListener { isBindService = false }
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
     * 静默下载。
     */
    fun silentDownload() {
        if (mCallback != null) {
            mCallback!!.onSilentDownload(this)
        }
    }

    private fun registerNetWorkReceiver() {
        if (!mNetWorkStateChangedReceiver.isRegister) {
            NetWorkStateUtil.registerReceiver(mApplicationContext, mNetWorkStateChangedReceiver)
        }
    }

    private fun unregisterNetWorkReceiver() {
        NetWorkStateUtil.unregisterReceiver(mApplicationContext, mNetWorkStateChangedReceiver)
    }

    /**
     * 判断当前版本是否是强制更新。
     *
     * @return 如果是返回true，否则返回false。
     */
    private fun isForceUpdate(updateInfo: UpdateInfo): Boolean {
        return isForceUpdate(updateInfo, mApplicationContext)
    }

    /**
     * 显示进度条对话框。
     */
    private fun showProgressDialog() {
        if (mDefaultDialog != null) {
            mDefaultDialog!!.show(loadDialogConfig, mDialogListener.changeState(STATE_DOWNLOAD))
        } else {
            if (dialogCallback != null) {
                mDefaultDialog!!.dismissAll()
                dialogCallback.onShowProgressDialog(this, isForceUpdate(mUpdateInfo!!))
            } else {
                throw IllegalArgumentException("you mast call ApkUpdater's \"setCallback(CompleteUpdateCallback callback)\" Method。")
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
    fun check(updateInfo: UpdateInfo, autoCheck: Boolean = true, autoInstall: Boolean = true) {
        require(!(!autoInstall && mCallback == null)) { "Because you neither set up to monitor installed automatically, so the check update is pointless." }
        val haveNewVersion = updateInfo.versionCode > localVersionCode
        if (!haveNewVersion) {
            if (mCallback != null) {
                mCallback!!.onSuccess(autoCheck, false, localVersionName, false)
                mCallback!!.onCompleted()
            }
            return
        }
        if (!NetWorkStateUtil.isConnected(mApplicationContext)) {
            if (mCallback != null) {
                mCallback!!.onFiled(autoCheck, false, haveNewVersion, localVersionName, 0, isForceUpdate(updateInfo))
                mCallback!!.onCompleted()
            }
            return
        }
        if (mUpdateInfo !== updateInfo) {
            mIsAutoCheck = autoCheck
            if (TextUtils.isEmpty(updateInfo.downLoadsUrl)) {
                if (mCallback != null) {
                    if (completeUpdateCallback != null) {
                        completeUpdateCallback!!.onDownloadFailed()
                    }
                    mCallback!!.onFiled(autoCheck, false, haveNewVersion, localVersionName, 0, isForceUpdate(updateInfo))
                    mCallback!!.onCompleted()
                }
                return
            }
            mHaveNewVersion = true
            mAutoInstall = autoInstall
            mIsChecked = true
            mUpdateInfo = updateInfo
            loadDialogConfig.isForceUpdate = isForceUpdate(updateInfo)
            informDialogConfig.message = updateInfo.updateMessage
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

    private fun onShowUpdateInformDialog() {
        if (mDefaultDialog != null) {
            informDialogConfig.isForceUpdate = isForceUpdate(mUpdateInfo!!)
            showUpdateInformDialog()
        } else {
            if (dialogCallback != null) {
                dialogCallback.onShowCheckHintDialog(this@ApkUpdater, mUpdateInfo!!, isForceUpdate(mUpdateInfo!!))
            } else {
                throw IllegalArgumentException("you mast call ApkUpdater's \"setCallback(CompleteUpdateCallback callback)\" Method。")
            }
        }
    }

    private val completeUpdateCallback: CompleteUpdateCallback?
        get() = if (mCallback is CompleteUpdateCallback) mCallback as CompleteUpdateCallback? else null

    /**
     * 显示更新提醒。
     */
    private fun showUpdateInformDialog() {
        mDefaultDialog!!.show(informDialogConfig, mDialogListener.changeState(STATE_CHECK_UPDATE))
    }

    /**
     * 设置检查更新的对话框的操作结果。如果你没有关闭默认的对话框使用自定义对话框的话请不要手动调用该方法。
     *
     * @param isContinue 是否继续，如果继续则说明统一更新，否则就是不统一更新。
     */
    fun setCheckHandlerResult(isContinue: Boolean) {
        check(!(mDefaultDialog != null || !mIsChecked)) {
            //如果不是自定义UI交互或没有使用API提供的check方法检测更新的话不允许调用该方法。
            "Because of your dialog is not custom, so you can't call the method."
        }
        respondCheckHandlerResult(isContinue)
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
                handlerDownloadSuccess(apkFile)
            } else {
                if (apkFile.exists()) {
                    removeOldApk(mApplicationContext)
                }
                if (checkCanDownloadable()) {
                    startDownload()
                }
            }
        } else {
            if (mCallback != null) {
                if (completeUpdateCallback != null) {
                    completeUpdateCallback!!.onDownloadCancelled()
                }
                mCallback!!.onFiled(mIsAutoCheck, true, mHaveNewVersion, localVersionName, 0, isForceUpdate(mUpdateInfo!!))
                mCallback!!.onCompleted()
            }
        }
    }

    private fun handlerDownloadSuccess(apkFile: File) {
        if (dialogCallback != null) {
            val length = apkFile.length()
            dialogCallback.onProgress(this@ApkUpdater, length, length, 100)
        }
        if (mAutoInstall) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OkPermission.with(ActivityStackManager.requireStackTopActivity())
                        .addDefaultPermissions(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                        .checkAndApply { granted, _ ->
                            if (granted) {
                                onInstallApk(apkFile)
                            } else {
                                val completeUpdateCallback = completeUpdateCallback
                                completeUpdateCallback?.onDownloadSuccess(apkFile, true)
                                if (mCallback != null) {
                                    mCallback!!.onFiled(mIsAutoCheck, isCanceled = true, haveNewVersion = true, curVersionName = localVersionName, checkMD5failedCount = 0, isForceUpdate = isForceUpdate(mUpdateInfo!!))
                                    mCallback!!.onCompleted()
                                }
                            }
                        }
            } else {
                onInstallApk(apkFile)
            }
        } else {
            val completeUpdateCallback = completeUpdateCallback
            completeUpdateCallback?.onDownloadSuccess(apkFile, true)
            if (mCallback != null) {
                mCallback!!.onSuccess(mIsAutoCheck, true, localVersionName, isForceUpdate(mUpdateInfo!!))
                mCallback!!.onCompleted()
            }
        }
    }

    private fun onInstallApk(apkFile: File?) {
        val completeUpdateCallback = completeUpdateCallback
        val installApk = installApk(mApplicationContext, apkFile)
        if (!installApk) {
            completeUpdateCallback?.onInstallFailed()
            if (mCallback != null) {
                mCallback!!.onFiled(mIsAutoCheck, isCanceled = false, haveNewVersion = true, curVersionName = localVersionName, checkMD5failedCount = 0, isForceUpdate = isForceUpdate(mUpdateInfo!!))
                mCallback!!.onCompleted()
            }
        } //如果installApk为true则不需要回调了，因为安装成功必定会杀死进程。杀掉进程后回调已经没有意义了。
    }

    private fun checkCanDownloadable(): Boolean {
        registerNetWorkReceiver() //注册一个网络状态改变的广播接收者。无论网络是否连接成功都要注册，因为下载过程中可能会断网。
        if (!NetWorkStateUtil.isConnected(mApplicationContext) || checkWiFiState && !NetWorkStateUtil.isWifiConnected(mApplicationContext)) {
            showWifiOrMobileUnusableDialog()
            return false
        }
        return true
    }

    private fun showWifiOrMobileUnusableDialog() {
        if (NetWorkStateUtil.isConnected(mApplicationContext)) {
            showWiFiUnusableDialog()
        } else {
            showNetWorkUnusableDialog()
        }
    }

    private fun showNetWorkUnusableDialog() {
        mDefaultDialog!!.showNetWorkUnusableDialog(mDialogListener.changeState(STATE_NETWORK_UNUSABLE))
    }

    private fun showWiFiUnusableDialog() {
        mDefaultDialog!!.showWiFiUnusableDialog(mDialogListener.changeState(STATE_WIFI_UNUSABLE))
    }

    private fun getApkName(updateInfo: UpdateInfo): String? {
        val apkName = updateInfo.apkName
        return if (TextUtils.isEmpty(apkName)) {
            defaultApkName
        } else {
            if (apkName!!.toLowerCase().endsWith(".apk")) apkName else "$apkName.apk"
        }
    }
    /**
     * 开始下载。
     *
     * @param updateInfo  更新信息对象。
     * @param autoInstall 是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。
     */
    /**
     * 开始下载。
     *
     * @param updateInfo 更新信息对象。
     */
    @JvmOverloads
    fun download(updateInfo: UpdateInfo, autoInstall: Boolean = true) {
        download(updateInfo, null, null, autoInstall)
    }

    /**
     * 开始下载。
     *
     * @param updateInfo        更新信息对象。
     * @param notifyCationTitle 下载过程中通知栏的标题。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。
     * @param notifyCationDesc  下载过程中通知栏的描述。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。
     * @param autoInstall       是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。
     */
    fun download(updateInfo: UpdateInfo, notifyCationTitle: CharSequence?, notifyCationDesc: CharSequence?, autoInstall: Boolean) {
        check(!mIsChecked) {
            //如果检查更新不是自己检查的就不能调用这个方法。
            "Because you update the action is completed, so you can't call this method."
        }
        require(!(!autoInstall && mCallback == null)) { "Because you have neither set up to monitor installed automatically, so the download is pointless." }
        if (TextUtils.isEmpty(updateInfo.downLoadsUrl)) {
            return
        }
        this.notifyCationTitle = if (TextUtils.isEmpty(notifyCationTitle)) "正在下载更新" else notifyCationTitle
        this.notifyCationDesc = notifyCationDesc
        mAutoInstall = autoInstall
        mUpdateInfo = updateInfo
        loadDialogConfig.isForceUpdate = isForceUpdate(updateInfo)
        if (checkCanDownloadable()) {
            startDownload()
        }
    }

    /**
     * 开始下载。
     */
    private fun startDownload() {
        mServiceIntent = Intent(mApplicationContext, DownloadService::class.java)
        mServiceIntent!!.putExtra(DownloadService.KEY_APK_NAME, getApkName(mUpdateInfo!!))
        mServiceIntent!!.putExtra(DownloadService.KEY_DOWNLOAD_URL, mUpdateInfo!!.downLoadsUrl)
        mServiceIntent!!.putExtra(DownloadService.KEY_IS_FORCE_UPDATE, isForceUpdate(mUpdateInfo!!))
        mServiceIntent!!.putExtra(DownloadService.KEY_NOTIFY_TITLE, notifyCationTitle)
        mServiceIntent!!.putExtra(DownloadService.KEY_NOTIFY_DESCRIPTION, notifyCationDesc)
        mApplicationContext.startService(mServiceIntent)
        isBindService = mApplicationContext.bindService(mServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * 获取默认的Apk名称。
     *
     * @return 返回一个以 "包名+日期" 命名的Apk名称。
     */
    private val defaultApkName: String
        private get() {
            val format = SimpleDateFormat("yyyy-M-d_HH-MM", Locale.CHINA)
            val formatDate = format.format(Date())
            return mApplicationContext.packageName + formatDate + ".apk"
        }

    class Builder {
        val informDialogConfig = DialogParams.informDialogParams
        val loadDialogConfig = DialogParams.downloadDialogParams
        /**
         * 用来配置下载的监听回调对象。
         */
        var callback: IUpdateCallback? = null
        /**
         * 通知栏的标题。
         */
        var mTitle: CharSequence? = null
        /**
         * 通知栏的描述。
         */
        var mDescription: CharSequence? = null
        /**
         * 是否没有对话框。
         */
        var noDialog = false
        /**
         * 是否检测WiFi链接状态。
         */
        var checkWiFiState = true
        var dialogCallback: DialogEventCallback? = null
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
         * 设置Dialog的样式。
         *
         * @param style 要设置的样式的资源ID。
         */
        fun setDialogTheme(@StyleRes style: Int): Builder {
            DialogParams.style = style
            return this
        }

        /**
         * 配置检查更新时对话框的标题。
         *
         * @param title 对话框的标题。
         */
        fun setCheckDialogTitle(title: CharSequence?): Builder {
            informDialogConfig.title = title
            return this
        }

        /**
         * 配置下载更新时对话框的标题。
         *
         * @param title 对话框的标题。
         */
        fun setDownloadDialogTitle(title: CharSequence?): Builder {
            loadDialogConfig.title = title
            if (mTitle == null) {
                mTitle = title
            }
            return this
        }

        /**
         * 配置下载更新时对话框的消息。
         *
         * @param message 对话框的消息。
         */
        fun setDownloadDialogMessage(message: String?): Builder {
            loadDialogConfig.message = message
            return this
        }

        /**
         * 设置通知栏的标题。
         */
        fun setNotifyTitle(title: CharSequence?): Builder {
            mTitle = title
            return this
        }

        /**
         * 设置通知栏的描述。
         */
        fun setNotifyDescription(description: CharSequence?): Builder {
            mDescription = description
            return this
        }

        /**
         * 如果你希望自己创建对话框，而不使用默认提供的对话框，可以调用该方法将默认的对话框关闭。
         * 如果你关闭了默认的对话框的话就必须自己实现UI交互，并且在用户更新提示做出反应的时候调用
         * [ApkUpdater.setCheckHandlerResult] 方法。
         */
        fun setNoDialog(callback: DialogEventCallback): Builder {
            noDialog = true
            dialogCallback = callback
            return this
        }

        /**
         * 设置不检查WiFi状态，默认是检查WiFi状态的，也就是说如果在下载更新的时候如果没有链接WiFi的话默认是会提示用户的。
         * 但是如果你不希望给予提示，就可以通过调用此方法，禁用WiFi检查。
         *
         * @param check 是否检测WiFi连接状态，true表示检测，false表示不检测。默认检测。
         */
        fun setCheckWiFiState(check: Boolean): Builder {
            checkWiFiState = check
            return this
        }

        /**
         * 构建 [ApkUpdater] 对象。
         *
         * @return 返回一个构建好的 [ApkUpdater] 对象。
         */
        fun builder(): ApkUpdater {
            return ApkUpdater(
                    mTitle,
                    mDescription,
                    checkWiFiState,
                    noDialog,
                    callback,
                    informDialogConfig,
                    loadDialogConfig,
                    dialogCallback
            )
        }

    }

    private inner class NetWorkStateChangedReceiver : ConnectivityChangeReceiver() {
        /**
         * 当链接断开的时候执行。
         *
         * @param type 表示当前断开链接的类型，是WiFi还是流量。如果为 [ConnectivityManager.TYPE_WIFI] 则说明当前断开链接
         * 的是WiFi，如果为 [ConnectivityManager.TYPE_MOBILE] 则说明当前断开链接的是流量。
         */
        override fun onDisconnected(type: Int) {
            showNetWorkUnusableDialog()
        }

        /**
         * 当链接成功后执行。
         *
         * @param type 表示当前链接的类型，是WiFi还是流量。如果为 [ConnectivityManager.TYPE_WIFI] 则说明当前链接
         * 成功的是WiFi，如果为 [ConnectivityManager.TYPE_MOBILE] 则说明当前链接成功的是流量。
         */
        override fun onConnected(type: Int) {
            when (type) {
                ConnectivityManager.TYPE_MOBILE -> if (isBindService) {
                    if (!mIsProgressDialogHidden) {
                        showProgressDialog()
                    }
                } else {
                    if (checkWiFiState) {
                        showWiFiUnusableDialog()
                    } else {
                        startDownload()
                    }
                }
                ConnectivityManager.TYPE_WIFI -> if (isBindService) {
                    showProgressDialog()
                } else {
                    startDownload()
                }
            }
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
            val signatureType = mUpdateInfo!!.signatureType
            val availableSignature = mUpdateInfo!!.signature
            if (signatureType != null && !TextUtils.isEmpty(availableSignature)) {
                TextUtils.equals(availableSignature, getFileSignature(apkFile, signatureType))
            } else {
                true
            }
        }
    }

    private inner class OnLoadProgressListener : DownloadProgressCallback {
        override fun onStartDownLoad() {
            if (completeUpdateCallback != null) {
                completeUpdateCallback!!.onStartDownLoad()
            }
            showProgressDialog()
        }

        override fun onProgress(total: Long, current: Long, percentage: Int) {
            if (percentage == 100 || total == current) {
                putApkVersionCode2Sp(mApplicationContext, mUpdateInfo!!.versionCode)
            }
            if (completeUpdateCallback != null) {
                completeUpdateCallback!!.onProgress(total, current, percentage)
            }
            dialogCallback?.onProgress(this@ApkUpdater, total, current, percentage)
            mDefaultDialog?.updateDownLoadsProgress(percentage)
        }

        override fun onLoadSuccess(apkFile: File, isCache: Boolean) {
            if (!checkFileSignature(apkFile)) {
                removeOldApk(mApplicationContext)
                downloadFailedCountPlus(mApplicationContext)
                mDefaultDialog!!.showCheckMD5FailedDialog(mDialogListener.changeState(STATE_CHECK_MD5_FAILED))
            } else {
                clearDownloadFailedCount(mApplicationContext)
                unregisterNetWorkReceiver()
                stopService() //结束服务
                handlerDownloadSuccess(apkFile)
            }
        }

        override fun onLoadFailed() {
            unregisterNetWorkReceiver()
            stopService() //结束服务
            mDefaultDialog!!.dismissAll()
            if (mCallback != null) {
                if (completeUpdateCallback != null) {
                    completeUpdateCallback!!.onDownloadFailed()
                }
                mCallback!!.onFiled(mIsAutoCheck, false, mHaveNewVersion, localVersionName, getDownloadFailedCount(mApplicationContext), isForceUpdate(mUpdateInfo!!))
                mCallback!!.onCompleted()
            } else {
                Toast.makeText(mApplicationContext, "sorry, 下载失败了~", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onLoadPaused() {
            if (completeUpdateCallback != null) {
                completeUpdateCallback!!.onDownloadPaused()
            }
        }

        override fun onLoadPending() {
            if (completeUpdateCallback != null) {
                completeUpdateCallback!!.onDownloadPending()
            }
        }
    }

    private inner class DefaultDialogListener : DialogListener {
        private var mCurrentState = 0
        /**
         * 改变状态。
         *
         * @param currentState 要改变新状态。
         * @return 返回 DefaultDialogListener 本身。
         */
        fun changeState(currentState: Int): DefaultDialogListener {
            mCurrentState = currentState
            return this
        }

        override fun onDialogDismiss(isSure: Boolean) {
            val callback = completeUpdateCallback
            when (mCurrentState) {
                Companion.STATE_CHECK_UPDATE -> respondCheckHandlerResult(isSure)
                Companion.STATE_NETWORK_UNUSABLE -> if (mCallback != null) {
                    if (isBindService) {
                        callback?.onDownloadPending()
                    } else {
                        mCallback!!.onFiled(mIsAutoCheck, false, mHaveNewVersion, localVersionName, 0, isForceUpdate(mUpdateInfo!!))
                        mCallback!!.onCompleted()
                    }
                }
                Companion.STATE_WIFI_UNUSABLE -> if (isSure) {
                    startDownload()
                } else {
                    if (mCallback != null) {
                        callback?.onDownloadCancelled()
                        mCallback!!.onFiled(mIsAutoCheck, true, mHaveNewVersion, localVersionName, 0, isForceUpdate(mUpdateInfo!!))
                        mCallback!!.onCompleted()
                    }
                }
                Companion.STATE_DOWNLOAD -> {
                    mIsProgressDialogHidden = true
                    silentDownload()
                }
                Companion.STATE_CHECK_MD5_FAILED -> if (isForceUpdate(mUpdateInfo!!)) {
                    unregisterNetWorkReceiver()
                    stopService() //结束服务
                    mDefaultDialog!!.dismissAll()
                    onShowUpdateInformDialog()
                } else {
                    mOnProgressListener!!.onLoadFailed()
                }
            }
        }
    }
}