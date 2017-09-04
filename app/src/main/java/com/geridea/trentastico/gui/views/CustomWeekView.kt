package com.geridea.trentastico.gui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.text.*
import android.text.format.DateFormat
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.OverScroller
import com.alamkanak.weekview.DateTimeInterpreter
import com.alamkanak.weekview.WeekViewEvent
import com.geridea.trentastico.utils.UIUtils
import com.geridea.trentastico.utils.time.CalendarUtils
import com.geridea.trentastico.utils.time.CalendarUtils.debuggableToday
import com.geridea.trentastico.utils.time.WeekDayTime
import com.geridea.trentastico.utils.time.WeekInterval
import com.geridea.trentastico.utils.time.WeekTime
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://alamkanak.github.io/
 *
 * Modified by Slava because why not on 12/03/2017.
 * Website:  I still don't have one! :(
 */
open class CustomWeekView @JvmOverloads constructor(private val mContext: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(mContext, attrs, defStyleAttr) {

    /**
     * Introduced settings:
     */
    private val mStartingHour = 7
    private val mEndingHour = 20
    private val mNumHoursToDisplay = mEndingHour - mStartingHour + 1

    private val mHoursTextColor = Color.BLACK
    private val mDisabledBackgroundColor = 0xFF888888.toInt()
    private val mPastDayBackgroundColor = 0xFFD5D5D5.toInt()

    private val enabledIntervals = ArrayList<WeekInterval>()

    protected fun addEvents(weekViewEvents: List<WeekViewEvent>) {
        sortAndCacheEvents(weekViewEvents)
        recalculatePositionsOfEvents()
        notifyDatasetChanged()
    }

    /**
     * Purges all events and enabled intervals from this WeekView and invalidates it.
     */
    fun clear() {
        mEventRects!!.clear()
        enabledIntervals.clear()

        invalidate()
    }

    protected fun addEnabledInterval(interval: WeekInterval) {
        enabledIntervals.add(interval)
        postInvalidate()
    }

    fun setEventsTextSize(fontSizeInSP: Int) {
        mEventTextPaint!!.textSize = UIUtils.convertSpToPixels(fontSizeInSP.toFloat(), context).toFloat()
        postInvalidate()
    }

    private enum class Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    private var mTimeTextPaint: Paint? = null
    private var mTimeTextWidth: Float = 0.toFloat()
    private var mTimeTextHeight: Float = 0.toFloat()
    private var mHeaderTextPaint: Paint? = null
    private var mHeaderTextHeight: Float = 0.toFloat()
    private var mGestureDetector: GestureDetectorCompat? = null
    private var mScroller: OverScroller? = null
    private val mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection: CustomWeekView.Direction = CustomWeekView.Direction.NONE
    private var mHeaderBackgroundPaint: Paint? = null
    private var mWidthPerDay: Float = 0.toFloat()
    private var mDayBackgroundPaint: Paint? = null
    private var mPastDayBackgroundPaint: Paint? = null
    private var mDisabledBackgroundPaint: Paint? = null
    private var mHourSeparatorPaint: Paint? = null
    private var mHeaderMarginBottom: Float = 0.toFloat()
    private var mTodayBackgroundPaint: Paint? = null
    private var mFutureBackgroundPaint: Paint? = null
    private var mPastBackgroundPaint: Paint? = null
    private var mFutureWeekendBackgroundPaint: Paint? = null
    private var mPastWeekendBackgroundPaint: Paint? = null
    private var mNowLinePaint: Paint? = null
    private var mTodayHeaderTextPaint: Paint? = null
    private var mEventBackgroundPaint: Paint? = null
    private var mHeaderColumnWidth: Float = 0.toFloat()
    private var mEventRects: MutableList<CustomWeekView.EventRect>? = ArrayList()
    private var mEventTextPaint: TextPaint? = null
    private var mHeaderColumnBackgroundPaint: Paint? = null
    private var mCurrentFlingDirection: CustomWeekView.Direction = CustomWeekView.Direction.NONE
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mIsZooming: Boolean = false
    val firstVisibleDay = debuggableToday
    private var mDefaultEventColor: Int = 0
    private var mMinimumFlingVelocity = 0
    private var mScaledTouchSlop = 0
    // Attributes and their default values.
    private var mHourHeight = 50
    private var mNewHourHeight = -1
    private var mMinHourHeight = 0 //no minimum specified (will be dynamic, based on screen)
    private var mEffectiveMinHourHeight = mMinHourHeight //compensates for the fact that you can't keep zooming out.
    private var mMaxHourHeight = 250
    private var mColumnGap = 10
    private var mFirstDayOfWeek = Calendar.MONDAY
    private var mTextSize = 12
    private var mHeaderColumnPadding = 10
    private var mHeaderColumnTextColor = Color.BLACK
    private var mNumberOfVisibleDays = 3
    private var mHeaderRowPadding = 10
    private var mHeaderRowBackgroundColor = Color.WHITE
    private var mDayBackgroundColor = Color.rgb(245, 245, 245)
    private var mPastBackgroundColor = Color.rgb(227, 227, 227)
    private var mFutureBackgroundColor = Color.rgb(245, 245, 245)
    private var mPastWeekendBackgroundColor = 0
    private var mFutureWeekendBackgroundColor = 0
    private var mNowLineColor = Color.rgb(102, 102, 102)
    private var mNowLineThickness = 5
    private var mHourSeparatorColor = Color.rgb(60, 60, 60)
    private var mTodayBackgroundColor = Color.rgb(239, 247, 254)
    private var mHourSeparatorHeight = 2
    private var mTodayHeaderTextColor = Color.rgb(39, 137, 228)
    private var mEventTextSize = DEFAULT_EVENT_FONT_SIZE
    private var mEventTextColor = Color.BLACK
    private var mEventPadding = 8
    private var mHeaderColumnBackgroundColor = Color.WHITE
    private var mIsFirstDraw = true
    private var mAreDimensionsInvalid = true
    private var mDayNameLength = LENGTH_LONG
    private var mOverlappingEventGap = 0
    private var mEventMarginVertical = 0
    private var mXScrollingSpeed = 1f
    private var mScrollToDay: Calendar? = null
    private var mScrollToHour = -1.0
    private var mEventCornerRadius = 0
    private var mShowDistinctWeekendColor = false
    private var mShowNowLine = false
    private var mShowDistinctPastFutureColor = false
    private var mHorizontalFlingEnabled = true
    private var mVerticalFlingEnabled = true

    // Listeners.
    var eventClickListener: CustomWeekView.EventClickListener? = null
        private set
    var eventLongPressListener: CustomWeekView.EventLongPressListener? = null
    var emptyViewClickListener: CustomWeekView.EmptyViewClickListener? = null
    var emptyViewLongPressListener: CustomWeekView.EmptyViewLongPressListener? = null
    private var mDateTimeInterpreter: DateTimeInterpreter? = null
    private var scrollListener: CustomWeekView.ScrollListener? = null

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            goToNearestOrigin()
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Check if view is zoomed.
            if (mIsZooming)
                return true

            when (mCurrentScrollDirection) {
                CustomWeekView.Direction.NONE -> {
                    // Allow scrolling only in one direction.
                    mCurrentScrollDirection = if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        if (distanceX > 0) {
                            CustomWeekView.Direction.LEFT
                        } else {
                            CustomWeekView.Direction.RIGHT
                        }
                    } else {
                        CustomWeekView.Direction.VERTICAL
                    }
                }
                CustomWeekView.Direction.LEFT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX < -mScaledTouchSlop) {
                        mCurrentScrollDirection = CustomWeekView.Direction.RIGHT
                    }
                }
                CustomWeekView.Direction.RIGHT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX > mScaledTouchSlop) {
                        mCurrentScrollDirection = CustomWeekView.Direction.LEFT
                    }
                }
                else -> Unit
            }

            // Calculate the new origin after scroll.
            when (mCurrentScrollDirection) {
                CustomWeekView.Direction.LEFT, CustomWeekView.Direction.RIGHT -> {
                    mCurrentOrigin.x -= distanceX * mXScrollingSpeed
                    ViewCompat.postInvalidateOnAnimation(this@CustomWeekView)
                }
                CustomWeekView.Direction.VERTICAL -> {
                    mCurrentOrigin.y -= distanceY
                    ViewCompat.postInvalidateOnAnimation(this@CustomWeekView)
                }
                else -> Unit
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (mIsZooming)
                return true

            if (mCurrentFlingDirection == CustomWeekView.Direction.LEFT && !mHorizontalFlingEnabled ||
                    mCurrentFlingDirection == CustomWeekView.Direction.RIGHT && !mHorizontalFlingEnabled ||
                    mCurrentFlingDirection == CustomWeekView.Direction.VERTICAL && !mVerticalFlingEnabled) {
                return true
            }

            mScroller!!.forceFinished(true)

            mCurrentFlingDirection = mCurrentScrollDirection
            when (mCurrentFlingDirection) {
                CustomWeekView.Direction.LEFT, CustomWeekView.Direction.RIGHT -> mScroller!!.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (velocityX * mXScrollingSpeed).toInt(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, (-((mHourHeight * mNumHoursToDisplay).toFloat() + mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom + mTimeTextHeight / 2 - height)).toInt(), 0)
                CustomWeekView.Direction.VERTICAL -> mScroller!!.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), 0, velocityY.toInt(), Integer.MIN_VALUE, Integer.MAX_VALUE, (-((mHourHeight * mNumHoursToDisplay).toFloat() + mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom + mTimeTextHeight / 2 - height)).toInt(), 0)
                else -> Unit
            }

            ViewCompat.postInvalidateOnAnimation(this@CustomWeekView)
            return true
        }


        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // If the tap was on an event then trigger the callback.
            if (mEventRects != null && eventClickListener != null) {
                val reversedEventRects = mEventRects
                Collections.reverse(reversedEventRects!!)
                for (event in reversedEventRects) {
                    if (event.rectF != null && e.x > event.rectF!!.left && e.x < event.rectF!!.right && e.y > event.rectF!!.top && e.y < event.rectF!!.bottom) {
                        eventClickListener!!.onEventClick(event.originalEvent, event.rectF)
                        playSoundEffect(SoundEffectConstants.CLICK)
                        return super.onSingleTapConfirmed(e)
                    }
                }
            }

            // If the tap was on in an empty space, then trigger the callback.
            if (emptyViewClickListener != null && e.x > mHeaderColumnWidth && e.y > mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom) {
                val selectedTime = getTimeFromPoint(e.x, e.y)
                if (selectedTime != null) {
                    playSoundEffect(SoundEffectConstants.CLICK)
                    emptyViewClickListener!!.onEmptyViewClicked(selectedTime)
                }
            }

            return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

            if (eventLongPressListener != null && mEventRects != null) {
                val reversedEventRects = mEventRects
                Collections.reverse(reversedEventRects!!)
                for (event in reversedEventRects) {
                    if (event.rectF != null && e.x > event.rectF!!.left && e.x < event.rectF!!.right && e.y > event.rectF!!.top && e.y < event.rectF!!.bottom) {
                        eventLongPressListener!!.onEventLongPress(event.originalEvent, event.rectF)
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        return
                    }
                }
            }

            // If the tap was on in an empty space, then trigger the callback.
            if (emptyViewLongPressListener != null && e.x > mHeaderColumnWidth && e.y > mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom) {
                val selectedTime = getTimeFromPoint(e.x, e.y)
                if (selectedTime != null) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    emptyViewLongPressListener!!.onEmptyViewLongPress(selectedTime)
                }
            }
        }
    }

    init {

        // Get the attribute values (if any).
        val a = mContext.theme.obtainStyledAttributes(attrs, com.alamkanak.weekview.R.styleable.WeekView, 0, 0)
        try {
            mFirstDayOfWeek = a.getInteger(com.alamkanak.weekview.R.styleable.WeekView_firstDayOfWeek, mFirstDayOfWeek)
            mHourHeight = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_hourHeight, mHourHeight)
            mMinHourHeight = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_minHourHeight, mMinHourHeight)
            mEffectiveMinHourHeight = mMinHourHeight
            mMaxHourHeight = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_maxHourHeight, mMaxHourHeight)
            mTextSize = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize.toFloat(), mContext.resources.displayMetrics).toInt())
            mHeaderColumnPadding = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_headerColumnPadding, mHeaderColumnPadding)
            mColumnGap = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_columnGap, mColumnGap)
            mHeaderColumnTextColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_headerColumnTextColor, mHeaderColumnTextColor)
            mNumberOfVisibleDays = a.getInteger(com.alamkanak.weekview.R.styleable.WeekView_noOfVisibleDays, mNumberOfVisibleDays)
            mHeaderRowPadding = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_headerRowPadding, mHeaderRowPadding)
            mHeaderRowBackgroundColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_headerRowBackgroundColor, mHeaderRowBackgroundColor)
            mDayBackgroundColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_dayBackgroundColor, mDayBackgroundColor)
            mFutureBackgroundColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_futureBackgroundColor, mFutureBackgroundColor)
            mPastBackgroundColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_pastBackgroundColor, mPastBackgroundColor)
            mFutureWeekendBackgroundColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_futureWeekendBackgroundColor, mFutureBackgroundColor) // If not set, use the same color as in the week
            mPastWeekendBackgroundColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_pastWeekendBackgroundColor, mPastBackgroundColor)
            mNowLineColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_nowLineColor, mNowLineColor)
            mNowLineThickness = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_nowLineThickness, mNowLineThickness)
            mHourSeparatorColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_hourSeparatorColor, mHourSeparatorColor)
            mTodayBackgroundColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_todayBackgroundColor, mTodayBackgroundColor)
            mHourSeparatorHeight = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_hourSeparatorHeight, mHourSeparatorHeight)
            mTodayHeaderTextColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_todayHeaderTextColor, mTodayHeaderTextColor)
            mEventTextSize = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_eventTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mEventTextSize.toFloat(), mContext.resources.displayMetrics).toInt())
            mEventTextColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_eventTextColor, mEventTextColor)
            mEventPadding = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_eventPadding, mEventPadding)
            mHeaderColumnBackgroundColor = a.getColor(com.alamkanak.weekview.R.styleable.WeekView_headerColumnBackground, mHeaderColumnBackgroundColor)
            mDayNameLength = a.getInteger(com.alamkanak.weekview.R.styleable.WeekView_dayNameLength, mDayNameLength)
            mOverlappingEventGap = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_overlappingEventGap, mOverlappingEventGap)
            mEventMarginVertical = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_eventMarginVertical, mEventMarginVertical)
            mXScrollingSpeed = a.getFloat(com.alamkanak.weekview.R.styleable.WeekView_xScrollingSpeed, mXScrollingSpeed)
            mEventCornerRadius = a.getDimensionPixelSize(com.alamkanak.weekview.R.styleable.WeekView_eventCornerRadius, mEventCornerRadius)
            mShowDistinctPastFutureColor = a.getBoolean(com.alamkanak.weekview.R.styleable.WeekView_showDistinctPastFutureColor, mShowDistinctPastFutureColor)
            mShowDistinctWeekendColor = a.getBoolean(com.alamkanak.weekview.R.styleable.WeekView_showDistinctWeekendColor, mShowDistinctWeekendColor)
            mShowNowLine = a.getBoolean(com.alamkanak.weekview.R.styleable.WeekView_showNowLine, mShowNowLine)
            mHorizontalFlingEnabled = a.getBoolean(com.alamkanak.weekview.R.styleable.WeekView_horizontalFlingEnabled, mHorizontalFlingEnabled)
            mVerticalFlingEnabled = a.getBoolean(com.alamkanak.weekview.R.styleable.WeekView_verticalFlingEnabled, mVerticalFlingEnabled)
        } finally {
            a.recycle()
        }

        init()
    }// Hold references.

    private fun init() {
        // Scrolling initialization.
        mGestureDetector = GestureDetectorCompat(mContext, mGestureListener)
        mScroller = OverScroller(mContext, FastOutLinearInInterpolator())

        mMinimumFlingVelocity = ViewConfiguration.get(mContext).scaledMinimumFlingVelocity
        mScaledTouchSlop = ViewConfiguration.get(mContext).scaledTouchSlop

        // Measure settings for time column.
        mTimeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTimeTextPaint!!.textAlign = Paint.Align.RIGHT
        mTimeTextPaint!!.textSize = mTextSize.toFloat()
        mTimeTextPaint!!.color = mHoursTextColor
        val rect = Rect()
        mTimeTextPaint!!.getTextBounds("00 PM", 0, "00 PM".length, rect)
        mTimeTextHeight = rect.height().toFloat()
        mHeaderMarginBottom = mTimeTextHeight / 2
        initTextTimeWidth()

        // Measure settings for header row.
        mHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mHeaderTextPaint!!.color = mHeaderColumnTextColor
        mHeaderTextPaint!!.textAlign = Paint.Align.CENTER
        mHeaderTextPaint!!.textSize = mTextSize.toFloat()
        mHeaderTextPaint!!.getTextBounds("00 PM", 0, "00 PM".length, rect)
        mHeaderTextHeight = rect.height().toFloat()
        mHeaderTextPaint!!.typeface = Typeface.DEFAULT_BOLD

        // Prepare header background paint.
        mHeaderBackgroundPaint = Paint()
        mHeaderBackgroundPaint!!.color = mHeaderRowBackgroundColor

        // Prepare day background color paint.
        mDayBackgroundPaint = Paint()
        mDayBackgroundPaint!!.color = mDayBackgroundColor

        mPastDayBackgroundPaint = Paint()
        mPastDayBackgroundPaint!!.color = mPastDayBackgroundColor

        mDisabledBackgroundPaint = Paint()
        mDisabledBackgroundPaint!!.color = mDisabledBackgroundColor

        mFutureBackgroundPaint = Paint()
        mFutureBackgroundPaint!!.color = mFutureBackgroundColor
        mPastBackgroundPaint = Paint()
        mPastBackgroundPaint!!.color = mPastBackgroundColor
        mFutureWeekendBackgroundPaint = Paint()
        mFutureWeekendBackgroundPaint!!.color = mFutureWeekendBackgroundColor
        mPastWeekendBackgroundPaint = Paint()
        mPastWeekendBackgroundPaint!!.color = mPastWeekendBackgroundColor

        // Prepare hour separator color paint.
        mHourSeparatorPaint = Paint()
        mHourSeparatorPaint!!.style = Paint.Style.STROKE
        mHourSeparatorPaint!!.strokeWidth = mHourSeparatorHeight.toFloat()
        mHourSeparatorPaint!!.color = mHourSeparatorColor

        // Prepare the "now" line color paint
        mNowLinePaint = Paint()
        mNowLinePaint!!.strokeWidth = mNowLineThickness.toFloat()
        mNowLinePaint!!.color = mNowLineColor

        // Prepare today background color paint.
        mTodayBackgroundPaint = Paint()
        mTodayBackgroundPaint!!.color = mTodayBackgroundColor

        // Prepare today header text color paint.
        mTodayHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTodayHeaderTextPaint!!.textAlign = Paint.Align.CENTER
        mTodayHeaderTextPaint!!.textSize = mTextSize.toFloat()
        mTodayHeaderTextPaint!!.typeface = Typeface.DEFAULT_BOLD
        mTodayHeaderTextPaint!!.color = mTodayHeaderTextColor

        // Prepare event background color.
        mEventBackgroundPaint = Paint()
        mEventBackgroundPaint!!.color = Color.rgb(174, 208, 238)

        // Prepare header column background color.
        mHeaderColumnBackgroundPaint = Paint()
        mHeaderColumnBackgroundPaint!!.color = mHeaderColumnBackgroundColor

        // Prepare event text size and color.
        mEventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG)
        mEventTextPaint!!.style = Paint.Style.FILL
        mEventTextPaint!!.color = mEventTextColor
        mEventTextPaint!!.textSize = mEventTextSize.toFloat()

        // Set default event color.
        mDefaultEventColor = Color.parseColor("#9fc6e7")

        mScaleDetector = ScaleGestureDetector(mContext, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                mIsZooming = false
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                mIsZooming = true
                goToNearestOrigin()
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                mNewHourHeight = Math.round(mHourHeight * detector.scaleFactor)
                invalidate()
                return true
            }
        })
    }

    // fix rotation changes
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mAreDimensionsInvalid = true
    }

    /**
     * Initialize time column width. Calculate value with all possible hours (supposed widest text).
     */
    private fun initTextTimeWidth() {
        mTimeTextWidth = 0f
        for (i in mStartingHour..mEndingHour) {
            // Measure time string and get max width.
            val time = dateTimeInterpreter.interpretTime(i) ?: throw IllegalStateException("A DateTimeInterpreter must not return null time")
            mTimeTextWidth = Math.max(mTimeTextWidth, mTimeTextPaint!!.measureText(time))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Hide everything in the first cell (top left corner).
        canvas.drawRect(0f, 0f, mTimeTextWidth + mHeaderColumnPadding * 2, mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderBackgroundPaint!!)

        // Draw the header row.
        drawHeaderRowAndEvents(canvas)

        // Draw the time column and all the axes/separators.
        drawTimeColumnAndAxes(canvas)
    }

    private fun drawTimeColumnAndAxes(canvas: Canvas) {
        // Draw the background color for the header column.
        canvas.drawRect(0f, mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderColumnWidth, height.toFloat(), mHeaderColumnBackgroundPaint!!)

        // Clip to paint in left column only.
        canvas.clipRect(0f, mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderColumnWidth, height.toFloat(), Region.Op.REPLACE)


        for (i in mStartingHour..mEndingHour) {
            val top = mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mCurrentOrigin.y + (mHourHeight * (i - mStartingHour)).toFloat() + mHeaderMarginBottom

            // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
            val time = dateTimeInterpreter.interpretTime(i) ?: throw IllegalStateException("A DateTimeInterpreter must not return null time")
            if (top < height) canvas.drawText(time, mTimeTextWidth + mHeaderColumnPadding, top + mTimeTextHeight, mTimeTextPaint!!)
        }
    }

    private fun drawHeaderRowAndEvents(canvas: Canvas) {
        // Calculate the available width for each day.
        mHeaderColumnWidth = mTimeTextWidth + mHeaderColumnPadding * 2
        mWidthPerDay = width.toFloat() - mHeaderColumnWidth - (mColumnGap * (mNumberOfVisibleDays - 1)).toFloat()
        mWidthPerDay /= mNumberOfVisibleDays

        val today = today()

        if (mAreDimensionsInvalid) {
            mEffectiveMinHourHeight = Math.max(mMinHourHeight, ((height.toFloat() - mHeaderTextHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom) / mNumHoursToDisplay).toInt())

            mAreDimensionsInvalid = false
            if (mScrollToDay != null)
                goToDate(mScrollToDay)

            mAreDimensionsInvalid = false
            if (mScrollToHour >= 0)
                goToHour(mScrollToHour)

            mScrollToDay = null
            mScrollToHour = -1.0
            mAreDimensionsInvalid = false
        }
        if (mIsFirstDraw) {
            mIsFirstDraw = false

            // If the week view is being drawn for the first time, then consider the first day of the week.
            if (mNumberOfVisibleDays >= 7 && today.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek) {
                val difference = 7 + (today.get(Calendar.DAY_OF_WEEK) - mFirstDayOfWeek)
                mCurrentOrigin.x += (mWidthPerDay + mColumnGap) * difference
            }
        }

        // Calculate the new height due to the zooming.
        if (mNewHourHeight > 0) {
            if (mNewHourHeight < mEffectiveMinHourHeight)
                mNewHourHeight = mEffectiveMinHourHeight
            else if (mNewHourHeight > mMaxHourHeight)
                mNewHourHeight = mMaxHourHeight

            mCurrentOrigin.y = mCurrentOrigin.y / mHourHeight * mNewHourHeight
            mHourHeight = mNewHourHeight
            mNewHourHeight = -1
        }

        // If the new mCurrentOrigin.y is invalid, make it valid.
        if (mCurrentOrigin.y < height.toFloat() - (mHourHeight * mNumHoursToDisplay).toFloat() - mHeaderTextHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom - mTimeTextHeight / 2)
            mCurrentOrigin.y = height.toFloat() - (mHourHeight * mNumHoursToDisplay).toFloat() - mHeaderTextHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom - mTimeTextHeight / 2

        // Don't put an "else if" because it will trigger a glitch when completely zoomed out and
        // scrolling vertically.
        if (mCurrentOrigin.y > 0) {
            mCurrentOrigin.y = 0f
        }

        // Consider scroll offset.
        val leftDaysWithGaps = (-Math.ceil((mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).toDouble())).toInt()
        val startFromPixel = mCurrentOrigin.x + (mWidthPerDay + mColumnGap) * leftDaysWithGaps +
                mHeaderColumnWidth
        var startPixel = startFromPixel

        // Prepare to iterate for each day.
        val day = today.clone() as Calendar
        day.add(Calendar.HOUR, 6)

        // Prepare to iterate for each hour to draw the hour lines.
        var lineCount = ((height.toFloat() - mHeaderTextHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom) / mHourHeight).toInt() + 1
        lineCount *= (mNumberOfVisibleDays + 1)
        val hourLines = FloatArray(lineCount * 4)

        // Clear the cache for event rectangles.
        if (mEventRects != null) {
            for (eventRect in mEventRects!!) {
                eventRect.rectF = null
            }
        }

        // Clip to paint events only.
        canvas.clipRect(mHeaderColumnWidth, mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom + mTimeTextHeight / 2, width.toFloat(), height.toFloat(), Region.Op.REPLACE)

        // Iterate through each day.
        val oldMillis = firstVisibleDay.timeInMillis
        firstVisibleDay.timeInMillis = today.timeInMillis
        firstVisibleDay.add(Calendar.DATE, -Math.round(mCurrentOrigin.x / (mWidthPerDay + mColumnGap)))
        if (firstVisibleDay.timeInMillis != oldMillis && scrollListener != null) {
            val oldFirstVisibleDay = today.clone() as Calendar
            oldFirstVisibleDay.timeInMillis = oldMillis
            scrollListener!!.onFirstVisibleDayChanged(firstVisibleDay, oldFirstVisibleDay)
        }

        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleDays + 1) {

            // Check if the day is today.
            day.timeInMillis = today.timeInMillis
            day.add(Calendar.DATE, dayNumber - 1)
            val isToday = CalendarUtils.isSameDay(day, today)

            // Draw background color for each day.
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (mWidthPerDay + startPixel - start > 0) {
                if (mShowDistinctPastFutureColor) {
                    val isWeekend = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    val pastPaint = if (isWeekend && mShowDistinctWeekendColor) mPastWeekendBackgroundPaint else mPastBackgroundPaint
                    val futurePaint = if (isWeekend && mShowDistinctWeekendColor) mFutureWeekendBackgroundPaint else mFutureBackgroundPaint
                    val startY = mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom + mCurrentOrigin.y

                    when {
                        isToday           -> {
                            val now = debuggableToday
                            val beforeNow = (now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0f) * mHourHeight
                            canvas.drawRect(start, startY, startPixel + mWidthPerDay, startY + beforeNow, pastPaint!!)
                            canvas.drawRect(start, startY + beforeNow, startPixel + mWidthPerDay, height.toFloat(), futurePaint!!)
                        }
                        day.before(today) -> canvas.drawRect(start, startY, startPixel + mWidthPerDay, height.toFloat(), pastPaint!!)
                        else              -> canvas.drawRect(start, startY, startPixel + mWidthPerDay, height.toFloat(), futurePaint!!)
                    }
                } else {
                    val paintToUseForThisDay: Paint? = when {
                        isADisabledDay(day) -> mDisabledBackgroundPaint
                        day.before(today)   -> mPastDayBackgroundPaint
                        isToday             -> mTodayBackgroundPaint
                        else                -> mDayBackgroundPaint
                    }

                    canvas.drawRect(start, mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom, startPixel + mWidthPerDay, height.toFloat(), paintToUseForThisDay!!)
                }
            }

            // Prepare the separator lines for hours.
            var i = 0
            for (hourNumber in mStartingHour..mEndingHour) {
                val top = mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mCurrentOrigin.y + (mHourHeight * (hourNumber - mStartingHour)).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom
                if (top > mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom - mHourSeparatorHeight && top < height && startPixel + mWidthPerDay - start > 0) {
                    hourLines[i * 4] = start
                    hourLines[i * 4 + 1] = top
                    hourLines[i * 4 + 2] = startPixel + mWidthPerDay
                    hourLines[i * 4 + 3] = top
                    i++
                }
            }

            // Draw the lines for hours.
            canvas.drawLines(hourLines, mHourSeparatorPaint!!)

            // Draw the events.
            drawEvents(day, startPixel, canvas)

            // Draw the line at the current time.
            if (mShowNowLine && isToday) {
                val startY = mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom + mCurrentOrigin.y
                val now = debuggableToday
                val beforeNow = (now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0f - mStartingHour) * mHourHeight
                canvas.drawLine(start, startY + beforeNow, startPixel + mWidthPerDay, startY + beforeNow, mNowLinePaint!!)
            }

            // In the next iteration, start from the next day.
            startPixel += mWidthPerDay + mColumnGap
        }


        // Clip to paint header row only.
        canvas.clipRect(mHeaderColumnWidth, 0f, width.toFloat(), mHeaderTextHeight + mHeaderRowPadding * 2, Region.Op.REPLACE)

        // Draw the header background.
        canvas.drawRect(0f, 0f, width.toFloat(), mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderBackgroundPaint!!)

        // Draw the header row texts.
        startPixel = startFromPixel
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleDays + 1) {
            // Check if the day is today.
            day.timeInMillis = today.timeInMillis
            day.add(Calendar.DATE, dayNumber - 1)
            val sameDay = CalendarUtils.isSameDay(day, today)

            // Draw the day labels.
            val dayLabel = dateTimeInterpreter.interpretDate(day) ?: throw IllegalStateException("A DateTimeInterpreter must not return null date")
            canvas.drawText(dayLabel, startPixel + mWidthPerDay / 2, mHeaderTextHeight + mHeaderRowPadding, if (sameDay) mTodayHeaderTextPaint else mHeaderTextPaint)
            startPixel += mWidthPerDay + mColumnGap
        }
    }

    private fun isADisabledDay(day: Calendar): Boolean =
            enabledIntervals.none { WeekTime(day) in it }

    /**
     * Get the time and date where the user clicked on.
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    private fun getTimeFromPoint(x: Float, y: Float): Calendar? {
        val leftDaysWithGaps = (-Math.ceil((mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).toDouble())).toInt()
        var startPixel = mCurrentOrigin.x + (mWidthPerDay + mColumnGap) * leftDaysWithGaps +
                mHeaderColumnWidth
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleDays + 1) {
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (mWidthPerDay + startPixel - start > 0 && x > start && x < startPixel + mWidthPerDay) {
                val day = today()
                day.add(Calendar.DATE, dayNumber - 1)
                val pixelsFromZero = y - mCurrentOrigin.y - mHeaderTextHeight
                -(mHeaderRowPadding * 2).toFloat() - mTimeTextHeight / 2 - mHeaderMarginBottom
                val hour = (pixelsFromZero / mHourHeight).toInt()
                val minute = (60 * (pixelsFromZero - hour * mHourHeight) / mHourHeight).toInt()
                day.add(Calendar.HOUR, hour)
                day.set(Calendar.MINUTE, minute)
                return day
            }
            startPixel += mWidthPerDay + mColumnGap
        }
        return null
    }

    /**
     * Draw all the events of a particular day.
     * @param date The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas The canvas to draw upon.
     */
    private fun drawEvents(date: Calendar, startFromPixel: Float, canvas: Canvas) {
        val todayWDT = WeekDayTime(date)

        if (mEventRects != null && mEventRects!!.size > 0) {
            for (i in mEventRects!!.indices) {
                if (mEventRects!![i].weekDayTime == todayWDT) {
                    // Calculate top.
                    val top = mHourHeight.toFloat() * 24f * mEventRects!![i].top / 1440 + mCurrentOrigin.y + mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom + mTimeTextHeight / 2 + mEventMarginVertical.toFloat()

                    // Calculate bottom.
                    var bottom = mEventRects!![i].bottom
                    bottom = mHourHeight.toFloat() * 24f * bottom / 1440 + mCurrentOrigin.y + mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom + mTimeTextHeight / 2 - mEventMarginVertical

                    // Calculate left and right.
                    var left = startFromPixel + mEventRects!![i].left * mWidthPerDay
                    if (left < startFromPixel)
                        left += mOverlappingEventGap.toFloat()
                    var right = left + mEventRects!![i].width * mWidthPerDay
                    if (right < startFromPixel + mWidthPerDay)
                        right -= mOverlappingEventGap.toFloat()

                    // Draw the event and the event name on top of it.
                    if (left < right &&
                            left < width &&
                            top < height &&
                            right > mHeaderColumnWidth &&
                            bottom > mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom) {
                        mEventRects!![i].rectF = RectF(left, top, right, bottom)

                        val bgColor = if (mEventRects!![i].event.color == 0) mDefaultEventColor else mEventRects!![i].event.color
                        mEventBackgroundPaint!!.color = bgColor
                        canvas.drawRoundRect(mEventRects!![i].rectF!!, mEventCornerRadius.toFloat(), mEventCornerRadius.toFloat(), mEventBackgroundPaint!!)
                        drawEventTitle(mEventRects!![i].event, mEventRects!![i].rectF, canvas, top, left)
                    } else
                        mEventRects!![i].rectF = null
                }
            }
        }
    }


    /**
     * Draw the name of the event on top of the event rectangle.
     * @param event The event of which the title (and location) should be drawn.
     * @param rect The rectangle on which the text is to be drawn.
     * @param canvas The canvas to draw upon.
     * @param originalTop The original top position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     * @param originalLeft The original left position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     */
    private fun drawEventTitle(event: WeekViewEvent, rect: RectF?, canvas: Canvas, originalTop: Float, originalLeft: Float) {
        if (rect!!.right - rect.left - (mEventPadding * 2).toFloat() < 0) return
        if (rect.bottom - rect.top - (mEventPadding * 2).toFloat() < 0) return

        // Prepare the name of the event.
        val bob = SpannableStringBuilder()
        if (event.name != null) {
            bob.append(event.name)
            bob.setSpan(StyleSpan(Typeface.NORMAL), 0, bob.length, 0)
            bob.append(' ')
        }

        // Prepare the location of the event.
        if (event.location != null) {
            bob.append(event.location)
        }

        val availableHeight = (rect.bottom - originalTop - (mEventPadding * 2).toFloat()).toInt()
        val availableWidth = (rect.right - originalLeft - (mEventPadding * 2).toFloat()).toInt()

        // Get text dimensions.
        var textLayout = StaticLayout(bob, mEventTextPaint, availableWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)

        val lineHeight = textLayout.height / textLayout.lineCount

        if (availableHeight >= lineHeight) {
            // Calculate available number of line counts.
            var availableLineCount = availableHeight / lineHeight
            do {
                // Ellipsize text to fit into event rect.
                textLayout = StaticLayout(TextUtils.ellipsize(bob, mEventTextPaint, (availableLineCount * availableWidth).toFloat(), TextUtils.TruncateAt.END), mEventTextPaint, (rect.right - originalLeft - (mEventPadding * 2).toFloat()).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)

                // Reduce line count.
                availableLineCount--

                // Repeat until text is short enough.
            } while (textLayout.height > availableHeight)

            // Draw text.
            canvas.save()
            canvas.translate(originalLeft + mEventPadding, originalTop + mEventPadding)
            textLayout.draw(canvas)
            canvas.restore()
        }
    }


    /**
     * A class to hold reference to the events and their visual representation. An EventRect is
     * actually the rectangle that is drawn on the calendar for a given event. There may be more
     * than one rectangle for a single event (an event that expands more than one day). In that
     * case two instances of the EventRect will be used for a single event. The given event will be
     * stored in "originalEvent". But the event that corresponds to rectangle the rectangle
     * instance will be stored in "event".
     */
    private inner class EventRect
    /**
     * Create a new instance of event rect. An EventRect is actually the rectangle that is drawn
     * on the calendar for a given event. There may be more than one rectangle for a single
     * event (an event that expands more than one day). In that case two instances of the
     * EventRect will be used for a single event. The given event will be stored in
     * "originalEvent". But the event that corresponds to rectangle the rectangle instance will
     * be stored in "event".
     * @param event Represents the event which this instance of rectangle represents.
     * @param originalEvent The original event that was passed by the user.
     * @param rectF The rectangle.
     */
    (val event: WeekViewEvent, val originalEvent: WeekViewEvent, var rectF: RectF?) {
        var left: Float = 0.toFloat()
        var width: Float = 0.toFloat()
        var top: Float = 0.toFloat()
        var bottom: Float = 0.toFloat()

        /**
         * This reference is hold here as a cache in order to prevent to make calculations on the
         * calendar, which is very slow.
         */
        val weekDayTime: WeekDayTime = WeekDayTime(event.startTime)

    }

    private fun recalculatePositionsOfEvents() {
        // Prepare to calculate positions of each events.
        val tempEvents = mEventRects
        mEventRects = ArrayList()

        // Iterate through each day with events to calculate the position of the events.
        while (tempEvents!!.size > 0) {
            val eventRects = ArrayList<CustomWeekView.EventRect>(tempEvents.size)

            // Get first event for a day.
            val eventRect1 = tempEvents.removeAt(0)
            eventRects.add(eventRect1)

            var i = 0
            while (i < tempEvents.size) {
                // Collect all other events for same day.
                val eventRect2 = tempEvents[i]
                if (CalendarUtils.isSameDay(eventRect1.event.startTime, eventRect2.event.startTime)) {
                    tempEvents.removeAt(i)
                    eventRects.add(eventRect2)
                } else {
                    i++
                }
            }

            computePositionOfEvents(eventRects)
        }
    }

    /**
     * Cache the event for smooth scrolling functionality.
     * @param event The event to cache.
     */
    private fun cacheEvent(event: WeekViewEvent) {
        if (event.startTime >= event.endTime)
            return

        deleteRectsHavingEventWithSameId(event)

        if (CalendarUtils.isSameDay(event.startTime, event.endTime)) {
            mEventRects!!.add(EventRect(event, event, null))
        } else {
            //Our event spans multiple days:

            // Add first day.
            val endTime = event.startTime.clone() as Calendar
            endTime.set(Calendar.HOUR_OF_DAY, 23)
            endTime.set(Calendar.MINUTE, 59)
            val event1 = WeekViewEvent(event.id, event.name, event.location, event.startTime, endTime)
            event1.color = event.color
            mEventRects!!.add(EventRect(event1, event, null))

            // Add other days.
            val otherDay = event.startTime.clone() as Calendar
            otherDay.add(Calendar.DATE, 1)
            while (!CalendarUtils.isSameDay(otherDay, event.endTime)) {
                val overDay = otherDay.clone() as Calendar
                overDay.set(Calendar.HOUR_OF_DAY, 0)
                overDay.set(Calendar.MINUTE, 0)
                val endOfOverDay = overDay.clone() as Calendar
                endOfOverDay.set(Calendar.HOUR_OF_DAY, 23)
                endOfOverDay.set(Calendar.MINUTE, 59)
                val eventMore = WeekViewEvent(event.id, event.name, overDay, endOfOverDay)
                eventMore.color = event.color
                mEventRects!!.add(EventRect(eventMore, event, null))

                // Add next day.
                otherDay.add(Calendar.DATE, 1)
            }

            // Add last day.
            val startTime = event.endTime.clone() as Calendar
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)
            val event2 = WeekViewEvent(event.id, event.name, event.location, startTime, event.endTime)
            event2.color = event.color
            mEventRects!!.add(EventRect(event2, event, null))
        }
    }

    private fun deleteRectsHavingEventWithSameId(eventToSearchFor: WeekViewEvent) {
        val rectsIterator = mEventRects!!.iterator()
        while (rectsIterator.hasNext()) {
            val currentRect = rectsIterator.next()
            if (currentRect.originalEvent.id == eventToSearchFor.id) {
                rectsIterator.remove()
            }
        }
    }

    /**
     * Sort and cache events.
     * @param events The events to be sorted and cached.
     */
    private fun sortAndCacheEvents(events: List<WeekViewEvent>) {
        sortEvents(events)
        for (event in events) {
            cacheEvent(event)
        }
    }

    /**
     * Sorts the events in ascending order.
     * @param events The events to be sorted.
     */
    private fun sortEvents(events: List<WeekViewEvent>) =
            Collections.sort(events) { event1, event2 ->
                val start1 = event1.startTime.timeInMillis
                val start2 = event2.startTime.timeInMillis
                var comparator = if (start1 > start2) 1 else if (start1 < start2) -1 else 0
                if (comparator == 0) {
                    val end1 = event1.endTime.timeInMillis
                    val end2 = event2.endTime.timeInMillis
                    comparator = if (end1 > end2) 1 else if (end1 < end2) -1 else 0
                }
                comparator
            }

    /**
     * Calculates the left and right positions of each events. This comes handy specially if events
     * are overlapping.
     * @param eventRects The events along with their wrapper class.
     */
    private fun computePositionOfEvents(eventRects: ArrayList<CustomWeekView.EventRect>) {
        // Make "collision groups" for all events that collide with others.
        val collisionGroups = ArrayList<ArrayList<CustomWeekView.EventRect>>()
        for (eventRect in eventRects) {
            var isPlaced = false
            outerLoop@ for (collisionGroup in collisionGroups) {
                for (groupEvent in collisionGroup) {
                    if (isEventsCollide(groupEvent.event, eventRect.event)) {
                        collisionGroup.add(eventRect)
                        isPlaced = true
                        break@outerLoop
                    }
                }
            }
            if (!isPlaced) {
                val newGroup = ArrayList<CustomWeekView.EventRect>()
                newGroup.add(eventRect)
                collisionGroups.add(newGroup)
            }
        }

        for (collisionGroup in collisionGroups) {
            expandEventsToMaxWidth(collisionGroup)
        }
    }

    /**
     * Expands all the events to maximum possible width. The events will try to occupy maximum
     * space available horizontally.
     * @param collisionGroup The group of events which overlap with each other.
     */
    private fun expandEventsToMaxWidth(collisionGroup: ArrayList<CustomWeekView.EventRect>) {
        // Expand the events to maximum possible width.
        val columns = ArrayList<ArrayList<CustomWeekView.EventRect>>()
        columns.add(ArrayList())
        for (eventRect in collisionGroup) {
            var isPlaced = false
            for (column in columns) {
                if (column.size == 0) {
                    column.add(eventRect)
                    isPlaced = true
                } else if (!isEventsCollide(eventRect.event, column[column.size - 1].event)) {
                    column.add(eventRect)
                    isPlaced = true
                    break
                }
            }
            if (!isPlaced) {
                val newColumn = ArrayList<CustomWeekView.EventRect>()
                newColumn.add(eventRect)
                columns.add(newColumn)
            }
        }


        // Calculate left and right position for all the events.
        // Get the maxRowCount by looking in all columns.
        val maxRowCount = columns.map { it.size }.max() ?: 0
        for (i in 0 until maxRowCount) {
            // Set the left and right values of the event.
            var j = 0f
            for (column in columns) {
                if (column.size >= i + 1) {
                    val eventRect = column[i]
                    eventRect.width = 1f / columns.size
                    eventRect.left = j / columns.size
                    eventRect.top = ((eventRect.event.startTime.get(Calendar.HOUR_OF_DAY) - mStartingHour) * 60 + eventRect.event.startTime.get(Calendar.MINUTE)).toFloat()
                    eventRect.bottom = ((eventRect.event.endTime.get(Calendar.HOUR_OF_DAY) - mStartingHour) * 60 + eventRect.event.endTime.get(Calendar.MINUTE)).toFloat()
                    mEventRects!!.add(eventRect)
                }
                j++
            }
        }
    }


    /**
     * Checks if two events overlap.
     * @param event1 The first event.
     * @param event2 The second event.
     * @return true if the events overlap.
     */
    private fun isEventsCollide(event1: WeekViewEvent, event2: WeekViewEvent): Boolean {
        val start1 = event1.startTime.timeInMillis
        val end1 = event1.endTime.timeInMillis
        val start2 = event2.startTime.timeInMillis
        val end2 = event2.endTime.timeInMillis
        return !(start1 >= end2 || end1 <= start2)
    }


    override fun invalidate() {
        super.invalidate()
        mAreDimensionsInvalid = true
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to setting and getting the properties.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * The interpreter which provides the text to show in the header column and the header row
     */
    // Refresh time column width.
    var dateTimeInterpreter: DateTimeInterpreter
        get() {
            if (mDateTimeInterpreter == null) {
                mDateTimeInterpreter = object : DateTimeInterpreter {
                    override fun interpretDate(date: Calendar): String {
                        return try {
                            val sdf = if (mDayNameLength == LENGTH_SHORT) SimpleDateFormat("EEEEE M/dd", Locale.getDefault()) else SimpleDateFormat("EEE M/dd", Locale.getDefault())
                            sdf.format(date.time).toUpperCase()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ""
                        }

                    }

                    override fun interpretTime(hour: Int): String {
                        val calendar = debuggableToday
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, 0)

                        return try {
                            val sdf = if (DateFormat.is24HourFormat(context)) SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("hh a", Locale.getDefault())
                            sdf.format(calendar.time)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ""
                        }

                    }
                }
            }
            return mDateTimeInterpreter as DateTimeInterpreter
        }
        set(dateTimeInterpreter) {
            this.mDateTimeInterpreter = dateTimeInterpreter
            initTextTimeWidth()
        }

    /**
     * The number of visible days in a week.
     */
    open var numberOfVisibleDays: Int
        get() = mNumberOfVisibleDays
        set(numberOfVisibleDays) {
            this.mNumberOfVisibleDays = numberOfVisibleDays
            mCurrentOrigin.x = 0f
            mCurrentOrigin.y = 0f
            postInvalidate()
        }


    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to scrolling.
    //
    /////////////////////////////////////////////////////////////////

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector!!.onTouchEvent(event)
        val `val` = mGestureDetector!!.onTouchEvent(event)

        // Check after call of mGestureDetector, so mCurrentFlingDirection and mCurrentScrollDirection are set.
        if (event.action == MotionEvent.ACTION_UP && !mIsZooming && mCurrentFlingDirection == CustomWeekView.Direction.NONE) {
            if (mCurrentScrollDirection == CustomWeekView.Direction.RIGHT || mCurrentScrollDirection == CustomWeekView.Direction.LEFT) {
                goToNearestOrigin()
            }
            mCurrentScrollDirection = CustomWeekView.Direction.NONE
        }

        return `val`
    }

    private fun goToNearestOrigin() {
        var leftDays = (mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).toDouble()

        leftDays = when {
            mCurrentFlingDirection != CustomWeekView.Direction.NONE   -> // snap to nearest day
                Math.round(leftDays).toDouble()
            mCurrentScrollDirection == CustomWeekView.Direction.LEFT  -> // snap to last day
                Math.floor(leftDays)
            mCurrentScrollDirection == CustomWeekView.Direction.RIGHT -> // snap to next day
                Math.ceil(leftDays)
            else                                                      -> // snap to nearest day
                Math.round(leftDays).toDouble()
        }

        val nearestOrigin = (mCurrentOrigin.x - leftDays * (mWidthPerDay + mColumnGap)).toInt()

        if (nearestOrigin != 0) {
            // Stop current animation.
            mScroller!!.forceFinished(true)
            // Snap to date.
            mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0, (Math.abs(nearestOrigin) / mWidthPerDay * 500).toInt())
            ViewCompat.postInvalidateOnAnimation(this@CustomWeekView)
        }
        // Reset scrolling and fling direction.
        mCurrentFlingDirection = CustomWeekView.Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection
    }


    override fun computeScroll() {
        super.computeScroll()

        if (mScroller!!.isFinished) {
            if (mCurrentFlingDirection != CustomWeekView.Direction.NONE) {
                // Snap to day after fling is finished.
                goToNearestOrigin()
            }
        } else {
            if (mCurrentFlingDirection != CustomWeekView.Direction.NONE && forceFinishScroll()) {
                goToNearestOrigin()
            } else if (mScroller!!.computeScrollOffset()) {
                mCurrentOrigin.y = mScroller!!.currY.toFloat()
                mCurrentOrigin.x = mScroller!!.currX.toFloat()
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    /**
     * Check if scrolling should be stopped.
     * @return true if scrolling should be stopped before reaching the end of animation.
     */
    private fun forceFinishScroll(): Boolean =
            mScroller!!.currVelocity <= mMinimumFlingVelocity


    /////////////////////////////////////////////////////////////////
    //
    //      Public methods.
    //
    /////////////////////////////////////////////////////////////////
    /**
     * Show a specific day on the week view.
     * @param date The date to show.
     */
    fun goToDate(date: Calendar?) {
        mScroller!!.forceFinished(true)
        mCurrentFlingDirection = CustomWeekView.Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection

        date!!.set(Calendar.HOUR_OF_DAY, 0)
        date.set(Calendar.MINUTE, 0)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)

        if (mAreDimensionsInvalid) {
            mScrollToDay = date
            return
        }


        val today = debuggableToday
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val day = 1000L * 60L * 60L * 24L
        val dateInMillis = date.timeInMillis + date.timeZone.getOffset(date.timeInMillis)
        val todayInMillis = today.timeInMillis + today.timeZone.getOffset(today.timeInMillis)
        val dateDifference = dateInMillis / day - todayInMillis / day
        mCurrentOrigin.x = -dateDifference * (mWidthPerDay + mColumnGap)
        postInvalidate()
    }

    /**
     * Refreshes the view and loads the events again.
     */
    private fun notifyDatasetChanged() = postInvalidate()

    /**
     * Vertically scroll to a specific hour in the week view.
     * @param hour The hour to scroll to in 24-hour format. Supported values are 0-24.
     */
    private fun goToHour(hour: Double) {
        if (mAreDimensionsInvalid) {
            mScrollToHour = hour
            return
        }

        var verticalOffset = 0
        if (hour > 24)
            verticalOffset = mHourHeight * 24
        else if (hour > 0)
            verticalOffset = (mHourHeight * hour).toInt()

        if (verticalOffset > (mHourHeight * 24 - height).toFloat() + mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom)
            verticalOffset = ((mHourHeight * 24 - height).toFloat() + mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom).toInt()

        mCurrentOrigin.y = (-verticalOffset).toFloat()
        invalidate()
    }


    /////////////////////////////////////////////////////////////////
    //
    //      Interfaces.
    //
    /////////////////////////////////////////////////////////////////

    interface EventClickListener {
        /**
         * Triggered when clicked on one existing event
         * @param event: event clicked.
         * @param eventRect: view containing the clicked event.
         */
        fun onEventClick(event: WeekViewEvent, eventRect: RectF?)
    }

    interface EventLongPressListener {
        /**
         * Similar to [CustomWeekView.EventClickListener] but with a long press.
         * @param event: event clicked.
         * @param eventRect: view containing the clicked event.
         */
        fun onEventLongPress(event: WeekViewEvent, eventRect: RectF?)
    }

    interface EmptyViewClickListener {
        /**
         * Triggered when the users clicks on a empty space of the calendar.
         * @param time: [Calendar] object set with the date and time of the clicked position on the view.
         */
        fun onEmptyViewClicked(time: Calendar?)
    }

    interface EmptyViewLongPressListener {
        /**
         * Similar to [CustomWeekView.EmptyViewClickListener] but with long press.
         * @param time: [Calendar] object set with the date and time of the long pressed position on the view.
         */
        fun onEmptyViewLongPress(time: Calendar?)
    }

    interface ScrollListener {
        /**
         * Called when the first visible day has changed.
         *
         * (this will also be called during the first draw of the weekview)
         * @param newFirstVisibleDay The new first visible day
         * @param oldFirstVisibleDay The old first visible day (is null on the first call).
         */
        fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar)
    }


    /////////////////////////////////////////////////////////////////
    //
    //      Helper methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Returns a calendar instance at the start of this day
     * @return the calendar instance
     */
    private fun today(): Calendar {
        val today = debuggableToday
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return today
    }

    companion object {

        val DEFAULT_EVENT_FONT_SIZE = 10

        val LENGTH_SHORT = 1
        val LENGTH_LONG = 2
    }
}
