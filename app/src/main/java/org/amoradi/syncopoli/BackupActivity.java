package org.amoradi.syncopoli;

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
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class BackupActivity extends AppCompatActivity implements IBackupHandler {
    private static final String TAG = "BackupActivity";

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
        new Perm(android.Manifest.permission.ACCESS_NETWORK_STATE, 6)
    };

    Account mAccount;
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

        mAccount = createSyncAccount(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long freq = Long.parseLong(prefs.getString(SettingsFragment.KEY_FREQUENCY, "8"));
        freq = freq * 3600; // hours to seconds

        ContentResolver.addPeriodicSync(mAccount, SYNC_AUTHORITY, new Bundle(), freq);

        copyExecutables();

        mBackupHandler = new BackupHandler(this);

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, false);
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

    public static Account createSyncAccount(Context ctx) {
        Account acc = new Account(SYNC_ACCOUNT_NAME, SYNC_ACCOUNT_TYPE);
        AccountManager accman = AccountManager.get(ctx);

        if (accman.addAccountExplicitly(acc, null, null)) {
            ContentResolver.setIsSyncable(acc, SYNC_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(acc, SYNC_AUTHORITY, true);
        }

        return acc;
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
        startService(i);
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
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    public int addBackup(BackupItem item) {
        if (mBackupHandler.addBackup(item) == BackupHandler.ERROR_EXISTS) {
            Toast.makeText(getApplicationContext(), "Profile '" + item.name + "' already exists", Toast.LENGTH_SHORT).show();
        }

        BackupListFragment f = new BackupListFragment();
        f.setBackupHandler(this);
        setCurrentFragment(f, true);
        return 0;
    }

    public int updateBackup(BackupItem item) {
        mBackupHandler.updateBackup(item);
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

    public int editBackup(BackupItem item) {
        AddBackupItemFragment f = new AddBackupItemFragment();
        f.setBackupContent(item);
        getFragmentManager().beginTransaction().replace(R.id.content_container, f).addToBackStack(null).commit();
        return 0;
    }

    public void updateBackupList() {
        mBackupHandler.updateBackupList();
    }

    public List<BackupItem> getBackups() {
        return mBackupHandler.getBackups();
    }

    public void updateBackupTimestamp(BackupItem b) {
        mBackupHandler.updateBackupTimestamp(b);
    }

    public int runBackup(BackupItem b) {
        syncBackups();
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

    public void copyExecutables() {
        copyExecutable("rsync");
        copyExecutable("ssh");
    }

    public void copyExecutable(String filename) {
        File file = getFileStreamPath(filename);

        if (file.exists()) {
            return;
        }

        try {
            // TODO: copy abi-compatible binaries
            InputStream src = getAssets().open("armeabi/" + filename);
            OutputStream dst = new DataOutputStream(openFileOutput(filename, Context.MODE_PRIVATE));

            byte data[] = new byte[4096];
            int count;
            while ((count = src.read(data)) != -1) {
                dst.write(data, 0, count);
            }

            src.close();
            dst.close();

	    File f = new File(getFilesDir(), filename);
	    f.setExecutable(true);
        } catch (Exception e) {
            Toast.makeText(this, "Download Error: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}
