package com.epg;

/**
 * Helper class that contains information about first visible child view and its invisible part (offset from left
 * edge of the screen).
 */
class FirstPositionInfo {
    private int mFirstChildIndex;
    private int mFirstChildInvisiblePart;

    public FirstPositionInfo(int firstChildIndex, int firstChildInvisiblePart) {
        this.mFirstChildIndex = firstChildIndex;
        this.mFirstChildInvisiblePart = firstChildInvisiblePart;
    }

    @Override
    public String toString() {
        return "FirstPositionInfo{" +
                "mFirstChildIndex=" + mFirstChildIndex +
                ", mFirstChildInvisiblePart=" + mFirstChildInvisiblePart +
                '}';
    }

    public int getFirstChildInvisiblePart() {
        return mFirstChildInvisiblePart;
    }

    public int getFirstChildIndex() {
        return mFirstChildIndex;
    }
}
