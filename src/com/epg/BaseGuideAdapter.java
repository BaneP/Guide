package com.epg;

/**
 * @author Branimir Pavlovic
 */
public abstract class BaseGuideAdapter implements IGuideAdapter {
    private final GuideDataSetObservable mDataSetObservable = new GuideDataSetObservable();

    public void registerDataSetObserver(GuideDataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(GuideDataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * Notifies the attached observers that the underlying data has been changed
     * and any View reflecting the data set should refresh itself.
     */
    public void notifyChannelListChanged() {
        mDataSetObservable.notifyChangedServiceList();
    }

    /**
     * Notifies the attached observers that the events underlying data has been changed
     * and any View reflecting the data set should refresh itself.
     */
    public void notifyEventListChanged(final int channelIndex) {
        mDataSetObservable.notifyChangedEventList(channelIndex);
    }

    /**
     * Notifies the attached observers that start or end time has changed.
     */
    public void notifyStartOrEndTimeChanged() {
        mDataSetObservable.notifyChangedStartOrEndTime();
    }

    /**
     * Notifies the attached observers that the underlying data is no longer valid
     * or available. Once invoked this adapter is no longer valid and should
     * not report further data set changes.
     */
    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    @Override
    public boolean isEmpty(int channel) {
        return getEventsCount(channel) == 0;
    }

    @Override
    public boolean isEmpty() {
        return getChannelsCount() == 0;
    }
}
