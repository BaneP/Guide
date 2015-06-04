package com.epg;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Time line drawing type of guide view. Determines if time line drawable should be drawn over events, time line, or
 * over the full guide.
 *
 * @author Branimir Pavlovic
 */
public enum TimeLineDrawType implements Parcelable {
    DRAW_OVER_EVENTS(0), DRAW_OVER_TIME_LINE(1), DRAW_OVER_GUIDE(2);
    int value;

    TimeLineDrawType(int value) {
        this.value = value;
    }

    /** Get int value of the component */
    public int getValue() {
        return value;
    }

    public static TimeLineDrawType fromValue(int value) {
        switch (value) {
        case 0: {
            return DRAW_OVER_EVENTS;
        }
        case 1: {
            return DRAW_OVER_TIME_LINE;
        }
        case 2:{
            return DRAW_OVER_GUIDE;
        }
        default: {
            return DRAW_OVER_EVENTS;
        }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public static final Creator<TimeLineDrawType> CREATOR = new Creator<TimeLineDrawType>() {
        @Override
        public TimeLineDrawType createFromParcel(final Parcel source) {
            return TimeLineDrawType.values()[source.readInt()];
        }

        @Override
        public TimeLineDrawType[] newArray(final int size) {
            return new TimeLineDrawType[size];
        }
    };
}