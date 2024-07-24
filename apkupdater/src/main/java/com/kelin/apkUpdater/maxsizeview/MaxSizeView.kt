package com.kelin.apkUpdater.maxsizeview

import android.content.res.TypedArray
import android.view.View
import com.kelin.apkupdater.R

/**
 * **描述: ** 可以限定最大尺寸的控件。
 *
 * **创建人: ** kelin
 *
 * **创建时间: ** 2018/6/14  上午11:11
 *
 * **版本: ** v 1.0.0
 */
interface MaxSizeView {
    fun setMaxWidth(maxWidth: Int)
    fun setMaxHeight(maxHeight: Int)
    fun onRealMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)

    companion object {
        fun parserStyleable(typedArray: TypedArray?, maxSizeView: MaxSizeView, recycle: Boolean = true) {
            if (typedArray != null) {
                maxSizeView.setMaxWidth(typedArray.getDimension(R.styleable.MaxSizeView_android_maxWidth, 0f).toInt())
                maxSizeView.setMaxHeight(typedArray.getDimension(R.styleable.MaxSizeView_android_maxHeight, 0f).toInt())
                if (recycle) {
                    typedArray.recycle()
                }
            }
        }

        fun measureSize(maxWidth: Int, maxHeight: Int, widthMeasureSpec: Int, heightMeasureSpec: Int, maxSizeView: MaxSizeView) {
            val width = if (maxWidth > 0) {
                View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST)
            } else {
                widthMeasureSpec
            }
            val height = if (maxHeight > 0) {
                View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
            } else {
                heightMeasureSpec
            }
            maxSizeView.onRealMeasure(width, height)
        }
    }
}