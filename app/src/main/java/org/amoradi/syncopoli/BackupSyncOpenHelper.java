package org.amoradi.syncopoli;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BackupSyncOpenHelper extends SQLiteOpenHelper {
    public BackupSyncOpenHelper(Context ctx) {
        super(ctx, BackupSyncSchema.DATABASE_NAME, null, BackupSyncSchema.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + BackupSyncSchema.TABLE_NAME + " (" +
                   BackupSyncSchema.COLUMN_TYPE         + " text, " +
                   BackupSyncSchema.COLUMN_NAME         + " text, " +
                   BackupSyncSchema.COLUMN_SOURCES      + " text, " +
                   BackupSyncSchema.COLUMN_DESTINATION  + " text, " +
                   BackupSyncSchema.COLUMN_LAST_UPDATE  + " text, " +
                   BackupSyncSchema.COLUMN_DIRECTION    + " text, " +
                   BackupSyncSchema.COLUMN_RSYNC_OPTIONS+ " text);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 2) {
            // missing COLUMN_RSYNC_OPTIONS
            db.execSQL("alter table " + BackupSyncSchema.TABLE_NAME + " add column " + BackupSyncSchema.COLUMN_RSYNC_OPTIONS + " text;");
            db.execSQL("update " + BackupSyncSchema.TABLE_NAME + " set " + BackupSyncSchema.COLUMN_RSYNC_OPTIONS + " = '';");
        } else {
            db.execSQL("drop table " + BackupSyncSchema.TABLE_NAME + ";");
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
