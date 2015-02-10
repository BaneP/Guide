package com.epg;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Base adapter view that contains listener interfaces and defined public methods
 */
public abstract class GuideAdapterView<T extends BaseGuideAdapter> extends ViewGroup {
    protected static final String TAG = BaseGuideView.class.getSimpleName();
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
    int mFirstChannelPosition = INVALID_POSITION;
    /**
     * Last visible channel position
     */
    int mLastChannelPosition = INVALID_POSITION;

    /**
     * The number of channel items in the current adapter.
     */
    int mChannelItemCount = 0;

    /**
     * Currently selected view
     */
    View mSelectedView;

    public GuideAdapterView(Context context) {
        super(context);
    }

    public GuideAdapterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GuideAdapterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public abstract T getAdapter();

    public abstract void setAdapter(T adapter);

    /**
     * @return Returns first visible channel.
     */
    public int getFirstChannelPosition() {
        return mFirstChannelPosition;
    }

    /**
     * @return Returns last visible channel.
     */
    public int getLastChannelPosition() {
        return mLastChannelPosition;
    }

    /**
     * @return Returns number of visible channels.
     */
    public int getNumberOfVisibleChannels() {
        return mLastChannelPosition - mFirstChannelPosition;
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
        return mChannelItemCount;
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

    void fireOnSelected() {
        //TODO
        //        if (mOnItemSelectedListener == null) {
        //            return;
        //        }
        //        if (mSelectedView == null) {
        //            mOnItemSelectedListener.onNothingSelected(this);
        //        }else {
        //            EpgView.LayoutParams lp=null;
        //            try {
        //                lp = (EpgView.LayoutParams) mSelectedView.getLayoutParams();
        //                mOnItemSelectedListener.onItemSelected(this, mSelectedView, lp.getChannelIndex(), lp.getEventIndex());
        //            }catch (ClassCastException e) {
        //                mOnItemSelectedListener.onNothingSelected(this);
        //            }
        //        }
    }

    protected void log(String msg) {
        Log.d(TAG, msg);
    }
}
