package com.epg;

import java.lang.ref.WeakReference;

/**
 * Class that wait end of normal scroll and switches to fast scroll
 *
 * @author Branimir Pavlovic
 */
class FastToFastScrollEndFinishedListener implements
        BaseGuideView.OnAnimationFinishedListener {

    private WeakReference<BaseGuideView> mBaseGuideView;

    FastToFastScrollEndFinishedListener(BaseGuideView baseGuideView) {
        mBaseGuideView = new WeakReference<BaseGuideView>(baseGuideView);
    }

    @Override
    public boolean animationFinished() {
        return mBaseGuideView.get().fastScrollEnd();
    }
}
