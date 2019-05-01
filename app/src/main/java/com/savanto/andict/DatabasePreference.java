package com.savanto.andict;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public final class DatabasePreference extends RemoteEntityPreference {
    public DatabasePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    int getProgressText() {
        return R.string.pref_databases_progress;
    }

    @Override
    void populateDefaultEntities(
            @NonNull LayoutInflater inflater,
            @NonNull RadioGroup list,
            @NonNull SparseArray<String> entities,
            boolean entitiesRetrieved) {
        final Context context = this.getContext();

        final String allMatches = context.getString(R.string.all_matches);
        final RadioButton allMatchesChoice = (RadioButton) inflater.inflate(
                R.layout.entity_choice,
                list,
                false
        );
        allMatchesChoice.setText(allMatches);
        allMatchesChoice.setId(View.NO_ID);
        list.addView(allMatchesChoice);
        entities.append(allMatchesChoice.getId(), allMatches);

        final String firstMatch = context.getString(R.string.first_match);
        final RadioButton firstMatchChoice = (RadioButton) inflater.inflate(
                R.layout.entity_choice,
                list,
                false
        );
        firstMatchChoice.setText(firstMatch);
        firstMatchChoice.setId(View.NO_ID);
        list.addView(firstMatchChoice);
        entities.append(firstMatchChoice.getId(), firstMatch);
    }

    @Override
    Entity[] fetchRemoteEntities(@NonNull String server, int port) {
        return NativeDict.showDatabases(server, port);
    }
}
