package com.savanto.andict;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.inputmethod.InputMethodManager;

import java.lang.ref.WeakReference;

/**
 * DefineTask class extends Android's AsyncTask to allow
 * background-thread tasks. In this case, the task is a network
 * connection and communication with a DICT server.
 */
final class DefineTask extends AsyncTask<Void, Void, String[]> {
    private final WeakReference<DictActivity> activityRef;
    private final WeakReference<ProgressDialog> pdRef;

    private final String server;
    private final int port;
    private final String database;
    private final String word;

    DefineTask(DictActivity activity, String server, int port, String database, String word) {
        super();
        this.activityRef = new WeakReference<>(activity);
        this.pdRef = new WeakReference<>(new ProgressDialog(activity));
        this.server = server;
        this.port = port;
        this.database = database;
        this.word = word;
    }

    @Override
    protected void onPreExecute() {
        // Hide the keyboard
        final Activity activity = activityRef.get();
        if (activity != null) {
            final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(activity.findViewById(android.R.id.content).getWindowToken(), 0);
            }

            // Show ProgressDialog
            final ProgressDialog pd = pdRef.get();
            if (pd != null) {
                pd.setMessage(activity.getString(R.string.progress));
                pd.show();
            }
        }
    }

    @Override
    protected String[] doInBackground(Void... v) {
        return NativeDict.define(this.server, this.port, this.database, this.word);
    }

    @Override
    protected void onPostExecute(String[] entries) {
        final DictActivity activity = activityRef.get();
        if (activity != null) {
            if (entries != null && entries.length != 0) {
                activity.displayStatus(Message.CONNECTED + server);
                activity.displayDefinitions(entries);
            } else if (entries != null) {
                activity.displayStatus(Message.NO_RESULT);
                activity.displayDefinitions(new String[]{});
            } else {
                activity.displayStatus(Message.NETWORK_ERROR);
            }
        }

        // Dismiss ProgressDialog
        final ProgressDialog pd = pdRef.get();
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }

    private static class Message {
        private static final String NETWORK_ERROR = "Network error.";
        private static final String CONNECTED = "Connected to ";
        private static final String NO_RESULT = "No result.";
    }
}
