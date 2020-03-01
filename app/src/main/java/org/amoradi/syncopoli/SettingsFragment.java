package org.amoradi.syncopoli;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;


public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Syncopoli";

    public final static String KEY_SERVER_ADDRESS = "pref_key_server_address"; // String
    public final static String KEY_PROTOCOL = "pref_key_protocol"; // String
    public final static String KEY_RSYNC_USERNAME = "pref_key_username"; // String
    public final static String KEY_RSYNC_OPTIONS = "pref_key_options"; // String
    public final static String KEY_PRIVATE_KEY = "pref_key_private_key"; // String
    public final static String KEY_PORT = "pref_key_port"; // int
    public final static String KEY_FREQUENCY = "pref_key_frequency"; // int
    public final static String KEY_RSYNC_PASSWORD = "pref_key_rsync_password"; // String
    public final static String KEY_SSH_PASSWORD = "pref_key_ssh_password"; // String
    public final static String KEY_WIFI_ONLY = "pref_key_wifi_only"; // boolean
    public final static String KEY_WIFI_NAME = "pref_key_wifi_name"; // String
    public final static String KEY_VERIFY_HOST = "pref_key_verify_host"; // String
    public final static String KEY_CLEAR_HOSTS = "pref_key_clear_hosts"; // String
    public final static String KEY_AS_ROOT = "pref_key_as_root"; // boolean
    public final static String KEY_VERSION_CODE = "pref_key_version_code";

	private final static int DEFAULT_RSYNC_PORT = 873;
	private final static int DEFAULT_SSH_PORT = 22;

    public final static String[] KEYS = {
        KEY_SERVER_ADDRESS,
        KEY_PROTOCOL,
        KEY_RSYNC_USERNAME,
        KEY_RSYNC_PASSWORD,
        KEY_RSYNC_OPTIONS,
        KEY_PRIVATE_KEY,
        KEY_PORT,
        KEY_FREQUENCY,
        KEY_SSH_PASSWORD,
        KEY_WIFI_ONLY,
        KEY_WIFI_NAME,
        KEY_AS_ROOT
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_WIFI_ONLY) || key.equals(KEY_AS_ROOT)) {
            return;
        }

		/*
		 * if user is changing the protocol and leaves the port as default, then we change
		 * the port to match the default for the protocol selected. Else, if the user has
		 * changed the port to a custom one, then leave it alone since User Knows Best (TM)
		 */

		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		if (key.equals(KEY_PROTOCOL)) {
			int port = Integer.parseInt(prefs.getString(KEY_PORT, "22"));

			int newport = -1;

			if (prefs.getString(KEY_PROTOCOL, "SSH").equals("SSH") && port == DEFAULT_RSYNC_PORT) {
				newport = DEFAULT_SSH_PORT;
			} else if (prefs.getString(KEY_PROTOCOL, "SSH").equals("Rsync") && port == DEFAULT_SSH_PORT) {
				newport = DEFAULT_RSYNC_PORT;
			}

			if (newport > 0) {
                prefs.edit().putString(KEY_PORT, Integer.toString(newport)).apply();
			}
		}

        if (key.equals(KEY_FREQUENCY)) {
            if (prefs.getString(KEY_FREQUENCY, "8").equals("")) {
                prefs.edit().putString(KEY_FREQUENCY, Integer.toString(0)).apply();
            }

            ((BackupActivity)getActivity()).setupSyncAccount();
        }

        /*
		 * hide passwords from preference screen
		 */
		if (key.equals(KEY_SSH_PASSWORD) || key.equals(KEY_RSYNC_PASSWORD)) {
            Preference p = findPreference(key);
			if (prefs.getString(key, "").length() > 0) {
				p.setSummary("******");
			} else {
                p.setSummary("");
            }
		} else {
            Preference pref = findPreference(key);
            String summary = sharedPreferences.getString(key, "Not set");
            pref.setSummary(summary);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        setHasOptionsMenu(true);

        initializeSummaries();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        Preference verifyButton = findPreference(KEY_VERIFY_HOST);
        verifyButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new GetHostFingerprintTask(getActivity().getWindow().getContext()).execute();
                return true;
            }
        });

        Preference clearButton = findPreference(KEY_CLEAR_HOSTS);
        clearButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new ClearHostsTask(getActivity().getWindow().getContext()).execute();
                return true;
            }
        });
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

	private class AcceptHostFingerprintTask extends AsyncTask<Void, Void, Boolean> {
		private Context mContext;
		private SSHManager sshman;
		private String fingerprint;

		AcceptHostFingerprintTask(Context ctx, String fp) {
		    mContext = ctx;
		    sshman = new SSHManager(mContext);
            fingerprint = fp;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
		    return sshman.acceptHostKeyFingerprint(fingerprint);
        }

        @Override
        protected void onPostExecute(Boolean result) {
		    if (result) {
		        Log.i(TAG, "Remote host fingerprint accepted");
            } else {
                Log.e(TAG, "Could not accept remote host fingerprint");
                Toast.makeText(mContext, "Could not accept remote host fingerprint, please see logcat for details", Toast.LENGTH_LONG).show();
            }
        }
	}

	private class ClearHostsTask extends AsyncTask<Void, String, Boolean> {
        private Context mContext;
        private SSHManager sshman;

        ClearHostsTask(Context ctx) {
            mContext = ctx;
            sshman = new SSHManager(mContext);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return sshman.clearAcceptedHostKeyFingerprints();
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            if (result) {
                Toast.makeText(mContext, "Cleared all verified hosts", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Failed to clear hosts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class GetHostFingerprintTask extends AsyncTask<Void, String, String> {
        private Context mContext;
        private SSHManager sshman;

        GetHostFingerprintTask(Context ctx) {
            mContext = ctx;
            sshman = new SSHManager(mContext);
        }

        @Override
        protected String doInBackground(Void... params) {
            return sshman.getRemoteHostFingerprint();
        }

        @Override
        protected void onPostExecute(final String result) {
            if (result == null) {
                Toast.makeText(mContext, "Failed to verify host.", Toast.LENGTH_SHORT).show();
                return;
            }

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            new AcceptHostFingerprintTask(mContext, result).execute();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AppTheme);
            builder.setMessage("Does the following fingerprint match the host?\n" + result);
            builder.setPositiveButton("Yes", dialogClickListener);
            builder.setNegativeButton("No", dialogClickListener);
            builder.show();
        }
    }
}
