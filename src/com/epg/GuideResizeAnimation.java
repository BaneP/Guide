package com.epg;

import android.graphics.Paint;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Animation used to expand guide from ON_NOW to FULL. It animates changing of guide width and also changes selected
 * event views width and position within guide.
 *
 * @author Branimir Pavlovic
 */
public class GuideResizeAnimation extends Animation {
    private WeakReference<BaseGuideView> mGuideViewWeakReference;
    private WeakReference<Paint> mTimeLinePaintText;
    private float mFromWidthGuide;
    private float mToWidthGuide;
    private WeakReference<SparseArray<GuideEventAnimInfo>> mEventsAnimationInfo;
    private WeakReference<View> mBackgroundViewWeakReference;
    private float mStartAlpha;
    private float mEndAlpha;
    private boolean runAlphaAnim = false;
    private int mTimeLineStartOffset, mTimeLineEndOffset;
    private boolean runTimeLineOffsetAnim = false;

    public GuideResizeAnimation(GuideView guideView, float fromWidthGuide,
            float toWidthGuide, SparseArray<GuideEventAnimInfo> eventsAnimationInfo, float startAlpha,
            float endAlpha) {
        this.mGuideViewWeakReference = new WeakReference<BaseGuideView>(guideView);
        this.mTimeLinePaintText = new WeakReference<Paint>(guideView.getTimeLinePaintText());
        this.mFromWidthGuide = fromWidthGuide;
        this.mToWidthGuide = toWidthGuide;
        this.mEventsAnimationInfo = new WeakReference<SparseArray<GuideEventAnimInfo>>(eventsAnimationInfo);
        if (guideView.getBackgroundView() != null) {
            mBackgroundViewWeakReference = new WeakReference<View>(guideView.getBackgroundView());
            mStartAlpha = startAlpha;
            mEndAlpha = endAlpha;
        }
        if (mBackgroundViewWeakReference != null && mBackgroundViewWeakReference.get() != null) {
            runAlphaAnim = true;
        }
        if (guideView.getTimeLineSpecificOffset() != GuideAdapterView.INVALID_POSITION) {
            runTimeLineOffsetAnim = true;
            mTimeLineStartOffset = guideView.getTimeLineSpecificOffset();
            Calendar calendar = Calendar.getInstance();
            mTimeLineEndOffset = GuideView.calculateDiffInMinutes(calendar, guideView.getAdapter().getStartTime()) *
                    guideView.getOneMinuteWidth() - guideView.mCurrentOffsetX;
        }
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        /**
         * Calculate new width of guide
         */
        int width = calculateCurrentValue(mFromWidthGuide, mToWidthGuide, interpolatedTime);
        final BaseGuideView view = mGuideViewWeakReference.get();
        ViewGroup.LayoutParams p = view.getLayoutParams();
        p.width = width;
        /**
         * Calculate new width of events and move it to new x coordinate
         */
        View eventView = null;
        final SparseArray<GuideEventAnimInfo> events = mEventsAnimationInfo.get();
        GuideEventAnimInfo animEventInfo;
        for (int i = 0; i < events.size(); i++) {
            animEventInfo = events.valueAt(i);
            eventView = animEventInfo.getEventView();
            BaseGuideView.LayoutParams params = (BaseGuideView.LayoutParams) eventView.getLayoutParams();
            params.width = calculateCurrentValue(animEventInfo.getStartWidth(), animEventInfo.getEndWidth(),
                    interpolatedTime);
            params.mLeftCoordinate = calculateCurrentValue(0, animEventInfo.getEndPosition(), interpolatedTime);
        }
        mTimeLinePaintText.get().setAlpha(calculateCurrentValue(0, 255, interpolatedTime));
        if (runAlphaAnim) {
            mBackgroundViewWeakReference.get()
                    .setAlpha(calculateCurrentValue(mStartAlpha, mEndAlpha, interpolatedTime));
        }
        if (runTimeLineOffsetAnim) {
            int newOffset = calculateCurrentValue(mTimeLineStartOffset, mTimeLineEndOffset, interpolatedTime);
            view.setTimeLineSpecificOffset(
                    newOffset == mTimeLineEndOffset ? GuideView.INVALID_POSITION : newOffset);
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
