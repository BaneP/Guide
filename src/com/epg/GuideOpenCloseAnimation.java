package com.epg;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

import java.lang.ref.WeakReference;

/**
 * @author Branimir Pavlovic
 */
public class GuideOpenCloseAnimation extends TranslateAnimation {
    private WeakReference<BaseGuideView> mBaseGuideViewWeakReference;
    private WeakReference<View> mBackgroundViewWeakReference;
    private boolean runAlphaAnim = false;
    private float mStartAlpha;
    private float mEndAlpha;

    public GuideOpenCloseAnimation(BaseGuideView baseGuideView, float startAlpha, float endAlpha, boolean open) {
        super(Animation.ABSOLUTE, open ? baseGuideView.getMeasuredWidth() : 0, Animation.ABSOLUTE,
                open ? 0 : baseGuideView.getMeasuredWidth(),
                Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0);
        mBaseGuideViewWeakReference = new WeakReference<BaseGuideView>(baseGuideView);
        if (baseGuideView.getParent() != null && baseGuideView.getParent() instanceof ViewGroup) {
            mBackgroundViewWeakReference = new WeakReference<View>(baseGuideView.getBackgroundView());
            mStartAlpha = startAlpha;
            mEndAlpha = endAlpha;
        }
        if (mBackgroundViewWeakReference != null && mBackgroundViewWeakReference.get() != null) {
            runAlphaAnim = true;
        }
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        if (runAlphaAnim) {
            float newAlpha = mStartAlpha + (mEndAlpha - mStartAlpha) * interpolatedTime;
            mBackgroundViewWeakReference.get().setAlpha(newAlpha);
        }
        super.applyTransformation(interpolatedTime, t);
    }
}
