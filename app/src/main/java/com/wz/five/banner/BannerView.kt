package com.wz.five.banner

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.Scroller
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Banner
 * 继承自RecyclerView
 */
class CustomBanner
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RecyclerView(context, attrs, defStyleAttr) {

    private var isAutoScroll = BannerConfig.IS_AUTO_SCROLL//自动滚动
    private var delayTime = BannerConfig.SCROLL_DELAY_TIME//滚动间隔时间
    private var scrollSpeed = BannerConfig.SCROLL_SPEED//滚动速度（0f-1.0f 速度和值成正比）
    private var defaultImage = R.drawable.img_banner_deault//默认背景图
    private var scaleType = BannerConfig.IMAGE_SCALE_TYPE//图片显示类型

    private val dataList = ArrayList<Any>()//加载图片的path、url、resourceId
    private lateinit var currAdapter: SimpleBannerAdapter//适配器
    private var delayScrollJob: Job? = null
    private var itemListener: OnBannerItemListener? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomBanner, defStyleAttr, 0)
        isAutoScroll = a.getBoolean(R.styleable.CustomBanner_cb_auto_scroll, BannerConfig.IS_AUTO_SCROLL)
        delayTime = a.getInt(R.styleable.CustomBanner_cb_delay_time, BannerConfig.SCROLL_DELAY_TIME)
        scrollSpeed = a.getFloat(R.styleable.CustomBanner_cb_scroll_speed, BannerConfig.SCROLL_SPEED)
        defaultImage = a.getResourceId(R.styleable.CustomBanner_cb_default_image, R.drawable.img_banner_deault)
        scaleType = a.getInt(R.styleable.CustomBanner_cb_scale_type, BannerConfig.IMAGE_SCALE_TYPE)
        a.recycle()

        setHasFixedSize(true)//确定大小，避免重新绘制
        overScrollMode = OVER_SCROLL_NEVER//去除滚动条
        scrollBarSize = 0

        layoutManager = CustomLayoutManager(context, HORIZONTAL, false)
        createAndInitAdapter()

        //自动回滚之中心位置
        BannerSnapHelper().attachToRecyclerView(this)

    }

    /**
     * 创建并初始化适配器
     */
    private fun createAndInitAdapter() {
        if (::currAdapter.isInitialized) return
        currAdapter = SimpleBannerAdapter(context, dataList, getRealScaleType(), itemListener)
        adapter = currAdapter
    }

    /**
     * 图片适配器...后续可追加
     */
    private fun getRealScaleType(): ImageView.ScaleType {
        return when (scaleType) {
            1 -> ImageView.ScaleType.CENTER
            2 -> ImageView.ScaleType.CENTER_CROP
            3 -> ImageView.ScaleType.CENTER_INSIDE
            4 -> ImageView.ScaleType.FIT_XY
            5 -> ImageView.ScaleType.FIT_CENTER
            else -> ImageView.ScaleType.CENTER_CROP
        }
    }

    private  fun cancelKotlinJob(job: Job?) {
        if (job != null && !job.isCancelled) {
            job.cancel()
        }
    }

    override fun onDetachedFromWindow() {
        stopAutoScroll()
        super.onDetachedFromWindow()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isAutoScroll) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> stopAutoScroll()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> startAutoScroll()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 更新数据
     */
    fun updateImageList(list: List<Any>) {
        setImageList(list)
        startAutoScroll()
    }

    /**
     * 初次设置数据
     */
    fun setImageList(list: List<Any>) {
        this.dataList.clear()
        if (list.isEmpty()) {
            this.dataList.add(defaultImage)
        } else {
            this.dataList.addAll(list)
        }
        currAdapter.notifyDataSetChanged()
        if (dataList.size > 1) {
            scrollToPosition(1)
        }
    }

    /**
     * 开启自滚
     */
    fun startAutoScroll() {
        cancelKotlinJob(delayScrollJob)
        delayScrollJob = GlobalScope.launch {
            if (dataList.size <= 1 || !isAutoScroll) {
                return@launch
            }
            while (isActive) {
                delay(delayTime.toLong())
                withContext(Dispatchers.Main) {
                    smoothScrollToPosition(getRealPosition() + 1)
                }
            }
        }
    }

    /**
     * 停止自滚
     */
    fun stopAutoScroll() {
        cancelKotlinJob(delayScrollJob)
    }

    /**
     * 当前item位置index
     */
    fun getRealPosition(): Int {
        if (layoutManager !is LinearLayoutManager) {
            throw RuntimeException("current layoutManager must be LinearLayoutManager!!")
        }
        return (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
    }

    fun isAutoScroll(): Boolean {
        return isAutoScroll
    }

    fun setAutoScroll(isAuto: Boolean) {
        this.isAutoScroll = isAuto
    }

    fun setScrollDelayTime(time: Int) {
        this.delayTime = time
    }

    fun setScrollSpeed(speed: Float) {
        this.scrollSpeed = speed
        if (scrollSpeed < 0) {
            scrollSpeed = 0f
        } else if (scrollSpeed > 1.0f) {
            scrollSpeed = 1.0f
        }
    }

    fun setDefaultImage(@DrawableRes imageRsId: Int) {
        this.defaultImage = imageRsId
    }

    fun setOnBannerItemListener(listener: OnBannerItemListener) {
        this.itemListener = listener
    }

    /**
     * 可自定义速度的滑动器
     */
    inner class CustomLayoutManager(context: Context, orientation: Int, reverseLayout: Boolean) :
            LinearLayoutManager(context, orientation, reverseLayout) {

        override fun smoothScrollToPosition(recyclerView: RecyclerView, state: State, position: Int) {
            val linearSmoothScroller = createLinearSmoothScroller(recyclerView.context)
            linearSmoothScroller.targetPosition = position
            this.startSmoothScroll(linearSmoothScroller)
        }

        private fun createLinearSmoothScroller(context: Context): LinearSmoothScroller {
            return object : LinearSmoothScroller(context) {

                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    //取值范围限定在（10-40）
                    return (10 + (1.0f - scrollSpeed) * 30) / displayMetrics.densityDpi
                }

                override fun calculateTimeForScrolling(dx: Int): Int {
                    return max(100, super.calculateTimeForScrolling(dx))
                }
            }
        }
    }
}

