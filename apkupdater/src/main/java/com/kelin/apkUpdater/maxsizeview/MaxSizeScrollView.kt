package com.kelin.apkUpdater.maxsizeview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import com.kelin.apkUpdater.R

/**
 * **描述: ** 可以指定最大高度的滚动控件。
 *
 * **创建人: ** kelin
 *
 * **创建时间: ** 2018/6/7  下午3:19
 *
 * **版本: ** v 1.0.0
 */
class MaxSizeScrollView @SuppressLint("CustomViewStyleable")  @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ScrollView(context, attrs, defStyleAttr), MaxSizeView {
    private var maxHeight = 0
    private var maxWidth = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        MaxSizeView.measureSize(maxWidth, maxHeight, widthMeasureSpec, heightMeasureSpec, this)
    }

    @SuppressLint("WrongCall")
    override fun onRealMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun setMaxWidth(maxWidth: Int) {
        this.maxWidth = maxWidth
    }

    override fun setMaxHeight(maxHeight: Int) {
        this.maxHeight = maxHeight
    }

    init {
        MaxSizeView.parserStyleable(context.obtainStyledAttributes(attrs, R.styleable.MaxSizeView), this)
    }
}