package com.savanto.andict;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import java.util.List;

public class DictActivity extends Activity {
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
        definitionsAdapter = new DictAdapter();
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

        // Get the server, port, and search strategy from SharedPreferences
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String server = prefs.getString(
                DictSettingsActivity.PREF_SERVER_KEY,
                getString(R.string.pref_server_default)
        );
        final int port = prefs.getInt(
                DictSettingsActivity.PREF_PORT_KEY,
                PortPreference.DEFAULT_PORT
        );
        final String database = prefs.getString(
                DictSettingsActivity.PREF_DATABASE_KEY,
                getString(R.string.pref_database_default)
        ).equals(getString(R.string.first_match)) ? "! " : "* ";

        // Perform lookup on separate thread
        defineTask = new DefineTask(DictActivity.this, server, port, database, word);
        defineTask.execute();
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
    protected void displayDefinitions(String[] entries) {
        final List<CharSequence> formattedEntries = new ArrayList<>();
        for (final String entry : entries) {
            formattedEntries.add(formatDefinition(entry));
        }

        this.definitionsAdapter.setEntries(formattedEntries);
    }

    private static final char LINK_START = '{';
    private static final char LINK_END = '}';
    private static final char ITALICS = '\\';
    private static final char NEWLINE = '\n';

    /**
     * Format plain definition to have links.
     *
     * @return SpannableString with the definition formatted.
     */
    private SpannableString formatDefinition(String definition) {
        final StringBuilder definitionBuilder = new StringBuilder();
        final List<Span> spans = new ArrayList<>();

        int linkStart = -1, adjLinkStart = -1, adjItalicsStart = -1;
        int pos = 0, adjPos = 0;
        boolean source = true;
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
                case NEWLINE:
                    if (source) {
                        spans.add(new Span<>(new StyleSpan(Typeface.BOLD), 0, adjPos));
                        source = false;
                    }
                    // FALL-THROUGH
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

    private final class LinkClickableSpan extends ClickableSpan {
        private final String word;

        LinkClickableSpan(String word) {
            this.word = word;
        }

        @Override
        public void onClick(View widget) {
            doLookup(this.word);
            editLookup.setText(this.word);
        }
    }

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
