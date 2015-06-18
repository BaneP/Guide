package com.epg;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Base adapter view that contains listener interfaces and defined public methods
 *
 * @author Branimir Pavlovic
 */
public abstract class GuideAdapterView<T extends BaseGuideAdapter> extends ViewGroup {
    protected static final String TAG = GuideAdapterView.class.getSimpleName();
    /**
     * Represents an invalid position. All valid positions are in the range 0 to 1 less than the
     * number of items in the current adapter.
     */
    public static final int INVALID_POSITION = -1;

    /**
     * When set to true, calls to requestLayout() will not propagate up the parent hierarchy.
     * This is used to layout the children during a layout pass.
     */
    boolean mBlockLayoutRequests = false;

    /**
     * Indicates that this view is currently being laid out.
     */
    boolean mInLayout = false;

    /**
     * First visible channel position
     */
    int mFirstItemPosition = INVALID_POSITION;
    /**
     * Last visible channel position
     */
    int mLastItemPosition = INVALID_POSITION;

    /**
     * The number of channel items in the current adapter.
     */
    int mChannelsCount = 0;

    /**
     * Position of selected item
     */
    int mSelectedItemPosition;

    /**
     * Position of selected event
     */
    int mSelectedEventItemPosition;

    /**
     * Currently selected view
     */
    View mSelectedView;

    /**
     * On long press scroll started/stopped listener
     */
    OnLongPressScrollListener mOnLongPressScrollListener;

    /**
     * Item click listener
     */
    OnItemClickListener mOnItemClickListener;

    /**
     * View select listener
     */
    OnItemSelectedListener mOnItemSelectedListener;

    public GuideAdapterView(Context context) {
        super(context);
    }

    public GuideAdapterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GuideAdapterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Callback that indicates changes of long press scroll
     */
    public interface OnLongPressScrollListener {
        /**
         * Long press scroll started
         */
        void onLongPressScrollStarted();

        /**
         * Long press scroll stopped
         */
        void onLongPressScrollStopped();
    }

    public OnLongPressScrollListener getOnLongPressScrollListener() {
        return mOnLongPressScrollListener;
    }

    public void setOnLongPressScrollListener(OnLongPressScrollListener mOnLongPressScrollListener) {
        this.mOnLongPressScrollListener = mOnLongPressScrollListener;
    }

