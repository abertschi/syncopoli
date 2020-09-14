package org.amoradi.syncopoli;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.annotation.LayoutRes;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import org.json.*;

public class BackupActivity extends AppCompatActivity implements IBackupHandler {
    private static final String TAG = "Syncopoli";

    public static final String SYNC_AUTHORITY = "org.amoradi.syncopoli.provider";
    public static final String SYNC_ACCOUNT_NAME = "Syncopoli Sync Account";
    public static final String SYNC_ACCOUNT_TYPE = "org.amoradi.syncopoli";

    protected class Perm {
        public String value;
        public int code;

        public Perm(String v, int c) {
            value = v;
            code = c;
        }
    }

    Perm[] mPerms = {
        new Perm(android.Manifest.permission.READ_EXTERNAL_STORAGE, 1),
        new Perm(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, 2),
        new Perm(android.Manifest.permission.READ_SYNC_SETTINGS, 3),
        new Perm(android.Manifest.permission.WRITE_SYNC_SETTINGS, 4),
        new Perm(android.Manifest.permission.INTERNET, 5),
        new Perm(android.Manifest.permission.ACCESS_WIFI_STATE, 6),
        new Perm(android.Manifest.permission.ACCESS_COARSE_LOCATION, 7),
        new Perm(android.Manifest.permission.WAKE_LOCK, 8)
    };

