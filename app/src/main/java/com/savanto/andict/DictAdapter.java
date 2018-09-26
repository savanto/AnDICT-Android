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
    private List<CharSequence> entries = new ArrayList<>();

    void setEntries(@NonNull List<CharSequence> entries) {
        this.entries = entries;
        this.notifyDataSetChanged();
    }

    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new DefinitionHolder(inflater.inflate(R.layout.definition, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((DefinitionHolder) holder).definitionView.setText(this.entries.get(position));
    }

    @Override
    public int getItemCount() {
        return this.entries.size();
    }

    private static final class DefinitionHolder extends RecyclerView.ViewHolder {
        private final TextView definitionView;

        DefinitionHolder(View itemView) {
            super(itemView);
            this.definitionView = itemView.findViewById(R.id.definition_view);
            this.definitionView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
