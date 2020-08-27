package com.kelin.updatemanagerdemo

import com.kelin.apkUpdater.SignatureType
import com.kelin.apkUpdater.UpdateInfo

/**
 * 创建人 kelin
 * 创建时间 2017/3/14  下午2:08
 * 版本 v 1.0.0
 */
class UpdateModel : UpdateInfo {
    override val versionCode: Int = 101

    override val versionName: String = "v1.3.1"

    override val downLoadsUrl: String = "http://test-cloud-yxholding-com.oss-cn-shanghai.aliyuncs.com/yx-logistics/file/file/20200703/1593709201374.apk"

    override val isForceUpdate: Boolean = false

    /**
     * 获取强制更新的版本号，如果你的本次强制更新是针对某个或某些版本的话，你可以在该方法中返回。前提是 [.isForceUpdate]
     * 返回值必须为true，否则该方法的返回值是没有意义的。
     *
     * @return 返回你要强制更新的版本号，可以返回 null ，如果返回 null 并且 [.isForceUpdate] 返回 true 的话
     * 则表示所有版本全部强制更新。
     */
    override val forceUpdateVersionCodes: IntArray? = null

    override val apkName: String? = null

    override val updateMessageTitle: CharSequence? = "更新以下内容"

    override val updateMessage: CharSequence = "1.修复了极端情况下可能导致下单失败的bug。\n2.增加了许多新的玩法，并且增加了app的稳定性。 \n3.这是测试内容，其实什么都没有更新。"

    override val signatureType: SignatureType? = SignatureType.MD5

    override val signature: String? = null
}