    BackupHandler mBackupHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        setup(true);
    }

    protected void setup(boolean checkPerms) {
        if (checkPerms) {
            checkRuntimePerms();
        }

        if (isFirstRun()) {
            setupSyncAccount();

            if (copyExecutables() != 0) {
                Toast.makeText(getApplicationContext(), "Unable to copy ssh and/or rsync executables. Please submit a bug report.", Toast.LENGTH_LONG).show();
            }

            if (ensureSSHDir() != 0) {
                Toast.makeText(getApplicationContext(), "Unable to create .ssh directory. Please submit a bug report.", Toast.LENGTH_LONG).show();
            }
        }


        mBackupHandler = new BackupHandler(this);

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, false);
    }

    private boolean isFirstRun() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int savedVersionCode = prefs.getInt(SettingsFragment.KEY_VERSION_CODE, -1);

        boolean isFirst = BuildConfig.VERSION_CODE != savedVersionCode;

        if (isFirst) {
            prefs.edit().putInt(SettingsFragment.KEY_VERSION_CODE, BuildConfig.VERSION_CODE).apply();
        }

        return isFirst;
    }

    protected boolean checkRuntimePerms() {
        if (Build.VERSION.SDK_INT >= 23) {
            for (Perm p : mPerms) {
                if (checkSelfPermission(p.value) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{p.value}, p.code);
                    return false;
                }
            }
        }       

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setup(false);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        super.setContentView(layoutResId);

        Toolbar t = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(t);
    }

    public Account getOrCreateSyncAccount() {
        /* get */
        AccountManager accman = AccountManager.get(this);

        for (Account acc : accman.getAccountsByType(SYNC_ACCOUNT_TYPE)) {
            if (acc.name.equals(SYNC_ACCOUNT_NAME)) {
                return acc;
            }
        }

        /* if not found, create */
        Account acc = new Account(SYNC_ACCOUNT_NAME, SYNC_ACCOUNT_TYPE);

        if (accman.addAccountExplicitly(acc, null, null)) {
            ContentResolver.setIsSyncable(acc, SYNC_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(acc, SYNC_AUTHORITY, true);
        }

        return acc;
    }
        
    public void setupSyncAccount() {
        Account acc = getOrCreateSyncAccount();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long freq = Long.parseLong(prefs.getString(SettingsFragment.KEY_FREQUENCY, "8"));
        freq = freq * 3600; // hours to seconds

        // ContentResolver.addPeriodicSync enforces a min of 1 hour
        if (freq == 0) {
            ContentResolver.removePeriodicSync(acc, SYNC_AUTHORITY, new Bundle());
        } else {
            ContentResolver.addPeriodicSync(acc, SYNC_AUTHORITY, new Bundle(), freq);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.backup, menu);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_run).setVisible(true);

        return true;
    }

    public void syncBackups() {
        Snackbar.make(findViewById(R.id.backuplist_coordinator),
                      "Running all sync tasks",
                      Snackbar.LENGTH_SHORT).show();

        List<BackupItem> bs = mBackupHandler.getBackups();
        BackupItem[] backups = new BackupItem[bs.size()];
        bs.toArray(backups);


        Intent i = new Intent(this, BackupBackgroundService.class);
        i.putExtra("items", backups);
        i.putExtra("force", true);
        BackupBackgroundService.enqueueWork(this, i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_run) {
            syncBackups();
        } else if (id == R.id.menu_settings) {
            setCurrentFragment(new SettingsFragment(), true);
		} else if (id == R.id.menu_export) {
			if (exportSettings() == 0) {
			    File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "syncopoli_export.json");
                Toast.makeText(getApplicationContext(), "Exported " + f.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } else {
			    Toast.makeText(getApplicationContext(), "Export failed, see logcat for details", Toast.LENGTH_LONG).show();
            }
		} else if (id == R.id.menu_import) {
			importSettings();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

	public int exportSettings() {
		List<BackupItem> backups = mBackupHandler.getBackups();
		
		JSONObject exportObj = new JSONObject();

        try {
            exportObj.put("version", 2);
        } catch (JSONException e) {
            Log.e(TAG, "ERROR setting export version number: " + e.getMessage());
            return -1;
        }

		/*
		 * get global configs
		 */
		JSONObject globals = new JSONObject();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		try {
			for (String k : SettingsFragment.KEYS) {
                if (k.equals(SettingsFragment.KEY_WIFI_ONLY)) {
                    globals.put(k, prefs.getBoolean(k, false));
                } else {
                    globals.put(k, prefs.getString(k, ""));
                }
			}
		} catch (JSONException e) {
			Log.e(TAG, "ERROR exporting global configurations while creating json object: " + e.getMessage());
			return -1;
		}

		try {
			exportObj.put("globals", globals);
		} catch (JSONException e) {
			Log.e(TAG, "ERROR exporting globals while adding to exportObj: " + e.getMessage());
			return -1;
		}
		
		/*
		 * get profile configs
		 */
		JSONArray profiles = new JSONArray();
		try {
			for (BackupItem i : backups) {
				JSONObject p = new JSONObject();
				p.put("name", i.name);

				JSONArray s = new JSONArray();
				for (String source : i.sources) {
				    s.put(source);
				}
				p.put("sources", s);

				p.put("destination", i.destination);
				p.put("rsync_options", i.rsync_options);

				if (i.direction == BackupItem.Direction.INCOMING) {
					p.put("direction", "INCOMING");
				} else {
					p.put("direction", "OUTGOING");
				}

				profiles.put(p);
			}
		} catch (JSONException e) {
			Log.e(TAG, "ERROR exporting profiles while creating json object: " + e.getMessage());
			return -1;
		}

		try {
			exportObj.put("profiles", profiles);
		} catch (JSONException e) {
			Log.e(TAG, "ERROR exporting profiles while adding to exportObj: " + e.getMessage());
			return -1;
		}

		try {
			File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "syncopoli_export.json");
			FileOutputStream s = new FileOutputStream(f);
			s.write(exportObj.toString().getBytes());
			s.close();
		} catch (IOException e) {
			Log.e(TAG, "ERROR exporting profiles while writing: " + e.getMessage());
			return -1;
		}
		
		return 0;
	}

	public int importSettings() {
		String content;

		try {
			File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "syncopoli_export.json");
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

            char[] buffer = new char[1024];
            StringBuilder output = new StringBuilder();
            while (reader.read(buffer) > 0) {
                output.append(new String(buffer));
            }
            reader.close();

            content = output.toString();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "ERROR importing profiles while getting input stream: " + e.getMessage());
			return -1;
		} catch (IOException e) {
			Log.e(TAG, "ERROR importing profiles while reading: " + e.getMessage());
			return -1;
		}

        /* version 1 of export was an array of profiles
         * starting with version 2, export config became an object
         */

        int ret = 0;

        JSONObject exportedSettings = null;
        JSONArray exportedSettingsV1 = null;

        try {
            exportedSettings = new JSONObject(content);
        } catch (JSONException e) {
            try {
                exportedSettingsV1 = new JSONArray(content);
            } catch (JSONException e2) {
                Log.e(TAG, "ERROR importing profiles: could not parse export config: " +
                      e2.getMessage());
                return -1;
            }
        }

        if (exportedSettingsV1 != null) {
            ret = importSettingsV1(exportedSettingsV1);
        } else {
            int version = 0;
            try {
                version = exportedSettings.getInt("version");
            } catch (JSONException e) {
                Log.e(TAG, "ERROR importing profiles: could not read version number: " +
                      e.getMessage());
                return -1;
            }
            
            if (version == 2) {
                ret = importSettingsV2(exportedSettings);
            } else {
                Log.e(TAG, "ERROR importing profiles: unknown version number");
                return -1;
            }
        }

        if (ret == 0) {
            updateBackupList();
            Toast.makeText(getApplicationContext(), "Import successful", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Import failed, see logcat for details", Toast.LENGTH_LONG).show();
        }

        return ret;
    }

    public int importProfileList(JSONArray profiles) {
        try {
            /* profiles */
            for (int i = 0; i < profiles.length(); i++) {
                JSONObject jb = profiles.getJSONObject(i);
                BackupItem b = new BackupItem();

                b.name = jb.getString("name");

                JSONArray sources = jb.optJSONArray("sources");
                if (sources != null) {
                    b.sources = new String[sources.length()];
                    for (int k= 0; k < sources.length(); k++) {
                        b.sources[k] = sources.getString(k);
                    }
                } else {
                    // compatibility with v1 config where sources was source
                    b.sources = new String[1];
                    b.sources[0] = jb.getString("source");
                }

                b.destination = jb.getString("destination");
                b.rsync_options = jb.getString("rsync_options");

                if (jb.getString("direction").equals("INCOMING")) {
                    b.direction = BackupItem.Direction.INCOMING;
                } else {
                    b.direction = BackupItem.Direction.OUTGOING;
                }

                mBackupHandler.addBackup(b);
            }
        } catch (JSONException e) {
            Log.e(TAG, "ERROR import profiles: " + e.getMessage());
            return -1;
        }

        return 0;
    }

    public int importGlobalSettings(JSONObject globals) {
		try {
			/* globals */
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();

            for (String k : SettingsFragment.KEYS) {
                if (k.equals(SettingsFragment.KEY_WIFI_ONLY)) {
                    editor.putBoolean(k, globals.getBoolean(k));
                } else {
                    editor.putString(k, globals.getString(k));
                }
            }

            editor.apply();

		} catch (JSONException e) {
			Log.e(TAG, "ERROR importing globals: " + e.getMessage());
			return -1;
		}

        return 0;
    }

    public int importSettingsV1(JSONArray profiles) {
        return importProfileList(profiles);
    }

    public int importSettingsV2(JSONObject obj) {
        JSONObject globals = null;
        JSONArray profiles = null;
        try {
            globals = obj.getJSONObject("globals");
            profiles = obj.getJSONArray("profiles");
        } catch (JSONException e) {
            Log.e(TAG, "ERROR import version 2 profiles: " + e.getMessage());
            return -1;
        }
        
        if (importGlobalSettings(globals) != 0) {
            return -1;
        }

        if (importProfileList(profiles) != 0) {
            return -1;
        }

        return 0;
    }

    public int addBackup(BackupItem item) {
        if (mBackupHandler.addBackup(item) == BackupHandler.ERROR_BACKUP_EXISTS) {
            Toast.makeText(getApplicationContext(), "Profile '" + item.name + "' already exists", Toast.LENGTH_SHORT).show();
        }

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, true);
        return 0;
    }

    public int updateBackup(String old_name, BackupItem item) {
        mBackupHandler.updateBackup(old_name, item);
        mBackupHandler.updateBackupList();

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, true);
        return 0;
    }

    public int removeBackup(BackupItem item) {
        int ret = mBackupHandler.removeBackup(item);
        if (ret == 0) {
            mBackupHandler.updateBackupList();
        }

        return ret;
    }

    public int copyBackup(BackupItem item) {
        int ret = mBackupHandler.copyBackup(item);
        if (ret == 0) {
            mBackupHandler.updateBackupList();
        }

        return ret;
    }

    public int editBackup(BackupItem item) {
        AddBackupItemFragment f = new AddBackupItemFragment();
        f.setBackupContent(item);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).addToBackStack(null).commit();
        return 0;
    }

    public void updateBackupList() {
        mBackupHandler.updateBackupList();

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, true);
    }

    public List<BackupItem> getBackups() {
        return mBackupHandler.getBackups();
    }

    public void updateBackupTimestamp(BackupItem b) {
        mBackupHandler.updateBackupTimestamp(b);
    }

    public int runBackup(BackupItem b) {
        Snackbar.make(findViewById(R.id.backuplist_coordinator),
                "Running '" + b.name + "'",
                Snackbar.LENGTH_SHORT).show();

        Intent i = new Intent(this, BackupBackgroundService.class);
        i.putExtra("item", b);
        i.putExtra("force", true);
        ContextCompat.startForegroundService(getApplicationContext(), i);
        return 0;
    }

    public void showLog(BackupItem b) {
        BackupLogFragment f = new BackupLogFragment();
        f.setBackupItem(b);
        setCurrentFragment(f, true);
    }


    private void setCurrentFragment(Fragment f, boolean stack) {
        FragmentTransaction tr = getFragmentManager().beginTransaction().replace(R.id.content_container, f);

        if (stack) {
            tr.addToBackStack(null);
        }

        tr.commit();
    }

    public int copyExecutables() {
		int ret = copyExecutable("rsync");

		if (ret != 0) {
			return ret;
		}
		
        return copyExecutable("ssh");
    }

    public int copyExecutable(String filename) {
        // copy and overwrite
        
        File file = getFileStreamPath(filename);

		String[] abis = {Build.CPU_ABI, Build.CPU_ABI2};
		if (Build.VERSION.SDK_INT >= 21) {
			abis = Build.SUPPORTED_ABIS;
		}

		InputStream src = null;
		
		// try to grab matching executable for a ABI supported by this device
		for (String abi : abis) {
            try {
                src = getAssets().open(abi + '/' + filename);
            } catch (IOException e) {
                // no need to close src here
				Log.d(TAG, abi + " is not supported");
                continue;
            }
		}

		if (src == null) {
			Log.e(TAG, "Could not find supported rsync binary for ABI: " + Arrays.toString(abis));
			return -1;
		}

		Log.d(TAG, "Found appropriate rsync binary: " + src);

		OutputStream dst = null;
		try {
			dst = new DataOutputStream(openFileOutput(filename, Context.MODE_PRIVATE));

			byte data[] = new byte[4096];
			int count;

			while ((count = src.read(data)) != -1) {
				dst.write(data, 0, count);
			}
		} catch (IOException e) {
			Log.e(TAG, "Error copying executable: " + e.toString());
			return -1;
		}

		try {
			src.close();
			dst.close();
		} catch  (IOException e) {
			Log.e(TAG, "Error closing input or output stream: " + e.toString());
		}

		File f = new File(getFilesDir(), filename);
		try {
			f.setExecutable(true);
		} catch (SecurityException e) {
			Log.e(TAG, "Error setting executable flag: " + e.toString());
			return -1;
		}

		return 0;
	}

    public int ensureSSHDir() {
        /* parent directory .ssh */
        File f = new File(getFilesDir(), ".ssh");
        if (!f.exists()) {
            if (!f.mkdirs()) {
                Log.e(TAG, "Could not create directory " + f.getAbsolutePath());
                return -1;
            }
        }

        if (!f.isDirectory()) {
            Log.e(TAG, f.getAbsolutePath() + " is not a directory");
            return -1;
        }

        if (!f.canWrite()) {
            Log.e(TAG, f.getAbsolutePath() + " is not writable");
            return -1;
        }

        /* child file known_hosts */
        File f2 = new File(f, "known_hosts");
        if (!f2.exists()) {
            try {
                if (!f2.createNewFile()) {
                    Log.e(TAG, "Could not create file " + f2.getAbsolutePath());
                    return -1;
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not create file " + f2.getAbsolutePath() + ": " + e.toString());
            }
        }

        if (!f2.isFile()) {
            Log.e(TAG, f2.getAbsolutePath() + " is not a file");
            return -1;
        }

        if (!f2.canWrite()) {
            Log.e(TAG, "file " + f2.getAbsolutePath() + " is not writable");
            return -1;
        }

        return 0;
    }
}

