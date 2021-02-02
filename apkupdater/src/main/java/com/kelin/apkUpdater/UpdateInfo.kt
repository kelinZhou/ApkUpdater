package com.kelin.apkUpdater

/**
 * 描述 需要更新的Apk信息对象。
 * 创建人 kelin
 * 创建时间 2017/3/13  下午2:32
 */
interface UpdateInfo {
    /**
     * 网络上的版本号。
     */
    val versionCode: Int

    /**
     * 网络上的版本名称。
     */
    val versionName: String?

    /**
     * 最新版本 apk 的下载链接。
     */
    val downLoadsUrl: String?

    /**
     * 更新类型。
     * @see [UpdateType.UPDATE_WEAK]
     * @see [UpdateType.UPDATE_NORMAL]
     * @see [UpdateType.UPDATE_FORCE]
     */
    val updateType: UpdateType

    /**
     * 获取强制更新的版本号，如果你的本次强制更新是针对某个或某些版本的话，你可以在该方法中返回。前提是 [.isForceUpdate]
     * 返回值必须为true，否则该方法的返回值是没有意义的。
     *
     * @return 返回你要强制更新的版本号，可以返回 null ，如果返回 null 并且 [.isForceUpdate] 返回 true 的话
     * 则表示所有版本全部强制更新。
     */
    val forceUpdateVersionCodes: IntArray?

    /**
     * 更新标题，例如"更新以下内容"，用于显示在弹窗中。
     */
    val updateMessageTitle: CharSequence?

    /**
     * 获取更新的内容。就是你本次更新了那些东西可以在这里返回，用于显示在弹窗中。
     */
    val updateMessage: CharSequence?

    /**
     * 服务端可提供的文件签名类型，目前只支持MD5或SHA1。用于Apk完整性校验，防止下载的过程中丢包或Apk遭到恶意串改。
     */
    val signatureType: SignatureType?

    /**
     * 服务端提供的文件签名，目前只支持MD5或SHA1。用于Apk完整性校验，防止下载的过程中丢包或Apk遭到恶意串改。
     */
    val signature: String?
}