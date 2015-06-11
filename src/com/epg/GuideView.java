package com.epg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Custom view for representing guide information. Contains logic for layout child views, changing states of guide
 * (ON_NOW, FULL), handling key events etc...
 *
 * @author Branimir Pavlovic
 */
public class GuideView extends BaseGuideView {
    /**
     * Open and close guide animation duration
     */
    public static final int FULL_GUIDE_ANIM_DURATION = 500;
    public static final int ON_NOW_GUIDE_ANIM_DURATION = 250;
    public static final int GUIDE_TRANSITION_ANIM_DURATION = 500;

    /**
     * Guide mode types
     */
    public static final int GUIDE_MODE_FULL = 0;
    public static final int GUIDE_MODE_ON_NOW = 1;
    static final int GUIDE_MODE_IN_TRANSITION = 2;

    private static final int PARENT_WIDTH_DIVIDE = 2;
    /**
     * Active guide mode
     */
    protected int mGuideMode = GUIDE_MODE_FULL;

    /**
     * This is used to be drawn above guide event views
     */
    private Paint mDividerOverlayPaint;

    /**
     * View to change alpha when opening/closing guide
     */
    private View mBackgroundView;

    /**
     * Used for calculating central selected item position
     */
    private int mEventsAreaMiddlePoint = INVALID_POSITION;
    /**
     * Previous system time of long press
     */
    private long mLongPressTime = 0;
    private boolean isInLongPress = false;
    /**
     * List of calculated vertical positions of channel rows
     */
    private ArrayList<GuideRowInfo> mRows;
    /**
     * Force redraw when force changing size of guide (when changing guide mode)
     */
    private boolean redrawOnSizeChanged = false;
    /**
     * Contains information about running events for each channel
     */
    private HashMap<Integer, GuideEventAnimInfo> mRunningEventInfo;

    /**
     * Guide hide animation listener
     */
    private Animation.AnimationListener mHideListener;

    /**
     * Position of event view that will be selected when guide scrolls to it
     */
    private int mDesiredEventPosition = INVALID_POSITION;

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
        //Initialize divider paint between channel indicators and events
        mDividerOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDividerOverlayPaint.setColor(Color.BLACK);

