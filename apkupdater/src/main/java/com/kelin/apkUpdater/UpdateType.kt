package com.kelin.apkUpdater

/**
 * **描述:** 更新类型。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2021/2/2 11:09 AM
 *
 * **版本:** v 1.0.0
 */
enum class UpdateType(val code: Int) {
    /**
     * 弱更新，当更新类型为该类型时应当可以让用户跳过本次更新(仅在自动更新时才会出现该类型)，
     * 即在下一个版本发布前不再提示用户更新，当用户真的需要更新时则可以手动检测更新。
     */
    UPDATE_WEAK(0x01),

    /**
     * 正常更新。
     */
    UPDATE_NORMAL(0x02),

    /**
     * 强制更新。强制用户更新，理论上如果用户不更新则无法继续使用App。
     */
    UPDATE_FORCE(0x03)
}