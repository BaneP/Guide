package com.epg;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Random;

public class MyActivity extends Activity {
    private GuideView mGuideView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mGuideView = (GuideView) findViewById(R.id.guideView);
        mGuideView.setAdapter(new Adapter());
    }

    private class Adapter extends BaseGuideAdapter {

        int[] widths = { 150, 550, 240, 370, 425, 150, 550, 240, 370, 425, 150,
                550, 240, 370, 425 };
        int[] widths1 = { 550, 240, 370, 425, 150, 150, 550, 240, 425, 150, 370,
                550, 240, 425, 370 };
        int[] widths2 = { 370, 150, 550, 240, 150, 550, 425, 240, 425, 150,
                240, 550, 370, 370, 425 };

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

        @Override
        public int getNowEventIndex(int channel) {
            return 0;
        }

        @Override
        public boolean hasRegularData(int channel, int event) {
            return false;
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
