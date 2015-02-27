package com.epg;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

/**
 * Custom view for representing guide information
 */
public class GuideView extends BaseGuideView {

    /**
     * These views are used to be drawn above guide event views
     */
    private View mOverlapChannelIndicatorsView;
    private View mOverlapTimeLineView;

    private int mEventsAreaMiddlePoint = INVALID_POSITION;
    /**
     * Previous system time of long press
     */
    private long mLongPressTime = 0;
    private boolean isInLongPress = false;

    public GuideView(Context context) throws Exception {
        super(context);
        init(context);
    }

    public GuideView(Context context, AttributeSet attrs) throws Exception {
        super(context, attrs);
        init(context);
    }

    public GuideView(Context context, AttributeSet attrs, int defStyle) throws Exception {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * Initialize view
     *
     * @param context
     */
    private void init(Context context) {
        mOverlapChannelIndicatorsView = new View(context);
        mOverlapTimeLineView = new View(context);
        //Set background color to BLACK non transparent
        mOverlapChannelIndicatorsView.setBackgroundColor(Color.BLACK);
        mOverlapTimeLineView.setBackgroundColor(Color.BLACK);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (mScroll.isFinished()) {
            if (mSelectedView != null) {
                handled = mSelectedView.dispatchKeyEvent(event);
            }
        }
        return handled || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isInLongPress) {
            isInLongPress = false;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAdapter.getChannelsCount() == 0) {
            return false;
        }
        final boolean isLongPress = event.isLongPress();
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP: {
            if (event.isLongPress()) {
                isInLongPress = true;
            }
            long now = System.currentTimeMillis();
            if (isInLongPress && (now - mLongPressTime > SMOOTH_FAST_SCROLL_DURATION - 60)) {
                mLongPressTime = now;
                if (mScrollState == SCROLL_STATE_NORMAL) {
                    changeScrollState(SCROLL_STATE_FAST_SCROLL, keyCode);
                } else if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                    mSmoothScrollRunnable.resumeVerticalScroll(-1);
                }
            } else if (!isInLongPress) {
                mSmoothScrollRunnable.startVerticalScrollToPosition(mSelectedItemPosition - 1, SMOOTH_SCROLL_DURATION);
            }

            return true;
        }
        case KeyEvent.KEYCODE_DPAD_DOWN: {
            if (event.isLongPress()) {
                isInLongPress = true;
            }
            long now = System.currentTimeMillis();
            if (isInLongPress && (now - mLongPressTime > SMOOTH_FAST_SCROLL_DURATION - 60)) {
                mLongPressTime = now;
                if (mScrollState == SCROLL_STATE_NORMAL) {
                    changeScrollState(SCROLL_STATE_FAST_SCROLL, keyCode);
                } else if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                    mSmoothScrollRunnable.resumeVerticalScroll(1);
                }
            } else if (!isInLongPress) {
                mSmoothScrollRunnable.startVerticalScrollToPosition(mSelectedItemPosition + 1, SMOOTH_SCROLL_DURATION);
            }
            return true;
        }
        case KeyEvent.KEYCODE_DPAD_LEFT: {
            changeScrollState(SCROLL_STATE_NORMAL, INVALID_POSITION);
            return true;
        }
        case KeyEvent.KEYCODE_DPAD_RIGHT: {
            return true;
        }
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER: {
            return performItemClick(mSelectedView);
        }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void layoutChannelIndicators() {
        if (mOverlapChannelIndicatorsView.getLayoutParams() == null) {
            addChildView(LAYOUT_TYPE_OVERLAP_VIEW, mOverlapChannelIndicatorsView, mRectChannelIndicators.left,
                    0, mRectChannelIndicators.right - mRectChannelIndicators.left,
                    mRectChannelIndicators.bottom, INVALID_POSITION, INVALID_POSITION);
        } else {
            bringChildToFront(mOverlapChannelIndicatorsView);
        }

        //Starting points for drawing events
        int currentY = mRectChannelIndicators.top;

        final int channelsCount = mChannelItemCount;
        mLastChannelPosition = INVALID_POSITION;
        int currentRowHeight = 0;
        // Calculate first child top invisible part
        if (mFirstChannelPosition == mExpandedChannelIndex) {
            int invisible = mExpandedChannelIndex * (mChannelRowHeight + mVerticalDividerHeight);
            currentY -= (mCurrentOffsetY - invisible);
        } else {
            currentY -= mCurrentOffsetY % (mChannelRowHeight + mVerticalDividerHeight);
        }
        int resizedPercent = INVALID_POSITION;
        //Loop through channels
        for (int i = mFirstChannelPosition; i < channelsCount; i++) {
            //Get view at the desired position, or null if view do not exist
            final View attached = isItemAttachedToWindow(LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION);
            //Calculate current row height
            currentRowHeight = calculateRowHeight(currentY, currentRowHeight,
                    attached == null ? INVALID_POSITION : attached
                            .getHeight(), i);
            if (attached == null) {
                View child = mAdapter.getChannelIndicatorView(i, mRecycler.getChannelIndicatorView(), GuideView.this);
                addChildView(LAYOUT_TYPE_CHANNEL_INDICATOR, child, mRectChannelIndicators.left, currentY,
                        mRectChannelIndicators.right - mRectChannelIndicators.left,
                        currentRowHeight, i, INVALID_POSITION);
            } else {
                bringChildToFront(attached);
                //If event height is changed we must resize its view dimension
                if (attached.getHeight() != currentRowHeight) {
                    if (resizedPercent == INVALID_POSITION) {
                        resizedPercent = 100 * (currentRowHeight - mChannelRowHeight) / mChannelRowHeightExpanded;
                    }
                    resizeChildView(attached, currentY, mRectChannelIndicators.right - mRectChannelIndicators.left,
                            currentRowHeight, resizedPercent);
                }
                //If current Y coordinate and top position of the view are misplaced we must move a view to desired Y value
                else if (currentY != attached.getTop()) {
                    attached.offsetTopAndBottom(currentY - attached.getTop());
                }
            }

            // If child row is out of screen
            if (currentY + currentRowHeight + mVerticalDividerHeight > getHeight()) {
                mLastChannelPosition = i;
                break;
            }
            //New child row is inside screen so we draw it
            else {
                currentY += currentRowHeight + mVerticalDividerHeight;
            }
        }
        if (mLastChannelPosition == INVALID_POSITION) {
            mLastChannelPosition = channelsCount - 1;
        }

    }

    @Override
    protected void layoutTimeLine() {
        if (mOverlapTimeLineView.getLayoutParams() == null) {
            addChildView(LAYOUT_TYPE_OVERLAP_VIEW, mOverlapTimeLineView, 0,
                    mRectTimeLine.top, mRectTimeLine.right,
                    mRectTimeLine.bottom - mRectTimeLine.top, INVALID_POSITION, INVALID_POSITION);
        } else {
            bringChildToFront(mOverlapTimeLineView);
        }

    }

    @Override
    protected void layoutEvents() {
        //Starting points for drawing events
        int currentX = mRectEventsArea.left;
        int currentY = mRectEventsArea.top;

        final int channelsCount = mChannelItemCount;
        mLastChannelPosition = INVALID_POSITION;
        calculateFirstChannelPosition();

        if (mEventsAreaMiddlePoint == INVALID_POSITION) {
            mEventsAreaMiddlePoint = mRectEventsArea.top + (mRectEventsArea.bottom - mRectEventsArea.top) / 2;
        }
        FirstPositionInfo firstPositionInfo;
        int currentRowHeight = 0;
        mSelectedItemPosition = mFirstChannelPosition;
        // Calculate first child top invisible part
        if (mFirstChannelPosition == mExpandedChannelIndex) {
            int invisible = mExpandedChannelIndex * (mChannelRowHeight + mVerticalDividerHeight);
            currentY -= (mCurrentOffsetY - invisible);
        } else {
            currentY -= mCurrentOffsetY % (mChannelRowHeight + mVerticalDividerHeight);
        }
        //Loop through channels
        for (int i = mFirstChannelPosition; i < channelsCount; i++) {
            //Get channel indicator view at the desired position, or null if view do not exist
            final View attached = isItemAttachedToWindow(LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION);
            //Calculate current row height
            currentRowHeight = calculateRowHeight(currentY, currentRowHeight,
                    attached == null ? INVALID_POSITION : attached
                            .getHeight(), i);
            //Calculate selected channel position
            if (currentY <= mEventsAreaMiddlePoint && currentRowHeight + currentY >= mEventsAreaMiddlePoint) {
                mSelectedItemPosition = i;
                //Save index of expanded channel
                if (mScrollState != SCROLL_STATE_FAST_SCROLL) {
                    mExpandedChannelIndex = mSelectedItemPosition;
                }
            }
            // Get first child position based on current scroll value
            // and calculate its invisible part
            firstPositionInfo = getPositionAndOffsetForScrollValue(
                    mCurrentOffsetX, i);
            // No data for desired channel, don't draw anything
            if (firstPositionInfo.getFirstChildInvisiblePart() < 0 || firstPositionInfo.getFirstChildIndex() < 0) {
                continue;
            }
            // Move X coordinate to left to support drawing of invisible part of event view
            currentX -= firstPositionInfo.getFirstChildInvisiblePart();
            if (firstPositionInfo.getFirstChildIndex() > 0) {
                currentX += mHorizontalDividerWidth;
            }
            //Layout all event views for channel
            layoutEventsRow(i, currentX, currentY, firstPositionInfo.getFirstChildIndex(), currentRowHeight);

            // If child row is out of screen
            if (currentY + currentRowHeight + mVerticalDividerHeight > getHeight()) {
                mLastChannelPosition = i;
                break;
            }
            //New child row is inside screen so we draw it
            else {
                currentY += currentRowHeight + mVerticalDividerHeight;
                currentX = mRectEventsArea.left;
            }
        }
        if (mLastChannelPosition == INVALID_POSITION) {
            mLastChannelPosition = channelsCount - 1;
        }
    }

    /**
     * Position whole row of epg data
     *
     * @param channelIndex     Index of channel to draw
     * @param currentX         Current X position on screen
     * @param currentY         Current Y position on screen
     * @param firstChildIndex  Index of first visible event index
     * @param currentRowHeight Current row height
     */
    private void layoutEventsRow(final int channelIndex, int currentX, int currentY, final int firstChildIndex,
            int currentRowHeight) {
        // Get number of events
        final int eventCount = getEventsCount(channelIndex);
        int resizedPercent = INVALID_POSITION;
        for (int j = firstChildIndex; j < eventCount; j++) {

            final int eventWidth = mAdapter.getEventWidth(channelIndex, j)
                    - (j == 0 ? 0 : mHorizontalDividerWidth);
            final View attached = isItemAttachedToWindow(LAYOUT_TYPE_EVENTS, channelIndex, j);
            if (attached == null) {
                View child = mAdapter.getEventView(channelIndex, j,
                        mRecycler.getEventView(eventWidth), GuideView.this);
                addChildView(LAYOUT_TYPE_EVENTS, child, currentX, currentY, eventWidth,
                        currentRowHeight, channelIndex, j);
            }
            //If event height is changed we must resize its view dimension
            else if (attached.getHeight() != currentRowHeight) {
                if (resizedPercent == INVALID_POSITION) {
                    resizedPercent = 100 * (currentRowHeight - mChannelRowHeight) / mChannelRowHeightExpanded;
                }
                resizeChildView(attached, currentY, eventWidth, currentRowHeight, resizedPercent);
            }
            //If current Y coordinate and top position of the view are misplaced we must move a view to desired Y value
            else if (currentY != attached.getTop()) {
                attached.offsetTopAndBottom(currentY - attached.getTop());
            }
            // If child right edge is larger or equals to right
            // bound of EpgView
            if (currentX + eventWidth >= getWidth()) {
                break;
            } else {
                currentX += eventWidth + mHorizontalDividerWidth;
            }
        }
    }
}