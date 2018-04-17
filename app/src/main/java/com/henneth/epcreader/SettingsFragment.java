package com.henneth.epcreader;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by hennethcheng on 29/8/2017.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
