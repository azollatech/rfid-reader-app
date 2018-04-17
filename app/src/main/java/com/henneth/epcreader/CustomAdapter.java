package com.henneth.epcreader;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by hennethcheng on 17/6/16.
 */
public class CustomAdapter extends ArrayAdapter<String> {
    protected static final int TRANSPARENT = Color.TRANSPARENT;
    protected final int DONE_COLOR = Color.rgb(0,150,100);

    private ArrayList<String> items;
    private LayoutInflater mInflater;
    private int viewResourceId;
    private Activity context;
    ArrayList<String> doneList = new ArrayList<String>();

    public CustomAdapter(Activity activity, int resourceId, ArrayList<String> list) {
        super(activity, resourceId, list);

        // Sets the layout inflater
        mInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Set a copy of the layout to inflate
        viewResourceId = resourceId;

        // Set a copy of the list
        items = list;

        // Set a copy of the activity
        context = activity;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        int size = items.size();
        int reversePosition = size - position - 1;

        LinearLayout ll = (LinearLayout) convertView;

        if (convertView == null) {
            ll = (LinearLayout) mInflater.inflate(viewResourceId, null);
        }

        TextView tv1 = (TextView)ll.findViewById(R.id.tv_epc);
        TextView tv2 = (TextView)ll.findViewById(R.id.tv_time);
        String fullString = items.get(position);
        String chipCode = fullString.split("\n")[0];
        String time = fullString.split("\n")[1];

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showFullChipCode = prefs.getBoolean("pref_showFullChipCode", false);
        if (!showFullChipCode){
            int startIndex = chipCode.length() - 4;
            chipCode = chipCode.substring(startIndex);
        }

        tv1.setText(chipCode);
        tv2.setText(time);

        if (doneList.contains(String.valueOf(reversePosition))) {
            tv1.setTextColor(DONE_COLOR);
            tv2.setTextColor(DONE_COLOR);
        } else {
            tv1.setTextColor(Color.BLACK);
            tv2.setTextColor(Color.BLACK);
        }

        return ll;
    }

    public void setDoneList(String position) {
        doneList.add(position);
    }

    public void setWholeDoneList(ArrayList<String> list) {
        for (String s : list) {
            doneList.add(s);
        }
    }

    public void resetDoneList() {
        doneList = new ArrayList<>();
    }
}
