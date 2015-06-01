package com.epg;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
        mGuideView.setBackgroundView(findViewById(R.id.backgroundView));
        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGuideView.isShown()) {
                    mGuideView.setVisibility(View.INVISIBLE);
                } else {
                    mGuideView.setVisibility(View.VISIBLE);
                }
            }
        });
        btn = (Button) findViewById(R.id.button2);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mGuideView.getGuideMode() == GuideView.GUIDE_MODE_ON_NOW) {
                    mGuideView.changeGuideMode(GuideView.GUIDE_MODE_FULL);
                } else {
                    mGuideView.changeGuideMode(GuideView.GUIDE_MODE_ON_NOW);
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MEDIA_RECORD: {
            if (mGuideView.getGuideMode() == GuideView.GUIDE_MODE_ON_NOW) {
                ((Adapter) mGuideView.getAdapter()).runningEvent += 1;
            } else {
                ((Adapter) mGuideView.getAdapter()).mStartTime.add(Calendar.MINUTE, 30);
            }
            mGuideView.getAdapter().notifyDataSetChanged();
            return true;
        }
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: {
            mGuideView.setAdapter(new Adapter());
            return true;
        }
        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
            mGuideView.setDrawTimeLine(true);
            return true;
        }
        case KeyEvent.KEYCODE_MEDIA_REWIND: {
            mGuideView.setDrawTimeLine(false);
            return true;
        }
        }
        return super.onKeyDown(keyCode, event);
    }

    private class Adapter extends BaseGuideAdapter {
        Calendar mStartTime;
        int runningEvent = 3;

        Adapter() {
            mStartTime = Calendar.getInstance();
            mStartTime.set(Calendar.MILLISECOND, 0);
            mStartTime.set(Calendar.SECOND, 0);
            if (mStartTime.get(Calendar.MINUTE) < 30) {
                mStartTime.set(Calendar.MINUTE, 0);
            } else {
                mStartTime.set(Calendar.MINUTE, 30);
            }
        }

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
         * Returns start time of time line
         *
         * @return Start time of time line, it should have 0 MILLISECONDS, SECONDS and MINUTES
         */
        @Override
        public Calendar getStartTime() {
            return mStartTime;
        }

        /**
         * Returns end time of time line
         *
         * @return End time of time line, it should have 0 MILLISECONDS, SECONDS and MINUTES
         */
        @Override
        public Calendar getEndTime() {
            Calendar endTime = (Calendar) mStartTime.clone();
            endTime.add(Calendar.DATE, 2);
            return endTime;
        }

        @Override
        public int getNowEventIndex(int channel) {
            return runningEvent;
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
                convertView.setBackgroundColor(0xFF999999);
            }
            ((TextView) convertView).setText("CHANNEL " + channel);
            return convertView;
        }
    }
}
