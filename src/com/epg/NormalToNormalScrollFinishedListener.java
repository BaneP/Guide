package com.epg;

import java.lang.ref.WeakReference;

/**
 * Class that wait end of normal scroll and switches to fast scroll
 */
class NormalToNormalScrollFinishedListener implements
        BaseGuideView.OnAnimationFinishedListener {
    private int mDesiredChannelPosition;
    private final int mScrollDuration;
    private WeakReference<BaseGuideView.SmoothScrollRunnable> mSmoothScrollRunnable;

    NormalToNormalScrollFinishedListener(BaseGuideView.SmoothScrollRunnable smoothScrollRunnable, int
            desiredChannelPosition,
            int duration) {
        this.mSmoothScrollRunnable = new WeakReference<BaseGuideView.SmoothScrollRunnable>(smoothScrollRunnable);
        this.mDesiredChannelPosition = desiredChannelPosition;
        this.mScrollDuration = duration;
    }

    @Override
    public boolean animationFinished() {
        mSmoothScrollRunnable.get().startVerticalScrollToPosition(
                mDesiredChannelPosition, mScrollDuration);
        return false;
    }
}
