package com.epg;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Base guide view class contains guide scroll implementation, selections.
 */
public abstract class BaseGuideView extends GuideAdapterView<BaseGuideAdapter> {
    static final int NUMBER_OF_MINUTES_IN_DAY = 1440;
    public static final int BIG_CHANNEL_MULTIPLIER = 3;
    public static final int DEFAULT_ONE_MINUTE_WIDTH = 1;
    /**
     * Types of layout pass
     */
    static final int LAYOUT_TYPE_CHANNEL_INDICATOR = 1;
    static final int LAYOUT_TYPE_EVENTS = 2;
    static final int LAYOUT_TYPE_OVERLAP_VIEW = 3;
    /**
     * Duration of smooth scroll animation while executing single scroll
     */
    public static final int SMOOTH_SCROLL_DURATION = 400;
    /**
     * Duration of smooth scroll animation while executing fast scroll
     */
    public static final int SMOOTH_FAST_SCROLL_DURATION = 150;

    /**
     * Duration of smooth fast scroll end animation
     */
    public static final int SMOOTH_FAST_SCROLL_END_DURATION = 170;

    /**
     * Duration of smooth left/right scroll animation
     */
    public static final int SMOOTH_LEFT_RIGHT_DURATION = 300;
    /**
     * Refresh interval of smooth scroll animations
     */
    static final int REFRESH_INTERVAL = 16;// We want at least 1000/16=60 FPS
    // while executing animation
    /**
     * Different scroll states
     */
    static final int SCROLL_STATE_NORMAL = 0;
    static final int SCROLL_STATE_FAST_SCROLL = 1;
    static final int SCROLL_STATE_FAST_SCROLL_END = 2;

    /**
     * Where should selected event position be relative to events area.
     */
    static final float DEFAULT_SELECTION_EVENT_POSITION = 0.2f;
    /**
     * Start and end time of guide
     */
    private Calendar mStartTime;
    private Calendar mEndTime;
    private int mTimeLineTextSize;
    private Paint mTimeLinePaintText;
    private Rect mTimeLineRectText;
    /**
     * Time line progress indicator drawable
     */
    // TODO draw time line progress on top of view
    private Rect mTimeLineProgressIndicatorRect;
    protected Drawable mTimeLineProgressIndicator;
    /**
     * Time line indicator text format
     */
    protected String mTimeLineTextFormat = "hh:mm";
    private SimpleDateFormat mTimeLineFormater;

    /**
     * Active scrolling state
     */
    int mScrollState = SCROLL_STATE_NORMAL;
    /**
     * Calculated total width and height (Represents total presentation area
     * size)
     */
    protected int mTotalWidth;
    protected int mTotalHeight;
    /**
     * Current scrolled X and Y values
     */
    protected int mCurrentOffsetX;
    protected int mCurrentOffsetY;

    /**
     * Index of expanded channel, used while in fast scroll state
     */
    int mExpandedItemIndex = INVALID_POSITION;

    /**
     * Calculated sections of guide window
     */
    protected Rect mRectTimeLine;
    protected Rect mRectChannelIndicators;
    protected Rect mRectEventsArea;
    protected Rect mRectSelectedRowArea;
    /**
     * Helper rectangle that is used for calculating row height
     */
    private Rect mChildRowHeightRect;
    /**
     * Calculated channel row heights
     */
    protected int mChannelRowHeight;
    protected int mChannelRowHeightExpanded;
    /**
     * Width of one minute declared in pixels
     */
    protected int mOneMinuteWidth;
    /**
     * Height of vertical divider declared in pixels
     */
    protected int mVerticalDividerHeight;
    /**
     * Width of horizontal divider declared in pixels
     */
    protected int mHorizontalDividerWidth;
    /**
     * Divider drawable
     */
    protected Drawable mDivider;
    /**
     * Indicates number of channels to display inside guide
     */
    protected int mNumberOfVisibleChannels;
    /**
     * Position of where on screen will be selected event
     */
    protected SelectionType mSelectionType;
    protected float mSelectionRelativePosition;
    protected int mSelectionAbsolutePosition;
    /**
     * Selector drawable
     */
    protected Drawable mSelector;
    /**
     * Selection rectangle position object, holds information about selected
     * view coordinates on screen
     */
    private Rect mSelectorRect = new Rect();

    /**
     * Epg data adapter
     */
    protected BaseGuideAdapter mAdapter = null;
    /**
     * Recycle bin instance. This is used for caching unused views for future
     * use.
     */
    protected Recycler mRecycler;

    /**
     * Object for guide scrolling horizontally and vertically.
     */
    Scroller mScroll;

    /**
     * Listener for fast scroll end animation
     */
    private Animation.AnimationListener mFastScrollEndAnimationListener;

    /**
     * Runnable that smooth scrolls list
     */
    SmoothScrollRunnable mSmoothScrollRunnable;

    /**
     * View that is first touched by user (used for user click)
     */
    private View mTouchedView;

