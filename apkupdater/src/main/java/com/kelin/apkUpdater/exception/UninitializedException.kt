package com.kelin.apkUpdater.exception

/**
 * **描述:** 未初始化异常。
 *
 *
 * **创建人:** kelin
 *
 *
 * **创建时间:** 2018/12/12  上午11:23
 *
 *
 * **版本:** v 1.0.0
 */
class UninitializedException : RuntimeException("You got to call the ApkUpdater.init(context) method!")