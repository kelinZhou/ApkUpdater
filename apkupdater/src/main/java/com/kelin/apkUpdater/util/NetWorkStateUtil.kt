package com.kelin.apkUpdater.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 描述 网络状态相关的工具类。
 * 创建人 kelin
 * 创建时间 2017/7/12  上午11:03
 * 版本 v 1.0.0
 */
internal object NetWorkStateUtil {
    /**
     * 是否已被初始化。
     */
    private var initialized = false

    private val networkChangedListeners by lazy { ArrayList<NetworkStateChangedListener>() }
    private val lifecycleNetworkChangedListeners by lazy { ArrayList<NetworkStateChangedListenerWrapper>() }

    /**
     * 判断网络是否可用。
     */
    var isNetworkAvailable = true
        private set
        get() = field && isNotVpn  //开启vpn检测后isNotVpn字段才可能为false，所以这里直接加上isNotVpn的校验(不开启vpn检测时isNotVpn永远为true)。

    /**
     * 判断是否开启了VPN。
     */
    var isNotVpn = true

    /**
     * 初始化。
     * @param context 应用上下文。
     * @param vpnCheck 是否开启VPN检测，只有开启VPN检测后isNotVpn才可能为false，同时isNetworkAvailable的判断逻辑才会加入vpn的校验。
     */
    fun init(context: Context, vpnCheck: Boolean) {
        if (!initialized) {
            initialized = true
            ContextCompat.getSystemService(context, ConnectivityManager::class.java).also { service ->
                if (service != null) {
                    isNetworkAvailable = service.getNetworkCapabilities(service.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        service.registerDefaultNetworkCallback(NetworkCallbackImpl(vpnCheck))
                    } else {
                        service.registerNetworkCallback(
                            NetworkRequest.Builder()
                                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                .build(),
                            NetworkCallbackImpl(vpnCheck)
                        )
                    }
                }
            }
        } else {
            Log.w("ApkUpdater", "NetWorkStateUtil: The Networks is initialized!!!")
        }
    }

    /**
     * 注册网络状态改变的监听，通过该方法注册的监听无需反注册，监听的声明周期会自动绑定到目标生命周期组件上。
     * @param owner 需要绑定的生命周期组件。
     * @param l 监听。
     */
    fun registerNetworkStateChangedListener(owner: LifecycleOwner, l: NetworkStateChangedListener) {
        owner.lifecycle.addObserver(NetworkStateChangedListenerWrapper(l).also {
            lifecycleNetworkChangedListeners.add(it)
        })
    }

    /**
     * 添加一个网络改变的监听，需要在合适的时机移除改监听，如果不想手动移除则需要通过registerNetworkStateChangedListener方法注册监听与声明周期组件进行绑定。
     */
    fun addNetworkStateChangedListener(l: NetworkStateChangedListener) {
        networkChangedListeners.add(l)
    }

    /**
     * 移除一个通过addNetworkStateChangedListener方法添加的监听。
     */
    fun removeNetworkStateChangedListener(l: NetworkStateChangedListener) {
        networkChangedListeners.remove(l)
    }

    private class NetworkCallbackImpl(private val vpnCheck: Boolean) : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            if (isNetworkAvailable) {  //去重判断，防止重复触发
                notifyStateChanged(false)
                Log.w("ApkUpdater", "NetWorkStateUtil: 网络连接已断开")
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val notVpn = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            if (vpnCheck) {
                isNotVpn = notVpn
            }
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                if (!isNetworkAvailable) {  //去重判断，防止重复触发
                    notifyStateChanged(true)
                    Log.w("ApkUpdater", "NetWorkStateUtil: 网络连接已恢复，VPN开启:${!notVpn}")
                }
            }
        }
    }

    private fun notifyStateChanged(connected: Boolean) {
        isNetworkAvailable = connected
        networkChangedListeners.forEach { it(connected) }
        lifecycleNetworkChangedListeners.forEach { it.onChanged(connected) }
    }

    private class NetworkStateChangedListenerWrapper(listener: NetworkStateChangedListener) : LifecycleEventObserver {

        private var innerListener: NetworkStateChangedListener? = listener

        fun onChanged(available: Boolean) {
            innerListener?.invoke(available)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                lifecycleNetworkChangedListeners.remove(this)
                innerListener = null
                source.lifecycle.removeObserver(this)
            }
        }
    }
}

internal typealias NetworkStateChangedListener = (connected: Boolean) -> Unit