    /**
     * Objects that helps with gesture events
     */
    private GestureDetector mGestureDetector;
    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            offsetBy(distanceX, distanceY);
            selectNextView(null);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            // We will use fast scroll for horizontal fling
            fling((int) -velocityX, 0);// (int) -velocityY);
            return true;
        }

        public boolean onDown(MotionEvent e) {
            // if touch down during fling animation - reset touched item so it
            // wouldn't be handled as item tap
            mTouchedView = mScroll.computeScrollOffset() ? null
                    : getViewAtCoordinates(e.getX(), e.getY());

            mScroll.forceFinished(true);
            postInvalidateOnAnimation();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mTouchedView == null) {
                return false;
            }
            LayoutParams lp = (LayoutParams) mTouchedView.getLayoutParams();
            if (lp != null) {
                performItemClick(mTouchedView);
            }
            selectNextView(mTouchedView);
            mTouchedView = null;
            return true;
        }
    };

    public BaseGuideView(Context context) throws Exception {
        super(context);
        init(context, null);
    }

    public BaseGuideView(Context context, AttributeSet attrs) throws Exception {
        super(context, attrs);
        init(context, attrs);
    }

    public BaseGuideView(Context context, AttributeSet attrs, int defStyle)
            throws Exception {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    /**
     * Initialize all fields for BaseGuideView
     */
    private void init(Context context, AttributeSet attrs) throws Exception {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.BaseGuideView, 0, 0);
            try {
                mNumberOfVisibleChannels = a.getInteger(
                        R.styleable.BaseGuideView_numberOfChannelsToDisplay, 5);
                mVerticalDividerHeight = a.getDimensionPixelSize(
                        R.styleable.BaseGuideView_verticalDividerHeight, 1);
                mHorizontalDividerWidth = a.getDimensionPixelSize(
                        R.styleable.BaseGuideView_horizontalDividerHeight, 0);
                mDivider = a.getDrawable(R.styleable.BaseGuideView_divider);
                mSelector = a.getDrawable(R.styleable.BaseGuideView_selector);
                mTimeLineProgressIndicator = a
                        .getDrawable(R.styleable.BaseGuideView_timeLineProgressIndicator);
                String format = a
                        .getNonResourceString(R.styleable.BaseGuideView_timeLineTextFormat);
                if (format != null && !format.equals("")) {
                    mTimeLineTextFormat = format;
                }
                mTimeLineTextSize = a.getDimensionPixelSize(R.styleable.BaseGuideView_timeLineTextSize, 20);
                int sel = a.getInt(R.styleable.BaseGuideView_selectionType, SelectionType.FIXED_ON_SCREEN.getValue());
                if (sel == SelectionType.FIXED_ON_SCREEN.getValue()) {
                    mSelectionType = SelectionType.FIXED_ON_SCREEN;
                } else if (sel == SelectionType.NOT_FIXED_ON_SCREEN.getValue()) {
                    mSelectionType = SelectionType.NOT_FIXED_ON_SCREEN;
                }
                mSelectionRelativePosition = a.getFloat(R.styleable.BaseGuideView_selectionFixedValue,
                        DEFAULT_SELECTION_EVENT_POSITION);
            } finally {
                a.recycle();
            }
        }

        // Check if number of visible channels is not even
        if (mNumberOfVisibleChannels % 2 == 0) {
            throw new Exception("Number of visible items must not be even!");
        }
        // Initialize gesture helper object
        mGestureDetector = new GestureDetector(getContext(), mGestureListener);
        // Initialize scroller
        mScroll = new Scroller(context, new LinearInterpolator());
        mSmoothScrollRunnable = new SmoothScrollRunnable();
        //Initialize fast scroll end animation listener
        mFastScrollEndAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mScrollState = SCROLL_STATE_FAST_SCROLL_END;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                changeScrollState(SCROLL_STATE_NORMAL,
                        INVALID_POSITION);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        // Initialize recycler
        mRecycler = new Recycler();
        // Used for calculating child row height
        mChildRowHeightRect = new Rect();
        // Initialize guide view
        setBackgroundColor(Color.BLACK);
        setFocusable(true);

        //Initialize time line text format
        mTimeLineFormater = new SimpleDateFormat(mTimeLineTextFormat);
        mTimeLineFormater.setTimeZone(TimeZone.getDefault());
        //Initialize time line paint
        mTimeLinePaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTimeLinePaintText.setColor(Color.WHITE);
        mTimeLinePaintText.setTextSize(mTimeLineTextSize);
        mTimeLineRectText = new Rect();
        mTimeLineProgressIndicatorRect = new Rect();
    }

    @Override
    protected void onAttachedToWindow() {
        // TODO
        // if (mAdapter != null && !mAdapterRegistered) {
        // mAdapterRegistered = true;
        // mAdapter.registerDataSetObserver(mDataSetObserver);
        // }
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        // TODO
        // if (mAdapterRegistered) {
        // mAdapterRegistered = false;
        // mAdapter.unregisterDataSetObserver(mDataSetObserver);
        // }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Workaround for SimpleOnGestureListener do not handle motion UP
        boolean detectedUp = event.getAction() == MotionEvent.ACTION_UP;
        mGestureDetector.onTouchEvent(event);
        if (detectedUp) {
            onUp(event);
        }
        return true;
    }

    private void onUp(MotionEvent event) {
        postOnAnimationDelayed(new Runnable() {
            @Override
            public void run() {
                moveSelectedViewsToSelectionBounds();
            }
        }, REFRESH_INTERVAL);
    }

    @Override
    public BaseGuideAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setAdapter(BaseGuideAdapter adapter) {
        // Clear all necessary data
        mAdapter = adapter;
        mCurrentOffsetX = 0;
        mCurrentOffsetY = 0;
        mSelectedView = null;
        mFirstItemPosition = 0;
        mLastItemPosition = INVALID_POSITION;
        mSelectedEventItemPosition = INVALID_POSITION;
        removeAllViewsInLayout();
        mRecycler.clearAll();

        // Initialize some elements from adapter
        if (mAdapter != null) {
            mChannelsCount = mAdapter.getChannelsCount();
            // Get minute pixel width
            mOneMinuteWidth = mAdapter.getOneMinuteWidth();
            if (mOneMinuteWidth < 1) {
                mOneMinuteWidth = DEFAULT_ONE_MINUTE_WIDTH;
            }
            // Calculate total grid width
            mStartTime = mAdapter.getStartTime();
            mEndTime = mAdapter.getEndTime();
            long diffInMs = mEndTime.getTimeInMillis() - mStartTime.getTimeInMillis();
            int diffInMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(diffInMs);
            mTotalWidth = mOneMinuteWidth * diffInMinutes;

            // We can not calculate total height if view is not finished its
            // layout pass.
            if (getMeasuredHeight() > 0) {
                mTotalHeight = calculateTotalHeight();
                mCurrentOffsetY = getTopOffsetBounds();
            } else {
                mTotalHeight = 0;
            }
        }

        requestLayout();
        invalidate();
    }

    /**
     * Set desired channel selected
     *
     * @param channelPosition New selected position
     */
    @Override
    public void setSelection(int channelPosition) {
        setSelection(channelPosition, INVALID_POSITION);
    }

    /**
     * Set desired channel selected with desired event
     *
     * @param channelPosition New selected position
     * @param eventPosition   New selected event position
     */
    @Override
    public void setSelection(int channelPosition, int eventPosition) {
        // TODO
        mScrollState = SCROLL_STATE_NORMAL;
        mCurrentOffsetY = getYScrollCoordinateForPosition(channelPosition);
        if (eventPosition > INVALID_POSITION) {
            //TODO
        }
        update();
    }

    @Override
    public View getSelectedView() {
        return mSelectedView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int viewWidth = getMeasuredWidth();
        final int viewHeight = getMeasuredHeight();
        log("ONMEASURE viewWidth=" + viewWidth + ", viewHeight=" + viewHeight);

        // Calculate rect objects for three different areas
        mRectTimeLine = new Rect();
        mRectChannelIndicators = new Rect();
        mRectEventsArea = new Rect();
        mRectSelectedRowArea = new Rect();

        // Calculate channel row height
        mChannelRowHeight = (int) ((float) viewHeight / (float) (mNumberOfVisibleChannels + BIG_CHANNEL_MULTIPLIER));
        mChannelRowHeight -= mVerticalDividerHeight;

        mChannelRowHeightExpanded = viewHeight
                - (mNumberOfVisibleChannels * mChannelRowHeight)
                - (mNumberOfVisibleChannels - 1) * mVerticalDividerHeight;
        mChannelRowHeightExpanded -= mVerticalDividerHeight;
        // Setup rect for sections
        mRectTimeLine.set(mChannelRowHeight, 0, viewWidth, mChannelRowHeight);
        mRectChannelIndicators.set(0, mChannelRowHeight, mChannelRowHeight,
                viewHeight);
        mRectEventsArea.set(mChannelRowHeight, mChannelRowHeight, viewWidth,
                viewHeight);

        // Setup selected row area
        final int selectedTop = mRectEventsArea.top
                + (mNumberOfVisibleChannels / 2)
                * (mChannelRowHeight + mVerticalDividerHeight);
        mRectSelectedRowArea.set(mChannelRowHeight, selectedTop, viewWidth,
                selectedTop + mChannelRowHeightExpanded);

        if (mSelectionType == SelectionType.FIXED_ON_SCREEN) {
            mSelectionAbsolutePosition = (int) ((mRectEventsArea.right - mRectEventsArea.left)
                    * mSelectionRelativePosition);
        }
        // Calculate total scroll value if adapter is setted
        if (mTotalHeight == 0 && mAdapter != null) {
            mTotalHeight = calculateTotalHeight();
        }
        if (mCurrentOffsetY == 0) {
            mCurrentOffsetY = getTopOffsetBounds();
        }

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //Clip the canvas so that only events and time line area can be drawn into
        canvas.clipRect(mRectTimeLine.left, 0, mRectTimeLine.right, mRectEventsArea.bottom);
        // Draws selector on top of Guide view
        drawSelector(canvas);
        //Draw time line and time line indicator
        drawTimeLine(canvas);
        drawTimeLineIndicator(canvas);

    }

    /**
     * Draw selector drawable
     *
     * @param canvas
     */
    private void drawSelector(Canvas canvas) {
        if (mSelector != null && mSelectedView != null) {
            mSelectorRect.left = mSelectedView.getLeft();
            mSelectorRect.right = mSelectedView.getRight();
            mSelectorRect.top = mSelectedView.getTop();
            mSelectorRect.bottom = mSelectedView.getBottom();
            mSelector.setBounds(mSelectorRect);
            mSelector.draw(canvas);
        }
    }

    /**
     * Draw times on time line
     */
    private void drawTimeLine(Canvas canvas) {
        Calendar calendar = (Calendar) mStartTime.clone();
        calendar.add(Calendar.MINUTE, mCurrentOffsetX / mOneMinuteWidth);
        Calendar leftEdgeOfGuide = (Calendar) calendar.clone();
        Calendar rightEdgeOfGuide = (Calendar) calendar.clone();
        rightEdgeOfGuide.add(Calendar.MINUTE, mRectEventsArea.width() / mOneMinuteWidth);
        if (calendar.get(Calendar.MINUTE) < 15) {
            calendar.set(Calendar.MINUTE, 0);
        } else if (calendar.get(Calendar.MINUTE) < 45) {
            calendar.set(Calendar.MINUTE, 30);
        } else {
            calendar.add(Calendar.MINUTE, 60 - calendar.get(Calendar.MINUTE));
        }
        String timeText;
        while (calendar.getTimeInMillis() < rightEdgeOfGuide.getTimeInMillis()) {
            timeText = mTimeLineFormater.format(calendar.getTime());
            mTimeLinePaintText.getTextBounds(timeText, 0,
                    timeText.length(), mTimeLineRectText);
            canvas.drawText(timeText, mRectTimeLine.left + (calendar.getTimeInMillis() -
                            leftEdgeOfGuide.getTimeInMillis()) / 60000 * mOneMinuteWidth,
                    mRectTimeLine.height() / 2 + mTimeLineTextSize / 2,
                    mTimeLinePaintText);
            calendar.add(Calendar.MINUTE, 30);
        }
    }

    /**
     * Draw time line indicator over the guide content
     */
    private void drawTimeLineIndicator(Canvas canvas) {
        if (mTimeLineProgressIndicator != null) {
            Calendar cal = Calendar.getInstance();
            Calendar leftEdgeOfGuide = (Calendar) mStartTime.clone();
            leftEdgeOfGuide.add(Calendar.MINUTE, mCurrentOffsetX / mOneMinuteWidth);
            mTimeLineProgressIndicatorRect.right = (int) (mRectTimeLine.left + (cal.getTimeInMillis() - leftEdgeOfGuide
                    .getTimeInMillis()) / 60000 * mOneMinuteWidth);
            mTimeLineProgressIndicatorRect.top = mRectEventsArea.top;
            mTimeLineProgressIndicatorRect.bottom = mRectEventsArea.bottom;
            mTimeLineProgressIndicatorRect.left = mTimeLineProgressIndicatorRect.right - mTimeLineProgressIndicator
                    .getIntrinsicWidth();
            mTimeLineProgressIndicator.setBounds(mTimeLineProgressIndicatorRect);
            mTimeLineProgressIndicator.draw(canvas);
        }
    }

    @Override
    public void requestLayout() {
        if (!mBlockLayoutRequests && !mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        if (!changed) {
            return;
        }
        // Layout children only if guide view changed its bounds
        layoutChildren();
        mInLayout = false;
    }

    /**
     * Position and draw children on the screen
     */
    private void layoutChildren() {
        final boolean blockLayoutRequests = mBlockLayoutRequests;
        if (blockLayoutRequests) {
            return;
        }
        mBlockLayoutRequests = true;
        try {
            if (mAdapter != null) {
                calculateRowPositions();
                layoutEvents();
                layoutChannelIndicators();
                layoutTimeLine();
            }
        } finally {
            if (!blockLayoutRequests) {
                mBlockLayoutRequests = false;
            }
        }
    }

    /**
     * Position channel indicators
     */
    protected abstract void layoutChannelIndicators();

    /**
     * Position time line items
     */
    protected abstract void layoutTimeLine();

    /**
     * Position guide events
     */
    protected abstract void layoutEvents();

    /**
     * Recalculates row positions
     */
    protected abstract void calculateRowPositions();

    /**
     * Updates screen while selection is moving or scrolling, this is used for drawing every frame
     */
    void update() {
        mRecycler.removeInvisibleItems();
        awakenScrollBars();
        layoutChildren();
        invalidate();
    }

    /**
     * Reposition and redraw items. You may want to call this method after the
     * data provider changes.
     */
    //TODO use this in data set observer
    public void redrawItems() {
        mRecycler.moveAllViewsToRecycle();
        removeAllViewsInLayout();
        layoutChildren();
        invalidate();
    }

    /**
     * @return Calculated maximum scroll value
     */
    private int calculateTotalHeight() {
        return mChannelsCount * mChannelRowHeight + mVerticalDividerHeight
                * mChannelsCount + mChannelRowHeightExpanded +
                (mNumberOfVisibleChannels / 2) * (mChannelRowHeight + mVerticalDividerHeight);
    }

    /**
     * Recalculates first channel position
     */
    protected void calculateFirstChannelPosition() {
        /**
         * In fast scroll we must take into account expanded channel
         */
        if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
            // Expanded item is on the screen and it is not first visible
            if (mFirstItemPosition < mExpandedItemIndex) {
                mFirstItemPosition = mCurrentOffsetY
                        / (mChannelRowHeight + mVerticalDividerHeight);
            }
            // Expanded was first visible on the screen, check if it is still
            // visible
            else if (mFirstItemPosition == mExpandedItemIndex) {
                // Calculate sum before expanded
                int sum = mExpandedItemIndex
                        * (mChannelRowHeight + mVerticalDividerHeight);
                // Expanded is moved down so every invisible channel is normal
                // size
                if (sum >= mCurrentOffsetY) {
                    mFirstItemPosition = mCurrentOffsetY
                            / (mChannelRowHeight + mVerticalDividerHeight);
                    return;
                }
                sum += (mChannelRowHeightExpanded + mVerticalDividerHeight);
                // Expanded is scrolled out of visible screen
                if (sum < mCurrentOffsetY) {
                    mFirstItemPosition = mExpandedItemIndex + 1;
                }
            }
            // Expanded is not visible, it is above visible area
            else {
                // We must take into account expanded item
                mFirstItemPosition = mCurrentOffsetY
                        / (mChannelRowHeight + mVerticalDividerHeight)
                        - (BIG_CHANNEL_MULTIPLIER - 1);
            }
        }
        /**
         * For normal scroll first visible position is easily calculated
         */
        else {
            mFirstItemPosition = mCurrentOffsetY
                    / (mChannelRowHeight + mVerticalDividerHeight);
        }
    }

    /**
     * Calculates Y overlap value of 2 rectangles
     *
     * @return Calculated overlap value
     */
    private int calculateYOverlapValue(Rect rect1, Rect rect2) {
        return Math.max(0, Math.min(rect1.bottom, rect2.bottom)
                - Math.max(rect1.top, rect2.top));
    }

    /**
     * Calculates current row height based on current row start Y position
     *
     * @param currentY          Current row start Y position
     * @param previousRowHeight Height of previously calculated row
     * @param oldHeightOfTheRow Height of the same row calculated in previous layout pass
     * @return Calculated row height
     */
    protected int calculateRowHeight(final int currentY, int previousRowHeight,
            int oldHeightOfTheRow, int channelIndex) {
        // Default row height
        int rowHeight = mChannelRowHeight;
        /**
         * For normal scroll we must calculate row height based on current Y
         * coordinate
         */
        if (mScrollState == SCROLL_STATE_NORMAL) {
            // Current Y coordinate is above selected row area
            if (currentY < mRectSelectedRowArea.top) {
                mChildRowHeightRect.set(mRectEventsArea.left, currentY,
                        mRectEventsArea.right, currentY + mChannelRowHeight);
                final int overlapValue = calculateYOverlapValue(
                        mRectSelectedRowArea, mChildRowHeightRect);
                // There is overlap between child and selection area
                if (overlapValue > 0) {
                    // Calculates row height based on current overlap value
                    rowHeight = (mRectSelectedRowArea.top - mChildRowHeightRect.top)
                            + (int) Math
                            .ceil((float) mChannelRowHeightExpanded
                                    * ((float) overlapValue / (float) mChannelRowHeight));
                }
            }
            // Current Y coordinate is at the top of selection area
            else if (currentY == mRectSelectedRowArea.top) {
                rowHeight = mChannelRowHeightExpanded;
            }
            // Current Y coordinate is in the selected row area
            else if (currentY <= mRectSelectedRowArea.bottom) {
                rowHeight = mChannelRowHeightExpanded + mChannelRowHeight
                        - previousRowHeight;
            }
        }
        /**
         * For fast scroll we just use already calculated row heights in
         * previous layout pass
         */
        else if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
            if (oldHeightOfTheRow != INVALID_POSITION) {
                rowHeight = oldHeightOfTheRow;
            } else if (channelIndex == mExpandedItemIndex) {
                rowHeight = mChannelRowHeightExpanded;
            }
        } else if (mScrollState == SCROLL_STATE_FAST_SCROLL_END) {
            if (oldHeightOfTheRow != INVALID_POSITION) {
                rowHeight = oldHeightOfTheRow;
            }
        }
        return rowHeight;
    }

    /**
     * Get information from adapter about first visible event position for
     * desired channel
     *
     * @param scroll  Current X scroll
     * @param channel Desired channel
     * @return Object containing information's about first visible child and its
     * offset from left edge of the guide
     */
    protected FirstPositionInfo getPositionAndOffsetForScrollValue(int scroll,
            int channel) {
        int sum = 0;
        final int count = mAdapter.getEventsCount(channel);
        int width = 0;
        for (int i = 0; i < count; i++) {
            width = mAdapter.getEventWidth(channel, i) * mOneMinuteWidth;
            sum += width;
            if (sum > scroll) {
                return new FirstPositionInfo(i, width - (sum - scroll));
            }
        }
        return new FirstPositionInfo(-1, -1);
    }

    /**
     * Change size and position of view that is currently visible on screen
     *
     * @param viewToResize Desired view to resize
     * @param currentY     Current Y coordinate on the screen
     * @param width        Width of the view
     * @param newHeight    New height of the view
     */
    protected void resizeChildView(View viewToResize, int currentY, int width,
            int newHeight, int currentRowExpandedPercent) {
        LayoutParams params = (LayoutParams) viewToResize.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(width, newHeight);
        } else {
            params.width = width;
            params.height = newHeight;
            params.mCurrentResizePercentValue = currentRowExpandedPercent;
        }
        measureEventItemView(viewToResize, width, newHeight);
        viewToResize.layout(viewToResize.getLeft(), currentY,
                viewToResize.getLeft() + width, currentY + newHeight);
    }

    /**
     * Add new view to the layout.
     *
     * @param layoutType   Type of layout pass, can be:
     *                     BaseGuideView.LAYOUT_TYPE_CHANNEL_INDICATOR
     *                     BaseGuideView.LAYOUT_TYPE_TIME_LINE
     *                     BaseGuideView.LAYOUT_TYPE_EVENTS
     * @param child        View to add.
     * @param left         Left position, relative to parent.
     * @param top          Top position, relative to parent.
     * @param width        Item view width.
     * @param height       Item view height.
     * @param channelIndex Channel index of view that is represent
     * @param eventIndex   Index of event this view represents
     */
    protected void addChildView(int layoutType, View child, int left, int top,
            int width, int height, int channelIndex, int eventIndex) {
        switch (layoutType) {
        case LAYOUT_TYPE_EVENTS: {
            mRecycler.addEventView(child);
            addViewToLayout(child, width, height, channelIndex, eventIndex);
            measureEventItemView(child, width, height);
            child.layout(left, top, left + width, top + height);
            break;
        }
        case LAYOUT_TYPE_CHANNEL_INDICATOR: {
            mRecycler.addChannelIndicatorView(child);
            addViewToLayout(child, width, height, channelIndex,
                    INVALID_POSITION);
            measureEventItemView(child, width, height);
            child.layout(left, top, left + width, top + height);
            break;
        }
        case LAYOUT_TYPE_OVERLAP_VIEW: {
            addViewToLayout(child, width, height, INVALID_POSITION,
                    INVALID_POSITION);
            measureEventItemView(child, width, height);
            child.layout(left, top, left + width, top + height);
            break;
        }
        }
        child.invalidate();
    }

    /**
     * Measures view with desired width and height
     *
     * @param view   View to measure
     * @param width  Desired width
     * @param height Desired height
     */
    private void measureEventItemView(final View view, final int width,
            final int height) {
        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
                MeasureSpec.EXACTLY);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(height,
                MeasureSpec.EXACTLY);
        view.measure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Adds created view to layout to be drawn
     *
     * @param view         View to add
     * @param width        Desired width of view
     * @param height       Desired height of view
     * @param channelIndex Channel index of view that is represent
     * @param eventIndex   Index of event this view represents
     */
    private void addViewToLayout(final View view, int width, int height,
            int channelIndex, int eventIndex) {
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(width, height);
        } else {
            params.width = width;
            params.height = height;
            params.mCurrentResizePercentValue = height == mChannelRowHeightExpanded ? 100
                    : 0;
        }
        params.mChannelIndex = channelIndex;
        params.mEventIndex = eventIndex;
        addViewInLayout(view, -1, params, true);
    }

    /**
     * Find view at desired coordinates.
     *
     * @param touchX X coordinate of touch.
     * @param touchY Y coordinate of touch.
     * @return Desired view or null if nothing was found.
     */
    private View getViewAtCoordinates(float touchX, float touchY) {
        // If touched view is from time line section or from channel indicators
        // section we ignore it
        if (mRectTimeLine.contains((int) touchX, (int) touchY)
                || mRectChannelIndicators.contains((int) touchX, (int) touchY)) {
            return null;
        }
        for (View view : mRecycler.mActiveEventsViews) {
            if (touchX > view.getLeft() && touchX < view.getRight()
                    && view.getTop() < touchY && view.getBottom() > touchY) {
                return view;
            }
        }
        return null;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        // offset content if the scroller fling is still active
        if (mScroll.computeScrollOffset()) {
            offsetBy(mScroll.getCurrX() - mCurrentOffsetX, mScroll.getCurrY()
                    - mCurrentOffsetY);
        }
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return mCurrentOffsetX;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return mCurrentOffsetY;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mTotalHeight;
    }

    /**
     * Offset all children views, so user will see an illusion of content
     * scrolling or flinging.
     *
     * @param offsetDeltaX Offset X delta.
     * @param offsetDeltaY Offset Y delta.
     */
    private void offsetBy(float offsetDeltaX, float offsetDeltaY) {
        // adjust offset values
        final int adjustedOffsetDeltaX = adjustOffsetDelta(mCurrentOffsetX,
                offsetDeltaX, getRightOffsetBounds(), false);
        final int adjustedOffsetDeltaY = adjustOffsetDelta(mCurrentOffsetY,
                offsetDeltaY, getBottomOffsetBounds(), true);
        // offset views
        // First offset event views
        int childCount = mRecycler.mActiveEventsViews.size();
        int i;
        for (i = 0; i < childCount; i++) {
            mRecycler.mActiveEventsViews.get(i).offsetLeftAndRight(
                    -adjustedOffsetDeltaX);
            mRecycler.mActiveEventsViews.get(i).offsetTopAndBottom(
                    -adjustedOffsetDeltaY);
        }
        // Offset channel indicators
        childCount = mRecycler.mActiveChannelIndicatorViews.size();
        for (i = 0; i < childCount; i++) {
            mRecycler.mActiveChannelIndicatorViews.get(i).offsetTopAndBottom(
                    -adjustedOffsetDeltaY);
        }
        // update state
        mCurrentOffsetX += adjustedOffsetDeltaX;
        mCurrentOffsetY += adjustedOffsetDeltaY;
        update();
    }

    /**
     * Starts fling scroll of EpgView
     *
     * @param velocityX
     * @param velocityY
     */
    private void fling(int velocityX, int velocityY) {
        mScroll.forceFinished(true);
        mScroll.fling(mCurrentOffsetX, mCurrentOffsetY, velocityX, velocityY,
                0, getRightOffsetBounds(), getTopOffsetBounds(), getBottomOffsetBounds());
        invalidate();
    }

    /**
     * Scroll guide so the selection area should be always populated with
     * selected views
     *
     * @return
     */
    protected boolean moveSelectedViewsToSelectionBounds() {
        if (mScrollState == SCROLL_STATE_NORMAL) {
            final View firstVisibleChild = isItemAttachedToWindow(
                    LAYOUT_TYPE_CHANNEL_INDICATOR, mFirstItemPosition,
                    INVALID_POSITION);
            if (firstVisibleChild != null) {
                int topInvisiblePart = mRectEventsArea.top
                        - firstVisibleChild.getTop();
                int topVisiblePart = firstVisibleChild.getHeight()
                        - topInvisiblePart;
                int scrollBy = (topInvisiblePart <= topVisiblePart ? -topInvisiblePart
                        : topVisiblePart + mVerticalDividerHeight);
                if (scrollBy != 0) {
                    mSmoothScrollRunnable.startScrollBy(0, scrollBy);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @return Returns maximum amount of scroll to the bottom (how much
     * mCurrentOffsetY can be)
     */
    private int getBottomOffsetBounds() {
        return computeVerticalScrollRange() - getHeight() + getPaddingBottom()
                + getPaddingTop() + (mScrollState == SCROLL_STATE_FAST_SCROLL ? (mChannelRowHeight
                + mVerticalDividerHeight) * BIG_CHANNEL_MULTIPLIER / 2 : 0);
    }

    /**
     * @return Returns maximum amount of scroll to the right (how much
     * mCurrentOffsetX can be)
     */
    private int getRightOffsetBounds() {
        return computeHorizontalScrollRange() - getWidth() + getPaddingLeft()
                + getPaddingRight();
    }

    /**
     * @return Returns Y top limit
     */
    private int getTopOffsetBounds() {
        if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
            return -mNumberOfVisibleChannels / 2 * (mChannelRowHeight + mVerticalDividerHeight) - (mChannelRowHeight
                    + mVerticalDividerHeight);
        } else {
            return -mNumberOfVisibleChannels / 2 * (mChannelRowHeight + mVerticalDividerHeight);
        }
    }

    /**
     * @param desiredPosition
     * @return Returns desired Y scroll coordinate for desired position and SCROLL_STATE_NORMAL
     */
    protected int getYScrollCoordinateForPosition(int desiredPosition) {
        return (desiredPosition - mNumberOfVisibleChannels / 2)
                * (mChannelRowHeight + mVerticalDividerHeight);
    }

    /**
     * Adjust requested offset delta to don't allow move content outside of the
     * content bounds.
     *
     * @param currentOffset    Current offset.
     * @param offsetDelta      Desired offset delta to apply.
     * @param maxAllowedOffset Max allowed offset to left. Usually it's difference between
     *                         content size and view size.
     * @return Adjusted offset delta.
     */
    private int adjustOffsetDelta(int currentOffset, float offsetDelta,
            int maxAllowedOffset, boolean isForY) {
        // if view content size is smaller than the view size, offset is 0, i.e.
        // we can't offset the content
        if (maxAllowedOffset < 0) {
            return 0;
        }

        // limit offset for top and left edges
        if (isForY) {
            if (currentOffset + offsetDelta <= getTopOffsetBounds()) {
                return -currentOffset + getTopOffsetBounds();
            }
        } else {
            if (currentOffset + offsetDelta <= 0) {
                return -currentOffset;
            }
        }

        // limit offset for bottom and right edges
        if (currentOffset + offsetDelta >= maxAllowedOffset) {
            return maxAllowedOffset - currentOffset;
        }
        return (int) offsetDelta;
    }

    /**
     * Check is view (at least one pixel of it) is inside the visible area or
     * not.
     *
     * @param view View to check is it visible or not.
     * @return TRUE if the view is invisible i.e. out of visible screen bounds,
     * FALSE - if at least one pixel of it is visible.
     */
    protected boolean isViewInvisible(Rect desiredRect, View view) {
        return view == null || view.getLeft() >= desiredRect.right
                || view.getRight() <= desiredRect.left
                || view.getTop() >= desiredRect.bottom
                || view.getBottom() <= desiredRect.top;
    }

    /**
     * Determine if view for desired channel and desired event is already
     * visible
     *
     * @param layoutType   Type of layout pass, can be:
     *                     BaseGuideView.LAYOUT_TYPE_CHANNEL_INDICATOR
     *                     BaseGuideView.LAYOUT_TYPE_TIME_LINE
     *                     BaseGuideView.LAYOUT_TYPE_EVENTS
     * @param channelIndex Desired channel index
     * @param eventIndex   Desired event index
     * @return View on the screen, NULL otherwise
     */
    protected View isItemAttachedToWindow(int layoutType, int channelIndex,
            int eventIndex) {
        switch (layoutType) {
        case LAYOUT_TYPE_CHANNEL_INDICATOR: {
            for (View v : mRecycler.mActiveChannelIndicatorViews) {
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                if (lp.mChannelIndex == channelIndex) {
                    return v;
                }
            }
            break;
        }
        case LAYOUT_TYPE_EVENTS: {
            for (View v : mRecycler.mActiveEventsViews) {
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                if (lp.mChannelIndex == channelIndex
                        && lp.mEventIndex == eventIndex) {
                    return v;
                }
            }
            break;
        }
        }
        return null;
    }

    /**
     * @param channelIndex
     * @return Returns last visible event for desired channel
     */
    protected View getLastVisibleEventView(int channelIndex) {
        View lastVisible = null;
        int lastEventIndex = 0;
        for (View v : mRecycler.mActiveEventsViews) {
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (lp.mChannelIndex == channelIndex
                    && lp.mEventIndex > lastEventIndex) {
                lastEventIndex = lp.mEventIndex;
                lastVisible = v;
            }
        }
        return lastVisible;
    }

    /**
     * @param channelIndex
     * @return Returns first visible event for desired channel
     */
    protected View getFirstVisibleEventView(int channelIndex) {
        View firstVisible = null;
        int firstEventIndex = Integer.MAX_VALUE;
        for (View v : mRecycler.mActiveEventsViews) {
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (lp.mChannelIndex == channelIndex
                    && lp.mEventIndex < firstEventIndex) {
                firstEventIndex = lp.mEventIndex;
                firstVisible = v;
            }
        }
        return firstVisible;
    }

    /**
     * Select events with LEFT/RIGHT keys
     *
     * @param keyCode KeyEvent.KEYCODE_DPAD_LEFT or KeyEvent.KEYCODE_DPAD_RIGHT
     * @return TRUE if key press was handled, FALSE otherwise
     */
    protected boolean selectRightLeftView(int keyCode) {
        //Vertical scroll can not be interrupted by LEFT/RIGHT keys.
        if (mScrollState != SCROLL_STATE_NORMAL || (mCurrentOffsetY % (mChannelRowHeight + mVerticalDividerHeight)
                != 0)) {
            return true;
        }
        int desiredEventIndex = mSelectedEventItemPosition + (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
        if (desiredEventIndex >= mAdapter.getEventsCount(mSelectedItemPosition) || desiredEventIndex <= -1) {
            return false;
        }
        final View nextView = isItemAttachedToWindow(LAYOUT_TYPE_EVENTS,
                mSelectedItemPosition, desiredEventIndex);
        int eventPosition = getEventPositionOnScreen(nextView, desiredEventIndex);
        if (mSelectionType == SelectionType.FIXED_ON_SCREEN) {
            if (nextView != null) {
                selectNextView(nextView);
            } else {
                mSelectedView = null;
                mSelectedEventItemPosition = desiredEventIndex;
            }

            int scrollBy = eventPosition - mSelectionAbsolutePosition;
            if (mCurrentOffsetX + scrollBy < 0) {
                scrollBy = -mCurrentOffsetX;
            }
            mSmoothScrollRunnable.startScrollBy(scrollBy, 0, SMOOTH_LEFT_RIGHT_DURATION);
        } else {
            //TODO Implement non fixed scroll
        }
        return true;
    }

    /**
     * @param nextView          Event view (NULL if it is not displayed on the screen)
     * @param desiredEventIndex Event index
     * @return Returns current absolute event position on screen
     */
    private int getEventPositionOnScreen(View nextView, int desiredEventIndex) {
        if (nextView != null) {
            return nextView.getLeft();
        } else {
            View firstLast = null;
            LayoutParams params = null;
            int desiredLeft = 0;
            if (desiredEventIndex > mSelectedEventItemPosition) {
                firstLast = getLastVisibleEventView(mSelectedItemPosition);
                if (firstLast == null) {
                    firstLast = mSelectedView;
                }
                if (firstLast == null) {
                    return mCurrentOffsetX + mOneMinuteWidth * 30;
                }
                params = (LayoutParams) firstLast.getLayoutParams();
                desiredLeft = firstLast.getRight();
                for (int i = params.mEventIndex + 1; i < desiredEventIndex; i++) {
                    desiredLeft += mAdapter.getEventWidth(mSelectedItemPosition, i) * mOneMinuteWidth;
                }
            } else {
                firstLast = getFirstVisibleEventView(mSelectedItemPosition);
                if (firstLast == null) {
                    firstLast = mSelectedView;
                }
                if (firstLast == null) {
                    return mCurrentOffsetX - mOneMinuteWidth * 30;
                }
                params = (LayoutParams) firstLast.getLayoutParams();
                desiredLeft = firstLast.getLeft();
                for (int i = params.mEventIndex - 1; i >= desiredEventIndex; i--) {
                    desiredLeft -= mAdapter.getEventWidth(mSelectedItemPosition, i) * mOneMinuteWidth;
                }
            }
            return desiredLeft;
        }
    }

    /**
     * Make new view selected. If desired view is not regular data (empty space) it can not be selected.
     *
     * @param newSelectedView New view that is selected
     */
    protected void selectNextView(View newSelectedView) {
        final View oldSelectedView = mSelectedView;
        if (oldSelectedView == null && newSelectedView == null) {
            return;
        }

        if (oldSelectedView != null) {
            oldSelectedView.setSelected(false);
        }

        if (newSelectedView == null) {
            mSelectedView = null;
            fireOnSelected();
            invalidate();
            return;
        }
        //If view represents empty space in guide
        LayoutParams params = (LayoutParams) newSelectedView.getLayoutParams();
        if (!mAdapter.hasRegularData(params.mChannelIndex, params.mEventIndex)) {
            mSelectedView = null;
            fireOnSelected();
            invalidate();
            return;
        }

        mSelectedView = newSelectedView;
        mSelectedEventItemPosition = ((LayoutParams) mSelectedView.getLayoutParams()).mEventIndex;
        newSelectedView.setSelected(true);
        fireOnSelected();
        invalidate();
    }

    /**
     * Fire on long press state change
     */
    void fireOnLongPressScrollStateChanged() {
        if (mOnLongPressScrollListener != null) {
            if (mScrollState == SCROLL_STATE_NORMAL) {
                mOnLongPressScrollListener.onLongPressScrollStopped();
            } else if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                mOnLongPressScrollListener.onLongPressScrollStarted();
            }
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(
            ViewGroup.LayoutParams p) {
        return new LayoutParams(p.width, p.height);
    }

    /**
     * Layout params class that is used for event views
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {

        public int mChannelIndex = INVALID_POSITION;
        public int mEventIndex = INVALID_POSITION;
        /**
         * Value that represents current size of view. If 0, view has small
         * size, if 100, view has fully expanded size
         */
        public int mCurrentResizePercentValue;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }

    void changeScrollState(int newScrollState, int keyCode) {
        log("changeScrollState, newScrollState=" + newScrollState
                + ", mScrollState=" + mScrollState + ", keyCode=" + keyCode);
        OnAnimationFinishedListener activeFinishListener = mSmoothScrollRunnable
                .getOnAnimationFinishedListener();
        switch (mScrollState) {
        case SCROLL_STATE_NORMAL: {
            if (newScrollState == SCROLL_STATE_FAST_SCROLL) {
                int difference = 0;
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    difference = 1;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    difference = -1;
                }
                //If fast scroll is starting with UP/DOWN key
                if (difference != 0) {
                    //Check if long press is pressed on up or down limits
                    //Bug fix for long press on limits (first or last channel)
                    if ((difference == 1 && mSelectedItemPosition > mChannelsCount - 2) || (difference == -1 &&
                            mSelectedItemPosition < 1)) {
                        return;
                    }
                    NormalToFastScrollFinishedListener animFinishedListener = new NormalToFastScrollFinishedListener(
                            difference);
                    if (mScroll.isFinished()) {
                        animFinishedListener.animationFinished();
                    } else if (activeFinishListener == null
                            || (activeFinishListener != null
                            && !(activeFinishListener instanceof NormalToFastScrollFinishedListener))) {
                        mSmoothScrollRunnable
                                .setOnAnimationFinishedListener(animFinishedListener);
                    }
                }
                //If fast scroll is starting programmatically
                else {
                    mScrollState = SCROLL_STATE_FAST_SCROLL;
                    fireOnLongPressScrollStateChanged();
                }
            }
            break;
        }
        case SCROLL_STATE_FAST_SCROLL: {
            if (newScrollState == SCROLL_STATE_NORMAL) {
                mScrollState = SCROLL_STATE_NORMAL;
                fireOnLongPressScrollStateChanged();
            } else if (newScrollState == SCROLL_STATE_FAST_SCROLL_END) {
                FastToFastScrollEndFinishedListener animFinishedListener = new FastToFastScrollEndFinishedListener();
                if (mScroll.isFinished()) {
                    animFinishedListener.animationFinished();
                } else {
                    mSmoothScrollRunnable
                            .setOnAnimationFinishedListener(animFinishedListener);
                }
            }
            break;
        }
        case SCROLL_STATE_FAST_SCROLL_END: {
            if (newScrollState == SCROLL_STATE_NORMAL) {
                mCurrentOffsetY = (mSelectedItemPosition - mNumberOfVisibleChannels / 2)
                        * (mChannelRowHeight + mVerticalDividerHeight);
                mScrollState = SCROLL_STATE_NORMAL;
                fireOnLongPressScrollStateChanged();
            }
            break;
        }
        }
    }

    /**
     * Internal listener for end of scroll runnable.
     */
    interface OnAnimationFinishedListener {
        /**
         * @return TRUE if new listener is set (so listener instance must be alive), FALSE otherwise (listener
         * instance can become NULL)
         */
        boolean animationFinished();
    }

    /**
     * Class that wait end of normal scroll and switches to fast scroll
     */
    private class NormalToFastScrollFinishedListener implements
            OnAnimationFinishedListener {
        private int mDifference;

        NormalToFastScrollFinishedListener(int difference) {
            this.mDifference = difference;
        }

        @Override
        public boolean animationFinished() {
            mScrollState = SCROLL_STATE_FAST_SCROLL;
            fireOnLongPressScrollStateChanged();
            mSmoothScrollRunnable.resumeVerticalScroll(mDifference);
            return false;
        }
    }

    /**
     * Class that wait end of normal scroll and switches to fast scroll
     */
    private class NormalToNormalScrollFinishedListener implements
            OnAnimationFinishedListener {
        private int mDesiredChannelPosition;
        private final int mScrollDuration;

        NormalToNormalScrollFinishedListener(int desiredChannelPosition,
                int duration) {
            this.mDesiredChannelPosition = desiredChannelPosition;
            this.mScrollDuration = duration;
        }

        @Override
        public boolean animationFinished() {
            mSmoothScrollRunnable.startVerticalScrollToPosition(
                    mDesiredChannelPosition, mScrollDuration);
            return false;
        }
    }

    /**
     * Class that wait end of normal scroll and switches to fast scroll
     */
    private class FastToFastScrollEndFinishedListener implements
            OnAnimationFinishedListener {

        FastToFastScrollEndFinishedListener() {
        }

        @Override
        public boolean animationFinished() {
            return fastScrollEnd();
        }
    }

    /**
     * Create animation that returns normal scroll from fast scroll
     */
    private boolean fastScrollEnd() {
        View selected = isItemAttachedToWindow(
                LAYOUT_TYPE_CHANNEL_INDICATOR, mSelectedItemPosition,
                INVALID_POSITION);
        if (selected != null) {
            // Expanded view can be null if it is out of screen
            final View expanded = isItemAttachedToWindow(
                    LAYOUT_TYPE_CHANNEL_INDICATOR, mExpandedItemIndex,
                    INVALID_POSITION);

            if (mSelectedItemPosition == mExpandedItemIndex && expanded != null
                    && mScrollState == SCROLL_STATE_FAST_SCROLL) {
                if (expanded.getTop() == mRectSelectedRowArea.top) {
                    changeScrollState(SCROLL_STATE_NORMAL, 0);
                    return false;
                }
                int difference = 1;
                if (expanded.getTop() > mRectSelectedRowArea.top) {
                    difference = -1;
                }
                mSmoothScrollRunnable.resumeVerticalScroll(difference);
                return true;
            } else {
                ResizeAnimation animation = new ResizeAnimation(
                        BaseGuideView.this, selected, expanded,
                        mChannelRowHeight, mChannelRowHeightExpanded);
                animation.setInterpolator(new AccelerateInterpolator());
                animation.setDuration(SMOOTH_FAST_SCROLL_END_DURATION);
                animation.setAnimationListener(mFastScrollEndAnimationListener);
                startAnimation(animation);
            }
        }
        return false;
    }

    /**
     * Smooth scroll runnable
     */
    class SmoothScrollRunnable implements Runnable {

        private OnAnimationFinishedListener mOnAnimationFinishedListener;
        private int mDesiredChannelPosition;

        /**
         * Calculates Y coordinate to scroll to
         *
         * @param newChannelPosition Scroll to new channel position
         * @return Calculated Y coordinate for new channel to be selected
         */
        private int calculateNewYPosition(int newChannelPosition) {
            if (newChannelPosition == mSelectedItemPosition) {
                return INVALID_POSITION;
            }
            /**
             * For normal scroll
             */
            if (mScrollState == SCROLL_STATE_NORMAL) {
                return getYScrollCoordinateForPosition(newChannelPosition);
            }
            /**
             * For fast scroll we must recalculate offset to scroll to
             */
            else if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                if (mCurrentOffsetY
                        % (mChannelRowHeight + mVerticalDividerHeight) == 0) {
                    return mCurrentOffsetY
                            + (newChannelPosition - mSelectedItemPosition)
                            * (mChannelRowHeight + mVerticalDividerHeight);
                } else {
                    return (newChannelPosition - (mNumberOfVisibleChannels
                            + BIG_CHANNEL_MULTIPLIER - 1) / 2)
                            * (mChannelRowHeight + mVerticalDividerHeight);
                }
            }
            return INVALID_POSITION;
        }

        /**
         * Resumes already active fast scroll to new position
         *
         * @param difference Difference of current selection to new selection
         */
        void resumeVerticalScroll(int difference) {
            if (!mScroll.isFinished()) {
                if (mDesiredChannelPosition >= mChannelsCount - 1) {
                    return;
                } else if (mDesiredChannelPosition <= 0) {
                    return;
                }
                /**
                 * If difference is in opposite direction of current scroll
                 */
                if ((mScroll.getCurrY() < mScroll.getFinalY() && difference < 0) || (mScroll.getCurrY() > mScroll
                        .getFinalY() && difference > 0)) {
                    mScroll.forceFinished(true);
                    mScroll.abortAnimation();
                    final int calculatedYCoordinate = calculateNewYPosition(mSelectedItemPosition + difference);
                    if (calculatedYCoordinate != INVALID_POSITION) {
                        startScrollTo(mCurrentOffsetX, calculatedYCoordinate,
                                SMOOTH_FAST_SCROLL_DURATION);
                    }
                    return;
                }

                final int newY = mScroll.getFinalY() + difference
                        * (mChannelRowHeight + mVerticalDividerHeight);
                mScroll.setFinalY(newY);
                mScroll.extendDuration(Math.abs(difference)
                        * SMOOTH_FAST_SCROLL_DURATION
                        + (mScroll.getDuration() - mScroll.timePassed()));
                mDesiredChannelPosition = mDesiredChannelPosition + difference;
            } else {
                final int calculatedYCoordinate = calculateNewYPosition(mSelectedItemPosition
                        + difference);
                startScrollTo(mCurrentOffsetX, calculatedYCoordinate,
                        SMOOTH_FAST_SCROLL_DURATION);
            }
        }

        /**
         * Starts vertical scroll to desired position
         *
         * @param newChannelPosition Desired position
         * @param duration           Duration of scroll
         */
        boolean startVerticalScrollToPosition(int newChannelPosition, int duration) {
            // if it is invisible perform fast scroll
            if (isScrollRunning()) {
                if (mScrollState == SCROLL_STATE_NORMAL) {
                    setOnAnimationFinishedListener(new NormalToNormalScrollFinishedListener(
                            newChannelPosition, duration));
                    return true;
                }
            } else {
                this.mDesiredChannelPosition = newChannelPosition;
                int calculatedYCoordinate = calculateNewYPosition(newChannelPosition);
                if (calculatedYCoordinate != INVALID_POSITION) {
                    /**
                     * If desired channel is off screen, fast scroll is used
                     */
                    int diff = Math.abs(newChannelPosition - mSelectedItemPosition);
                    if (diff > mNumberOfVisibleChannels / 2) {
                        changeScrollState(SCROLL_STATE_FAST_SCROLL, INVALID_POSITION);
                        calculatedYCoordinate +=
                                (calculatedYCoordinate > mCurrentOffsetY ? 1 : -1) * (mChannelRowHeight +
                                        mVerticalDividerHeight);
                    } else {
                        duration = diff * SMOOTH_SCROLL_DURATION;
                    }
                    return startScrollTo(mCurrentOffsetX, calculatedYCoordinate,
                            duration);
                }
            }
            return false;
        }

        boolean startScrollBy(int byX, int byY) {
            return startScrollBy(byX, byY, SMOOTH_SCROLL_DURATION);
        }

        boolean startScrollBy(int byX, int byY, int duration) {
            log("SmoothScrollRunnable startScrollBy, byX=" + byX + ", byY="
                    + byY);
            // Dont start scroll if difference is 0
            if (byX != 0 || byY != 0) {
                mScroll.forceFinished(true);// TODO check if this can be removed
                BaseGuideView.this.removeCallbacks(this);
                mScroll.startScroll(mCurrentOffsetX, mCurrentOffsetY, byX, byY,
                        duration);
                BaseGuideView.this.postOnAnimation(this);
                return true;
            }
            return false;
        }

        boolean startScrollTo(int toX, int toY) {
            return startScrollTo(toX, toY, SMOOTH_SCROLL_DURATION);
        }

        boolean startScrollTo(int toX, int toY, int duration) {
            if (toY > getBottomOffsetBounds() || toY < getTopOffsetBounds()
                    || toX > getRightOffsetBounds() || toX < 0) {
                return false;
            }
            return startScrollBy(toX - mCurrentOffsetX, toY - mCurrentOffsetY,
                    duration);
        }

        @Override
        public void run() {
            if (mScroll.isFinished()) {
                log("scroller is finished, done with smooth scroll");
                mDesiredChannelPosition = mSelectedItemPosition;
                if (mOnAnimationFinishedListener != null) {
                    if (!mOnAnimationFinishedListener.animationFinished()) {
                        mOnAnimationFinishedListener = null;
                    }
                } else if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                    fastScrollEnd();
                }
                return;
            }
            boolean animationRunning = mScroll.computeScrollOffset();
            int x = mScroll.getCurrX();
            int y = mScroll.getCurrY();
            //log("SmoothScrollRunnable RUNNNNN  y=" + y);
            int diffX = x - mCurrentOffsetX;
            int diffY = y - mCurrentOffsetY;
            if (diffX != 0 || diffY != 0) {
                offsetBy(diffX, diffY);
            }
            if (animationRunning) {
                BaseGuideView.this.postOnAnimationDelayed(this,
                        REFRESH_INTERVAL);
            }
        }

        public boolean isScrollRunning() {
            return !mScroll.isFinished();
        }

        public OnAnimationFinishedListener getOnAnimationFinishedListener() {
            return mOnAnimationFinishedListener;
        }

        public void setOnAnimationFinishedListener(
                OnAnimationFinishedListener mOnAnimationFinishedListener) {
            this.mOnAnimationFinishedListener = mOnAnimationFinishedListener;
        }
    }

    /**
     * Class for caching views that are no longer visible on the screen
     */
    class Recycler {
        /**
         * Recycle bin for unused guide events views
         */
        SparseArray<ArrayDeque<View>> mRecycledEventsViews = new SparseArray<ArrayDeque<View>>();
        /**
         * List for active event views that is currently visible on screen
         */
        ArrayList<View> mActiveEventsViews = new ArrayList<View>();

        /**
         * Recycle bin for unused channel indicators views
         */
        ArrayDeque<View> mRecycledChannelIndicatorViews = new ArrayDeque<View>();
        /**
         * List for active channel indicator views that is currently visible on
         * screen
         */
        ArrayList<View> mActiveChannelIndicatorViews = new ArrayList<View>();

        /**
         * Recycle bin for unused channel indicators views
         */
        ArrayDeque<View> mRecycledTimeLineViews = new ArrayDeque<View>();
        /**
         * List for active channel indicator views that is currently visible on
         * screen
         */
        ArrayList<View> mActiveTimeLineViews = new ArrayList<View>();

        /**
         * Add new view to list of active views
         *
         * @param view View to add
         */
        void addEventView(View view) {
            mActiveEventsViews.add(view);
        }

        /**
         * Add new channel indicator view to list of active views
         *
         * @param view View to add
         */
        void addChannelIndicatorView(View view) {
            mActiveChannelIndicatorViews.add(view);
        }

        /**
         * Add new time line view to list of active views
         *
         * @param view View to add
         */
        void addTimeLineView(View view) {
            mActiveChannelIndicatorViews.add(view);
        }

        /**
         * Get event view from recycler
         *
         * @param viewWidth Width of view to pull
         * @return Pulled view, or NULL if there is no such a view inside
         * recycler
         */
        View getEventView(final int viewWidth) {
            ArrayDeque<View> list = mRecycledEventsViews.get(viewWidth);
            if (list != null) {
                return list.poll();
            }
            return null;
        }

        /**
         * Get channel indicator view from recycler
         *
         * @return Pulled view, or NULL if there is no such a view inside
         * recycler
         */
        View getChannelIndicatorView() {
            return mRecycledChannelIndicatorViews.poll();
        }

        /**
         * Get time line view from recycler
         *
         * @return Pulled view, or NULL if there is no such a view inside
         * recycler
         */
        View getTimeLineView() {
            return mRecycledTimeLineViews.poll();
        }

        /**
         * Move view from active views to recycler. This method must be used
         * when view is no longer visible on screen so it should be recycled.
         *
         * @param view View that is no longer visible on screen
         */
        void recycleEventViews(View view) {
            final int viewWidth = view.getWidth();
            ArrayDeque<View> list = mRecycledEventsViews.get(viewWidth);
            if (list == null) {
                list = new ArrayDeque<View>();
                mRecycledEventsViews.put(viewWidth, list);
            }
            list.offer(view);
        }

        /**
         * Move view from active views to recycler. This method must be used
         * when view is no longer visible on screen so it should be recycled.
         *
         * @param view View that is no longer visible on screen
         */
        void recycleChannelIndicatorViews(View view) {
            mRecycledChannelIndicatorViews.offer(view);
        }

        /**
         * Move view from active views to recycler. This method must be used
         * when view is no longer visible on screen so it should be recycled.
         *
         * @param view View that is no longer visible on screen
         */
        void recycleTimeLineViews(View view) {
            mRecycledChannelIndicatorViews.offer(view);
        }

        /**
         * Move all views from active views to recycler views
         */
        void moveAllViewsToRecycle() {
            // Recycle event views
            for (View v : mActiveEventsViews) {
                recycleEventViews(v);
            }
            mActiveEventsViews.clear();
            // Recycle channel indicator views
            for (View v : mActiveChannelIndicatorViews) {
                recycleChannelIndicatorViews(v);
            }
            mActiveChannelIndicatorViews.clear();
            // Recycle time line views
            for (View v : mActiveTimeLineViews) {
                recycleTimeLineViews(v);
            }
            mActiveTimeLineViews.clear();
        }

        /**
         * Clear all views from cache, this method should be called on setting
         * new adapter to GridView
         */
        void clearAll() {
            // Clear all cache of event views
            mActiveEventsViews.clear();
            final int length = mRecycledEventsViews.size();
            // Make sure to clear all views
            for (int i = 0; i < length; i++) {
                ArrayDeque<View> list = mRecycledEventsViews
                        .get(mRecycledEventsViews.keyAt(i));
                list.clear();
            }
            mRecycledEventsViews.clear();
            // Clear all cache of channel indicators views
            mActiveChannelIndicatorViews.clear();
            mRecycledChannelIndicatorViews.clear();
            // Clear all cache of time line views
            mActiveTimeLineViews.clear();
            mRecycledTimeLineViews.clear();
        }

        /**
         * Remove all views that belongs to desired channel position
         */
        void removeItemsAtPosition(int channelPosition) {
            View v = null;
            LayoutParams lp = null;
            int i;
            // Remove event views
            for (i = mRecycler.mActiveEventsViews.size() - 1; --i >= 0; ) {
                v = mRecycler.mActiveEventsViews.get(i);
                lp = (LayoutParams) v.getLayoutParams();
                if (lp.mChannelIndex == channelPosition) {
                    mRecycler.mActiveEventsViews.remove(i);
                    mRecycler.recycleEventViews(v);
                    removeViewInLayout(v);
                }
            }
            // Remove channel indicator views
            for (i = mRecycler.mActiveChannelIndicatorViews.size() - 1; --i >= 0; ) {
                v = mRecycler.mActiveChannelIndicatorViews.get(i);
                lp = (LayoutParams) v.getLayoutParams();
                if (lp.mChannelIndex == channelPosition) {
                    mRecycler.mActiveChannelIndicatorViews.remove(i);
                    mRecycler.recycleChannelIndicatorViews(v);
                    removeViewInLayout(v);
                }
            }
        }

        /**
         * Remove invisible views and recycle them for later use
         */
        void removeInvisibleItems() {
            View v = null;
            // Check events recycler
            int i;
            for (i = mRecycler.mActiveEventsViews.size() - 1; --i >= 0; ) {
                v = mRecycler.mActiveEventsViews.get(i);
                if (isViewInvisible(mRectEventsArea, v)) {
                    mRecycler.mActiveEventsViews.remove(i);
                    mRecycler.recycleEventViews(v);
                    removeViewInLayout(v);
                }
            }
            // Check channel indicators recycler
            for (i = mRecycler.mActiveChannelIndicatorViews.size() - 1; --i >= 0; ) {
                v = mRecycler.mActiveChannelIndicatorViews.get(i);
                if (isViewInvisible(mRectChannelIndicators, v)) {
                    mRecycler.mActiveChannelIndicatorViews.remove(i);
                    mRecycler.recycleChannelIndicatorViews(v);
                    removeViewInLayout(v);
                }
            }
            // Check time line recycler
            for (i = mRecycler.mActiveTimeLineViews.size() - 1; --i >= 0; ) {
                v = mRecycler.mActiveTimeLineViews.get(i);
                if (isViewInvisible(mRectTimeLine, v)) {
                    mRecycler.mActiveTimeLineViews.remove(i);
                    mRecycler.recycleChannelIndicatorViews(v);
                    removeViewInLayout(v);
                }
            }
        }
    }
}
