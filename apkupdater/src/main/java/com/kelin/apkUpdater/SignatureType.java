package com.kelin.apkUpdater;

/**
 * **描述:** 签名类型。
 * <p>
 * **创建人:** kelin
 * <p>
 * **创建时间:** 2019-07-10  14:56
 * <p>
 * **版本:** v 1.0.0
 */
public enum SignatureType {
    MD5("MD5"), SHA1("SHA-1");

    private String typeName;

    SignatureType(String type) {
        typeName = type;
    }

    public String getTypeName() {
        return typeName;
    }}
