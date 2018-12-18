package com.kelin.updatemanagerdemo;

import android.support.annotation.Nullable;

import com.kelin.apkUpdater.UpdateInfo;

/**
 * 创建人 kelin
 * 创建时间 2017/3/14  下午2:08
 * 版本 v 1.0.0
 */

public class UpdateModel implements UpdateInfo {

    @Override
    public int getVersionCode() {
        return 101;
    }

    @Override
    public String getVersionName() {
        return null;
    }

    @Override
    public String getDownLoadsUrl() {
        return "http://file.lieluobo.testing/o_1cntnsvto19mn11p1qgtfvvdd77.apk";
    }

    @Override
    public boolean isForceUpdate() {
        return false;
    }

    /**
     * 获取强制更新的版本号，如果你的本次强制更新是针对某个或某些版本的话，你可以在该方法中返回。前提是 {@link #isForceUpdate()}
     * 返回值必须为true，否则该方法的返回值是没有意义的。
     *
     * @return 返回你要强制更新的版本号，可以返回 null ，如果返回 null 并且 {@link #isForceUpdate()} 返回 true 的话
     * 则表示所有版本全部强制更新。
     */
    @Nullable
    @Override
    public int[] getForceUpdateVersionCodes() {
        return null;
    }

    @Nullable
    @Override
    public String getApkName() {
        return null;
    }

    @Override
    public CharSequence getUpdateMessage() {
        return "1.修复了极端情况下可能导致下单失败的bug。\n2.增加了许多新的玩法，并且增加了app的稳定性。 \n3.这是测试内容，其实什么都没有更新。";
    }

    @Nullable
    @Override
    public String getMd5() {
        return null;
    }
}
