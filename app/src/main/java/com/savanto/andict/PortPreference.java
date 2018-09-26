package com.savanto.andict;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.Locale;

public final class PortPreference extends DialogPreference {
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;
    static final int DEFAULT_PORT = 2628;

    private EditText portField;

    private int port;

    public PortPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView() {
        // Get current port value from saved preferences, or default port value if unable to retrieve it
        port = getPersistedInt(DEFAULT_PORT);

        // Inflate layout
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.port_preference, null);

        // Setup the "number picker"
        final Button add = view.findViewById(R.id.button_add);
        final Button sub = view.findViewById(R.id.button_sub);
        portField = view.findViewById(R.id.port_field);
        portField.setText(String.valueOf(port));

        // Setup "number picker" controls
        add.setOnClickListener(v -> {
            if (port < MAX_PORT) {
                portField.setText(String.valueOf(++port));
            }
        });
        sub.setOnClickListener(v -> {
            if (port > MIN_PORT) {
                portField.setText(String.valueOf(--port));
            }
        });

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            // Verify new port number
            try {
                final int newPort = Integer.parseInt(portField.getText().toString());
                if (newPort >= MIN_PORT && newPort <= MAX_PORT) {
                    port = newPort;
                    if (shouldPersist()) {
                        persistInt(port);
                    }
                    notifyChanged();
                } else {
                    final AlertDialog invalidPort = new AlertDialog.Builder(getContext())
                            .setTitle(R.string.invalid_title)
                            .setMessage(String.format(
                                    Locale.US,
                                    getContext().getResources().getString(R.string.invalid_message),
                                    MIN_PORT,
                                    MAX_PORT
                            ))
                            .setNeutralButton(
                                    R.string.invalid_button,
                                    (dialog, which) -> dialog.dismiss()
                            )
                            .create();

                    final Window window = invalidPort.getWindow();
                    if (window != null ) {
                        final WindowManager.LayoutParams lp = window.getAttributes();
                        lp.token = portField.getWindowToken();
                        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
                        window.setAttributes(lp);
                        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

                        invalidPort.show();
                    }
                }
            } catch (NumberFormatException e) { /* Do nothing for bad user input */ }
        }
    }
}
