package com.epg;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Selection type of guide view. Determines if selection will be on fixed part of guide or it will jump to next event
 * without scroll
 *
 * @author Branimir Pavlovic
 */
public enum SelectionType implements Parcelable {
    FIXED_ON_SCREEN(0), NOT_FIXED_ON_SCREEN(1);
    int value;

    SelectionType(int value) {
        this.value = value;
    }

    /** Get int value of the component */
    public int getValue() {
        return value;
    }

    public static SelectionType fromValue(int value) {
        switch (value) {
        case 0: {
            return FIXED_ON_SCREEN;
        }
        case 1: {
            return NOT_FIXED_ON_SCREEN;
        }
        default: {
            return FIXED_ON_SCREEN;
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

    public static final Creator<SelectionType> CREATOR = new Creator<SelectionType>() {
        @Override
        public SelectionType createFromParcel(final Parcel source) {
            return SelectionType.values()[source.readInt()];
        }

        @Override
        public SelectionType[] newArray(final int size) {
            return new SelectionType[size];
        }
    };
}