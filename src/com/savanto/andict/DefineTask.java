package com.savanto.andict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.ListIterator;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.inputmethod.InputMethodManager;

/**
 * DefineTask class extends Android's AsyncTask to allow
 * background-thread tasks. In this case, the task is a network
 * connection and communication with a DICT server.
 *
 * DefineTask<Params, Progress, Result>
 * Params:
 * Progress:
 * Result: LinkedList<String> holding lines of response.
 */
public class DefineTask extends AsyncTask<Void, Void, LinkedList<String> >
{
	private final DictActivity activity;
	private final String server;
	private final int port;
	private final LinkedList<String> commands;

	private Socket socket;
	private BufferedReader input;
	private PrintWriter output;
	private ProgressDialog pd;

	private String message = null;

	public DefineTask(DictActivity activity, String server, int port, LinkedList<String> commands)
	{
		super();
		this.activity = activity;
		this.server = server;
		this.port = port;
		this.commands = commands;
		this.pd = new ProgressDialog(activity);

	}

	@Override
	protected void onPreExecute()
	{
		// Hide the keyboard
		((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
			.hideSoftInputFromWindow(activity.findViewById(android.R.id.content).getWindowToken(), 0);

		// Show ProgressDialog
		pd.setMessage("Looking up word...");
		pd.show();
	}

	@Override
	protected LinkedList<String> doInBackground(Void... v)
	{
		// Check that there are indeed commands to be sent to a server
		if (commands.isEmpty())
		{
			message = Message.INVALID_COMMANDS;
			cancel(true);
			return null;
		}

		// Check that task has not been cancelled
		if (isCancelled())
			return null;

		// Create the socket
		try
		{
			socket = new Socket(server, port);
		}
		catch (UnknownHostException e)
		{
			message = Message.UNKNOWN_HOST + server + ":" + port;
			cancel(true);
			return null;
		}
		catch (IOException e)
		{
			message = Message.NETWORK_ERROR;
			cancel(true);
			return null;
		}

		// Check that socket is connected
		if (! socket.isConnected())
		{
			message = Message.CANNOT_CONNECT + server + ":" + port;
			cancel(true);
			return null;
		}

		// Check that task has not been cancelled
		if (isCancelled())
			return null;

		LinkedList<String> response = new LinkedList<String>();
		try
		{
			// Create the input and output streams
			output = new PrintWriter(socket.getOutputStream(), true);
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// Send commands to the socket
			ListIterator<String> i = commands.listIterator();
			while (i.hasNext())
				output.println(i.next());

			// Read the server response from socket
			String line;
			while ((line = input.readLine()) != null)
				response.add(line);
		}
		catch (NullPointerException e)
		{
			message = Message.CANNOT_CONNECT + server + ":" + port;
			cancel(true);
			return null;
		}
		catch (IOException e)
		{
			message = Message.NETWORK_ERROR;
			cancel(true);
			return null;
		}

		// Check that task has not been cancelled
		if (isCancelled())
			return null;

		// Close socket and streams
		try
		{
			output.close();
			input.close();
			socket.close();
		}
		catch (IOException e)
		{
			message = Message.NETWORK_ERROR;
			cancel(true);
			return null;
		}

		return response;
	}

	@Override
	protected void onPostExecute(LinkedList<String> response)
	{
		if (isCancelled())
			activity.displayStatus(message);
		else
		{
			activity.displayStatus(Message.CONNECTED + server);
			activity.displayDefinitions(new DictParser(response));
		}
		// Dismiss ProgressDialog
		if (pd.isShowing())
			pd.dismiss();
	}

	@Override
	protected void onCancelled()
	{
		try
		{
			// Close the streams and socket
			output.close();
			input.close();
			socket.close();
		}
		catch (IOException e)
		{
		}

		// Display error
		activity.displayStatus(message);

		// Dismiss ProgressDialog
		if (pd.isShowing())
			pd.dismiss();
	}

	private static class Message
	{
		private static final String INVALID_COMMANDS		= "Invalid commands to DICT server.";
		private static final String UNKNOWN_HOST			= "Unknown server: ";
		private static final String NETWORK_ERROR			= "Network error.";
		private static final String CANNOT_CONNECT			= "Cannot connect to: ";
		private static final String CONNECTED				= "Connected to ";
	}
}
