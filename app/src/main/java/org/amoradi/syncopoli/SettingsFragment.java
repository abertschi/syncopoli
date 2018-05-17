package org.amoradi.syncopoli;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public final static String KEY_SERVER_ADDRESS = "pref_key_server_address";
    public final static String KEY_PROTOCOL = "pref_key_protocol";
    public final static String KEY_RSYNC_USERNAME = "pref_key_username";
    public final static String KEY_RSYNC_OPTIONS = "pref_key_options";
    public final static String KEY_PRIVATE_KEY = "pref_key_private_key";
    public final static String KEY_PORT = "pref_key_port";
    public final static String KEY_FREQUENCY = "pref_key_frequency";
    public final static String KEY_RSYNC_PASSWORD = "pref_key_rsync_password";
    public final static String KEY_SSH_PASSWORD = "pref_key_ssh_password";
    public final static String KEY_WIFI_ONLY = "pref_key_wifi_only";
    public final static String KEY_WIFI_NAME = "pref_key_wifi_name";

	private final static int DEFAULT_RSYNC_PORT = 873;
	private final static int DEFAULT_SSH_PORT = 22;
	

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_WIFI_ONLY) || key.equals(KEY_RSYNC_PASSWORD)) {
            return;
        }

		/*
		 * if user is changing the protocol and leaves the port as default, then we change
		 * the port to match the default for the protocol selected. Else, if the user has
		 * changed the port to a custom one, then leave it alone since User Knows Best (TM)
		 */
		
		if (key.equals(KEY_PROTOCOL)) {
			SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
			int port = Integer.parseInt(prefs.getString(KEY_PORT, "22"));

			int newport = -1;

			if (prefs.getString(KEY_PROTOCOL, "SSH").equals("SSH") && port == DEFAULT_RSYNC_PORT) {
				newport = DEFAULT_SSH_PORT;
			} else if (prefs.getString(KEY_PROTOCOL, "SSH").equals("Rsync") && port == DEFAULT_SSH_PORT) {
				newport = DEFAULT_RSYNC_PORT;
			}

			if (newport) {
				getPreferenceScreen()
					.getSharedPreferences()
					.edit()
					.putString(KEY_PORT, newport)
					.apply();
			}
		}

		Preference pref = findPreference(key);
        String summary = sharedPreferences.getString(key, "Not set");
        pref.setSummary(summary);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        setHasOptionsMenu(true);

        initializeSummaries();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.menu_settings).setVisible(false);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_run).setVisible(false);
    }

    private void initializeSummaries() {
        String[] keys = {KEY_SERVER_ADDRESS, KEY_PROTOCOL, KEY_RSYNC_USERNAME,
		KEY_RSYNC_OPTIONS, KEY_PRIVATE_KEY, KEY_PORT, KEY_FREQUENCY};
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

        for (String key : keys) {
            onSharedPreferenceChanged(sp, key);
        }
    }
}
