package com.epg;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Animation used to expand guide from ON_NOW to FULL. It animates changing of guide width and also changes selected
 * event views width and position within guide.
 *
 * @author Branimir Pavlovic
 */
public class GuideResizeAnimation extends Animation {
    private WeakReference<BaseGuideView> mGuideViewWeakReference;
    private float mFromWidthGuide;
    private float mToWidthGuide;
    private WeakReference<HashMap<Integer, GuideEventAnimInfo>> mEventsAnimationInfo;

    public GuideResizeAnimation(BaseGuideView guideView, float fromWidthGuide,
            float toWidthGuide, HashMap<Integer, GuideEventAnimInfo> eventsAnimationInfo) {
        this.mGuideViewWeakReference = new WeakReference<BaseGuideView>(guideView);
        this.mFromWidthGuide = fromWidthGuide;
        this.mToWidthGuide = toWidthGuide;
        this.mEventsAnimationInfo = new WeakReference<HashMap<Integer, GuideEventAnimInfo>>(eventsAnimationInfo);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        /**
         * Calculate new width of guide
         */
        int width = calculateCurrentValue(mFromWidthGuide, mToWidthGuide, interpolatedTime);
        final View view = mGuideViewWeakReference.get();
        ViewGroup.LayoutParams p = view.getLayoutParams();
        p.width = width;
        /**
         * Calculate new width of events and move it to new x coordinate
         */
        View eventView = null;
        final HashMap<Integer, GuideEventAnimInfo> events = mEventsAnimationInfo.get();
        for (GuideEventAnimInfo animEventInfo : events.values()) {
            eventView = animEventInfo.getEventView();
            BaseGuideView.LayoutParams params = (BaseGuideView.LayoutParams) eventView.getLayoutParams();
            params.width = calculateCurrentValue(animEventInfo.getStartWidth(), animEventInfo.getEndWidth(),
                    interpolatedTime);
            params.mLeftCoordinate = calculateCurrentValue(0, animEventInfo.getEndPosition(), interpolatedTime);
        }
        view.requestLayout();
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }

    private int calculateCurrentValue(float startValue, float endValue, float interpolatedTime) {
        return (int) ((endValue - startValue) * interpolatedTime + startValue);
    }
}
