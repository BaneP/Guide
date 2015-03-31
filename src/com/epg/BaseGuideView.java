package com.epg;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
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

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Base guide view class contains guide scroll implementation, selections.
 */
public abstract class BaseGuideView extends GuideAdapterView<BaseGuideAdapter> {
    static final int NUMBER_OF_MINUTES_IN_DAY = 1440;
    public static final int BIG_CHANNEL_MULTIPLIER = 3;
    /**
     * Types of layout pass
     */
    static final int LAYOUT_TYPE_TIME_LINE = 0;
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
    public static final int SMOOTH_FAST_SCROLL_DURATION = 170;

    /**
     * Duration of smooth fast scroll end animation
     */
    public static final int SMOOTH_FAST_SCROLL_END_DURATION = 200;
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
    int mExpandedChannelIndex = INVALID_POSITION;

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
     * Selector drawable
     */
    protected Drawable mSelector;
    /**
     * Selection rectangle position object, holds information about selected
     * view coordinates on screen
     */
    private Rect mSelectorRect = new Rect();
    /**
     * Time line progress indicator drawable
     */
    // TODO draw time line progress on top of view
    protected Drawable mTimeLineProgressIndicator;
    /**
     * Time line indicator text format
     */
    protected String mTimeLineTextFormat = "h:mm";
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
            // We will use fast scroll for vertical fling
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
                // performItemClick(mTouchedView, lp.mChannelIndex,
                // lp.mEventIndex);
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
                mTimeLineTextFormat = a
                        .getNonResourceString(R.styleable.BaseGuideView_timeLineTextFormat);
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
        // Initialize recycler
        mRecycler = new Recycler();
        // Used for calculating child row height
        mChildRowHeightRect = new Rect();
        // Initialize guide view
        setBackgroundColor(Color.BLACK);
        setFocusable(true);

