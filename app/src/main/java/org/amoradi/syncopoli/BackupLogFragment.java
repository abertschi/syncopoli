package org.amoradi.syncopoli;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupLogFragment extends Fragment {
    private static final String TAG = "Syncopoli";

    private BackupItem mBackupItem;
    private Thread textReaderThread;
    private TextLineAdapter textLineAdapter;
    private Handler textLineHandler;
    private RecyclerView recycleView;

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
        View view = null;
        if (mBackupItem != null) {
            textLineHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Log.i(TAG, "Handle new message on Main Thread");
                    textLineAdapter.addTextLines((List<TextLine>) msg.obj);
                }
            };
            view = inflater.inflate(R.layout.fragment_backuplog, container, false);
            textLineAdapter = new TextLineAdapter(container.getContext());
            recycleView = view.findViewById(R.id.recyclerView);
            recycleView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recycleView.setAdapter(textLineAdapter);
        } else {
            textLineAdapter.addTextLine(new TextLine(0, "mBackupItem is null"));
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startWorker();
    }

    private void stopWorker() {
        if (textReaderThread != null && !textReaderThread.isInterrupted()) {
            textReaderThread.interrupt();
            try {
                textReaderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startWorker() {
        if (textLineAdapter == null){
            throw new IllegalStateException("Cannot start worker without initializing adapter.");
        }
        stopWorker();
        textLineAdapter.clear();
        try {
            FileInputStream in = getActivity().getApplicationContext().openFileInput(mBackupItem.getLogFileName());
            textReaderThread = new TextReaderThread(in, textLineHandler);
            textReaderThread.start();
        } catch (FileNotFoundException e) {
            textLineAdapter.addTextLine(new TextLine(0, e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopWorker();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_done).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_run).setVisible(false);
        menu.findItem(R.id.action_scroll_down).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            stopWorker();
            startWorker();
        } else if (id == R.id.action_scroll_down) {
            recycleView.scrollToPosition(textLineAdapter.getItemCount() - 1);
        }
        else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * POJO for a single text line
     **/
    class TextLine {
        private int lineNr;
        private String content;

        public TextLine(int lineNr, String content) {
            this.lineNr = lineNr;
            this.content = content;
        }

        public int getLineNr() {
            return lineNr;
        }

        public void setLineNr(int lineNr) {
            this.lineNr = lineNr;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

    }

    /**
     * Adapter to render many text lines in recycle view
     **/
    private class TextLineAdapter extends RecyclerView.Adapter<ViewHolder> {
        Context mContext;
        List<TextLine> mItems;

        public TextLineAdapter(Context context) {
            mContext = context;
            mItems = new ArrayList<>();
        }

        public void addTextLines(List<TextLine> textLine) {
            int size = textLine.size();
            if (size > 0) {
                mItems.addAll(textLine);
                notifyItemRangeInserted(mItems.size() - size - 1, size);
            }
        }

        public void addTextLine(TextLine textLine) {
            mItems.add(textLine);
            notifyItemInserted(mItems.size() - 1);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(
                    LayoutInflater.from(mContext).inflate(R.layout.log_item, parent, false)
            );
        }

        public void clear() {
            mItems.clear();
            notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.lineNumberView.setText(Integer.toString(mItems.get(position).getLineNr()));
            holder.textLineView.setText(mItems.get(position).getContent());
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView lineNumberView;
        final TextView textLineView;

        public ViewHolder(@NonNull View view) {
            super(view);
            lineNumberView = view.findViewById(R.id.lineNumber);
            textLineView = view.findViewById(R.id.textLine);
        }
    }

    /**
     * Worker to read from an input stream and inform a handler on changes
     */
    private class TextReaderThread extends Thread {
        private final int READ_CHUNK = 100; // How many lines to read before update
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private InputStream in;
        private Handler lineHandler;

        public TextReaderThread(InputStream in, Handler lineHandler) {
            this.in = in;
            this.lineHandler = lineHandler;
        }

        public void run() {
            Log.i(TAG, "Start text reader thread");
            stopped.set(false);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            int lineNumber = 1;
            final int ARRAY_CAP = READ_CHUNK + 1;

            try {
                String textLine = bufferedReader.readLine();
                ArrayList<TextLine> lines = new ArrayList<>(ARRAY_CAP);
                int readChunk = 0;

                while (null != textLine) {
                    lines.add(new TextLine(lineNumber, textLine));
                    if (readChunk >= READ_CHUNK) {
                        Message.obtain(lineHandler, 0, lines).sendToTarget();
                        readChunk = 0;
                        lines = new ArrayList<>(ARRAY_CAP);
                    }
                    readChunk++;
                    if (stopped.get()) {
                        break;
                    }
                    textLine = bufferedReader.readLine();
                    lineNumber++;
                }
                if (lines.size() > 0) {
                    Message.obtain(lineHandler, 0, lines).sendToTarget();
                }
            } catch (IOException e) {
                ArrayList<TextLine> msg = new ArrayList<>();
                msg.add(new TextLine(0, e.getMessage()));
                Message.obtain(lineHandler, 0, msg).sendToTarget();
                e.printStackTrace();
            }
        }

        @Override
        public void interrupt() {
            stopped.set(true);
            super.interrupt();
        }
    }
}
