package com.epg;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
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
    protected void layoutChannelIndicators() {
        log("layoutChannelIndicators ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
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
        currentY -= mCurrentOffsetY % (mChannelRowHeight + mVerticalDivider);
        //Loop through channels
        for (int i = mFirstChannelPosition; i < channelsCount; i++) {
            //Calculate current row height
            currentRowHeight = calculateRowHeight(currentY, currentRowHeight);
            final View attached = isItemAttachedToWindow(LAYOUT_TYPE_CHANNEL_INDICATOR, i, INVALID_POSITION);
            if (attached == null) {
                View child = mAdapter.getChannelIndicatorView(i, mRecycler.getChannelIndicatorView(), GuideView.this);
                addChildView(LAYOUT_TYPE_CHANNEL_INDICATOR, child, mRectChannelIndicators.left, currentY,
                        mRectChannelIndicators.right - mRectChannelIndicators.left,
                        currentRowHeight, i, INVALID_POSITION);
            } else {
                bringChildToFront(attached);
                //If event height is changed we must resize its view dimension
                if (attached.getHeight() != currentRowHeight) {
                    resizeChildView(attached, currentY, mRectChannelIndicators.right - mRectChannelIndicators.left,
                            currentRowHeight);
                }
                //If current Y coordinate and top position of the view are misplaced we must move a view to desired Y value
                else if (currentY != attached.getTop()) {
                    attached.offsetTopAndBottom(currentY - attached.getTop());
                }
            }

            // If child row is out of screen
            if (currentY + currentRowHeight + mVerticalDivider > getHeight()) {
                mLastChannelPosition = i;
                break;
            }
            //New child row is inside screen so we draw it
            else {
                currentY += currentRowHeight + mVerticalDivider;
            }
        }
        if (mLastChannelPosition == INVALID_POSITION) {
            mLastChannelPosition = channelsCount - 1;
        }

    }

    @Override
    protected void layoutTimeLine() {
        log("layoutTimeLine ###################################");
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

        log("layoutEvents mFirstChannelPosition="
                + mFirstChannelPosition);
        FirstPositionInfo firstPositionInfo;
        int currentRowHeight = 0;
        // Calculate first child top invisible part
        currentY -= mCurrentOffsetY % (mChannelRowHeight + mVerticalDivider);
        //Loop through channels
        for (int i = mFirstChannelPosition; i < channelsCount; i++) {
            //Calculate current row height
            currentRowHeight = calculateRowHeight(currentY, currentRowHeight);
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
                currentX += mHorizontalDivider;
            }
            //Layout all event views for channel
            layoutEventsRow(i, currentX, currentY, firstPositionInfo.getFirstChildIndex(), currentRowHeight);

            // If child row is out of screen
            if (currentY + currentRowHeight + mVerticalDivider > getHeight()) {
                mLastChannelPosition = i;
                break;
            }
            //New child row is inside screen so we draw it
            else {
                currentY += currentRowHeight + mVerticalDivider;
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
        for (int j = firstChildIndex; j < eventCount; j++) {

            final int eventWidth = mAdapter.getEventWidth(channelIndex, j)
                    - (j == 0 ? 0 : mHorizontalDivider);
            final View attached = isItemAttachedToWindow(LAYOUT_TYPE_EVENTS, channelIndex, j);
            if (attached == null) {
                View child = mAdapter.getEventView(channelIndex, j,
                        mRecycler.getEventView(eventWidth), GuideView.this);
                addChildView(LAYOUT_TYPE_EVENTS, child, currentX, currentY, eventWidth,
                        currentRowHeight, channelIndex, j);
            }
            //If event height is changed we must resize its view dimension
            else if (attached.getHeight() != currentRowHeight) {
                resizeChildView(attached, currentY, eventWidth, currentRowHeight);
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
                currentX += eventWidth + mHorizontalDivider;
            }
        }
    }
}
