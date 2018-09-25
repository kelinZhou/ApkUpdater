package com.kelin.apkUpdater.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.kelin.apkUpdater.R;
import com.kelin.apkUpdater.Updater;
import com.kelin.apkUpdater.util.ActivityStackManager;

import java.util.Locale;

/**
 * 描述 更新信息对话框。
 * 创建人 kelin
 * 创建时间 2017/7/5  下午3:59
 * 版本 v 1.0.0
 */

public class DefaultDialog {
    private AlertDialog mDialog;
    private AlertDialog mNetWorkUnusableDialog;
    private AlertDialog mWiFiUnusableDialog;
    private AlertDialog mMD5FailedDialog;
    private ProgressBar mProgressBar;
    private TextView mPercentageView;
    private DialogClickListener mOnClickListener;
    private DialogParams mConfig;
    //为了让进度条走完才销毁。
    private Runnable mAction = new Runnable() {
        @Override
        public void run() {
            dismiss(mDialog);
            mDialog = null;
        }
    };
    private final ActivityStackManager DM;

    public DefaultDialog() {
        DM = ActivityStackManager.getInstance();
    }

    /**
     * 显示对话框。
     */
    public void show(DialogParams config) {
        show(config, null);
    }

    /**
     * 显示对话框。
     *
     * @param listener 对话框的监听。
     */
    @SuppressLint("InflateParams")
    public void show(DialogParams config, final DialogListener listener) {
        Activity curActivity = DM.getStackTopActivity();
        if (curActivity != null) {
            dismiss(mWiFiUnusableDialog);
            mWiFiUnusableDialog = null;
            dismiss(mNetWorkUnusableDialog);
            mNetWorkUnusableDialog = null;
            if (mDialog == null || config != mConfig) {
                mConfig = config;
                //构建AlertDialog。
                AlertDialog.Builder builder = new AlertDialog.Builder(curActivity, DialogParams.getStyle());
                builder.setCancelable(false);
                if (config instanceof DownloadDialogParams) {
                    View contentView = LayoutInflater.from(curActivity).inflate(R.layout.com_kelin_apkupdater_layout_progress_layout, null);
                    mProgressBar = (ProgressBar) contentView.findViewById(R.id.progress);
                    int drawableRes;
                    if (ContextCompat.getColor(mProgressBar.getContext(), R.color.colorPrimary) == Color.WHITE) {
                        drawableRes = R.drawable.com_kelin_apkupdater_shape_progressbar_mini_default;
                    } else {
                        drawableRes = R.drawable.com_kelin_apkupdater_shape_progressbar_mini;
                    }
                    mProgressBar.setProgressDrawable(ContextCompat.getDrawable(mProgressBar.getContext(), drawableRes));
                    mPercentageView = (TextView) contentView.findViewById(R.id.tv_percentage);
                    builder.setView(contentView);
                    if (!config.isForceUpdate()) {
                        builder.setPositiveButton("悄悄的下载", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss(mDialog);
                                mDialog = null;
                                listener.onDialogDismiss(false);
                            }
                        });
                    }
                } else {
                    if (listener != null && mOnClickListener == null) {
                        mOnClickListener = new DialogClickListener();
                    }
                    mOnClickListener.setListener(listener);
                    builder.setPositiveButton("立刻安装", mOnClickListener);  //设置确定按钮
                    if (!config.isForceUpdate()) {
                        builder.setNegativeButton("稍候安装", mOnClickListener); //如果不是强制更新则设置取消按钮
                    }
                }

                builder.setIcon(config.getIcon()) //设置图标
                        .setTitle(config.getTitle()) //设置标题
                        .setMessage(config.getMessage());  //设置内容
                mDialog = builder.create();
            }
            mDialog.show();
        } else {
            new NullPointerException("the curActivity is Null!").printStackTrace();
        }
    }

    public void updateDownLoadsProgress(int percentage) {
        mProgressBar.setProgress(percentage);
        mPercentageView.setText(String.format(Locale.CHINA, "%d %%", percentage));
        if (percentage == mProgressBar.getMax()) {
            mProgressBar.post(mAction);
        }
    }

    public void showWiFiUnusableDialog(final DialogListener listener) {
        Activity curActivity = DM.getStackTopActivity();
        if (curActivity != null) {
            dismiss(mNetWorkUnusableDialog);
            mNetWorkUnusableDialog = null;
            dismiss(mDialog);
            mDialog = null;
            if (listener != null && mOnClickListener == null) {
                mOnClickListener = new DialogClickListener();
            }
            mOnClickListener.setListener(listener);
            if (mWiFiUnusableDialog == null) {
                mWiFiUnusableDialog = new AlertDialog.Builder(curActivity, DialogParams.getStyle())
                        .setCancelable(false)
                        .setTitle("提示：")
                        .setMessage("当前为非WiFi网络，是否继续下载？")
                        .setPositiveButton("继续下载", mOnClickListener)
                        .setNegativeButton("稍后下载", mOnClickListener)
                        .create();
            }
            mWiFiUnusableDialog.show();
        }
    }

    public void dismissAll() {
        dismiss(mDialog);
        mDialog = null;
        dismiss(mNetWorkUnusableDialog);
        mNetWorkUnusableDialog = null;
        dismiss(mWiFiUnusableDialog);
        mWiFiUnusableDialog = null;
    }

    private void dismiss(AlertDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void showNetWorkUnusableDialog(final DialogListener listener) {
        dismiss(mWiFiUnusableDialog);
        mWiFiUnusableDialog = null;
        dismiss(mDialog);
        mDialog = null;
        if (listener != null && mOnClickListener == null) {
            mOnClickListener = new DialogClickListener();
        }

        mOnClickListener.setListener(listener);
        Activity curActivity = DM.getStackTopActivity();
        if (curActivity != null) {
            if (mNetWorkUnusableDialog == null) {
                mNetWorkUnusableDialog = new AlertDialog.Builder(curActivity, DialogParams.getStyle())
                        .setCancelable(false)
                        .setTitle("提示：")
                        .setMessage("网络连接已经断开，请稍后再试。")
                        .setNegativeButton("确定", mOnClickListener)
                        .create();
            }
            mNetWorkUnusableDialog.show();
        }
    }

    public void showCheckMD5FailedDialog(final DialogListener listener) {
        dismiss(mDialog);
        mDialog = null;

        if (listener != null && mOnClickListener == null) {
            mOnClickListener = new DialogClickListener();
        }

        mOnClickListener.setListener(listener);
        Activity curActivity = DM.getStackTopActivity();
        if (curActivity != null) {
            if (mMD5FailedDialog == null) {
                mMD5FailedDialog = new AlertDialog.Builder(curActivity, DialogParams.getStyle())
                        .setCancelable(false)
                        .setTitle("提示：")
                        .setMessage("下载失败，请尝试切换您的网络环境后再试~")
                        .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mMD5FailedDialog.dismiss();
                                mMD5FailedDialog = null;
                                mOnClickListener.onClick(dialog, which);
                            }
                        })
                        .create();
            }
            mMD5FailedDialog.show();
        }
    }

    private class DialogClickListener implements DialogInterface.OnClickListener {
        private DialogListener mListener;

        void setListener(DialogListener listener) {
            mListener = listener;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            mListener.onDialogDismiss(which == DialogInterface.BUTTON_POSITIVE);
        }
    }

    public interface DialogListener {

        /**
         * 当用户点击了取消按钮,或通过其他方式销毁了{@link com.kelin.apkUpdater.dialog.DefaultDialog}后回调的方法。
         *
         * @param isSure 是否是通过点击确认按钮后销毁的。<code color="blue">true</code>表示是,
         *                 <code color="blue">false</code>则表示不是。
         */
        void onDialogDismiss(boolean isSure);
    }
}
