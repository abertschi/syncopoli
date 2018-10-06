package org.amoradi.syncopoli;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class BackupLogFragment extends Fragment {
    private BackupItem mBackupItem;
    private int mFileLine;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void setBackupItem(BackupItem b) {
        mBackupItem = b;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_backuplog, container, false);
        if (mBackupItem != null) {
            mFileLine = 0;
            ((TextView) v.findViewById(R.id.backuplog_textview)).setText(getLogString(mBackupItem.getLogFileName()));
        } else {
            ((TextView) v.findViewById(R.id.backuplog_textview)).setText("mBackupItem is null");
        }

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_run).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            ((TextView) getView().findViewById(R.id.backuplog_textview)).setText(getLogString(mBackupItem.getLogFileName()));
            ((ScrollView) getView().findViewById(R.id.backuplog_scrollview)).fullScroll(View.FOCUS_DOWN);
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    public String getLogString(String filename) {
        try {
            FileInputStream ins = getActivity().getApplicationContext().openFileInput(filename);

            LineNumberReader reader = new LineNumberReader(new InputStreamReader(ins));
            reader.setLineNumber(mFileLine);

            StringBuilder output = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                output.append(line + "\n");
                mFileLine = reader.getLineNumber();
                line = reader.readLine();
            }
            reader.close();
            ins.close();

            return output.toString();
        } catch (FileNotFoundException e) {
            return "Log file not found.";
        } catch (IOException e) {
            e.printStackTrace();
            return "An error occurred while trying to read log file.";
        }
    }
}
