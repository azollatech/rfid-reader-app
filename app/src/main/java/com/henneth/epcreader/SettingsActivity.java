package com.henneth.epcreader;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

//    private CheckBoxPreference pref_ckpt_name;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Log.d("Setting", "onCreate");

        String pref_ckpt_name_string = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_ckpt_name", "(empty)");
        Preference pref_ckpt_name = (Preference) findPreference("pref_ckpt_name");
        pref_ckpt_name.setSummary("Current name: " + pref_ckpt_name_string);

        String pref_server_string = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_server", "(empty)");
        Preference pref_server = (Preference) findPreference("pref_server");
        pref_server.setSummary("Current server: " + pref_server_string);

        String pref_port_string = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_port", "(empty)");
        Preference pref_port = (Preference) findPreference("pref_port");
        pref_port.setSummary("Current port: " + pref_port_string);

        setPreferenceChangeListener();
    }

    public boolean onPreferenceTreeClick (PreferenceScreen preferenceScreen,
                                          Preference preference) {
        Log.d("hi", "settings");
        return true;
    }
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("changed", "changed");
//        Preference pref = findPreference(key);

    }
    public void setPreferenceChangeListener(){
        final EditTextPreference ckpt_name_pref = (EditTextPreference) findPreference("pref_ckpt_name");
        final ListPreference server_pref = (ListPreference) findPreference("pref_server");
        final EditTextPreference port_pref = (EditTextPreference) findPreference("pref_port");
//        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("pref_ckpt_name", "Default Title");
//        pref.setTitle(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("pref_ckpt_name", "Default Title"));
        // Loads the title for the first time
        // Listens for change in value, and then changes the title if required.
        ckpt_name_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("newValue", newValue.toString());
                ckpt_name_pref.setSummary("Current name: " + newValue.toString());
                return true;
            }
        });
        server_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("newValue", newValue.toString());
                server_pref.setSummary("Current server: " + newValue.toString());
                return true;
            }
        });
        port_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("newValue", newValue.toString());
                port_pref.setSummary("Current port: " + newValue.toString());
                return true;
            }
        });
    }

}
