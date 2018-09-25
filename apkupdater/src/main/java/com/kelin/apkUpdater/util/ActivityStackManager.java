package com.kelin.apkUpdater.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * **描述:** 活动栈的管理者。
 * <p>
 * **创建人:** kelin
 * <p>
 * **创建时间:** 2018/9/25  上午9:37
 * <p>
 * **版本:** v 1.0.0
 */
public class ActivityStackManager {

    private final ArrayList<Activity> activityStack = new ArrayList<>();
    private Context applicationContext;

    @SuppressLint("StaticFieldLeak")
    private static final ActivityStackManager ourInstance = new ActivityStackManager();

    public static ActivityStackManager getInstance() {
        return ourInstance;
    }

    private ActivityStackManager() {
    }

    public void initUpdater(Context context) {
        applicationContext = context.getApplicationContext();
        ((Application)applicationContext).registerActivityLifecycleCallbacks(new ApplicationActivityLifecycleCallbacks());
    }

    @Nullable
    public Activity getStackTopActivity() {
        return activityStack.isEmpty() ? null : activityStack.get(0);
    }

    public List<Activity> getAllActivity() {
        return new ArrayList<>(activityStack);
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    private class ApplicationActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            activityStack.add(0, activity);
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            activityStack.remove(activity);
        }
    }
}
