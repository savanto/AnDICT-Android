package com.savanto.andict;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ViewSwitcher;

import java.lang.ref.WeakReference;

public final class DatabasePreference extends DialogPreference {
    private final SparseArray<String> databases = new SparseArray<>();
    private ViewSwitcher switcher;
    private RadioGroup list;

    public DatabasePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView() {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.database_preference, null);

        this.switcher = view.findViewById(R.id.database_preference_switcher);
        this.list = view.findViewById(R.id.database_preference_choices);

        final Context context = view.getContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String server = prefs.getString(
                DictSettingsActivity.PREF_SERVER_KEY,
                context.getString(R.string.pref_server_default)
        );
        final int port = prefs.getInt(DictSettingsActivity.PREF_PORT_KEY, PortPreference.DEFAULT_PORT);

        new ShowDatabasesTask(this, server, port).execute();

        return view;
    }

    private void onRetrievedChoices(Entity[] entities) {
        final Context context = this.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        final String allMatches = context.getString(R.string.all_matches);
        final RadioButton allMatchesChoice = (RadioButton) inflater.inflate(
                R.layout.entity_choice,
                this.list,
                false
        );
        allMatchesChoice.setText(allMatches);
        allMatchesChoice.setId(View.NO_ID);
        this.list.addView(allMatchesChoice);
        this.databases.append(allMatchesChoice.getId(), allMatches);

        final String firstMatch = context.getString(R.string.first_match);
        final RadioButton firstMatchChoice = (RadioButton) inflater.inflate(
                R.layout.entity_choice,
                list,
                false
        );
        firstMatchChoice.setText(firstMatch);
        firstMatchChoice.setId(View.NO_ID);
        this.list.addView(firstMatchChoice);
        this.databases.append(firstMatchChoice.getId(), firstMatch);

        for (final Entity entity : entities) {
            final RadioButton itemChoice = (RadioButton) inflater.inflate(
                    R.layout.entity_choice,
                    list,
                    false
            );
            itemChoice.setText(entity.description);
            itemChoice.setId(View.NO_ID);
            this.list.addView(itemChoice);
            this.databases.append(itemChoice.getId(), entity.name);
        }

        this.switcher.showNext();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult && this.shouldPersist()) {
            final String database = this.databases.get(this.list.getCheckedRadioButtonId());
            if (database != null) {
                this.persistString(database);
            }
        }
    }


    private static final class ShowDatabasesTask extends AsyncTask<Void, Void, Entity[]> {
        private final WeakReference<DatabasePreference> prefRef;

        private final String server;
        private final int port;

        ShowDatabasesTask(DatabasePreference pref, @NonNull String server, int port) {
            this.prefRef = new WeakReference<>(pref);
            this.server = server;
            this.port = port;
        }

        @Override
        protected Entity[] doInBackground(Void... voids) {
            return NativeDict.showDatabases(this.server, this.port);
        }

        @Override
        protected void onPostExecute(Entity[] entities) {
            final DatabasePreference pref = this.prefRef.get();
            if (pref != null) {
                pref.onRetrievedChoices(entities);
            }
        }
    }
}
