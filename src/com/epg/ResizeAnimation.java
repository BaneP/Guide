package com.epg;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by bane on 2/03/15.
 */
class ResizeAnimation extends Animation {
    private final int mStartHeight;
    private final int mFinalHeight;
    private View mView;
    private BaseGuideView mParent;

    ResizeAnimation(BaseGuideView parent, View view, int finalHeight) {
        this.mFinalHeight = finalHeight;
        this.mStartHeight = view.getHeight();
        this.mView = view;
        this.mParent = parent;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int newHeight = (int) (mStartHeight + (mFinalHeight - mStartHeight) * interpolatedTime);
        Log.d("ResizeAnimation" , "newHeight="+newHeight);
        mView.getLayoutParams().height = newHeight;
        mParent.update();
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
