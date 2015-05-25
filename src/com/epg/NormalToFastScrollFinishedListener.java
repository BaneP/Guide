package com.epg;

import java.lang.ref.WeakReference;

/**
 * Class that wait end of normal scroll and switches to fast scroll
 */
class NormalToFastScrollFinishedListener implements
        BaseGuideView.OnAnimationFinishedListener {
    private int mDifference;
    private WeakReference<BaseGuideView> mBaseGuideView;

    NormalToFastScrollFinishedListener(BaseGuideView baseGuideView, int difference) {
        mBaseGuideView = new WeakReference<BaseGuideView>(baseGuideView);
        this.mDifference = difference;
    }

    @Override
    public boolean animationFinished() {
        final BaseGuideView baseGuideView = mBaseGuideView.get();
        baseGuideView.mScrollState = BaseGuideView.SCROLL_STATE_FAST_SCROLL;
        baseGuideView.fireOnLongPressScrollStateChanged();
        baseGuideView.mSmoothScrollRunnable.resumeVerticalScroll(mDifference);
        return false;
    }
}
