package com.epg;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Used to resize new selected view and expanded view in SMOOTH_SCROLL_END animation
 *
 * @author Branimir Pavlovic
 */
class ResizeAnimation extends Animation {
    private final int mNormalHeight;
    private final int mExpandedHeight;
    private View mSelectedView;
    private View mExpandedView;
    private BaseGuideView mParent;

    ResizeAnimation(BaseGuideView parent, View selectedView, View expandedView,
            int normalHeight, int expandedHeight) {
        this.mExpandedHeight = expandedHeight;
        this.mNormalHeight = normalHeight;
        this.mSelectedView = selectedView;
        this.mExpandedView = expandedView;
        this.mParent = parent;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int newHeight = (int) (mNormalHeight + (mExpandedHeight - mNormalHeight)
                * interpolatedTime);
        mSelectedView.getLayoutParams().height = newHeight;
        if (mExpandedView != null) {
            mExpandedView.getLayoutParams().height = mExpandedHeight
                    + mNormalHeight - newHeight;
        }
        mParent.update();
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
