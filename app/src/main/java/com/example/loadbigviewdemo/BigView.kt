package com.example.loadbigviewdemo

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import java.io.InputStream

/**
 * @author Chuck
 * @description: 加载长图-采用分段加载的方式
 * @date :2020-07-24 11:22
 */
class BigView : View, GestureDetector.OnGestureListener, View.OnTouchListener {


    private var mImageHeight: Int = 0
    private var mImageWidth: Int = 0
    private var mRect: Rect? = null
    private var mOptions: BitmapFactory.Options
    private var mGestureDetector: GestureDetector
    private var mScroller: Scroller

    private var mDecoder: BitmapRegionDecoder? = null

    private var mViewWidth: Int = 0
    private var mViewHeight: Int = 0
    private var mScale: Float = 0.0f

    private var mBitmap: Bitmap? = null //复用的bitmap


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {

        /**
         * 第1步：设置用到的成员变量
         */
        mRect = Rect()
        //内存复用
        mOptions = BitmapFactory.Options()
        //手势监听
        mGestureDetector = GestureDetector(context, this)
        //滚动类
        mScroller = Scroller(context)

        setOnTouchListener(this)

    }

    /**
     * 第2步 设置图片,得到用到的图片信息
     */
    fun setImage(inputStream: InputStream) {
        //获取图片宽高 注意不能将图片整个加载到内存里面 mOptions.inJustDecodeBounds  成对出现
        mOptions.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, mOptions)
        mImageWidth = mOptions.outWidth
        mImageHeight = mOptions.outHeight

        //开启复用
        mOptions.inMutable = true
        //设置格式为RGB-565  一个像素2个字节
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565

        mOptions.inJustDecodeBounds = false

        //区域解码器
        mDecoder = BitmapRegionDecoder.newInstance(inputStream, false)

        requestLayout()//开始绘制

    }

    /**
     * 第3步 开始测量了，得到View的宽高，测量出具体加载的图片缩放的比例
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mViewWidth = measuredWidth
        mViewHeight = measuredHeight
        //确认加载的区域
        mRect?.left = 0
        mRect?.top = 0
        mRect?.right = mImageWidth
        //计算缩放因子
        mScale = mViewWidth / mImageWidth.toFloat()
        mRect?.bottom = (mViewHeight / mScale).toInt()
    }

    /**
     * 第4步 画出具体的内容了
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mDecoder == null) {
            return
        }
        //开始内存复用
        mOptions.inBitmap = mBitmap
        //指定解码区域
        mBitmap = mDecoder!!.decodeRegion(mRect, mOptions)
        //得到一个矩阵进行缩放，相当于得到View的大小
        val matrix = Matrix()
        matrix.setScale(mScale, mScale)

        mBitmap?.let { canvas?.drawBitmap(it, matrix, null) }
    }


    /**
     * 第5步  处理点击事件
     */
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        //将点击事件传递给手势监听
        return mGestureDetector.onTouchEvent(event)
    }

    /**
     * 第6步 手按下去
     */
    override fun onDown(e: MotionEvent?): Boolean {
        //手按下去，如果没有停止，就强行停止
        if (!mScroller.isFinished) {
            mScroller.forceFinished(true)
        }

        return true //true 继续执行后续操作
    }

    /**
     * 第7步 处理滑动事件
     */
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        //上下移动的时候 mRect需要改变现实的区域
        mRect?.offset(0, distanceY.toInt())
        //移动时，滑动到顶部,底部的情况
        if (mRect?.bottom!! > mImageHeight) {
            mRect?.top = mImageHeight - (mViewHeight / this.mScale).toInt()
            mRect?.bottom = mImageHeight
        }
        if (mRect?.top!! < 0) {
            mRect?.top = 0
            mRect?.bottom = (mViewHeight / this.mScale).toInt()
        }
        invalidate()
        return false
    }

    /**
     * 第8步 惯性处理
     */
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        mScroller.fling(
            0,
            mRect?.top!!,
            0,
            -velocityY.toInt(),
            0,
            0,
            0,
            (mImageHeight - mViewHeight / this.mScale).toInt()
        )

        return false
    }

    /**
     * 处理计算结果
     */
    override fun computeScroll() {
        super.computeScroll()
        if (mScroller.isFinished) {
            return
        }
        if (mScroller.computeScrollOffset()) {//滚动没还没有结束
            mRect?.top = mScroller.currY
            mRect?.bottom = mRect?.top!! + (mViewHeight / mScale).toInt()
            invalidate()
        }
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }


    override fun onLongPress(e: MotionEvent?) {
    }

}