        //
        mTimeList = new ColorDrawable(0x22EE0000);// RED
        mChannels = new ColorDrawable(0x2200EE00);// GREEN
        mEvents = new ColorDrawable(0x220000EE);// BLUE
        mSelectionRow = new ColorDrawable(0x88EE0000);// RED
    }

    // FIXME Delete this
    private Drawable mTimeList, mChannels, mEvents, mSelectionRow;

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
        mFirstChannelPosition = 0;
        mLastChannelPosition = INVALID_POSITION;
        removeAllViewsInLayout();
        mRecycler.clearAll();

        // Initialize some elements from adapter
        if (mAdapter != null) {
            mChannelItemCount = mAdapter.getChannelsCount();
            // Get minute pixel width
            mOneMinuteWidth = mAdapter.getOneMinuteWidth();
            // Calculate total grid width
            // TODO change to calculated
            mTotalWidth = 5205;// mOneMinutePixelWidth *
            // NUMBER_OF_MINUTES_IN_DAY*
            // mNumberOfDaysToDisplayData;

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
        // TODO

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
        if (mRectTimeLine != null) {
            mTimeList.setBounds(mRectTimeLine);
            mTimeList.draw(canvas);
        }
        if (mRectChannelIndicators != null) {
            mChannels.setBounds(mRectChannelIndicators);
            mChannels.draw(canvas);
        }
        if (mRectEventsArea != null) {
            mEvents.setBounds(mRectEventsArea);
            mEvents.draw(canvas);
        }
        if (mRectSelectedRowArea != null) {
            mSelectionRow.setBounds(mRectSelectedRowArea);
            mSelectionRow.draw(canvas);
        }
        // Draws selector on top of Guide view
        if (mSelectedView != null) {
            mSelectorRect.left = mSelectedView.getLeft();
            mSelectorRect.top = mSelectedView.getTop();
            mSelectorRect.right = mSelectedView.getRight();
            mSelectorRect.bottom = mSelectedView.getBottom();
            mSelector.setBounds(mSelectorRect);
            mSelector.draw(canvas);
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
     * Updates screen while selection is moving or scrolling
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
        return mChannelItemCount * mChannelRowHeight + mVerticalDividerHeight
                * mChannelItemCount + mChannelRowHeightExpanded +
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
            if (mFirstChannelPosition < mExpandedChannelIndex) {
                mFirstChannelPosition = mCurrentOffsetY
                        / (mChannelRowHeight + mVerticalDividerHeight);
            }
            // Expanded was first visible on the screen, check if it is still
            // visible
            else if (mFirstChannelPosition == mExpandedChannelIndex) {
                // Calculate sum before expanded
                int sum = mExpandedChannelIndex
                        * (mChannelRowHeight + mVerticalDividerHeight);
                // Expanded is moved down so every invisible channel is normal
                // size
                if (sum >= mCurrentOffsetY) {
                    mFirstChannelPosition = mCurrentOffsetY
                            / (mChannelRowHeight + mVerticalDividerHeight);
                    return;
                }
                sum += (mChannelRowHeightExpanded + mVerticalDividerHeight);
                // Expanded is scrolled out of visible screen
                if (sum < mCurrentOffsetY) {
                    mFirstChannelPosition = mExpandedChannelIndex + 1;
                }
            }
            // Expanded is not visible, it is above visible area
            else {
                // We must take into account expanded item
                mFirstChannelPosition = mCurrentOffsetY
                        / (mChannelRowHeight + mVerticalDividerHeight)
                        - (BIG_CHANNEL_MULTIPLIER - 1);
            }
        }
        /**
         * For normal scroll first visible position is easily calculated
         */
        else {
            mFirstChannelPosition = mCurrentOffsetY
                    / (mChannelRowHeight + mVerticalDividerHeight);
        }
    }

    /**
     * Calculates Y overlap value of 2 rectangles
     *
     * @return Calculated overlap value
     */
    private int calculateYOverlapValue(Rect rect1, Rect rect2) {
        return Math.max(
                0,
                Math.min(rect1.bottom, rect2.bottom)
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
            } else if (channelIndex == mExpandedChannelIndex) {
                rowHeight = mChannelRowHeightExpanded;
            }
        } else if (mScrollState == SCROLL_STATE_FAST_SCROLL_END) {
            // TODO
            // if (channelIndex == mSelectedItemPosition) {
            if (oldHeightOfTheRow != INVALID_POSITION) {
                rowHeight = oldHeightOfTheRow;
            }
            // }
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
            width = mAdapter.getEventWidth(channel, i);
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
        case LAYOUT_TYPE_TIME_LINE: {
            // TODO
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
        // TODO offset time line views

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
                    LAYOUT_TYPE_CHANNEL_INDICATOR, mFirstChannelPosition,
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
                + mVerticalDividerHeight) : 0);
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
        case LAYOUT_TYPE_TIME_LINE: {
            // TODO what to do here?
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
     * Make new view selected.
     *
     * @param newSelectedView New view that is selected
     */
    protected void selectNextView(View newSelectedView) {
        // TODO try this
        final View oldSelectedView = mSelectedView;
        if (oldSelectedView == null && newSelectedView == null) {
            return;
        }
        Rect oldViewRect = null, newViewRect;
        if (oldSelectedView != null) {
            oldSelectedView.setSelected(false);
            oldViewRect = new Rect(oldSelectedView.getLeft(),
                    oldSelectedView.getTop(), oldSelectedView.getRight(),
                    oldSelectedView.getBottom());
        }
        mSelectedView = newSelectedView;
        if (oldViewRect != null) {
            invalidate(oldViewRect);
        }
        if (newSelectedView == null) {
            fireOnSelected();
            return;
        }
        newSelectedView.setSelected(true);
        newViewRect = new Rect(newSelectedView.getLeft(),
                newSelectedView.getTop(), newSelectedView.getRight(),
                newSelectedView.getBottom());
        invalidate(newViewRect);
        fireOnSelected();
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
                int difference = keyCode == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1;
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
            break;
        }
        case SCROLL_STATE_FAST_SCROLL: {
            // TODO
            if (newScrollState == SCROLL_STATE_NORMAL) {
                mScrollState = SCROLL_STATE_NORMAL;
            } else if (newScrollState == SCROLL_STATE_FAST_SCROLL_END) {
                FastToFastlScrollEndFinishedListener animFinishedListener = new FastToFastlScrollEndFinishedListener();
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
            }
            break;
        }
        }
    }

    interface OnAnimationFinishedListener {
        void animationFinished();
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
        public void animationFinished() {
            mScrollState = SCROLL_STATE_FAST_SCROLL;
            mSmoothScrollRunnable.resumeVerticalScroll(mDifference);
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
        public void animationFinished() {
            mSmoothScrollRunnable.startVerticalScrollToPosition(
                    mDesiredChannelPosition, mScrollDuration);
        }
    }

    /**
     * Class that wait end of normal scroll and switches to fast scroll
     */
    private class FastToFastlScrollEndFinishedListener implements
            OnAnimationFinishedListener {

        FastToFastlScrollEndFinishedListener() {
        }

        @Override
        public void animationFinished() {
            // postDelayed(new Runnable() {
            // @Override
            // public void run() {
            log("FastToFastlScrollEndFinishedListener mSelectedItemPosition="
                    + mSelectedItemPosition);
            View selected = isItemAttachedToWindow(
                    LAYOUT_TYPE_CHANNEL_INDICATOR, mSelectedItemPosition,
                    INVALID_POSITION);
            if (selected != null) {
                // Expanded view can be null if it is out of screen
                final View expanded = isItemAttachedToWindow(
                        LAYOUT_TYPE_CHANNEL_INDICATOR, mExpandedChannelIndex,
                        INVALID_POSITION);
                log("Start resize animation expanded=" + expanded);
                ResizeAnimation animation = new ResizeAnimation(
                        BaseGuideView.this, selected, expanded,
                        mChannelRowHeight, mChannelRowHeightExpanded);
                animation.setInterpolator(new AccelerateInterpolator());
                animation.setDuration(5000);// SMOOTH_FAST_SCROLL_END_DURATION);
                animation
                        .setAnimationListener(new Animation.AnimationListener() {
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
                        });
                startAnimation(animation);
            }
            // }
            // }, 2000);
        }
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
                return (newChannelPosition - mNumberOfVisibleChannels / 2)
                        * (mChannelRowHeight + mVerticalDividerHeight);
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
                if (mDesiredChannelPosition >= mChannelItemCount - 1) {
                    return;
                } else if (mDesiredChannelPosition <= 0) {
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
        void startVerticalScrollToPosition(int newChannelPosition, int duration) {
            // TODO if new channel index is visible on screen do normal scroll,
            // if it is invisible perform fast scroll
            if (isScrollRunning()) {
                setOnAnimationFinishedListener(new NormalToNormalScrollFinishedListener(
                        newChannelPosition, duration));
            } else {
                this.mDesiredChannelPosition = newChannelPosition;
                final int calculatedYCoordinate = calculateNewYPosition(newChannelPosition);
                log("startVerticalScrollToPosition, newChannelPosition=" + newChannelPosition
                        + ", calculatedYCoordinate "
                        + "= " + calculatedYCoordinate);
                if (calculatedYCoordinate != INVALID_POSITION) {
                    startScrollTo(mCurrentOffsetX, calculatedYCoordinate,
                            duration);
                }
            }
        }

        void startScrollBy(int byX, int byY) {
            startScrollBy(byX, byY, SMOOTH_SCROLL_DURATION);
        }

        void startScrollBy(int byX, int byY, int duration) {
            log("SmoothScrollRunnable startScrollBy, byX=" + byX + ", byY="
                    + byY);
            // Dont start scroll if difference is 0
            if (byX != 0 || byY != 0) {
                mScroll.forceFinished(true);// TODO check if this can be removed
                BaseGuideView.this.removeCallbacks(this);
                mScroll.startScroll(mCurrentOffsetX, mCurrentOffsetY, byX, byY,
                        duration);
                BaseGuideView.this.postOnAnimation(this);
            }
        }

        void startScrollTo(int toX, int toY) {
            startScrollTo(toX, toY, SMOOTH_SCROLL_DURATION);
        }

        void startScrollTo(int toX, int toY, int duration) {
            if (toY > getBottomOffsetBounds() || toY < getTopOffsetBounds()
                    || toX > getRightOffsetBounds() || toX < 0) {
                return;
            }
            startScrollBy(toX - mCurrentOffsetX, toY - mCurrentOffsetY,
                    duration);
        }

        @Override
        public void run() {
            if (mScroll.isFinished()) {
                log("scroller is finished, done with smooth scroll");
                mDesiredChannelPosition = mSelectedItemPosition;
                if (mOnAnimationFinishedListener != null) {
                    mOnAnimationFinishedListener.animationFinished();
                    mOnAnimationFinishedListener = null;
                }
                return;
            }
            boolean animationRunning = mScroll.computeScrollOffset();
            int x = mScroll.getCurrX();
            int y = mScroll.getCurrY();
            log("SmoothScrollRunnable RUNNNNN  y=" + y);
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
