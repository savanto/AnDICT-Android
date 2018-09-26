package com.savanto.andict;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DictActivity extends Activity {
    /**
     * User interface elements
     **/
    private LinearLayout linearLayout; // LinearLayout within ScrollView; holds the retrieved definitions
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
        linearLayout = findViewById(R.id.linear_layout);

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
        // Reset the definition display
        linearLayout.removeAllViews();

        // LayoutParams for all of the definition TextViews
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Create and display the definition TextViews
        int i = 0;
        for (final String entry : entries) {
            if (i % 2 == 0) {
                // Format and display dictionary information line
                final SpannableString dictionary = new SpannableString(entry);
                dictionary.setSpan(new StyleSpan(Typeface.BOLD), 0, dictionary.length(), 0);
                final TextView dictionaryView = new TextView(this);
                dictionaryView.setLayoutParams(lp);
                dictionaryView.setText(dictionary);
                linearLayout.addView(dictionaryView);
            } else {
                // Format and display definition
                final SpannableString definition = formatDefinition(new SpannableString(entry));
                final TextView definitionView = new TextView(this);
                definitionView.setLayoutParams(lp);
                definitionView.setText(definition);
                // Make links in definition clickable
                definitionView.setMovementMethod(LinkMovementMethod.getInstance());
                linearLayout.addView(definitionView);
            }

            ++i;
        }
    }

    /**
     * Format plain definition to have links.
     *
     * @param definition - SpannableString containing unformatted definition.
     * @return SpannableString with the definition formatted.
     */
    private SpannableString formatDefinition(SpannableString definition) {
        // The characters to search for that delineate links
        final String START = "{";
        final String END = "}";

        // Loop through the definition until no more occurrences are found
        String text = definition.toString();
        int start = 0, end = 0, offset = 0;
        while (start > -1 && end > -1) {
            offset += end + 1;
            // Definition is truncated each time to avoid finding the same occurrences over and over
            text = text.substring(end + 1);
            start = text.indexOf(START);
            end = text.indexOf(END);
            if (start > -1 && end > -1 && start < end) {
                // Get the string of the link
                final String newLookup = text.substring(start + 1, end);
                // Make a new clickable span with the action to be performed
                ClickableSpan cs = new ClickableSpan() {
                    @Override
                    public void onClick(View v) {
                        // Perform lookup on the link string
                        doLookup(newLookup);
                        // Update editLookup with the new word
                        editLookup.setText(newLookup);
                    }
                };
                // Set the span to be a link
                definition.setSpan(cs, start + 1 + offset, end + offset, 0);
            }
        }
        return definition;
    }
}
