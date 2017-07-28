package com.kelin.apkUpdater.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.net.ConnectivityManagerCompat;

import java.lang.reflect.Method;

/**
 * 描述 网络状态相关的工具类。
 * 创建人 kelin
 * 创建时间 2017/7/12  上午11:03
 * 版本 v 1.0.0
 */

public class NetWorkStateUtil {

    private static boolean sIsWifiConnected;

    private NetWorkStateUtil() {
        throw new InstantiationError("Utility class don't need to instantiate！");
    }

    /**
     * 检查当前WIFI是否连接，两层意思——是否连接，连接是不是WIFI
     *
     * @param context 上下文。
     * @return true 表示当前网络处于连接状态，且是WIFI，否则返回false。
     */
    public static boolean isWifiConnected(Context context) {
        NetworkInfo info = getConnectivityManager(context).getActiveNetworkInfo();
        return info != null && info.isConnected() && ConnectivityManager.TYPE_WIFI == info.getType();
    }

    /**
     * 检查当前GPRS是否连接，两层意思——是否连接，连接是不是GPRS
     *
     * @param context 上下文。
     * @return true 表示当前网络处于连接状态，且是GPRS，否则返回false。
     */
    public static boolean isGprsConnected(Context context) {
        NetworkInfo info = getConnectivityManager(context).getActiveNetworkInfo();
        return info != null && info.isConnected() && ConnectivityManager.TYPE_MOBILE == info.getType();
    }

    /**
     * 检查当前是否连接。
     *
     * @param context 上下文。
     * @return true 表示当前网络处于连接状态，否则返回false。
     */
    public static boolean isConnected(Context context) {
        NetworkInfo info = getConnectivityManager(context).getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    /**
     * 对大数据传输时，需要调用该方法做出判断，如果流量敏感，应该提示用户。
     *
     * @param context 上下文。
     * @return true表示流量敏感，false表示不敏感。
     */
    public static boolean isActiveNetworkMetered(Context context) {
        return ConnectivityManagerCompat.isActiveNetworkMetered(getConnectivityManager(context));
    }

    /**
     * 注册网络改变广播。
     *
     * @param context  上下文。
     * @param receiver {@link ConnectivityChangeReceiver} 广播对象。
     */
    public static Intent registerReceiver(Context context, ConnectivityChangeReceiver receiver) {
        receiver.setRegister(true);
        return context.registerReceiver(receiver, ConnectivityChangeReceiver.FILTER);
    }

    /**
     * 反注册网络改变广播。
     *
     * @param context  上下文。
     * @param receiver 已经被注册过的 {@link ConnectivityChangeReceiver} 广播对象。
     */
    public static void unregisterReceiver(Context context, ConnectivityChangeReceiver receiver) {
        context.unregisterReceiver(receiver);
        receiver.setRegister(false);
    }

    /**
     * 网络改变广播。
     */
    public static abstract class ConnectivityChangeReceiver extends BroadcastReceiver {
        public static final IntentFilter FILTER = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        private boolean isRegister;

        public void setRegister(boolean register) {
            isRegister = register;
        }

        public boolean isRegister() {
            return isRegister;
        }

        @Override
        public final void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getExtras().getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
            int type;
            if (networkInfo != null) {
                type = networkInfo.getType();
            } else {
                return;
            }
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean isMobileEnabled = isMobileEnabled(context);
                if (networkInfo.isConnected()) {
                    if (type == ConnectivityManager.TYPE_WIFI && !sIsWifiConnected) {
                        sIsWifiConnected = true;
                        onConnected(ConnectivityManager.TYPE_WIFI);
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        sIsWifiConnected = false;
                        onConnected(ConnectivityManager.TYPE_MOBILE);
                    }
                } else {
                    sIsWifiConnected = false;
                    if (!isMobileEnabled) {
                        onDisconnected(type);
                    }
                }
            }
        }

        /**
         * 当链接断开的时候执行。如果该方法被执行就说明当前已经没有任何可以使用网络了。
         *
         * @param type 表示当前断开链接的类型，是WiFi还是流量。如果为 {@link ConnectivityManager#TYPE_WIFI} 则说明当前断开链接
         *             的是WiFi，如果为 {@link ConnectivityManager#TYPE_MOBILE} 则说明当前断开链接的是流量。
         */
        protected abstract void onDisconnected(int type);

        /**
         * 当链接成功后执行。
         *
         * @param type 表示当前链接的类型，是WiFi还是流量。如果为 {@link ConnectivityManager#TYPE_WIFI} 则说明当前链接
         *             成功的是WiFi，如果为 {@link ConnectivityManager#TYPE_MOBILE} 则说明当前链接成功的是流量。
         */
        protected abstract void onConnected(int type);
    }

    /**
     * 移动流量开关是否被打开。
     *
     * @param context 上下文。
     * @return 返回true表示流量是开启的，false表示是关闭的。注意开启并不代表当前的网络连接就是移动流量，只是单单开启了开关而已。
     */
    public static boolean isMobileEnabled(Context context) {
        try {
            Method getMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabledMethod.setAccessible(true);
            return (Boolean) getMobileDataEnabledMethod.invoke(getConnectivityManager(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 反射失败，默认开启
        return true;
    }

    private static ConnectivityManager getConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
}
