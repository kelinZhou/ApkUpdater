package com.kelin.apkUpdater

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.kelin.apkUpdater.exception.UninitializedException
import java.util.*


//Activity被创建的监听。
internal typealias ActivityCreatedListener = (activity: Activity) -> Boolean

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
internal object ActivityStackManager {
    private val activityStack = ArrayList<Activity>()
    private var application: Context? = null
    private val stackTopActivityListeners by lazy { ArrayList<ActivityCreatedListenerWrapper>() }
    internal val applicationContext: Context
        get() = application ?: throw UninitializedException()

    internal fun initUpdater(context: Context) {
        application = context.applicationContext
        (applicationContext as Application?)!!.registerActivityLifecycleCallbacks(ApplicationActivityLifecycleCallbacks())
    }

    val stackTopActivity: Activity?
        get() = if (activityStack.isEmpty()) null else activityStack[0]

    /**
     * 实时观察当前栈顶的Activity。
     * @param onlyFragmentActivity 是否只获取FragmentActivity。
     * @param l 得到栈顶Activity后的回调，回调如果返回true则表示后续不需要在继续监听了，为false则表示后续还想要继续监听。
     */
    fun watchStackTopActivity(onlyFragmentActivity: Boolean = false, l: ActivityCreatedListener) {
        stackTopActivity?.takeIf {
            !onlyFragmentActivity || it is FragmentActivity
        }.also {
            if (it != null && (it as? LifecycleOwner)?.lifecycle?.currentState != Lifecycle.State.DESTROYED) {
                if (!l(it)) {
                    stackTopActivityListeners.add(ActivityCreatedListenerWrapper(onlyFragmentActivity, l))
                }
            } else {
                stackTopActivityListeners.add(ActivityCreatedListenerWrapper(onlyFragmentActivity, l))
            }
        }
    }

    private class ApplicationActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            stackTopActivityListeners.forEach {
                if (!it.onlyFragmentActivity || activity is FragmentActivity) {
                    if (it.listener.invoke(activity)) {
                        stackTopActivityListeners.remove(it)
                    }
                }
            }
            activityStack.add(0, activity)
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            stackTopActivity?.also { top ->
                stackTopActivityListeners.forEach {
                    if (!it.onlyFragmentActivity || top is FragmentActivity) {
                        if (it.listener.invoke(top)) {
                            stackTopActivityListeners.remove(it)
                        }
                    }
                }
            }
            activityStack.remove(activity)
        }
    }
}

private data class ActivityCreatedListenerWrapper(val onlyFragmentActivity: Boolean, val listener: ActivityCreatedListener)