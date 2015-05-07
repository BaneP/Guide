package com.epg;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;

/**
 * Created by bane on 4/02/15.
 */
public interface IGuideAdapter {
    /**
     * Register an observer that is called when changes happen to the data used
     * by this adapter.
     *
     * @param observer the object that gets notified when the data set changes.
     */
    void registerDataSetObserver(DataSetObserver observer);

    /**
     * Unregister an observer that has previously been registered with this
     * adapter via {@link #registerDataSetObserver}.
     *
     * @param observer the object to unregister.
     */
    void unregisterDataSetObserver(DataSetObserver observer);

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    int getChannelsCount();

    /**
     * How many event items are in the channel data set.
     *
     * @param channel index for events
     * @return Count of events.
     */
    int getEventsCount(int channel);

    /**
     * Return width for desired channel event. Width should already be calculated based on minute pixel size (getOneMinuteWidth())
     *
     * @param channel index
     * @param event   index
     * @return Calculated event width in pixels
     */
    int getEventWidth(int channel, int event);

    /**
     * Returns start time of time line
     *
     * @return Start time of time line, its MILLISECONDS, SECONDS and MINUTES will be ignored
     */
    Calendar getStartTime();

    /**
     * Returns end time of time line
     *
     * @return End time of time line, its MILLISECONDS, SECONDS and MINUTES will be ignored
     */
    Calendar getEndTime();

    /**
     * Now event index of desired channel.
     *
     * @param channel index of desired channel
     * @return Index of active event for desired channel.
     */
    int getNowEventIndex(int channel);

    /**
     * If event with defined channel index and event index contains regular data.
     *
     * @param channel Index of channel.
     * @param event   index of desired event.
     * @return True if event is regular data object, False if it is dummy space.
     */
    boolean hasRegularData(int channel, int event);

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param channel Position of the item whose data we want within the adapter's
     *                data set.
     * @return The data at the specified position.
     */
    Object getItem(int channel);

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param channel Position of the item whose data we want within the adapter's
     *                data set.
     * @param event   index for desired channel
     * @return The data at the specified position.
     */
    Object getItem(int channel, int event);

    /**
     * Get a View that displays the data at the specified position in the data
     * set. You can either create a View manually or inflate it from an XML
     * layout file. When the View is inflated, the parent View will apply layout
     * parameters using {@link IGuideAdapter#getEventWidth(int, int)} unless you
     * use
     * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param channel     The position of the item within the adapter's data set of the
     *                    item whose view we want.
     * @param event       The position of event for desired channel.
     * @param convertView The old view to reuse, if possible. Note: You should check
     *                    that this view is non-null and of an appropriate type before
     *                    using. If it is not possible to convert this view to display
     *                    the correct data, this method can create a new view.
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    View getEventView(int channel, int event, View convertView, ViewGroup parent);

    /**
     * Get a View that displays the channel data at the specified position in the data
     * set. You can either create a View manually or inflate it from an XML
     * layout file. When the View is inflated.
     *
     * @param channel     The position of the item within the adapter's data set of the
     *                    item whose view we want.
     * @param convertView The old view to reuse, if possible. Note: You should check
     *                    that this view is non-null and of an appropriate type before
     *                    using. If it is not possible to convert this view to display
     *                    the correct data, this method can create a new view.
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    View getChannelIndicatorView(int channel, View convertView, ViewGroup parent);

    /**
     * @return true if this adapter doesn't contain any data. This is used to
     * determine whether the empty view should be displayed. A typical
     * implementation will return getCount() == 0 but since getCount()
     * includes the headers and footers, specialized adapters might want
     * a different behavior.
     */
    boolean isEmpty();

    /**
     * @return true if this adapter doesn't contain any data. This is used to
     * determine whether the empty view should be displayed. A typical
     * implementation will return getCount() == 0 but since getCount()
     * includes the headers and footers, specialized adapters might want
     * a different behavior.
     */
    boolean isEmpty(int channel);
}
