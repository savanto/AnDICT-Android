package com.savanto.andict;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public final class StrategyPreference extends RemoteEntityPreference {
    public StrategyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    int getProgressText() {
        return R.string.pref_strategies_progress;
    }

    @Override
    void populateDefaultEntities(
            @NonNull LayoutInflater inflater,
            @NonNull RadioGroup list,
            @NonNull SparseArray<String> entities,
            boolean entitiesRetrieved) {
        // Do not use default strategies if server returns any.
        if (entitiesRetrieved) {
            return;
        }

        final Context context = this.getContext();

        final String exact = context.getString(R.string.strategy_exact);
        final RadioButton exactChoice = (RadioButton) inflater.inflate(
                R.layout.entity_choice,
                list,
                false
        );
        exactChoice.setText(exact);
        exactChoice.setId(View.NO_ID);
        list.addView(exactChoice);
        entities.append(exactChoice.getId(), exact);

        final String prefix = context.getString(R.string.strategy_prefix);
        final RadioButton prefixChoice = (RadioButton) inflater.inflate(
                R.layout.entity_choice,
                list,
                false
        );
        prefixChoice.setText(prefix);
        prefixChoice.setId(View.NO_ID);
        list.addView(prefixChoice);
        entities.append(prefixChoice.getId(), prefix);
    }

    @Override
    Entity[] fetchRemoteEntities(@NonNull String server, int port) {
        return NativeDict.showStrategies(server, port);
    }
}
