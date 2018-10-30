package org.amoradi.syncopoli;

interface IBackupItemClickHandler {
    void onBackupShowLog(int pos);
    void onBackupEdit(int pos);
    void onBackupCopy(int pos);
    void onBackupDelete(int pos);
    void onBackupRun(int pos);
}
