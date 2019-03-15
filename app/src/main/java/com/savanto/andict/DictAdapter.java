package com.savanto.andict;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class DictAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Definition> definitions = new ArrayList<>();
    private final DefinitionFormatter definitionFormatter;

    DictAdapter(@NonNull DefinitionFormatter definitionFormatter) {
        this.definitionFormatter = definitionFormatter;
    }

    void setDefinitions(@NonNull List<Definition> definitions) {
        this.definitions = definitions;
        this.notifyDataSetChanged();
    }

    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new DefinitionHolder(inflater.inflate(R.layout.definition, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final DefinitionHolder definitionHolder = (DefinitionHolder) holder;
        final Definition definition = this.definitions.get(position);
        definitionHolder.databaseView.setText(definition.database);
        definitionHolder.definitionView.setText(this.definitionFormatter.formatDefinition(
                definition.definition
        ));
    }

    @Override
    public int getItemCount() {
        return this.definitions.size();
    }

    private static final class DefinitionHolder extends RecyclerView.ViewHolder {
        private final TextView databaseView;
        private final TextView definitionView;

        DefinitionHolder(View itemView) {
            super(itemView);
            this.databaseView = itemView.findViewById(R.id.definition_database);
            this.definitionView = itemView.findViewById(R.id.definition_definition);
            this.definitionView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
