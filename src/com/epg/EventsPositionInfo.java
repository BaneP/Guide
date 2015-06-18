package com.epg;

/**
 * Contains information about currently first visible and last visible event index.
 *
 * @author Branimir Pavlovic
 */
public class EventsPositionInfo {
    private int mFirstVisibleEvent, mLastVisibleEvent;

    public EventsPositionInfo(int mFirstVisibleEvent, int mLastVisibleEvent) {
        this.mFirstVisibleEvent = mFirstVisibleEvent;
        this.mLastVisibleEvent = mLastVisibleEvent;
    }

    public int getFirstVisibleEvent() {
        return mFirstVisibleEvent;
    }

    public int getLastVisibleEvent() {
        return mLastVisibleEvent;
    }
}
