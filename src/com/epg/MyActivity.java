package com.epg;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Random;

public class MyActivity extends Activity {
    private GuideView mGuideView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mGuideView = (GuideView) findViewById(R.id.guideView);
        mGuideView.setAdapter(new Adapter());
        mGuideView.setOnItemClickListener(new GuideAdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(GuideAdapterView<?> parent, View view, int channelPosition,
                    int eventPosition) {
                Log.d("MyActivity",
                        "GUIDE VIEW ON CLICK, channelPosition=" + channelPosition + ", eventPosition=" + eventPosition);
            }
        });
        mGuideView.setOnItemSelectedListener(new GuideAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(GuideAdapterView<?> parent, View view, int channelPosition,
                    int eventPosition) {
                Log.d("MyActivity",
                        "GUIDE VIEW ON ITEM SELECTED, channelPosition=" + channelPosition + ", eventPosition=" +
                                eventPosition);
            }

            @Override
            public void onNothingSelected(GuideAdapterView<?> parent) {
                Log.d("MyActivity",
                        "GUIDE VIEW ON NOTHING SELECTED");
            }
        });
        mGuideView.setOnLongPressScrollListener(new GuideAdapterView.OnLongPressScrollListener() {
            @Override
            public void onLongPressScrollStarted() {
                Log.d("MyActivity",
                        "GUIDE VIEW ON LONG PRESS SCROLL STARTED");
            }

            @Override
            public void onLongPressScrollStopped() {
                Log.d("MyActivity",
                        "GUIDE VIEW ON LONG PRESS SCROLL STOPPED");
            }
        });
    }

    private class Adapter extends BaseGuideAdapter {

        int[] widths = { 15, 55, 24, 37, 4, 15, 55, 24, 37, 4, 15,
                55, 24, 37, 4 };
        int[] widths1 = { 55, 24, 37, 4, 15, 15, 55, 24, 4, 15, 37,
                55, 24, 4, 37 };
        int[] widths2 = { 37, 15, 55, 24, 15, 55, 4, 24, 4, 15,
                24, 55, 37, 37, 4 };

        @Override
        public int getChannelsCount() {
            return 45;
        }

        @Override
        public int getEventsCount(int channel) {
            return 15;
        }

        @Override
        public int getEventWidth(int channel, int event) {
            Random rand = new Random();
            int i = channel % 3;
            switch (i) {
            case 0: {
                return widths[event];
            }
            case 1: {
                return widths1[event];
            }
            case 2: {
                return widths2[event];
            }
            }
            return widths[event];
        }

        /**
         * Return width for one pixel
         *
         * @return Calculated one minute pixel size
         */
        @Override
        public int getOneMinuteWidth() {
            return 15;
        }

        /**
         * Returns start time of time line
         *
         * @return Start time of time line, it should have 0 MILLISECONDS, SECONDS and MINUTES
         */
        @Override
        public Calendar getStartTime() {
            Calendar startTime = Calendar.getInstance();
            startTime.set(Calendar.MILLISECOND,0);
            startTime.set(Calendar.SECOND,0);
            startTime.set(Calendar.MINUTE,0);
            return startTime;
        }

        /**
         * Returns end time of time line
         *
         * @return End time of time line, it should have 0 MILLISECONDS, SECONDS and MINUTES
         */
        @Override
        public Calendar getEndTime() {
            Calendar endTime = Calendar.getInstance();
            endTime.set(Calendar.MILLISECOND,0);
            endTime.set(Calendar.SECOND,0);
            endTime.set(Calendar.MINUTE,0);
            endTime.add(Calendar.DATE, 10);
            return endTime;
        }

        @Override
        public int getNowEventIndex(int channel) {
            return 0;
        }

        @Override
        public boolean hasRegularData(int channel, int event) {
            return true;
        }

        @Override
        public Object getItem(int channel) {
            return null;
        }

        @Override
        public Object getItem(int channel, int event) {
            return null;
        }

        @Override
        public View getEventView(int channel, int event, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(parent.getContext());
                Random rand = new Random();
                int r = rand.nextInt(255);
                int g = rand.nextInt(255);
                int b = rand.nextInt(255);
                convertView.setBackgroundColor(Color.argb(128, r, g, b));
            }
            ((TextView) convertView).setText("channel " + channel + ", event "
                    + event);

            return convertView;
        }

        @Override
        public View getChannelIndicatorView(int channel, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(parent.getContext());
                convertView.setBackgroundColor(0x55999999);
            }
            ((TextView) convertView).setText("CHANNEL " + channel);
            return convertView;
        }
    }
}
