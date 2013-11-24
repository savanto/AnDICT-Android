package com.savanto.andict;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class DictSettingsActivity extends PreferenceActivity
{
	private EditTextPreference server;
	private PortPreference port;
	private ListPreference database;

	private SharedPreferences prefs;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Add the available settings controls
		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		// Get references to preferences
		server = (EditTextPreference) findPreference(getString(R.string.pref_server_key));
		port = (PortPreference) findPreference(getString(R.string.pref_port_key));
		database = (ListPreference) findPreference(getString(R.string.pref_database_key));

		// Get the SharedPreferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		// Set initial dynamic summary values
		server.setSummary(prefs.getString(getString(R.string.pref_server_key), getString(R.string.pref_server_default)));
		port.setSummary(Integer.toString(prefs.getInt(getString(R.string.pref_port_key),
				getResources().getInteger(R.integer.pref_port_default))));
		database.setSummary(prefs.getString(getString(R.string.pref_database_key), getString(R.string.pref_database_default)));

		// Register listener for SharedPreferences changes in order to update dynamic summaries
		prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		// Unregister SharedPreferences change listener
		prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	}

	private OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
	{
		@SuppressWarnings("deprecation")
		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
		{
			if (key == getString(R.string.pref_port_key))
				findPreference(key).setSummary(Integer.toString(prefs.getInt(key, -1)));
			else
				findPreference(key).setSummary(prefs.getString(key,  ""));
		}
	};
}
