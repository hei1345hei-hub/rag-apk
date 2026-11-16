package com.rag.interview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat

class LiquidGlassDockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class TabItem(
        val key: String,
        val name: String,
        val path: String,
        val iconRes: Int
    )

    val tabs = listOf(
        TabItem("dashboard", "首页", "/pages/dashboard/dashboard", R.drawable.ic_tab_home),
        TabItem("chat", "AI问答", "/pages/chat/chat", R.drawable.ic_tab_chat),
        TabItem("interview", "模拟面试", "/pages/interview/interview", R.drawable.ic_tab_interview),
        TabItem("question", "题库精选", "/pages/question/question", R.drawable.ic_tab_question),
        TabItem("user", "个人中心", "/pages/user/user", R.drawable.ic_tab_user)
    )

    var onTabSelectedListener: ((TabItem) -> Unit)? = null
    var onShaderParamUpdateListener: ((dockX: Float, dockY: Float, dockW: Float, dockH: Float, pillCenterX: Float, pillCenterY: Float, pillW: Float, pillH: Float, pressProgress: Float) -> Unit)? = null

    var activeIndex: Int = 0
        private set

    private var currentPillOffset: Float = 0f
    private var isDragging: Boolean = false
    private var dragStartX: Float = 0f
    private var dragStartOffset: Float = 0f
    private var isDragMoved: Boolean = false

    // Kyant DampedDragAnimation dynamic scale parameters
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var pressProgress: Float = 0f
    private var touchX: Float = 0f
    private var touchY: Float = 0f

    private var animator: ValueAnimator? = null
    private var pressAnimator: ValueAnimator? = null

    // Paints
    private val containerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(135, 252, 252, 252)
    }

    private val outerShadowPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(16, 15, 23, 42)
    }

    private val outerShadowPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(8, 15, 23, 42)
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
    }

    private val rimHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.2f)
    }

    private val pillShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(25, 0, 0, 0)
    }

    private val pillSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(22, 0, 0, 0) // Kyant exact smoky translucent capsule
    }

    private val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = dpToPx(11f)
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Pre-rendered Vector Bitmaps for 120Hz smooth rendering
    private val iconBitmaps = mutableMapOf<Int, Bitmap>()

    init {
        tabs.forEach { tab ->
            loadVectorBitmap(tab.iconRes)
        }
    }

    private fun loadVectorBitmap(resId: Int): Bitmap? {
        if (!iconBitmaps.containsKey(resId)) {
            val drawable: Drawable? = ContextCompat.getDrawable(context, resId)
            if (drawable != null) {
                val size = dpToPx(24f).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                iconBitmaps[resId] = bmp
            }
        }
        return iconBitmaps[resId]
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        post { updateShaderParams() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        currentPillOffset = activeIndex * getTabWidth()
        post { updateShaderParams() }

        // Top specular caustic arc highlight
        rimHighlightPaint.shader = LinearGradient(
            w * 0.15f, 0f, w * 0.85f, 0f,
            intArrayOf(Color.TRANSPARENT, Color.argb(220, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // Kyant 45-degree directional highlight on outer border
        borderPaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(
                Color.argb(210, 255, 255, 255),
                Color.argb(120, 255, 255, 255),
                Color.argb(40, 255, 255, 255),
                Color.argb(15, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun getTabWidth(): Float {
        val usableW = (width - paddingLeft - paddingRight).coerceAtLeast(1)
        return usableW.toFloat() / tabs.size
    }

    fun selectTab(index: Int, animate: Boolean = true) {
        if (index < 0 || index >= tabs.size) return
        activeIndex = index
        val targetOffset = index * getTabWidth()

        if (animate) {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(currentPillOffset, targetOffset).apply {
                duration = 380
                interpolator = OvershootInterpolator(1.2f)
                addUpdateListener { va ->
                    currentPillOffset = va.animatedValue as Float
                    updateShaderParams()
                    invalidate()
                }
                start()
            }
        } else {
            currentPillOffset = targetOffset
            updateShaderParams()
            invalidate()
        }
    }

    fun selectTabByPath(path: String) {
        val idx = tabs.indexOfFirst { it.path == path }
        if (idx >= 0 && idx != activeIndex) {
            selectTab(idx, true)
        }
    }

    fun updateShaderParams() {
        if (width <= 0 || height <= 0) return
        val dockX = x
        val dockY = y
        val dockW = width.toFloat()
        val dockH = height.toFloat()

        val tabW = getTabWidth()
        val pillMarginH = dpToPx(4f)
        val pillMarginV = dpToPx(6f)
        val pillW = tabW - pillMarginH * 2
        val pillH = dockH - pillMarginV * 2

        val pillCenterX = dockX + paddingLeft + currentPillOffset + tabW * 0.5f
        val pillCenterY = dockY + dockH * 0.5f

        onShaderParamUpdateListener?.invoke(
            dockX, dockY, dockW, dockH,
            pillCenterX, pillCenterY, pillW, pillH, pressProgress
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tabW = getTabWidth()
        val maxOffset = (tabs.size - 1) * tabW

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                dragStartX = event.x
                dragStartOffset = currentPillOffset
                isDragging = true
                isDragMoved = false
                animator?.cancel()

                // Kyant press animation (pressProgress -> 1f, pressedScale -> 1.39x)
                pressAnimator?.cancel()
                pressAnimator = ValueAnimator.ofFloat(pressProgress, 1f).apply {
                    duration = 180
                    addUpdateListener { va ->
                        pressProgress = va.animatedValue as Float
                        updateShaderParams()
                        invalidate()
                    }
                    start()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    touchX = event.x
                    touchY = event.y
                    val deltaX = event.x - dragStartX
                    if (Math.abs(deltaX) > dpToPx(6f)) {
                        isDragMoved = true
                    }
                    val target = dragStartOffset + deltaX
                    currentPillOffset = target.coerceIn(-tabW * 0.35f, maxOffset + tabW * 0.35f)

                    // Kyant DampedDrag velocity scaling
                    val velocityStretch = (Math.abs(deltaX) / (tabW * 2.2f)).coerceAtMost(0.25f)
                    scaleX = (1f + velocityStretch) * (1f + 0.12f * pressProgress)
                    scaleY = (1f - velocityStretch * 0.4f) * (1f + 0.08f * pressProgress)

                    updateShaderParams()
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    val targetIndex = if (isDragMoved) {
                        Math.round(currentPillOffset / tabW).coerceIn(0, tabs.size - 1)
                    } else {
                        val clickedIndex = ((event.x - paddingLeft) / tabW).toInt().coerceIn(0, tabs.size - 1)
                        clickedIndex
                    }

                    // Kyant release animation
                    pressAnimator?.cancel()
                    pressAnimator = ValueAnimator.ofFloat(pressProgress, 0f).apply {
                        duration = 240
                        addUpdateListener { va ->
                            pressProgress = va.animatedValue as Float
                            scaleX = 1f
                            scaleY = 1f
                            updateShaderParams()
                            invalidate()
                        }
                        start()
                    }

                    scaleX = 1f
                    scaleY = 1f
                    selectTab(targetIndex, true)
                    onTabSelectedListener?.invoke(tabs[targetIndex])
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = h * 0.5f

        // 0. Soft ambient drop shadow behind capsule (Kyant Shadow)
        val s1 = RectF(dpToPx(2f), dpToPx(3f), w - dpToPx(2f), h + dpToPx(3f))
        canvas.drawRoundRect(s1, radius, radius, outerShadowPaint1)
        val s2 = RectF(dpToPx(4f), dpToPx(6f), w - dpToPx(4f), h + dpToPx(6f))
        canvas.drawRoundRect(s2, radius, radius, outerShadowPaint2)

        // 1. Draw outer dock capsule surface & border (Kyant onDrawSurface containerColor)
        val dockRect = RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f)
        canvas.drawRoundRect(dockRect, radius, radius, containerPaint)
        canvas.drawRoundRect(dockRect, radius, radius, borderPaint)

        // 2. Interactive Spotlight tracking touch (Kyant InteractiveHighlight)
        if (isDragging) {
            val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    touchX, touchY, dpToPx(55f),
                    intArrayOf(Color.argb(30, 255, 255, 255), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(dockRect, radius, radius, spotPaint)
        }

        // 3. Top specular caustic rim reflection
        val rimPath = Path().apply {
            arcTo(RectF(1f, 1f, w - 1f, h - 1f), 195f, 150f, true)
        }
        canvas.drawPath(rimPath, rimHighlightPaint)

        // 4. Active liquid pill indicator (Kyant LiquidBottomTabs Box)
        val tabW = getTabWidth()
        val pillMarginH = dpToPx(4f)
        val pillMarginV = dpToPx(6f)
        val pillW = tabW - pillMarginH * 2
        val pillH = h - pillMarginV * 2
        val pillRadius = pillH * 0.5f

        val pillLeft = paddingLeft + currentPillOffset + pillMarginH
        val pillTop = pillMarginV
        val pillRight = pillLeft + pillW
        val pillBottom = pillTop + pillH

        canvas.save()
        val pillCenterX = pillLeft + pillW * 0.5f
        val pillCenterY = pillTop + pillH * 0.5f
        canvas.scale(scaleX, scaleY, pillCenterX, pillCenterY)

        // Pill shadow
        val shadowRect = RectF(pillLeft, pillTop + dpToPx(1.5f), pillRight, pillBottom + dpToPx(1.5f))
        canvas.drawRoundRect(shadowRect, pillRadius, pillRadius, pillShadowPaint)

        // Kyant exact onDrawSurface: translucent smoky glass tint (Color.Black.copy(0.08f))
        val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillSurfacePaint)

        // Kyant InnerShadow & Specular Rim Highlight on the Pill
        pillBorderPaint.shader = LinearGradient(
            pillLeft, pillTop, pillLeft, pillBottom,
            intArrayOf(Color.argb(160, 255, 255, 255), Color.argb(40, 255, 255, 255)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillBorderPaint)
        canvas.restore()

        // 5. Draw 5 Tab Items with Backdrop Catalog Electric Blue & Slate Charcoal
        val activeBlue = Color.parseColor("#0088FF") // Kyant Electric Blue
        val inactiveSlate = Color.parseColor("#475569") // Clean Dark Slate

        for (i in tabs.indices) {
            val tab = tabs[i]
            val isActive = (i == activeIndex)
            val tabCenterX = paddingLeft + i * tabW + tabW * 0.5f

            val iconSize = dpToPx(22f)
            val iconTop = h * 0.18f
            val bmp = iconBitmaps[tab.iconRes]

            if (bmp != null) {
                val dstRect = RectF(
                    tabCenterX - iconSize * 0.5f,
                    iconTop,
                    tabCenterX + iconSize * 0.5f,
                    iconTop + iconSize
                )
                if (isActive) {
                    iconPaint.colorFilter = PorterDuffColorFilter(activeBlue, PorterDuff.Mode.SRC_IN)
                } else {
                    iconPaint.colorFilter = PorterDuffColorFilter(inactiveSlate, PorterDuff.Mode.SRC_IN)
                }
                canvas.drawBitmap(bmp, null, dstRect, iconPaint)
            }

            textPaint.color = if (isActive) activeBlue else inactiveSlate
            textPaint.typeface = if (isActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textPaint.textSize = if (isActive) dpToPx(11.5f) else dpToPx(11f)
            val textY = h * 0.82f
            canvas.drawText(tab.name, tabCenterX, textY, textPaint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
