package com.epg;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

import java.lang.ref.WeakReference;

/**
 * Simple translate animation that is used to open/close guide view. It also contains logic to animate alpha of
 * background view behind guide.
 *
 * @author Branimir Pavlovic
 */
public class GuideOpenCloseAnimation extends TranslateAnimation {
    private WeakReference<View> mBackgroundViewWeakReference;
    private boolean runAlphaAnim = false;
    private float mStartAlpha;
    private float mEndAlpha;

    public GuideOpenCloseAnimation(GuideView guideView, float startX, float endX, float startAlpha, float endAlpha) {
        super(Animation.ABSOLUTE, startX, Animation.ABSOLUTE, endX, Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0);
        if (guideView.getBackgroundView() != null) {
            mBackgroundViewWeakReference = new WeakReference<View>(guideView.getBackgroundView());
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
