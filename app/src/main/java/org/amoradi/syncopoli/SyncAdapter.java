package org.amoradi.syncopoli;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import java.util.List;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    public SyncAdapter(Context ctx, boolean autoInit) {
        super(ctx, autoInit);
    }

    public SyncAdapter(Context ctx, boolean autoInit, boolean autoParallel) {
        super(ctx, autoInit, autoParallel);
    }

    @Override
    public void onPerformSync(Account acc, Bundle bun, String authority, ContentProviderClient cpc, SyncResult res) {
        BackupHandler backupHandler = new BackupHandler(getContext());

        List<BackupItem> bs = backupHandler.getBackups();
        BackupItem[] backups = new BackupItem[bs.size()];
        bs.toArray(backups);

        Intent i = new Intent(getContext(), BackupBackgroundService.class);
        i.putExtra("items", backups);
        ContextCompat.startForegroundService(getContext(), i);
    }
}
