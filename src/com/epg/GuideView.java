package com.epg;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Custom view for representing guide information
 */
public class GuideView extends BaseGuideView {

    /**
     * These views are used to be drawn above guide event views
     */
    private View mOverlapChannelIndicatorsView;
    private View mOverlapTimeLineView;
    /**
     * Used for calculating central selected item position
     */
    private int mEventsAreaMiddlePoint = INVALID_POSITION;
    /**
     * Previous system time of long press
     */
    private long mLongPressTime = 0;
    private boolean isInLongPress = false;

    private ArrayList<GuideRowInfo> mRows;

    public GuideView(Context context) throws Exception {
        super(context);
        init(context);
    }

    public GuideView(Context context, AttributeSet attrs) throws Exception {
        super(context, attrs);
        init(context);
    }

    public GuideView(Context context, AttributeSet attrs, int defStyle)
            throws Exception {
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
        // Set background color to BLACK non transparent
        mOverlapChannelIndicatorsView.setBackgroundColor(Color.BLACK);
        mOverlapTimeLineView.setBackgroundColor(Color.BLACK);

        // Init rows information holder
        mRows = new ArrayList<GuideRowInfo>();
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
        boolean handled = false;
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP: {
            log("ON KEY UP, KEYCODE_DPAD_UP, mScrollState=" + mScrollState + ", isInLongPress=" + isInLongPress);
            if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                changeScrollState(SCROLL_STATE_FAST_SCROLL_END, keyCode);
            }
            handled = true;
            break;
        }
        case KeyEvent.KEYCODE_DPAD_DOWN: {
            log("ON KEY UP, KEYCODE_DPAD_DOWN, mScrollState=" + mScrollState + ", isInLongPress=" + isInLongPress);
            if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                changeScrollState(SCROLL_STATE_FAST_SCROLL_END, keyCode);
            }
            handled = true;
            break;
        }
        }
        if (isInLongPress) {
            isInLongPress = false;
        }
        return handled || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAdapter.getChannelsCount() == 0) {
            return false;
        }
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP: {
            if (event.isLongPress()) {
                isInLongPress = true;
            }
            long now = System.currentTimeMillis();
            log("ON KEY DOWN, KEYCODE_DPAD_UP, mScrollState=" + mScrollState + ", isInLongPress=" + isInLongPress);
            /**
             * Long press
             */
            if (isInLongPress
                    && (now - mLongPressTime > SMOOTH_FAST_SCROLL_DURATION - 60)) {
                mLongPressTime = now;
                if (mScrollState == SCROLL_STATE_NORMAL) {
                    changeScrollState(SCROLL_STATE_FAST_SCROLL, keyCode);
                } else if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                    mSmoothScrollRunnable.resumeVerticalScroll(-1);
                }
            }
            /**
             * Normal press
             */
            else if (!isInLongPress) {
                mSmoothScrollRunnable.startVerticalScrollToPosition(
                        mSelectedItemPosition - 1, SMOOTH_SCROLL_DURATION);
            }
            return true;
        }
        case KeyEvent.KEYCODE_DPAD_DOWN: {
            if (event.isLongPress()) {
                isInLongPress = true;
            }
            long now = System.currentTimeMillis();
            log("ON KEY DOWN, KEYCODE_DPAD_DOWN, mScrollState=" + mScrollState + ", isInLongPress=" + isInLongPress);
            /**
             * Long press
             */
            if (isInLongPress
                    && (now - mLongPressTime > SMOOTH_FAST_SCROLL_DURATION - 60)) {
                mLongPressTime = now;
                if (mScrollState == SCROLL_STATE_NORMAL) {
                    changeScrollState(SCROLL_STATE_FAST_SCROLL, keyCode);
                } else if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                    mSmoothScrollRunnable.resumeVerticalScroll(1);
                }
            }
            /**
             * Normal press
             */
            else if (!isInLongPress) {
                mSmoothScrollRunnable.startVerticalScrollToPosition(
                        mSelectedItemPosition + 1, SMOOTH_SCROLL_DURATION);
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

    /**
     * Smoothly scroll to position
     *
     * @param position New position to scroll to
     */
    public void smoothScrollToPosition(int position) {
        int diff = Math.abs(mSelectedItemPosition - position);
        mSmoothScrollRunnable.startVerticalScrollToPosition(position, (int) (diff * SMOOTH_FAST_SCROLL_DURATION * 0.6));
    }

    @Override
    protected void layoutChannelIndicators() {
        if (mOverlapChannelIndicatorsView.getLayoutParams() == null) {
            addChildView(LAYOUT_TYPE_OVERLAP_VIEW,
                    mOverlapChannelIndicatorsView, mRectChannelIndicators.left,
                    0, mRectChannelIndicators.right
                            - mRectChannelIndicators.left,
                    mRectChannelIndicators.bottom, INVALID_POSITION,
                    INVALID_POSITION);
        } else {
            bringChildToFront(mOverlapChannelIndicatorsView);
        }
        layoutViews(LAYOUT_TYPE_CHANNEL_INDICATOR);
    }

    @Override
    protected void layoutTimeLine() {
        if (mOverlapTimeLineView.getLayoutParams() == null) {
            addChildView(LAYOUT_TYPE_OVERLAP_VIEW, mOverlapTimeLineView, 0,
                    mRectTimeLine.top, mRectTimeLine.right,
                    mRectTimeLine.bottom - mRectTimeLine.top, INVALID_POSITION,
                    INVALID_POSITION);
        } else {
            bringChildToFront(mOverlapTimeLineView);
        }

    }

    @Override
    protected void layoutEvents() {
        layoutViews(LAYOUT_TYPE_EVENTS);
    }

    /**
     * Calculates resized percent of channel row
     *
     * @param currentRowHeight Height Height of current row
     * @return Integer in the range [0 - 100]
     */
    private int calculateResizedPercentOfView(int currentRowHeight) {
        return 100 * (currentRowHeight - mChannelRowHeight)
                / (mChannelRowHeightExpanded - mChannelRowHeight);
    }

    @Override
    protected void calculateRowPositions() {
        mRows.clear();
        int currentRowHeight = 0;
        int resizedPercent = 0;
        //log("calculateRowPositions, mCurrentOffsetY=" + mCurrentOffsetY);
        // For fast scroll end we must recalculate heights different way
        if (mScrollState == SCROLL_STATE_FAST_SCROLL_END) {
            int i;
            int currentYUp = 0, currentYDown = 0;

            /**
             * From selected to top of the screen
             */
            for (i = mSelectedItemPosition; i >= 0; i--) {
                View attached = isItemAttachedToWindow(
                        LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION);
                // Calculate current row height
                currentRowHeight = calculateRowHeight(INVALID_POSITION,
                        currentRowHeight, attached == null ? INVALID_POSITION
                                : attached.getLayoutParams().height, i);
                // Calculate resized percent
                resizedPercent = calculateResizedPercentOfView(currentRowHeight);
                // For central element we need to calculate Y up and down
                // coordinate
                if (i == mSelectedItemPosition) {
                    currentYUp = mRectSelectedRowArea.top
                            + (mChannelRowHeightExpanded - currentRowHeight)
                            / 2;
                    currentYDown = currentYUp + currentRowHeight
                            + mVerticalDividerHeight;
                }
                // For the rest of rows we recalculate new Y based on row height
                else {
                    currentYUp = currentYUp
                            - (currentRowHeight + mVerticalDividerHeight);
                }
                mRows.add(new GuideRowInfo(i, currentYUp, currentRowHeight,
                        resizedPercent));
                // If current Y coordinate is out of screen
                if (currentYUp <= mRectChannelIndicators.top) {
                    mFirstChannelPosition = i;
                    mRecycler.removeItemsAtPosition(i - 1);
                    break;
                }

            }
            /**
             * From selected to the bottom of the screen
             */
            for (i = mSelectedItemPosition + 1; i < mChannelItemCount; i++) {
                View attached = isItemAttachedToWindow(
                        LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION);
                // Calculate current row height
                currentRowHeight = calculateRowHeight(INVALID_POSITION,
                        currentRowHeight, attached == null ? INVALID_POSITION
                                : attached.getLayoutParams().height, i);
                // Calculate resized percent
                resizedPercent = calculateResizedPercentOfView(currentRowHeight);
                mRows.add(new GuideRowInfo(i, currentYDown, currentRowHeight,
                        resizedPercent));
                // If current Y coordinate is out of screen
                if (currentYDown + currentRowHeight + mVerticalDividerHeight >= mRectChannelIndicators.bottom) {
                    mLastChannelPosition = i;
                    mRecycler.removeItemsAtPosition(i + 1);
                    break;
                } else {
                    currentYDown += currentRowHeight + mVerticalDividerHeight;
                }
            }
        }
        // For normal scroll and fast scroll
        else {
            int currentY = mRectChannelIndicators.top;
            final int channelsCount = mChannelItemCount;

            mLastChannelPosition = INVALID_POSITION;
            mSelectedItemPosition = mFirstChannelPosition;

            calculateFirstChannelPosition();

            // Calculate events area middle point
            if (mEventsAreaMiddlePoint == INVALID_POSITION) {
                mEventsAreaMiddlePoint = mRectEventsArea.top
                        + (mRectEventsArea.bottom - mRectEventsArea.top) / 2;
            }

            // Calculate first child top invisible part
            // In fast scroll, first visible position can be expanded item
            if (mFirstChannelPosition == mExpandedChannelIndex) {
                // Current offset Y - invisible part of guide
                currentY -= (mCurrentOffsetY - (mExpandedChannelIndex * (mChannelRowHeight + mVerticalDividerHeight)));
            } else {
                currentY -= mCurrentOffsetY % (mChannelRowHeight + mVerticalDividerHeight);
            }

            // Loop through channels
            for (int i = mFirstChannelPosition; i < channelsCount; i++) {

                // Get channel indicator view at the desired position, or null
                // if
                // view do not exist
                final View attached = isItemAttachedToWindow(
                        LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION);
                // Calculate current row height
                currentRowHeight = calculateRowHeight(currentY,
                        currentRowHeight, attached == null ? INVALID_POSITION
                                : attached.getHeight(), i);

                resizedPercent = calculateResizedPercentOfView(currentRowHeight);

                // Calculate selected channel position
                if (currentY <= mEventsAreaMiddlePoint
                        && currentRowHeight + currentY >= mEventsAreaMiddlePoint) {
                    mSelectedItemPosition = i;
                    // Save index of expanded channel
                    if (mScrollState != SCROLL_STATE_FAST_SCROLL) {
                        mExpandedChannelIndex = mSelectedItemPosition;
                    }
                }

                if (i >= 0) {
                    mRows.add(new GuideRowInfo(i, currentY, currentRowHeight,
                            resizedPercent));
                }
                // If child row is out of screen
                if (currentY + currentRowHeight + mVerticalDividerHeight >= getHeight()) {
                    mLastChannelPosition = i;
                    break;
                }
                // New child row is inside screen so we draw it
                else {
                    currentY += currentRowHeight + mVerticalDividerHeight;
                }
            }
        }
        //log("calculateRowPositions(), ROWS=" + mRows.toString());
    }

    /**
     * Layout views in guide view
     *
     * @param layoutType Type of layout pass, can be {@link LAYOUT_TYPE_CHANNEL_INDICATOR} or {@link LAYOUT_TYPE_EVENTS}
     */
    private void layoutViews(int layoutType) {
        GuideRowInfo guideRowInfo = null;
        FirstPositionInfo firstPositionInfo;
        int currentX = mRectEventsArea.left;
        for (int i = 0; i < mRows.size(); i++) {
            guideRowInfo = mRows.get(i);
            if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) {
                final View attached = isItemAttachedToWindow(
                        LAYOUT_TYPE_CHANNEL_INDICATOR,
                        guideRowInfo.getmChannelIndex(), INVALID_POSITION);
                layoutChildView(layoutType, attached,
                        guideRowInfo.getmHeight(), guideRowInfo.getmTop(),
                        mRectChannelIndicators.left,
                        guideRowInfo.getmResizedPercent(),
                        guideRowInfo.getmChannelIndex(), INVALID_POSITION);

            } else {
                // Get first child position based on current scroll value
                // and calculate its invisible part
                firstPositionInfo = getPositionAndOffsetForScrollValue(
                        mCurrentOffsetX, guideRowInfo.getmChannelIndex());
                // No data for desired channel, don't draw anything
                if (firstPositionInfo.getFirstChildInvisiblePart() < 0
                        || firstPositionInfo.getFirstChildIndex() < 0) {
                    continue;
                }
                // Move X coordinate to left to support drawing of invisible
                // part of event view
                currentX -= firstPositionInfo.getFirstChildInvisiblePart();
                if (firstPositionInfo.getFirstChildIndex() > 0) {
                    currentX += mHorizontalDividerWidth;
                }
                // Layout all event views for channel
                layoutEventsRow(guideRowInfo.getmChannelIndex(), currentX,
                        guideRowInfo.getmTop(),
                        firstPositionInfo.getFirstChildIndex(),
                        guideRowInfo.getmHeight());
                currentX = mRectEventsArea.left;
            }
        }
    }

    /**
     * Position whole row of guide data
     *
     * @param channelIndex     Index of channel to draw
     * @param currentX         Current X position on screen
     * @param currentY         Current Y position on screen
     * @param firstChildIndex  Index of first visible event index
     * @param currentRowHeight Current row height
     */
    private void layoutEventsRow(final int channelIndex, int currentX,
            int currentY, final int firstChildIndex, int currentRowHeight) {
        // Get number of events
        final int eventCount = getEventsCount(channelIndex);
        final int resizedPercent = calculateResizedPercentOfView(currentRowHeight);
        int eventWidth = 0;
        for (int j = firstChildIndex; j < eventCount; j++) {
            final View attached = isItemAttachedToWindow(LAYOUT_TYPE_EVENTS,
                    channelIndex, j);
            eventWidth = layoutChildView(LAYOUT_TYPE_EVENTS, attached,
                    currentRowHeight, currentY, currentX, resizedPercent,
                    channelIndex, j);
            // If child right edge is larger or equals to right
            // bound of EpgView
            if (currentX + eventWidth >= getWidth()) {
                break;
            } else {
                currentX += eventWidth + mHorizontalDividerWidth;
            }
        }
    }

    /**
     * Layout single child view
     *
     * @param layoutType       Type of layout pass
     * @param attached         View on screen or NULL if there is no view
     * @param currentRowHeight Height of current row
     * @param currentY         Current Y coordinate on the screen
     * @param resizedPercent   Resized percent of the view
     * @param channelIndex     Index of channel
     * @param eventIndex       Index of Event
     * @return Calculated event width, valid only in LAYOUT_TYPE_EVENTS
     */
    private int layoutChildView(int layoutType, View attached,
            int currentRowHeight, int currentY, int currentX,
            int resizedPercent, int channelIndex, int eventIndex) {
        int eventWidth = 0;
        if (layoutType == LAYOUT_TYPE_EVENTS) {
            eventWidth = mAdapter.getEventWidth(channelIndex, eventIndex)
                    - (eventIndex == 0 ? 0 : mHorizontalDividerWidth);
        } else if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) {
            eventWidth = mRectChannelIndicators.right
                    - mRectChannelIndicators.left;
        }
        if (attached == null) {
            View child = null;
            if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) {
                child = mAdapter.getChannelIndicatorView(channelIndex,
                        mRecycler.getChannelIndicatorView(), GuideView.this);
            } else if (layoutType == LAYOUT_TYPE_EVENTS) {
                child = mAdapter.getEventView(channelIndex, eventIndex,
                        mRecycler.getEventView(eventWidth), GuideView.this);
            }
            addChildView(layoutType, child, currentX, currentY, eventWidth,
                    currentRowHeight, channelIndex, eventIndex);
        } else {
            if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) {
                bringChildToFront(attached);
            }
            // If event height is changed we must resize its view dimension
            if (attached.getHeight() != currentRowHeight) {
                resizeChildView(attached, currentY, eventWidth,
                        currentRowHeight, resizedPercent);
            }
            // If current Y coordinate and top position of the view are
            // misplaced we must move a view to desired Y value
            else if (currentY != attached.getTop()) {
                attached.offsetTopAndBottom(currentY - attached.getTop());
            }
        }
        return eventWidth;
    }

    /**
     * Offset the whole row of events in fast scroll end state
     *
     * @param channelIndex
     *            Index of channel to offset
     * @param firstChildIndex
     *            Index of first child
     * @param currentY
     */
    /*
     * private void offsetEventsRow(final int channelIndex, int firstChildIndex,
	 * int currentY) { // Get number of events final int eventCount =
	 * getEventsCount(channelIndex); for (int j = firstChildIndex; j <
	 * eventCount; j++) { final View attached =
	 * isItemAttachedToWindow(LAYOUT_TYPE_EVENTS, channelIndex, j); if (attached
	 * != null) { // If current Y coordinate and top position of the view are //
	 * misplaced we must move a view to desired Y value if (currentY !=
	 * attached.getTop()) { attached.offsetTopAndBottom(currentY -
	 * attached.getTop()); } } else { break; } } }
	 */

    /**
     * Resize the whole row of events if fast scroll end state
     *
     * @param channelIndex
     * @param firstChildIndex
     * @param currentY
     * @param currentRowHeight
     */
    /*
     * private void resizeEventsRow(final int channelIndex, int firstChildIndex,
	 * int currentY, int currentRowHeight) { // Get number of events final int
	 * eventCount = getEventsCount(channelIndex); final int resizedPercent =
	 * calculateResizedPercentOfView(currentRowHeight); for (int j =
	 * firstChildIndex; j < eventCount; j++) { final View attached =
	 * isItemAttachedToWindow(LAYOUT_TYPE_EVENTS, channelIndex, j); if (attached
	 * != null) { // If event height is changed we must resize its view
	 * dimension if (attached.getHeight() != currentRowHeight) {
	 * resizeChildView(attached, currentY, attached.getWidth(),
	 * currentRowHeight, resizedPercent); } } else { break; } } }
	 */

	/*
     * private void layoutEventsNormal() { //Starting points for drawing events
	 * int currentX = mRectEventsArea.left; int currentY = mRectEventsArea.top;
	 * 
	 * final int channelsCount = mChannelItemCount; mLastChannelPosition =
	 * INVALID_POSITION; calculateFirstChannelPosition();
	 * 
	 * if (mEventsAreaMiddlePoint == INVALID_POSITION) { mEventsAreaMiddlePoint
	 * = mRectEventsArea.top + (mRectEventsArea.bottom - mRectEventsArea.top) /
	 * 2; } FirstPositionInfo firstPositionInfo; int currentRowHeight = 0;
	 * mSelectedItemPosition = mFirstChannelPosition; // Calculate first child
	 * top invisible part if (mFirstChannelPosition == mExpandedChannelIndex) {
	 * int invisible = mExpandedChannelIndex * (mChannelRowHeight +
	 * mVerticalDividerHeight); currentY -= (mCurrentOffsetY - invisible); }
	 * else { currentY -= mCurrentOffsetY % (mChannelRowHeight +
	 * mVerticalDividerHeight); } //Loop through channels for (int i =
	 * mFirstChannelPosition; i < channelsCount; i++) { //Get channel indicator
	 * view at the desired position, or null if view do not exist final View
	 * attached = isItemAttachedToWindow(LAYOUT_TYPE_CHANNEL_INDICATOR, i,
	 * INVALID_POSITION); //Calculate current row height currentRowHeight =
	 * calculateRowHeight(currentY, currentRowHeight, attached == null ?
	 * INVALID_POSITION : attached .getHeight(), i); //Calculate selected
	 * channel position if (currentY <= mEventsAreaMiddlePoint &&
	 * currentRowHeight + currentY >= mEventsAreaMiddlePoint) {
	 * mSelectedItemPosition = i; log("layoutEvents(), mSelectedItemPosition=" +
	 * mSelectedItemPosition); //Save index of expanded channel if (mScrollState
	 * != SCROLL_STATE_FAST_SCROLL) { mExpandedChannelIndex =
	 * mSelectedItemPosition; } } // Get first child position based on current
	 * scroll value // and calculate its invisible part firstPositionInfo =
	 * getPositionAndOffsetForScrollValue( mCurrentOffsetX, i); // No data for
	 * desired channel, don't draw anything if
	 * (firstPositionInfo.getFirstChildInvisiblePart() < 0 ||
	 * firstPositionInfo.getFirstChildIndex() < 0) { continue; } // Move X
	 * coordinate to left to support drawing of invisible part of event view
	 * currentX -= firstPositionInfo.getFirstChildInvisiblePart(); if
	 * (firstPositionInfo.getFirstChildIndex() > 0) { currentX +=
	 * mHorizontalDividerWidth; } //Layout all event views for channel
	 * layoutEventsRow(i, currentX, currentY,
	 * firstPositionInfo.getFirstChildIndex(), currentRowHeight);
	 * 
	 * // If child row is out of screen if (currentY + currentRowHeight +
	 * mVerticalDividerHeight > getHeight()) { mLastChannelPosition = i; break;
	 * } //New child row is inside screen so we draw it else { currentY +=
	 * currentRowHeight + mVerticalDividerHeight; currentX =
	 * mRectEventsArea.left; } } if (mLastChannelPosition == INVALID_POSITION) {
	 * mLastChannelPosition = channelsCount - 1; } }
	 */

	/*
     * private void layoutChannelIndicatorsNormal() { //Starting points for
	 * drawing events int currentY = mRectChannelIndicators.top;
	 * 
	 * final int channelsCount = mChannelItemCount; mLastChannelPosition =
	 * INVALID_POSITION; int currentRowHeight = 0; // Calculate first child top
	 * invisible part if (mFirstChannelPosition == mExpandedChannelIndex) { int
	 * invisible = mExpandedChannelIndex * (mChannelRowHeight +
	 * mVerticalDividerHeight); currentY -= (mCurrentOffsetY - invisible); }
	 * else { currentY -= mCurrentOffsetY % (mChannelRowHeight +
	 * mVerticalDividerHeight); } int resizedPercent = INVALID_POSITION; //Loop
	 * through channels for (int i = mFirstChannelPosition; i < channelsCount;
	 * i++) { //Get view at the desired position, or null if view do not exist
	 * final View attached =
	 * isItemAttachedToWindow(LAYOUT_TYPE_CHANNEL_INDICATOR, i,
	 * INVALID_POSITION); //Calculate current row height currentRowHeight =
	 * calculateRowHeight(currentY, currentRowHeight, attached == null ?
	 * INVALID_POSITION : attached .getLayoutParams().height, i);
	 * 
	 * log("calculateRowHeight(),  currentRowHeight=" + currentRowHeight +
	 * ", POSITION=" + i + ", previousHeight=" + (attached == null ?
	 * INVALID_POSITION : attached.getLayoutParams().height));
	 * 
	 * if (attached == null) { View child = mAdapter.getChannelIndicatorView(i,
	 * mRecycler.getChannelIndicatorView(), GuideView.this);
	 * addChildView(LAYOUT_TYPE_CHANNEL_INDICATOR, child,
	 * mRectChannelIndicators.left, currentY, mRectChannelIndicators.right -
	 * mRectChannelIndicators.left, currentRowHeight, i, INVALID_POSITION); }
	 * else { bringChildToFront(attached); //If event height is changed we must
	 * resize its view dimension if (attached.getHeight() != currentRowHeight) {
	 * if (resizedPercent == INVALID_POSITION) { resizedPercent = 100 *
	 * (currentRowHeight - mChannelRowHeight) / mChannelRowHeightExpanded; }
	 * resizeChildView(attached, currentY, mRectChannelIndicators.right -
	 * mRectChannelIndicators.left, currentRowHeight, resizedPercent); } //If
	 * current Y coordinate and top position of the view are misplaced we must
	 * move a view to desired Y value else if (currentY != attached.getTop()) {
	 * attached.offsetTopAndBottom(currentY - attached.getTop()); } }
	 * 
	 * // If child row is out of screen if (currentY + currentRowHeight +
	 * mVerticalDividerHeight > getHeight()) { mLastChannelPosition = i; break;
	 * } //New child row is inside screen so we draw it else { currentY +=
	 * currentRowHeight + mVerticalDividerHeight; } } if (mLastChannelPosition
	 * == INVALID_POSITION) { mLastChannelPosition = channelsCount - 1; } }
	 */

    /**
     * Layout views in end of fast scroll scenario
     *
     * @param layoutType
     *            Type of layout
     */
    /*
     * private void layoutViewsFastScrollEnd(int layoutType) { int i; int
	 * currentYUp = 0, currentYDown = 0; int currentX = 0; int currentRowHeight
	 * = 0; int resizedPercent = 0; FirstPositionInfo firstPositionInfo;
	 * 
	 * View centralView = isItemAttachedToWindow( LAYOUT_TYPE_CHANNEL_INDICATOR,
	 * mSelectedItemPosition, INVALID_POSITION); // Calculate current row height
	 * currentRowHeight = calculateRowHeight(INVALID_POSITION, currentRowHeight,
	 * centralView == null ? INVALID_POSITION :
	 * centralView.getLayoutParams().height, mSelectedItemPosition);
	 * 
	 * // Resize selected child view resizedPercent =
	 * calculateResizedPercentOfView(currentRowHeight); currentYUp =
	 * mRectSelectedRowArea.top + (mChannelRowHeightExpanded - currentRowHeight)
	 * / 2;
	 * 
	 * if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) {
	 * resizeChildView(centralView, currentYUp, mRectChannelIndicators.right -
	 * mRectChannelIndicators.left, currentRowHeight, resizedPercent); } else if
	 * (layoutType == LAYOUT_TYPE_EVENTS) { // Get first child position based on
	 * current scroll value // and calculate its invisible part
	 * firstPositionInfo = getPositionAndOffsetForScrollValue( mCurrentOffsetX,
	 * mSelectedItemPosition); // No data for desired channel, don't draw
	 * anything if (firstPositionInfo.getFirstChildInvisiblePart() >= 0 &&
	 * firstPositionInfo.getFirstChildIndex() >= 0) {
	 * resizeEventsRow(mSelectedItemPosition,
	 * firstPositionInfo.getFirstChildIndex(), currentYUp, currentRowHeight); }
	 * }
	 * 
	 * // Recalculates new up and down Y values currentYDown = currentYUp +
	 * currentRowHeight + mVerticalDividerHeight; currentYUp = currentYUp -
	 * (mChannelRowHeight + mVerticalDividerHeight); for (i =
	 * mSelectedItemPosition; --i >= 0;) {
	 * log("layoutChannelIndicatorsFastScrollEnd i=" + i + ", currentYUp=" +
	 * currentYUp); // Get view at the desired position, or null if view do not
	 * exist final View attached = isItemAttachedToWindow(
	 * LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION); if (attached !=
	 * null) { // Calculate current row height currentRowHeight =
	 * calculateRowHeight(currentYUp, currentRowHeight, attached == null ?
	 * INVALID_POSITION : attached.getLayoutParams().height, i);
	 * 
	 * if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) { // If current Y
	 * coordinate and top position of the view are // misplaced we must move a
	 * view to desired Y value if (currentYUp != attached.getTop()) {
	 * attached.offsetTopAndBottom(currentYUp - attached.getTop()); } } else if
	 * (layoutType == LAYOUT_TYPE_EVENTS) { // Get first child position based on
	 * current scroll value // and calculate its invisible part
	 * firstPositionInfo = getPositionAndOffsetForScrollValue( mCurrentOffsetX,
	 * i); // No data for desired channel, don't draw anything if
	 * (firstPositionInfo.getFirstChildInvisiblePart() >= 0 &&
	 * firstPositionInfo.getFirstChildIndex() >= 0) { offsetEventsRow(i,
	 * firstPositionInfo.getFirstChildIndex(), currentYUp); } }
	 * 
	 * // If child row is out of screen if (currentYUp - (currentRowHeight +
	 * mVerticalDividerHeight) < -mChannelRowHeight) { mFirstChannelPosition =
	 * i; break; } // New child row is inside screen so we draw it else {
	 * currentYUp -= (currentRowHeight + mVerticalDividerHeight); } } else {
	 * break; } }
	 * 
	 * for (i = mSelectedItemPosition + 1; i < mChannelItemCount; i++) {
	 * log("layoutChannelIndicatorsFastScrollEnd i=" + i + ", currentYDown=" +
	 * currentYDown); // Get view at the desired position, or null if view do
	 * not exist final View attached = isItemAttachedToWindow(
	 * LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION); if (attached !=
	 * null) { // Calculate current row height currentRowHeight =
	 * calculateRowHeight(currentYDown, currentRowHeight, attached == null ?
	 * INVALID_POSITION : attached.getLayoutParams().height, i); if (layoutType
	 * == LAYOUT_TYPE_CHANNEL_INDICATOR) { bringChildToFront(attached); // If
	 * current Y coordinate and top position of the view are // misplaced we
	 * must move a view to desired Y value if (currentYDown !=
	 * attached.getTop()) { attached.offsetTopAndBottom(currentYDown -
	 * attached.getTop()); } } else if (layoutType == LAYOUT_TYPE_EVENTS) { //
	 * Get first child position based on current scroll value // and calculate
	 * its invisible part firstPositionInfo =
	 * getPositionAndOffsetForScrollValue( mCurrentOffsetX, i); // No data for
	 * desired channel, don't draw anything if
	 * (firstPositionInfo.getFirstChildInvisiblePart() >= 0 &&
	 * firstPositionInfo.getFirstChildIndex() >= 0) { offsetEventsRow(i,
	 * firstPositionInfo.getFirstChildIndex(), currentYDown); } } // If child
	 * row is out of screen if (currentYDown + currentRowHeight +
	 * mVerticalDividerHeight > getHeight()) { mLastChannelPosition = i; break;
	 * } // New child row is inside screen so we draw it else { currentYDown +=
	 * currentRowHeight + mVerticalDividerHeight; } } else { break; } } }
	 */

    /**
     * Layout event and channel indicator views.
     *
     * @param layoutType
     *            Type of layout pass
     */
    /*
     * private void layoutViewsNormal(int layoutType) { // Starting points for
	 * drawing events int currentX = mRectEventsArea.left; int currentY =
	 * layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR ? mRectChannelIndicators.top
	 * : mRectEventsArea.top;
	 * 
	 * final int channelsCount = mChannelItemCount; mLastChannelPosition =
	 * INVALID_POSITION; calculateFirstChannelPosition();
	 * 
	 * // Calculate events area middle point if (mEventsAreaMiddlePoint ==
	 * INVALID_POSITION) { mEventsAreaMiddlePoint = mRectEventsArea.top +
	 * (mRectEventsArea.bottom - mRectEventsArea.top) / 2; }
	 * 
	 * FirstPositionInfo firstPositionInfo; int currentRowHeight = 0; if
	 * (layoutType == LAYOUT_TYPE_EVENTS) { mSelectedItemPosition =
	 * mFirstChannelPosition; } // Calculate first child top invisible part //
	 * In fast scroll, first visible position can be expanded item if
	 * (mFirstChannelPosition == mExpandedChannelIndex) { int invisible =
	 * mExpandedChannelIndex (mChannelRowHeight + mVerticalDividerHeight);
	 * currentY -= (mCurrentOffsetY - invisible); } else { currentY -=
	 * mCurrentOffsetY % (mChannelRowHeight + mVerticalDividerHeight); } int
	 * resizedPercent = 0; // Loop through channels for (int i =
	 * mFirstChannelPosition; i < channelsCount; i++) {
	 * 
	 * // Get channel indicator view at the desired position, or null if // view
	 * do not exist final View attached = isItemAttachedToWindow(
	 * LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION); // Calculate current
	 * row height currentRowHeight = calculateRowHeight(currentY,
	 * currentRowHeight, attached == null ? INVALID_POSITION :
	 * attached.getHeight(), i);
	 * 
	 * if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) { resizedPercent =
	 * calculateResizedPercentOfView(currentRowHeight);
	 * layoutChildView(LAYOUT_TYPE_CHANNEL_INDICATOR, attached,
	 * currentRowHeight, currentY, mRectChannelIndicators.left, resizedPercent,
	 * i, INVALID_POSITION);
	 * 
	 * } else if (layoutType == LAYOUT_TYPE_EVENTS) { // Calculate selected
	 * channel position only in events layout // pass if (currentY <=
	 * mEventsAreaMiddlePoint && currentRowHeight + currentY >=
	 * mEventsAreaMiddlePoint) { mSelectedItemPosition = i;
	 * log("layoutEvents(), mSelectedItemPosition=" + mSelectedItemPosition); //
	 * Save index of expanded channel if (mScrollState !=
	 * SCROLL_STATE_FAST_SCROLL) { mExpandedChannelIndex =
	 * mSelectedItemPosition; } }
	 * 
	 * // Get first child position based on current scroll value // and
	 * calculate its invisible part firstPositionInfo =
	 * getPositionAndOffsetForScrollValue( mCurrentOffsetX, i); // No data for
	 * desired channel, don't draw anything if
	 * (firstPositionInfo.getFirstChildInvisiblePart() < 0 ||
	 * firstPositionInfo.getFirstChildIndex() < 0) { continue; } // Move X
	 * coordinate to left to support drawing of invisible // part of event view
	 * currentX -= firstPositionInfo.getFirstChildInvisiblePart(); if
	 * (firstPositionInfo.getFirstChildIndex() > 0) { currentX +=
	 * mHorizontalDividerWidth; } // Layout all event views for channel
	 * layoutEventsRow(i, currentX, currentY,
	 * firstPositionInfo.getFirstChildIndex(), currentRowHeight); }
	 * 
	 * // If child row is out of screen if (currentY + currentRowHeight +
	 * mVerticalDividerHeight >= getHeight()) { mLastChannelPosition = i; break;
	 * } // New child row is inside screen so we draw it else { currentY +=
	 * currentRowHeight + mVerticalDividerHeight; currentX =
	 * mRectEventsArea.left; } }
	 * 
	 * if (layoutType == LAYOUT_TYPE_EVENTS && mLastChannelPosition ==
	 * INVALID_POSITION) { mLastChannelPosition = channelsCount - 1; } }
	 */
}