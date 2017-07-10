package com.chengshi.apkUpdater.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chengshi.apkUpdater.R;
import com.chengshi.apkUpdater.callback.DialogListener;

import java.util.Locale;

/**
 * 描述 更新信息对话框。
 * 创建人 kelin
 * 创建时间 2017/7/5  下午3:59
 * 版本 v 1.0.0
 */

public class DefaultDialog {

    private final Context mContext;
    private final DialogConfig mConfig;
    private AlertDialog mDialog;
    private ProgressBar mProgressBar;
    private TextView mPercentageView;

    public DefaultDialog(Context context, DialogConfig config) {
        mContext = context;
        mConfig = config;
    }


    /**
     * 显示对话框。
     */
    public void show() {
        show(null);
    }

    /**
     * 显示对话框。
     *
     * @param listener 对话框的监听。
     */
    @SuppressLint("InflateParams")
    public void show(final DialogListener listener) {
        //构建AlertDialog。
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        if (mConfig instanceof DownloadDialogConfig) {
            View contentView = LayoutInflater.from(mContext).inflate(R.layout.com_cheng_shi_layout_progress_layout, null);
            mProgressBar = (ProgressBar) contentView.findViewById(R.id.progress);
            mPercentageView = (TextView) contentView.findViewById(R.id.tv_percentage);
            builder.setView(contentView);
            if (!mConfig.isForceUpdate()) {
                builder.setPositiveButton("悄悄的下载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialog.dismiss();
                    }
                });
            }
        } else {
            DialogInterface.OnClickListener onClickListener = null;
            if (listener != null) {
                onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean isSure = which == DialogInterface.BUTTON_POSITIVE;
                        listener.onDialogDismiss(isSure);
                    }
                };
            }
            builder.setPositiveButton("立刻安装", onClickListener);  //设置确定按钮
            if (!mConfig.isForceUpdate()) {
                builder.setCancelable(true).setNegativeButton("稍候安装", onClickListener); //如果不是强制更新则设置取消按钮
            }
        }

        builder.setIcon(mConfig.getIcon()) //设置图标
                .setTitle(mConfig.getTitle()) //设置标题
                .setMessage(mConfig.getMessage());  //设置内容
        mDialog = builder.create();
        mDialog.show();
    }

    public void updateDownLoadsProgress(int percentage) {
        mProgressBar.setProgress(percentage);
        mPercentageView.setText(String.format(Locale.CHINA, "%d %%", percentage));
        if (percentage == mProgressBar.getMax()) {
            mDialog.dismiss();
        }
    }
}
