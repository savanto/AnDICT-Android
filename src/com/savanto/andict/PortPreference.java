package com.savanto.andict;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class PortPreference extends DialogPreference
{
	private final Context context;
	private final int minPort;
	private final int maxPort;
	private final int defaultPort;

	private int port;

	private EditText portField;

	public PortPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
		minPort = context.getResources().getInteger(R.integer.pref_port_min);
		maxPort = context.getResources().getInteger(R.integer.pref_port_max);
		defaultPort = context.getResources().getInteger(R.integer.pref_port_default);
	}

	@Override
	protected View onCreateDialogView()
	{
		// Get current port value from saved preferences, or default port value if unable to retrieve it
		port = getPersistedInt(defaultPort);

		// Inflate layout
		View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.port_preference, null);

		// Setup the "number picker"
		final Button add = (Button) view.findViewById(R.id.button_add);
		final Button sub = (Button) view.findViewById(R.id.button_sub);
		portField = (EditText) view.findViewById(R.id.port_field);
		portField.setText(Integer.toString(port));

		// Setup "number picker" controls
		add.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (port < maxPort)
					portField.setText(Integer.toString(++port));
			}
		});
		sub.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (port > minPort)
					portField.setText(Integer.toString(--port));
			}
		});

		return view;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		super.onDialogClosed(positiveResult);
		if (positiveResult)
		{
			// Verify new port number
			try
			{
				int newPort = Integer.parseInt(portField.getText().toString());
				if (newPort >= minPort && newPort <= maxPort)
				{
					port = newPort;
					if (shouldPersist())
						persistInt(port);
					notifyChanged();
				}
				else
				{
					AlertDialog invalidPort = new AlertDialog.Builder(PortPreference.this.getContext())
						.setTitle(R.string.invalid_title)
						.setMessage(PortPreference.this.getContext().getResources().getString(R.string.invalid_message)
								+ " " + minPort + "-" + maxPort + ".")
						.setNeutralButton(R.string.invalid_button, new DialogInterface.OnClickListener()
							{ @Override public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); } })
						.create();

					Window window = invalidPort.getWindow();
					WindowManager.LayoutParams lp = window.getAttributes();
					lp.token = portField.getWindowToken();
					lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
					window.setAttributes(lp);
					window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

					invalidPort.show();
				}
			}
			catch (NumberFormatException e) { /* Do nothing for bad user input */ }
		}
	}
}