/**
 * Banner默认配置
 */
object BannerConfig {
    const val IS_AUTO_SCROLL = true
    const val IMAGE_SCALE_TYPE = 4
    const val SCROLL_DELAY_TIME = 3000
    const val SCROLL_SPEED = 0.5f
}

/**
 * 默认Adapter
 */
private class SimpleBannerAdapter(
        val context: Context,
        val dataList: ArrayList<Any>,
        val scaleType: ImageView.ScaleType,
        val itemListener: OnBannerItemListener? = null
) : RecyclerView.Adapter<SimpleBannerAdapter.SimpleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        return SimpleViewHolder(createBannerImage())
    }

    override fun getItemCount() = if (dataList.size > 1) dataList.size + 2 else 1

    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        val rightPosition = getRightPosition(position)
        holder.itemView.setOnClickListener {
            itemListener?.onItemClick(rightPosition)
        }
        Glide.with(context).load(dataList[rightPosition]).into(holder.aiv)
    }

    /**
     * 获取正确位置
     */
    private fun getRightPosition(position: Int): Int {
        return if (itemCount <= 1) {
            0
        } else {
            when (position) {
                itemCount - 1 -> 0
                0 -> itemCount - 3
                else -> position - 1
            }
        }
    }

    private fun createBannerImage(): ImageView {
        val aiv = ImageView(context)
        aiv.layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        aiv.scaleType = scaleType
        return aiv
    }

    inner class SimpleViewHolder(val aiv: ImageView) : RecyclerView.ViewHolder(aiv)
}

/**
 * Item监听事件...后续可追加方法
 */
interface OnBannerItemListener {
    fun onItemClick(position: Int)
}

/**
 * RecyclerView类似ViewGroup滑动处理
 */
open class BannerSnapHelper : RecyclerView.OnFlingListener() {

    companion object {
        /**
         * 值越大,滑动速度越慢, 源码默认速度是25F
         */
        const val MILLISECONDS_PER_INCH = 100f
    }

