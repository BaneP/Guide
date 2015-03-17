package com.epg;

/**
 * Holds informations about row position
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
		return "GuideRowInfo [mTop=" + mTop + ", mBottom=" + mBottom
				+ ", mHeight=" + mHeight + ", mResizedPercent="
				+ mResizedPercent + ", mChannelIndex=" + mChannelIndex + "]";
	}

	public int getmTop() {
		return mTop;
	}

	public int getmBottom() {
		return mBottom;
	}

	public int getmHeight() {
		return mHeight;
	}

	public int getmResizedPercent() {
		return mResizedPercent;
	}

	public int getmChannelIndex() {
		return mChannelIndex;
	}

}
