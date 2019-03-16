package com.savanto.andict;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public final class DictSettingsActivity extends PreferenceActivity {
    static final String PREF_SERVER_KEY = "pref_server_key";
    static final String PREF_PORT_KEY = "pref_port_key";
    static final String PREF_DATABASE_KEY = "pref_database_key";
    static final String PREF_STRATEGY_ENABLE_KEY = "pref_strategy_enable_key";
    static final String PREF_STRATEGY_KEY = "pref_strategy_key";

    private Preference server;
    private Preference port;
    private Preference database;
    private Preference strategy;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add the available settings controls
        addPreferencesFromResource(R.xml.settings);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        // Get references to preferences
        server = findPreference(PREF_SERVER_KEY);
        port = findPreference(PREF_PORT_KEY);
        database = findPreference(PREF_DATABASE_KEY);
        strategy = findPreference(PREF_STRATEGY_KEY);

        // Get the SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set initial dynamic summary values
        server.setSummary(prefs.getString(PREF_SERVER_KEY, getString(R.string.pref_server_default)));
        port.setSummary(Integer.toString(prefs.getInt(PREF_PORT_KEY, PortPreference.DEFAULT_PORT)));
        database.setSummary(prefs.getString(PREF_DATABASE_KEY, getString(R.string.pref_database_default)));
        strategy.setSummary(prefs.getString(PREF_STRATEGY_KEY, ""));

        // Register listener for SharedPreferences changes in order to update dynamic summaries
        prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister SharedPreferences change listener
        prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (prefs, key) -> {
        switch (key) {
            case PREF_PORT_KEY:
                this.findPreference(key).setSummary(Integer.toString(prefs.getInt(key, -1)));
                break;
            case PREF_STRATEGY_ENABLE_KEY:
                // NOP
                break;
            default:
                this.findPreference(key).setSummary(prefs.getString(key, ""));
        }
    };
}
