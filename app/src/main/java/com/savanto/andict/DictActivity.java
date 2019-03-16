package com.savanto.andict;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DictActivity extends Activity implements DefinitionFormatter {
    /**
     * User interface elements
     **/
    private DictAdapter definitionsAdapter;
    private TextView connectionStatus; // TextView displaying the status of the lookup
    private EditText editLookup;       // EditText into which user enters word to lookup

    /**
     * Background thread to perform lookups
     **/
    private DefineTask defineTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Define the user controls from the layout
        editLookup = findViewById(R.id.edit_word);
        final Button buttonLookup = findViewById(R.id.button_lookup);
        connectionStatus = findViewById(R.id.connection_status);
        definitionsAdapter = new DictAdapter(this);
        final RecyclerView definitionsView = findViewById(R.id.definitions_view);
        definitionsView.setAdapter(definitionsAdapter);
        definitionsView.setLayoutManager(new LinearLayoutManager(this));

        // Set the lookup button action
        buttonLookup.setOnClickListener(v -> doLookup(editLookup.getText().toString()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                // Launch DictSettingsActivity
                final Intent intent = new Intent(this.getBaseContext(), DictSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
        }

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Kill DefineTask, if running
        if (defineTask != null && ! defineTask.isCancelled()) {
            defineTask.cancel(true);
        }
    }

    /**
     * Retrieves the stored server/port/database settings, builds the server commands,
     * and executes the lookup on a separate thread.
     *
     * @param word - String with word to send to the DICT server.
     */
    private void doLookup(String word) {
        // Input check
        if (word.isEmpty()) {
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String server = prefs.getString(
                DictSettingsActivity.PREF_SERVER_KEY,
                getString(R.string.pref_server_default)
        );

        final int port = prefs.getInt(
                DictSettingsActivity.PREF_PORT_KEY,
                PortPreference.DEFAULT_PORT
        );

        final String allMatches = this.getString(R.string.all_matches);
        final String firstMatch = this.getString(R.string.first_match);
        final String storedDatabase = prefs.getString(
                DictSettingsActivity.PREF_DATABASE_KEY,
                getString(R.string.pref_database_default)
        );
        final String database;
        if (allMatches.equals(storedDatabase)) {
            database = "*";
        } else if (firstMatch.equals(storedDatabase)) {
            database = "!";
        } else {
            database = storedDatabase;
        }

        final String strategy;
        if (prefs.getBoolean(DictSettingsActivity.PREF_STRATEGY_ENABLE_KEY, false)) {
            strategy = prefs.getString(
                    DictSettingsActivity.PREF_STRATEGY_KEY,
                    this.getString(R.string.strategy_exact)
            );
        } else {
            strategy = null;
        }

        // Perform lookup on separate thread
        this.defineTask = new DefineTask(
                DictActivity.this,
                server,
                port,
                database,
                strategy,
                word
        );
        this.defineTask.execute();
    }

    /**
     * Display the status of the attempted lookup.
     *
     * @param message - String containing lookup status.
     */
    protected void displayStatus(String message) {
        connectionStatus.setText(message);
    }

    /**
     * Display the returned definitions, if any.
     */
    protected void displayDefinitions(Definition[] definitions) {
        this.definitionsAdapter.setDefinitions(Arrays.asList(definitions));
    }

    private final class LinkClickableSpan extends ClickableSpan {
        private final String word;

        LinkClickableSpan(String word) {
            this.word = word;
        }

        @Override
        public void onClick(@NonNull View widget) {
            doLookup(this.word);
            editLookup.setText(this.word);
        }
    }

    /**
     * Format plain definition to have links, italics, and other formatting.
     *
     * @return SpannableString with the definition formatted.
     */
    public SpannableString formatDefinition(String definition) {
        final StringBuilder definitionBuilder = new StringBuilder();
        final List<Span> spans = new ArrayList<>();

        int linkStart = -1, adjLinkStart = -1, adjItalicsStart = -1;
        int pos = 0, adjPos = 0;
        for (final char c : definition.toCharArray()) {
            switch (c) {
                case LINK_START:
                    linkStart = pos;
                    adjLinkStart = adjPos;
                    break;
                case LINK_END:
                    if (linkStart >= 0 && linkStart < pos) {
                        spans.add(new Span<ClickableSpan>(
                                new LinkClickableSpan(definition.substring(linkStart + 1, pos)),
                                adjLinkStart,
                                adjPos
                        ));
                    }
                    linkStart = adjLinkStart = -1;
                    break;
                case ITALICS:
                    if (adjItalicsStart >= 0 && adjItalicsStart < adjPos) {
                        spans.add(new Span<>(new StyleSpan(Typeface.ITALIC), adjItalicsStart, adjPos));
                        adjItalicsStart = -1;
                    } else {
                        adjItalicsStart = adjPos;
                    }
                    break;
                default:
                    definitionBuilder.append(c);
                    ++adjPos;
                    break;
            }
            ++pos;
        }

        final SpannableString definitionSpan = new SpannableString(definitionBuilder.toString());
        for (final Span span : spans) {
            span.setSpan(definitionSpan);
        }

        return definitionSpan;
    }

    private static final char LINK_START = '{';
    private static final char LINK_END = '}';
    private static final char ITALICS = '\\';

    private static final class Span<T extends CharacterStyle> {
        private final T what;
        private final int start;
        private final int end;

        Span(T what, int start, int end) {
            this.what = what;
            this.start = start;
            this.end = end;
        }

        void setSpan(SpannableString spannableString) {
            spannableString.setSpan(this.what, this.start, this.end, 0);
        }
    }
}
