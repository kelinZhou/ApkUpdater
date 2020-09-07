package com.kelin.apkUpdater

/**
 * 描述 需要更新的Apk信息对象。
 * 创建人 kelin
 * 创建时间 2017/3/13  下午2:32
 */
class UpdateInfoImpl(
        /**
         * 最新版本的下载链接。
         */
        override val downLoadsUrl: String?,
        /**
         * downLoadsUrl下载链接所对应的版本号。
         */
        override val versionCode: Int,
        /**
         * downLoadsUrl下载链接所对应的版本名称。
         */
        override val versionName: String?,
        /**
         * 是否是强制更新。
         */
        override val isForceUpdate: Boolean,
        /**
         * 更新标题。
         */
        override val updateMessageTitle: CharSequence?,
        /**
         * 更新的内容。
         */
        override val updateMessage: CharSequence?,
        /**
         * 服务端提供的文件签名类型，目前只支持MD5或SHA1。用于Apk完整性校验，防止下载的过程中丢包或Apk遭到恶意串改。
         */
        override val signatureType: SignatureType? = null,
        /**
         * 服务端提供的文件签名，目前只支持MD5或SHA1。用于Apk完整性校验，防止下载的过程中丢包或Apk遭到恶意串改。
         */
        override val signature: String? = null,
        /**
         * 获取强制更新的版本号，如果你的本次强制更新是针对某个或某些版本的话，你可以在该方法中返回。前提是 [.isForceUpdate]
         * 返回值必须为true，否则该方法的返回值是没有意义的。
         *
         * @return 返回你要强制更新的版本号，可以返回 null ，如果返回 null 并且 [.isForceUpdate] 返回 true 的话
         * 则表示所有版本全部强制更新。
         */
        override val forceUpdateVersionCodes: IntArray? = null
) : UpdateInfo