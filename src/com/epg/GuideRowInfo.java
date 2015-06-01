package com.epg;

/**
 * Holds information about rows. It contains top, bottom, height, resized percent of row and represented channel index.
 *
 * @author Branimir Pavlovic
 */
class GuideRowInfo {

    private int mTop;
    private int mBottom;
    private int mHeight;
    private int mResizedPercent;
    private int mChannelIndex;

    GuideRowInfo(int channelIndex, int top, int height, int resizedPercent) {
        mChannelIndex = channelIndex;
        mTop = top;
        mHeight = height;
        mBottom = top + height;
        mResizedPercent = resizedPercent;
    }

    @Override
    public String toString() {
        return "GuideRowInfo [mTop=" + mTop + ", mChannelIndex=" + mChannelIndex + "]";
    }

    public int getTop() {
        return mTop;
    }

    public int getBottom() {
        return mBottom;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getResizedPercent() {
        return mResizedPercent;
    }

    public int getChannelIndex() {
        return mChannelIndex;
    }
}