        // Init rows information holder
        mRows = new ArrayList<GuideRowInfo>();
        //Init running event info
        mRunningEventInfo = new HashMap<Integer, GuideEventAnimInfo>();
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
            if (mGuideMode != GUIDE_MODE_IN_TRANSITION) {
                log("ON KEY UP, KEYCODE_DPAD_UP, mScrollState=" + mScrollState + ", isInLongPress=" + isInLongPress);
                if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                    changeScrollState(SCROLL_STATE_FAST_SCROLL_END, keyCode);
                }
            }
            handled = true;
            break;
        }
        case KeyEvent.KEYCODE_DPAD_DOWN: {
            if (mGuideMode != GUIDE_MODE_IN_TRANSITION) {
                log("ON KEY UP, KEYCODE_DPAD_DOWN, mScrollState=" + mScrollState + ", isInLongPress=" + isInLongPress);
                if (mScrollState == SCROLL_STATE_FAST_SCROLL) {
                    changeScrollState(SCROLL_STATE_FAST_SCROLL_END, keyCode);
                }
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
            if (mGuideMode != GUIDE_MODE_IN_TRANSITION) {
                log("ON KEY DOWN, KEYCODE_DPAD_DOWN, mScrollState=" + mScrollState + ", isInLongPress=" +
                        isInLongPress);
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
                    return mSmoothScrollRunnable.startVerticalScrollToPosition(
                            mSelectedItemPosition - 1, SMOOTH_SCROLL_DURATION);
                }
            }
            return true;
        }
        case KeyEvent.KEYCODE_DPAD_DOWN: {
            if (event.isLongPress()) {
                isInLongPress = true;
            }
            long now = System.currentTimeMillis();
            if (mGuideMode != GUIDE_MODE_IN_TRANSITION) {
                log("ON KEY DOWN, KEYCODE_DPAD_DOWN, mScrollState=" + mScrollState + ", isInLongPress="
                        + isInLongPress);
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
                    return mSmoothScrollRunnable.startVerticalScrollToPosition(
                            mSelectedItemPosition + 1, SMOOTH_SCROLL_DURATION);
                }
            }
            return true;
        }
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT: {
            if (mGuideMode == GUIDE_MODE_FULL) {
                return selectRightLeftView(keyCode);
            }
            if (mGuideMode == GUIDE_MODE_ON_NOW && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                changeGuideMode(GUIDE_MODE_FULL);
                return true;
            }
            break;
        }
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER: {
            return performItemClick(mSelectedView);
        }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void setVisibility(int visibility) {
        if (getVisibility() == visibility) {
            return;
        }
        if (visibility == View.GONE) {
            throw new IllegalArgumentException("visibility can not be GONE!!!");
        }
        int width = 0;
        if (getParent() != null && getParent() instanceof ViewGroup) {
            width = ((ViewGroup) getParent()).getMeasuredWidth();
        }
        final boolean show = visibility == View.VISIBLE;
        Animation anim = null;
        switch (mGuideMode) {
        case GUIDE_MODE_FULL: {
            anim = new GuideOpenCloseAnimation(this, show ? width : 0, show ? 0 : width, show ? 0f : 1f,
                    show ? 1f : 0f);
            anim.setInterpolator(new AccelerateInterpolator());
            anim.setDuration(FULL_GUIDE_ANIM_DURATION);
            break;
        }
        case GUIDE_MODE_ON_NOW: {
            anim = new GuideOpenCloseAnimation(this, show ? width / PARENT_WIDTH_DIVIDE : 0,
                    show ? 0 : width / PARENT_WIDTH_DIVIDE, show ? 0f : 0.5f, show ? 0.5f : 0f);
            anim.setInterpolator(new AccelerateInterpolator());
            anim.setDuration(ON_NOW_GUIDE_ANIM_DURATION);
            break;
        }
        default: {
            return;
        }
        }

        //Return scroll to beginning
        if (visibility == View.VISIBLE) {
            mCurrentOffsetX = 0;
            unselectSeletedViewWithoutCallback();
            redrawItems();
        }
        if (visibility == View.INVISIBLE) {
            unselectSeletedViewWithoutCallback();
        }
        super.setVisibility(visibility);
        if (anim != null) {
            if (visibility == INVISIBLE) {
                anim.setAnimationListener(mHideListener);
            }
            setAnimation(anim);
        }
    }

    /**
     * Change guide mode, if guide is visible animation is started, else just change mode and refresh
     *
     * @param newMode Desired new mode
     */
    public void changeGuideMode(int newMode) {
        switch (mGuideMode) {
        case GUIDE_MODE_FULL: {
            //TODO cant change mode from full guide, you can only close it
            if (newMode == GUIDE_MODE_ON_NOW) {
                mGuideMode = GUIDE_MODE_ON_NOW;
                resizeGuideWithoutAnimation();
            }
            break;
        }
        case GUIDE_MODE_IN_TRANSITION: {
            if (newMode == GUIDE_MODE_FULL) {
                mGuideMode = GUIDE_MODE_FULL;
                //TODO what to do here?
            }
            break;
        }
        case GUIDE_MODE_ON_NOW: {
            if (newMode == GUIDE_MODE_FULL) {
                //If guide is visible we should start animation
                if (isShown()) {
                    changeGuideMode(GUIDE_MODE_IN_TRANSITION);
                }
                //If guide is invisible just resize and refresh it
                else {
                    mGuideMode = GUIDE_MODE_FULL;
                    resizeGuideWithoutAnimation();
                }
            } else if (newMode == GUIDE_MODE_IN_TRANSITION) {
                startGuideTransitionMode();
            }
            break;
        }
        }
    }

    /**
     * Starts animation that will expand guide to FULL
     */
    private void startGuideTransitionMode() {
        int width = 0;
        if (getParent() != null && getParent() instanceof ViewGroup) {
            width = ((ViewGroup) getParent()).getMeasuredWidth();
        }
        mRunningEventInfo.clear();
        final ArrayList<View> views = mRecycler.getActiveEventViews();
        int channelIndex = 0;
        int eventIndex = 0;
        //Populate animation info for active events
        for (View eventView : views) {
            channelIndex = ((LayoutParams) eventView.getLayoutParams()).mChannelIndex;
            eventIndex = ((LayoutParams) eventView.getLayoutParams()).mEventIndex;
            mRunningEventInfo.put(channelIndex, new GuideEventAnimInfo
                    (eventView, eventIndex, eventView.getMeasuredWidth(), calculateEventWidth(channelIndex, eventIndex),
                            getOffsetForSelectedEventFromBeginning(channelIndex, eventIndex)));
            log("EVENT ANIM INFO: channelIndex=" + channelIndex + mRunningEventInfo.get(channelIndex).toString());
        }

        GuideResizeAnimation animation = new GuideResizeAnimation(this, getMeasuredWidth(), width, mRunningEventInfo,
                mBackgroundView.getAlpha(), 1f);
        animation.setDuration(GUIDE_TRANSITION_ANIM_DURATION);
        animation.setInterpolator(new AccelerateInterpolator());
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mGuideMode = GUIDE_MODE_IN_TRANSITION;
                drawTimeLine = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                changeGuideMode(GUIDE_MODE_FULL);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        startAnimation(animation);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (redrawOnSizeChanged) {
            redrawOnSizeChanged = false;
            unselectSeletedViewWithoutCallback();
            redrawItems();
        }
    }

    /**
     * Resize guide quickly without running animation
     */
    private void resizeGuideWithoutAnimation() {
        int width = 0;
        ViewGroup.LayoutParams params = getLayoutParams();
        if (getParent() != null && getParent() instanceof ViewGroup) {
            width = ((ViewGroup) getParent()).getMeasuredWidth();
        }
        params.width = mGuideMode == GUIDE_MODE_FULL ? width : width / PARENT_WIDTH_DIVIDE;
        if (mGuideMode == GUIDE_MODE_ON_NOW) {
            if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).gravity = Gravity.RIGHT;
            } else if (params instanceof RelativeLayout.LayoutParams) {
                ((RelativeLayout.LayoutParams) params).addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            } else if (params instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) params).gravity = Gravity.RIGHT;
            }
        } else if (mGuideMode == GUIDE_MODE_FULL) {
            if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).gravity = Gravity.NO_GRAVITY;
            } else if (params instanceof RelativeLayout.LayoutParams) {
                ((RelativeLayout.LayoutParams) params).addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            } else if (params instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) params).gravity = Gravity.NO_GRAVITY;
            }
        }
        redrawOnSizeChanged = true;
        setLayoutParams(params);
        drawTimeLine = mGuideMode == GUIDE_MODE_FULL;
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

    /**
     * Smoothly scroll to position horizontally
     *
     * @param position New event position to scroll to
     */
    public void smoothScrollToEventPosition(int position) {
        if (getSelectedItemEventPosition() == position) {
            return;
        }
        final View nextView = findItemAttachedToWindow(LAYOUT_TYPE_EVENTS,
                mSelectedItemPosition, position);
        int eventPosition = getEventPositionOnScreen(nextView, 0);
        int scrollBy = eventPosition - mSelectionAbsolutePosition;
        if (mCurrentOffsetX + scrollBy < 0) {
            scrollBy = -mCurrentOffsetX;
        }
        if (mSmoothScrollRunnable.startScrollBy(scrollBy, 0, SMOOTH_LEFT_RIGHT_DURATION)) {
            mDesiredEventPosition = position;
            unselectSeletedViewWithoutCallback();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //Draw divider between channel indicators and events
        if (mRows != null && mRows.size() > 0) {
            canvas.drawRect(mRectChannelIndicators.right, mRows.get(0).getTop() < mRectEventsArea.top ? mRectEventsArea
                            .top : mRows.get(0).getTop(),
                    mRectChannelIndicators
                            .right + mHorizontalDividerWidth, mRows.get(mRows.size() - 1).getBottom(),
                    mDividerOverlayPaint);
        }
    }

    @Override
    protected void layoutChannelIndicators() {
        layoutViews(LAYOUT_TYPE_CHANNEL_INDICATOR);
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
                View attached = findItemAttachedToWindow(
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
                    mFirstItemPosition = i;
                    mRecycler.removeItemsAtPosition(i - 1);
                    break;
                }

            }
            /**
             * From selected to the bottom of the screen
             */
            for (i = mSelectedItemPosition + 1; i < mChannelsCount; i++) {
                View attached = findItemAttachedToWindow(
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
                    mLastItemPosition = i;
                    mRecycler.removeItemsAtPosition(i + 1);
                    break;
                } else {
                    currentYDown += currentRowHeight + mVerticalDividerHeight;
                }
            }
        }
        // For normal scroll and fast scroll
        else {
            int currentY = mRectChannelIndicators.top + mEventsVerticalOffset;
            final int channelsCount = mChannelsCount;

            calculateFirstChannelPosition();

            // Calculate events area middle point
            if (mEventsAreaMiddlePoint == INVALID_POSITION) {
                mEventsAreaMiddlePoint = mRectEventsArea.top
                        + mRectEventsArea.height() / 2;
            }

            // Calculate first child top invisible part
            // In fast scroll, first visible position can be expanded item
            if (mFirstItemPosition == mExpandedItemIndex) {
                // Current offset Y - invisible part of guide
                currentY -= (mCurrentOffsetY - (mExpandedItemIndex * (mChannelRowHeight + mVerticalDividerHeight)));
            } else {
                currentY -= mCurrentOffsetY % (mChannelRowHeight + mVerticalDividerHeight);
            }

            // Loop through channels
            for (int i = mFirstItemPosition; i < channelsCount; i++) {

                // Get channel indicator view at the desired position, or null
                // if
                // view do not exist
                final View attached = findItemAttachedToWindow(
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
                        mExpandedItemIndex = mSelectedItemPosition;
                    }
                }
                //Do not show channels with index in minus
                if (i >= 0) {
                    mRows.add(new GuideRowInfo(i, currentY, currentRowHeight,
                            resizedPercent));
                }
                // If child row is out of screen
                if (currentY + currentRowHeight + mVerticalDividerHeight >= mRectEventsArea.bottom) {
                    mLastItemPosition = i;
                    mRecycler.removeItemsAtPosition(i + 1);
                    break;
                }
                // New child row is inside screen so we draw it
                else {
                    currentY += currentRowHeight + mVerticalDividerHeight;
                }
            }
        }
        //For fast scroll dispatch On nothing selected callback
        if (mSelectedView != null && (mScrollState == SCROLL_STATE_FAST_SCROLL || mScrollState ==
                SCROLL_STATE_FAST_SCROLL_END)) {
            selectNextView(null);
        } else if (mScrollState == SCROLL_STATE_NORMAL && mSelectedView != null
                && mSelectedItemPosition != getSelectedItemChannelPosition()) {
            unselectSeletedViewWithoutCallback();
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
                View attached = findItemAttachedToWindow(
                        LAYOUT_TYPE_CHANNEL_INDICATOR,
                        guideRowInfo.getChannelIndex(), INVALID_POSITION);
                attached = layoutChildView(layoutType, attached,
                        guideRowInfo.getHeight(), guideRowInfo.getTop(),
                        mRectChannelIndicators.left,
                        guideRowInfo.getResizedPercent(),
                        guideRowInfo.getChannelIndex(), INVALID_POSITION);
                //Mark central channel indicator as selected
                if (mScrollState == SCROLL_STATE_NORMAL && guideRowInfo.getChannelIndex() ==
                        mSelectedItemPosition) {
                    attached.setSelected(true);
                } else {
                    attached.setSelected(false);
                }

            } else {
                // Get first child position based on current scroll value
                // and calculate its invisible part
                firstPositionInfo = getPositionAndOffsetForScrollValue(
                        mCurrentOffsetX, guideRowInfo.getChannelIndex());
                // No data for desired channel, don't draw anything
                if (firstPositionInfo.getFirstChildInvisiblePart() < 0
                        || firstPositionInfo.getFirstChildIndex() < 0) {
                    continue;
                }
                if (mGuideMode == GUIDE_MODE_ON_NOW) {
                    layoutEventsOnNowMode(guideRowInfo);
                } else if (mGuideMode == GUIDE_MODE_IN_TRANSITION) {
                    layoutEventsInTransitionMode(guideRowInfo);
                } else {
                    // Move X coordinate to left to support drawing of invisible
                    // part of event view
                    currentX -= firstPositionInfo.getFirstChildInvisiblePart();
                    if (firstPositionInfo.getFirstChildIndex() > 0) {
                        currentX += mHorizontalDividerWidth;
                    }
                    // Layout all event views for channel
                    layoutEventsRow(guideRowInfo.getChannelIndex(), currentX,
                            guideRowInfo.getTop(),
                            firstPositionInfo.getFirstChildIndex(),
                            guideRowInfo.getHeight());
                    currentX = mRectEventsArea.left;
                }
            }
        }
    }

    /**
     * Position running events in ON_NOW mode.
     *
     * @param guideRowInfo Information about current row
     */
    private void layoutEventsOnNowMode(GuideRowInfo guideRowInfo) {

        final int eventIndex = mAdapter.getNowEventIndex(guideRowInfo.getChannelIndex());
        View attached = findItemAttachedToWindow(
                LAYOUT_TYPE_EVENTS,
                guideRowInfo.getChannelIndex(), eventIndex);
        attached = layoutChildView(LAYOUT_TYPE_EVENTS, attached,
                guideRowInfo.getHeight(), guideRowInfo.getTop(),
                mRectEventsArea.left,
                guideRowInfo.getResizedPercent(),
                guideRowInfo.getChannelIndex(), eventIndex);
        if (guideRowInfo.getChannelIndex() == mSelectedItemPosition && mSelectedView == null
                && mScrollState == SCROLL_STATE_NORMAL) {
            selectNextView(attached);
            mTempSelectedViewOffset = INVALID_POSITION;
        }
    }

    /**
     * Position running events in TRANSITION mode.
     *
     * @param guideRowInfo Information about current row
     */
    private void layoutEventsInTransitionMode(GuideRowInfo guideRowInfo) {
        final int eventIndex = mRunningEventInfo.get(guideRowInfo.getChannelIndex()).getEventPosition();

        //Resize and layout running event
        View attached = findItemAttachedToWindow(
                LAYOUT_TYPE_EVENTS,
                guideRowInfo.getChannelIndex(), eventIndex);
        attached = layoutChildView(LAYOUT_TYPE_EVENTS, attached,
                guideRowInfo.getHeight(), guideRowInfo.getTop(),
                ((LayoutParams) attached.getLayoutParams()).mLeftCoordinate + mRectEventsArea.left,
                guideRowInfo.getResizedPercent(),
                guideRowInfo.getChannelIndex(), eventIndex);

        int xCoordinateRight = attached.getRight() + mHorizontalDividerWidth;
        int xCoordinateLeft = attached.getLeft() - mHorizontalDividerWidth;
        // Layout events to the right
        layoutEventsRow(guideRowInfo.getChannelIndex(), xCoordinateRight, guideRowInfo.getTop(), eventIndex + 1,
                guideRowInfo.getHeight());
        //Layout events to the left
        layoutEventsRowToLeft(guideRowInfo.getChannelIndex(), xCoordinateLeft, guideRowInfo.getTop(), eventIndex - 1,
                guideRowInfo.getHeight());
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
        //log("GUIDE VIEW layoutEventsRow channelIndex=" + channelIndex);
        // Get number of events
        final int eventCount = getEventsCount(channelIndex);
        final int resizedPercent = calculateResizedPercentOfView(currentRowHeight);
        int eventWidth = 0;
        int right = 0;
        View viewToSelect = null;
        int minCalculatedOffset = Integer.MAX_VALUE;
        for (int j = firstChildIndex; j < eventCount; j++) {
            View attached = findItemAttachedToWindow(LAYOUT_TYPE_EVENTS,
                    channelIndex, j);
            attached = layoutChildView(LAYOUT_TYPE_EVENTS, attached,
                    currentRowHeight, currentY, currentX, resizedPercent,
                    channelIndex, j);
            eventWidth = attached.getMeasuredWidth();
            right = currentX + eventWidth;
            /**
             * If selected view is null we must mark some selected channel event selected
             */
            if (channelIndex == mSelectedItemPosition && mSelectedView == null && mScrollState == SCROLL_STATE_NORMAL) {
                if (mDesiredEventPosition != INVALID_POSITION) {
                    if (mDesiredEventPosition == j) {
                        viewToSelect = attached;
                    }
                    //TODO NOT FIXED ON SCREEN SHOULD BE IMPLEMENTED
                } else if (mSelectionType == SelectionType.FIXED_ON_SCREEN) {
                    if (mSelectedEventItemPosition == INVALID_POSITION) {
                        int offset = minCalculatedOffset == 0 ? 0 : calculateOffsetFromFixedSelection(currentX, right);
                        if (minCalculatedOffset > 0 && offset < minCalculatedOffset) {
                            minCalculatedOffset = offset;
                            viewToSelect = attached;
                        }
                    } else if (mSelectedEventItemPosition == j) {
                        viewToSelect = attached;
                    }
                } else if (viewToSelect == null) {
                    viewToSelect = attached;
                }
            }
            // If child right edge is larger or equals to right
            // bound of EpgView
            if (right >= getWidth()) {
                break;
            } else {
                currentX = right + mHorizontalDividerWidth;
            }
        }
        if (viewToSelect != null) {
            selectNextView(viewToSelect);
            mDesiredEventPosition = INVALID_POSITION;
            mTempSelectedViewOffset = INVALID_POSITION;
        }
    }

    /**
     * Position whole row of guide data from right to left.
     *
     * @param channelIndex     Index of channel to draw
     * @param currentX         Current X position on screen
     * @param currentY         Current Y position on screen
     * @param firstChildIndex  Index of first visible event index
     * @param currentRowHeight Current row height
     */
    private void layoutEventsRowToLeft(final int channelIndex, int currentX,
            int currentY, final int firstChildIndex, int currentRowHeight) {
        // Get number of events
        final int resizedPercent = calculateResizedPercentOfView(currentRowHeight);
        int eventWidth = 0;
        int left = 0;
        for (int j = firstChildIndex; j >= 0; j--) {
            View attached = findItemAttachedToWindow(LAYOUT_TYPE_EVENTS,
                    channelIndex, j);
            eventWidth = calculateEventWidth(channelIndex, j);
            attached = layoutChildView(LAYOUT_TYPE_EVENTS, attached,
                    currentRowHeight, currentY, currentX - eventWidth, resizedPercent,
                    channelIndex, j);
            left = currentX - eventWidth;
            // If child right edge is larger or equals to right
            // bound of EpgView
            if (left <= 0) {
                break;
            } else {
                currentX = left - mHorizontalDividerWidth;
            }
        }
    }

    /**
     * Calculate offset from absolute selection position
     *
     * @param left  Left edge of view
     * @param right Right edge of view
     * @return Calculated offset
     */
    private int calculateOffsetFromFixedSelection(int left, int right) {
        int desiredXValue = mSelectionAbsolutePosition;
        if (mTempSelectedViewOffset > INVALID_POSITION) {
            desiredXValue = mTempSelectedViewOffset;
        }
        if (left <= desiredXValue && right >= desiredXValue) {
            return 0;
        } else if (right < desiredXValue) {
            return (int) (desiredXValue - right);
        } else {
            return (int) (left - desiredXValue);
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
     * @return Created child view
     */
    private View layoutChildView(int layoutType, View attached,
            int currentRowHeight, int currentY, int currentX,
            int resizedPercent, int channelIndex, int eventIndex) {
        //log("GUIDE VIEW layoutChildView channelIndex=" + channelIndex + ", eventIndex=" + eventIndex);
        int eventWidth = 0;
        if (layoutType == LAYOUT_TYPE_EVENTS) {
            if (mGuideMode == GUIDE_MODE_ON_NOW) {
                eventWidth = mRectEventsArea.width();
            } else if (mGuideMode == GUIDE_MODE_IN_TRANSITION
                    && mRunningEventInfo.get(channelIndex).getEventPosition() == eventIndex) {
                eventWidth = attached.getLayoutParams().width;
            } else {
                eventWidth = calculateEventWidth(channelIndex, eventIndex);
            }
        } else if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) {
            eventWidth = mRectChannelIndicators.width();
        }
        if (attached == null) {
            if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) {
                attached = mAdapter.getChannelIndicatorView(channelIndex,
                        mRecycler.getChannelIndicatorView(), GuideView.this);
            } else if (layoutType == LAYOUT_TYPE_EVENTS) {
                //log("VIEW IS NULL got through adapter");
                attached = mAdapter.getEventView(channelIndex, eventIndex,
                        mRecycler.getEventView(eventWidth), GuideView.this);
            }
            addChildView(layoutType, attached, currentX, currentY, eventWidth,
                    currentRowHeight, channelIndex, eventIndex);
        } else {
            if (layoutType == LAYOUT_TYPE_CHANNEL_INDICATOR) {
                bringChildToFront(attached);
            }
            // If event height is changed we must resize its view dimension
            if (attached.getHeight() != currentRowHeight || attached.getWidth() != eventWidth || currentX != attached
                    .getLeft()) {
                resizeChildView(attached, currentY, currentX, eventWidth,
                        currentRowHeight, resizedPercent);
            }
            // If current Y coordinate and top position of the view are
            // misplaced we must move a view to desired Y value
            else if (currentY != attached.getTop()) {
                attached.offsetTopAndBottom(currentY - attached.getTop());
            }
        }
        return attached;
    }

    /**
     * Calculate width of view that represents desired event.
     *
     * @param channelIndex Channel index
     * @param eventIndex   Event index
     * @return Calculated width of view.
     */
    private int calculateEventWidth(int channelIndex, int eventIndex) {
        return mAdapter.getEventWidth(channelIndex, eventIndex) * mOneMinuteWidth
                - (eventIndex == 0 ? 0 : mHorizontalDividerWidth);
    }

    public int getGuideMode() {
        return mGuideMode;
    }

    public View getBackgroundView() {
        return mBackgroundView;
    }

    public void setBackgroundView(View mBackgroundView) {
        this.mBackgroundView = mBackgroundView;
    }

    public Animation.AnimationListener getHideListener() {
        return mHideListener;
    }

    public void setHideListener(Animation.AnimationListener mHideListener) {
        this.mHideListener = mHideListener;
    }
}