    public interface OnItemClickListener {
        void onItemClick(GuideAdapterView<?> parent, View view, int channelPosition, int eventPosition);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public final OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    /**
     * Call the OnItemClickListener, if it is defined. Performs all normal
     * actions associated with clicking: reporting accessibility event, playing
     * a sound, etc.
     *
     * @param view The view within the AdapterView that was clicked.
     * @return True if there was an assigned OnItemClickListener that was
     * called, false otherwise is returned.
     */
    boolean performItemClick(View view) {
        if (mOnItemClickListener != null && view != null) {
            BaseGuideView.LayoutParams lp = (BaseGuideView.LayoutParams) view.getLayoutParams();
            mOnItemClickListener.onItemClick(this, view, lp.mChannelIndex, lp.mEventIndex);
            if (view != null) {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
            return true;
        }
        return false;
    }

    /**
     * TODO Define specific listeners here
     */
    public interface OnItemSelectedListener {
        void onItemSelected(GuideAdapterView<?> parent, View view, int channelPosition, int eventPosition);

        void onNothingSelected(GuideAdapterView<?> parent);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    public final OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    /**
     * Fire on selected listener
     */
    void fireOnSelected() {
        if (mOnItemSelectedListener == null) {
            return;
        }
        if (mSelectedView == null) {
            mOnItemSelectedListener.onNothingSelected(this);
        } else {
            BaseGuideView.LayoutParams lp = null;
            try {
                lp = (BaseGuideView.LayoutParams) mSelectedView.getLayoutParams();
                mOnItemSelectedListener.onItemSelected(this, mSelectedView, lp.mChannelIndex, lp.mEventIndex);
            } catch (ClassCastException e) {
                mOnItemSelectedListener.onNothingSelected(this);
            }
        }
    }

    public abstract T getAdapter();

    public abstract void setAdapter(T adapter);

    /**
     * @return Returns first visible channel.
     */
    public int getFirstChannelPosition() {
        return mFirstItemPosition >= 0 ? mFirstItemPosition : 0;
    }

    /**
     * @return Returns last visible channel.
     */
    public int getLastChannelPosition() {
        if (getAdapter() == null) {
            return INVALID_POSITION;
        }
        return mLastItemPosition < getAdapter().getChannelsCount() ?
                mLastItemPosition :
                getAdapter().getChannelsCount() - 1;
    }

    /**
     * @return Returns number of visible channels.
     */
    public int getNumberOfVisibleChannels() {
        return mLastItemPosition - mFirstItemPosition;
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child Ignored.
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void addView(View child) {
        throw new UnsupportedOperationException("addView(View) is not supported in GuideAdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child Ignored.
     * @param index Ignored.
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void addView(View child, int index) {
        throw new UnsupportedOperationException("addView(View, int) is not supported in GuideAdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child  Ignored.
     * @param params Ignored.
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void addView(View child, LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, LayoutParams) "
                + "is not supported in GuideAdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child  Ignored.
     * @param index  Ignored.
     * @param params Ignored.
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void addView(View child, int index, LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, int, LayoutParams) "
                + "is not supported in GuideAdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child Ignored.
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void removeView(View child) {
        throw new UnsupportedOperationException("removeView(View) is not supported in GuideAdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param index Ignored.
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void removeViewAt(int index) {
        throw new UnsupportedOperationException("removeViewAt(int) is not supported in GuideAdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void removeAllViews() {
        throw new UnsupportedOperationException("removeAllViews() is not supported in GuideAdapterView");
    }

    /**
     * @return The number of channel items owned by the EpgAdapter associated with this
     * GuideAdapterView. (This is the number of data items, which may be
     * larger than the number of visible views.)
     */
    public int getChannelsCount() {
        return mChannelsCount;
    }

    /**
     * @return The number of channel items owned by the EpgAdapter associated with this
     * GuideAdapterView. (This is the number of data items, which may be
     * larger than the number of visible views.)
     */
    public int getEventsCount(int channelIndex) {
        return getAdapter().getEventsCount(channelIndex);
    }

    /**
     * Set desired channel selected
     *
     * @param channelPosition New selected position
     */
    public abstract void setSelection(int channelPosition);

    /**
     * Set desired channel selected with desired event
     *
     * @param channelPosition New selected position
     * @param eventPosition   New selected event position
     */
    public abstract void setSelection(int channelPosition, int eventPosition);

    /**
     * @return Currently selected view.
     */
    public abstract View getSelectedView();

    /**
     * @return Currently selected channel position
     */
    public int getSelectedItemChannelPosition() {
        int selectedChannel = INVALID_POSITION;
        try {
            selectedChannel = ((BaseGuideView.LayoutParams) mSelectedView.getLayoutParams())
                    .mChannelIndex;
        } catch (Exception e) {
        }
        return selectedChannel;
    }

    /**
     * @return Currently selected event position
     */
    public int getSelectedItemEventPosition() {
        int selectedEvent = INVALID_POSITION;
        try {
            selectedEvent = ((BaseGuideView.LayoutParams) mSelectedView.getLayoutParams())
                    .mEventIndex;
        } catch (Exception e) {
        }
        return selectedEvent;
    }

    /**
     * Gets the data associated with the specified position in the list.
     *
     * @param position Which data to get
     * @return The data associated with the specified position in the list
     */
    public Object getChannelItemAtPosition(int position) {
        T adapter = getAdapter();
        if (adapter != null && adapter.getChannelsCount() > position && position >= 0) {
            return adapter.getItem(position);
        } else {
            return null;
        }
    }

    /**
     * Gets the data associated with the specified position in the list.
     *
     * @param channelPosition Which channel data to get
     * @param eventPosition   Which event data to get
     * @return The data associated with the specified position in the list
     */
    public Object getEventItemAtPosition(int channelPosition, int eventPosition) {
        T adapter = getAdapter();
        if (adapter != null && adapter.getChannelsCount() > channelPosition && channelPosition >= 0
                && adapter.getEventsCount(channelPosition) > eventPosition && eventPosition >= 0) {
            return adapter.getItem(channelPosition, eventPosition);
        } else {
            return null;
        }
    }

    /**
     * @return Calculated difference between two calendars in minutes
     */
    public static int calculateDiffInMinutes(Calendar endTime, Calendar startTime) {
        long diffInMs = Math.abs(endTime.getTimeInMillis() - startTime.getTimeInMillis());
        return (int) TimeUnit.MILLISECONDS.toMinutes(diffInMs);
    }

    /**
     * Calculates Y overlap value of 2 rectangles
     *
     * @return Calculated overlap value
     */
    protected int calculateYOverlapValue(Rect rect1, Rect rect2) {
        return Math.max(0, Math.min(rect1.bottom, rect2.bottom)
                - Math.max(rect1.top, rect2.top));
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

    protected void log(String msg) {
        Log.d(TAG, msg);
    }
}