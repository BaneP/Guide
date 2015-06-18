package com.epg;

import android.database.Observable;

/**
 * @author Branimir Pavlovic
 */
public class GuideDataSetObservable extends Observable<GuideDataSetObserver> {
    /**
     * Invokes {@link GuideDataSetObserver#onChangedServiceList} on each observer.
     * Called when the contents of the service list data set have changed. The recipient
     * will obtain the new contents the next time it queries the data set.
     */
    public void notifyChangedServiceList() {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChangedServiceList();
            }
        }
    }

    /**
     * Invokes {@link GuideDataSetObserver#onChangedEventList} on each observer.
     * Called when the contents of the event data set for channel index have changed. The recipient
     * will obtain the new contents the next time it queries the data set.
     */
    public void notifyChangedEventList(final int channelIndex) {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChangedEventList(channelIndex);
            }
        }
    }

    /**
     * Invokes {@link GuideDataSetObserver#onChangedStartOrEndTime} on each observer.
     * Called when the start or end time has been changed.
     */
    public void notifyChangedStartOrEndTime() {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChangedStartOrEndTime();
            }
        }
    }

    /**
     * Invokes {@link GuideDataSetObserver#onInvalidated} on each observer.
     * Called when the data set is no longer valid and cannot be queried again,
     * such as when the data set has been closed.
     */
    public void notifyInvalidated() {
        synchronized (mObservers) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onInvalidated();
            }
        }
    }
}
