package com.savanto.andict;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import java.lang.ref.WeakReference;

import static com.savanto.andict.DictActivity.LOGTAG;

/**
 * DefineTask class extends Android's AsyncTask to allow
 * background-thread tasks. In this case, the task is a network
 * connection and communication with a DICT server.
 */
final class DefineTask extends AsyncTask<Void, Void, Definition[]> {
    private final WeakReference<DictActivity> activityRef;
    private final WeakReference<ProgressDialog> pdRef;

    private final String server;
    private final int port;
    private final String database;
    private final String strategy;
    private final String word;

    DefineTask(
            DictActivity activity,
            @NonNull String server,
            int port,
            @NonNull String database,
            @Nullable String strategy,
            @NonNull String word) {
        super();
        this.activityRef = new WeakReference<>(activity);
        this.pdRef = new WeakReference<>(new ProgressDialog(activity));
        this.server = server;
        this.port = port;
        this.database = database;
        this.strategy = strategy;
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
    protected Definition[] doInBackground(Void... v) {
        if (this.strategy != null) {
            Log.d(LOGTAG, "DefineTask: match + define");
            return NativeDict.defineWithStrategy(this.server, this.port, this.database, this.strategy, this.word);
        } else {
            Log.d(LOGTAG, "DefineTask: define");
            return NativeDict.define(this.server, this.port, this.database, this.word);
        }
    }

    @Override
    protected void onPostExecute(Definition[] definitions) {
        final DictActivity activity = activityRef.get();
        if (activity != null) {
            final Resources res = activity.getResources();
            if (definitions != null && definitions.length != 0) {
                Log.d(LOGTAG, "DefineTask: found " + definitions.length + " definitions");
                activity.displayStatus(Message.get(res, Message.CONNECTED, this.server));
                activity.displayDefinitions(definitions);
            } else if (definitions != null) {
                Log.d(LOGTAG, "DefineTask: no result");
                activity.displayStatus(Message.get(res, Message.NO_RESULT));
                activity.displayDefinitions(new Definition[]{});
            } else {
                Log.d(LOGTAG, "DefineTask: error");
                activity.displayStatus(Message.get(res, Message.NETWORK_ERROR));
            }
        }

        // Dismiss ProgressDialog
        final ProgressDialog pd = pdRef.get();
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }

    private static class Message {
        private static final @StringRes int NETWORK_ERROR = R.string.message_network_error;
        private static final @StringRes int CONNECTED = R.string.message_connected;
        private static final @StringRes int NO_RESULT = R.string.message_no_result;

        static @NonNull String get(@NonNull Resources res, @StringRes int message, Object... formatArgs) {
            return res.getString(message, formatArgs);
        }
    }
}