    private lateinit var recyclerView: CustomBanner
    private var horizontalHelper: OrientationHelper? = null
    private var verticalHelper: OrientationHelper? = null
    private lateinit var gravityScroller: Scroller
    private var scrollListener = object : RecyclerView.OnScrollListener() {

        var isScrolled = false

        override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (isScrolled) {
                    isScrolled = false
                    snapToTargetExistingView()
                }
                val currentItem = recyclerView.getRealPosition()
                val count = recyclerView.adapter?.itemCount ?: 0
                if (count < 1) return
                if (currentItem == count - 1) {
                    recyclerView.scrollToPosition(1)
                } else if (currentItem == 0) {
                    recyclerView.scrollToPosition(count - 2)
                }
            }
        }

        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (dx != 0 || dy != 0) {
                isScrolled = true
            }
        }
    }

    @Throws(IllegalStateException::class)
    fun attachToRecyclerView(rv: CustomBanner?) {
        if (::recyclerView.isInitialized) {
            if (recyclerView == rv) return
            destroyCallbacks()
        } else {
            if (rv == null) return
            this.recyclerView = rv
            setupCallbacks()
            gravityScroller = Scroller(recyclerView.context, DecelerateInterpolator())
            snapToTargetExistingView()
        }
    }

    @Throws(IllegalStateException::class)
    private fun setupCallbacks() {
        if (this.recyclerView.onFlingListener != null) {
            throw IllegalStateException("An instance of OnFlingListener already set.")
        } else {
            this.recyclerView.addOnScrollListener(this.scrollListener)
            this.recyclerView.onFlingListener = this
        }
    }

    private fun destroyCallbacks() {
        this.recyclerView.removeOnScrollListener(this.scrollListener)
        this.recyclerView.onFlingListener = null
    }

    /**
     * 滑动到中间停止时的回调
     */
    protected open fun onSnap(snapView: View) {
    }

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        val lm = recyclerView.layoutManager ?: return false
        recyclerView.adapter ?: return false
        val minFlingVelocity = recyclerView.minFlingVelocity
        return (abs(velocityY) > minFlingVelocity || abs(velocityX) > minFlingVelocity)
                && snapFromFling(lm, velocityX, velocityY)
    }

    private fun snapFromFling(lm: RecyclerView.LayoutManager, velocityX: Int, velocityY: Int): Boolean {
        if (lm !is RecyclerView.SmoothScroller.ScrollVectorProvider) return false
        val smoothScroller = createSnapScroller(lm) ?: return false
        val targetPosition = findTargetSnapPosition(lm, velocityX, velocityY)
        if (targetPosition == RecyclerView.NO_POSITION) return false
        smoothScroller.targetPosition = targetPosition
        lm.startSmoothScroll(smoothScroller)
        return true
    }

    /**
     * 创建滑动对象
     */
    private fun createSnapScroller(lm: RecyclerView.LayoutManager): LinearSmoothScroller? {
        if (lm !is RecyclerView.SmoothScroller.ScrollVectorProvider) return null
        return object : LinearSmoothScroller(recyclerView.context) {
            override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
                val snapDistance = calculateDistanceToFinalSnap(recyclerView.layoutManager!!, targetView)
                val dx = snapDistance[0]
                val dy = snapDistance[1]
                val time = calculateTimeForDeceleration(max(abs(dx), abs(dy)))
                if (time > 0) {
                    action.update(dx, dy, time, this.mDecelerateInterpolator)
                }
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return min(100, super.calculateTimeForScrolling(dx))
            }
        }
    }

    /**
     * 计算水平和垂直方向到中心点距离
     */
    private fun calculateDistanceToFinalSnap(lm: RecyclerView.LayoutManager, targetView: View): IntArray {
        val out = IntArray(2)
        if (lm.canScrollHorizontally()) {
            out[0] = distanceToCenter(lm, targetView, getHorizontalHelper(lm))
        } else {
            out[0] = 0
        }
        if (lm.canScrollVertically()) {
            out[1] = distanceToCenter(lm, targetView, getVerticalHelper(lm))
        } else {
            out[1] = 0
        }
        return out
    }

    /**
     * 计算到中心点距离
     */
    private fun distanceToCenter(lm: RecyclerView.LayoutManager, targetView: View, helper: OrientationHelper): Int {
        val childCenter = helper.getDecoratedStart(targetView) + helper.getDecoratedMeasurement(targetView) / 2
        val containerCenter = if (lm.clipToPadding) {
            helper.startAfterPadding + helper.totalSpace / 2
        } else {
            helper.end / 2
        }
        return childCenter - containerCenter
    }

    /**
     * 水平滑动辅助
     */
    private fun getHorizontalHelper(lm: RecyclerView.LayoutManager): OrientationHelper {
        if (horizontalHelper == null || horizontalHelper?.layoutManager != lm) {
            horizontalHelper = OrientationHelper.createHorizontalHelper(lm)
        }
        return horizontalHelper!!
    }

    /**
     * 垂直滑动辅助
     */
    private fun getVerticalHelper(lm: RecyclerView.LayoutManager): OrientationHelper {
        if (verticalHelper == null || verticalHelper?.layoutManager != lm) {
            verticalHelper = OrientationHelper.createVerticalHelper(lm)
        }
        return verticalHelper!!
    }

    /**
     * 查找目标滑动位置
     */
    private fun findTargetSnapPosition(lm: RecyclerView.LayoutManager, velocityX: Int, velocityY: Int): Int {
        val itemCount = lm.itemCount
        if (itemCount == 0) return RecyclerView.NO_POSITION

        val mostStartChild = (if (lm.canScrollVertically()) {
            findStartView(lm, getVerticalHelper(lm))
        } else {
            findStartView(lm, getHorizontalHelper(lm))
        }) ?: return RecyclerView.NO_POSITION

        val centerPosition = lm.getPosition(mostStartChild)
        if (centerPosition == RecyclerView.NO_POSITION) return RecyclerView.NO_POSITION

        val forwardDirection = if (lm.canScrollHorizontally()) {
            velocityX > 0
        } else {
            velocityY > 0
        }

        var reverseLayout = false
        if (lm is RecyclerView.SmoothScroller.ScrollVectorProvider) {
            val vectorForEnd = lm.computeScrollVectorForPosition(itemCount - 1)
            if (vectorForEnd != null) {
                reverseLayout = vectorForEnd.x < 0f || vectorForEnd.y < 0f
            }
        }
        return if (reverseLayout) {
            if (forwardDirection) centerPosition - 1 else centerPosition
        } else {
            if (forwardDirection) centerPosition + 1 else centerPosition
        }
    }

    /**
     * 查找开始View
     */
    private fun findStartView(lm: RecyclerView.LayoutManager, helper: OrientationHelper): View? {
        val childCount = lm.childCount
        if (childCount == 0) return null
        var closestChild: View? = null
        var startClosest = Int.MAX_VALUE
        for (index in 0 until childCount) {
            val child = lm.getChildAt(index)
            val childStart = helper.getDecoratedStart(child)
            if (childStart < startClosest) {
                startClosest = childStart
                closestChild = child
            }
        }
        return closestChild
    }

    /**
     * 定位到存在的View
     */
    fun snapToTargetExistingView() {
        val lm = recyclerView.layoutManager ?: return
        val snapView = findSnapView(lm) ?: return
        val snapDistance = calculateDistanceToFinalSnap(lm, snapView)
        if (snapDistance[0] != 0 || snapDistance[1] != 0) {//X或Y轴仍有偏移,此处需要滑动较正
            recyclerView.smoothScrollBy(snapDistance[0], snapDistance[1])
        } else {//在滑动到当前item中心位置的时候,检测是否需要调当前RecyclerView的位置
            onSnap(snapView)
        }
    }

    /**
     * 查找滑动的View
     */
    private fun findSnapView(lm: RecyclerView.LayoutManager): View? {
        return if (lm.canScrollVertically()) {
            findCenterView(lm, getVerticalHelper(lm))
        } else {
            if (lm.canScrollHorizontally()) {
                findCenterView(lm, getHorizontalHelper(lm))
            } else {
                null
            }
        }
    }

    /**
     * 查找中心View
     */
    private fun findCenterView(lm: RecyclerView.LayoutManager, helper: OrientationHelper): View? {
        val childCount = lm.childCount
        if (childCount == 0) return null
        val center = if (lm.clipToPadding) {
            helper.startAfterPadding + helper.totalSpace / 2
        } else {
            helper.end / 2
        }
        var closestChild: View? = null
        var absClosest = Int.MAX_VALUE
        for (index in 0 until childCount) {
            val child = lm.getChildAt(index)
            val childCenter = helper.getDecoratedStart(child) + helper.getDecoratedMeasurement(child) / 2
            val absDistance = abs(childCenter - center)
            if (absDistance < absClosest) {
                absClosest = absDistance
                closestChild = child
            }
        }
        return closestChild
    }
}