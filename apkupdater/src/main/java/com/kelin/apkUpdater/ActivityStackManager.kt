package com.kelin.apkUpdater

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import com.kelin.apkUpdater.exception.IllegalCallException
import com.kelin.apkUpdater.exception.UninitializedException
import java.util.*

/**
 * **描述:** 活动栈的管理者。
 *
 *
 * **创建人:** kelin
 *
 *
 * **创建时间:** 2018/9/25  上午9:37
 *
 *
 * **版本:** v 1.0.0
 */
object ActivityStackManager {
    private val activityStack = ArrayList<Activity>()
    private var application: Context? = null
    internal val applicationContext: Context
        get() = application ?: throw UninitializedException()

    internal fun initUpdater(context: Context) {
        application = context.applicationContext
        (applicationContext as Application?)!!.registerActivityLifecycleCallbacks(ApplicationActivityLifecycleCallbacks())
    }

    val stackTopActivity: Activity?
        get() = if (activityStack.isEmpty()) null else activityStack[0]

    fun requireStackTopActivity(): Activity {
        return activityStack.firstOrNull() ?: throw IllegalCallException("No Activity is currently started！")
    }

    val allActivity: List<Activity>
        get() = ArrayList(activityStack)

    private class ApplicationActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activityStack.add(0, activity)
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}
        override fun onActivityDestroyed(activity: Activity) {
            activityStack.remove(activity)
        }
    }
}