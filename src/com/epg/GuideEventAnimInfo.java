package com.epg;

import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Class that contains information necessary for ON_NOW to FULL_GUIDE animation to work
 *
 * @author Branimir Pavlovic
 */
public class GuideEventAnimInfo {
    private WeakReference<View> mEventViewWeakReference;
    private int mEventPosition;
    private int mStartWidth;
    private int mEndWidth;
    private int mEndPosition;

    public GuideEventAnimInfo(View eventView, int eventPosition, int mStartWidth, int mEndWidth, int mEndPosition) {
        mEventViewWeakReference = new WeakReference<View>(eventView);
        this.mEventPosition = eventPosition;
        this.mStartWidth = mStartWidth;
        this.mEndWidth = mEndWidth;
        this.mEndPosition = mEndPosition;
    }

    @Override
    public String toString() {
        return "GuideEventAnimInfo{" +
                "mStartWidth=" + mStartWidth +
                ", mEndWidth=" + mEndWidth +
                ", mEndPosition=" + mEndPosition +
                '}';
    }

    public int getStartWidth() {
        return mStartWidth;
    }

    public int getEndWidth() {
        return mEndWidth;
    }

    public int getEndPosition() {
        return mEndPosition;
    }

    public View getEventView() {
        return mEventViewWeakReference.get();
    }

    public int getEventPosition() {
        return mEventPosition;
    }

}
