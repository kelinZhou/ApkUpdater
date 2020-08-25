package com.kelin.apkUpdater.exception;

/**
 * **描述:** 未初始化异常。
 * <p>
 * **创建人:** kelin
 * <p>
 * **创建时间:** 2018/12/12  上午11:23
 * <p>
 * **版本:** v 1.0.0
 */
public class UninitializedException extends RuntimeException {
    public UninitializedException() {
        super("You have to call ApkUpdater's init method first！");
    }
}
