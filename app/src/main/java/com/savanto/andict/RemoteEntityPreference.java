package com.savanto.andict;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.lang.ref.WeakReference;

public abstract class RemoteEntityPreference extends DialogPreference {
    private final SparseArray<String> entities = new SparseArray<>();
    private ViewSwitcher switcher;
    private RadioGroup list;

    RemoteEntityPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    abstract @StringRes int getProgressText();

    abstract void populateDefaultEntities(
            @NonNull LayoutInflater inflater,
            @NonNull RadioGroup list,
            @NonNull SparseArray<String> entities,
            boolean entitiesRetrieved);

    @WorkerThread
    abstract Entity[] fetchRemoteEntities(@NonNull String server, int port);

    @Override
    protected final View onCreateDialogView() {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams") final View view = inflater.inflate(
                R.layout.remote_entity_preference,
                null
        );

        this.switcher = view.findViewById(R.id.entity_preference_switcher);
        this.list = view.findViewById(R.id.entity_preference_choices);

        final TextView progressView = view.findViewById(R.id.entity_preference_progress);
        progressView.setText(this.getProgressText());

        final Context context = view.getContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String server = prefs.getString(
                DictSettingsActivity.PREF_SERVER_KEY,
                context.getString(R.string.pref_server_default)
        );
        final int port = prefs.getInt(DictSettingsActivity.PREF_PORT_KEY, PortPreference.DEFAULT_PORT);

        new ShowEntitiesTask(this, server, port).execute();

        return view;
    }

    private void onRetrievedChoices(Entity[] entities) {
        final Context context = this.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        final boolean entitiesRetrieved = entities != null && entities.length != 0;
        this.populateDefaultEntities(inflater, this.list, this.entities, entitiesRetrieved);

        if (entitiesRetrieved) {
            for (final Entity entity : entities) {
                final RadioButton itemChoice = (RadioButton) inflater.inflate(
                        R.layout.entity_choice,
                        this.list,
                        false
                );
                itemChoice.setText(String.format("%s: %s", entity.name, entity.description));
                itemChoice.setId(View.NO_ID);
                this.list.addView(itemChoice);
                this.entities.append(itemChoice.getId(), entity.name);
            }
        }

        this.switcher.showNext();
    }

    @Override
    protected final void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult && this.shouldPersist()) {
            final String choice = this.entities.get(this.list.getCheckedRadioButtonId());
            if (choice != null) {
                this.persistString(choice);
            }
        }
    }


    private static final class ShowEntitiesTask extends AsyncTask<Void, Void, Entity[]> {
        private final WeakReference<RemoteEntityPreference> prefRef;

        private final String server;
        private final int port;

        ShowEntitiesTask(RemoteEntityPreference pref, @NonNull String server, int port) {
            this.prefRef = new WeakReference<>(pref);
            this.server = server;
            this.port = port;
        }

        @Override
        protected Entity[] doInBackground(Void... voids) {
            final RemoteEntityPreference pref = this.prefRef.get();
            if (pref != null) {
                return pref.fetchRemoteEntities(this.server, this.port);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Entity[] entities) {
            final RemoteEntityPreference pref = this.prefRef.get();
            if (pref != null) {
                pref.onRetrievedChoices(entities);
            }
        }
    }
}
