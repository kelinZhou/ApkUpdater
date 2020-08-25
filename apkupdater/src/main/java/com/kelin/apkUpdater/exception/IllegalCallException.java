package com.kelin.apkUpdater.exception;

/**
 * **描述:** 非法调用异常。
 * <p>
 * **创建人:** kelin
 * <p>
 * **创建时间:** 2018/12/12  上午11:34
 * <p>
 * **版本:** v 1.0.0
 */
public class IllegalCallException extends RuntimeException {
    public IllegalCallException(String message) {
        super(message);
    }
}
