package com.epg;

/**
 * Guide data set observer
 *
 * @author Branimir Pavlovic
 */
public abstract class GuideDataSetObserver {

    /**
     * This method is called when the entire data set has changed,
     * most likely through a call to {@link Cursor#requery()} on a {@link Cursor}.
     */
    public void onChangedServiceList() {
        // Do nothing
    }

    /**
     * This method is called when the entire event data set for one channel has changed.
     */
    public void onChangedEventList(int channelIndex) {
        // Do nothing
    }

    /**
     * This method is called when start or end time is changed
     */
    public void onChangedStartOrEndTime() {
        // Do nothing
    }

    /**
     * This method is called when the entire data becomes invalid,
     * most likely through a call to {@link Cursor#deactivate()} or {@link Cursor#close()} on a
     * {@link Cursor}.
     */
    public void onInvalidated() {
        // Do nothing
    }
}
