package org.amoradi.syncopoli;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import static android.app.Activity.RESULT_OK;

public class AddBackupItemFragment extends Fragment {
    IBackupHandler mHandler;
    BackupItem mBackup = null;
    int SOURCE_REQUEST_CODE = 1;

    private TextInputEditText v_name;
    private TextInputEditText v_src;
    private TextInputEditText v_dst;
    private TextInputEditText v_opts;

    @Override
    public void onAttach(Activity acc) {
        super.onAttach(acc);
        mHandler = (IBackupHandler) acc;
    }

    public void setBackupContent(BackupItem b) {
        mBackup = b;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addbackupitem, container, false);

        v_name = (TextInputEditText) v.findViewById(R.id.addbackupitem_name);
        v_src = (TextInputEditText) v.findViewById(R.id.addbackupitem_source);
        v_dst = (TextInputEditText) v.findViewById(R.id.addbackupitem_destination);
        v_opts = (TextInputEditText) v.findViewById(R.id.addbackupitem_rsync_options);

		/*
        v_src.setOnLongClickListener(new View.OnLongClickListener () {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
                intent.putExtra("org.openintents.extra.TITLE", "Source Directory");
                intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Select Directory");
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(intent, SOURCE_REQUEST_CODE);
                } else {
                    Toast.makeText(getActivity(), "Error: requires OI File Manager", Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });
		*/

        Spinner v_dir = (Spinner) v.findViewById(R.id.addbackupitem_direction);
        String[] items = getResources().getStringArray(R.array.addbackupitem_direction_entries);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        v_dir.setAdapter(adapter);

        if (mBackup == null) {
            return v;
        }

        if (mBackup.direction == BackupItem.Direction.OUTGOING) {
            v_dir.setSelection(1);
        } else {
            v_dir.setSelection(0);
        }

        v_name.setText(mBackup.name);
        v_src.setText(TextUtils.join("\n", mBackup.sources));
        v_dst.setText(mBackup.destination);
        v_opts.setText(mBackup.rsync_options);

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SOURCE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri path = data.getData();
            v_src.setText(path.getPath());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_done).setVisible(true);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_run).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_done) {
            BackupItem i = new BackupItem();

            View v = getView();

            EditText t = (EditText) v.findViewById(R.id.addbackupitem_source);
            i.sources = t.getText().toString().replaceAll("(?m)^[ \t]*\n", "").split("\n");

            t = (EditText) v.findViewById(R.id.addbackupitem_destination);
            i.destination = t.getText().toString();

            t = (EditText) v.findViewById(R.id.addbackupitem_name);
            i.name = t.getText().toString();

            t = (EditText) v.findViewById(R.id.addbackupitem_rsync_options);
            i.rsync_options = t.getText().toString();

            Spinner s = (Spinner) v.findViewById(R.id.addbackupitem_direction);
            if (s.getSelectedItemPosition() == 0) {
                i.direction = BackupItem.Direction.INCOMING;
            } else {
                i.direction = BackupItem.Direction.OUTGOING;
            }

            if (mBackup == null) {
                mHandler.addBackup(i);
            } else {
                mHandler.updateBackup(mBackup.name, i);
            }
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
