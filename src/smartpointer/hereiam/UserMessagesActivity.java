package smartpointer.hereiam;

import java.util.ArrayList;
import java.util.Date;

import android.R.anim;
import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

public class UserMessagesActivity extends ListActivity implements
		OnClickListener {

	private static final String MESSAGES = "msg";
	private ArrayList<Message> messages;
	private ArrayAdapter<Message> adapter;
	private User user;
	private ListView list;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_messages);
		user = (User) (getIntent().hasExtra(Const.USER) ? getIntent()
				.getSerializableExtra(Const.USER) : null);
		if (user == null) {
			finish();
			return;
		}
		setTitle(user.toString());
		list =  (ListView) findViewById(android.R.id.list);
		
		// findViewById(R.id.buttonCancel).setOnClickListener(this);
		// findViewById(R.id.buttonRemoveAll).setOnClickListener(this);
		findViewById(R.id.buttonSend).setOnClickListener(this);
		MyApplication.getInstance().setMessagesActivity(this);
	}

	@Override
	protected void onDestroy() {
		MyApplication.getInstance().setMessagesActivity(null);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		messages = Message.getMessages(this, user);

		adapter = new MyMessageAdapter(this, R.layout.mymessagerow, messages);
		setListAdapter(adapter);
		refreshLabelVisibility();
		super.onResume();
	}

	
	private void refreshLabelVisibility() {
		findViewById(R.id.textViewNoMessages).setVisibility(
				(messages.size() > 0) ? View.GONE : View.VISIBLE);
		list.post(new Runnable() {
	        @Override
	        public void run() {
	            // Select the last row so it will scroll into view...
	        	list.setSelection(adapter.getCount() - 1);
	        }
	    });
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(MESSAGES, messages);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final int i = position;
		final Message message = (Message) l.getItemAtPosition(i);
		Helper.dialogMessage(this, R.string.delete_this_message_,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						messages.remove(message);
						message.removeFromDB(UserMessagesActivity.this);
						adapter.notifyDataSetChanged();
						refreshLabelVisibility();
					}
				}, null);

		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.buttonCancel)
			finish();
		else if (view.getId() == R.id.buttonSend) {
			EditText txt = (EditText) findViewById(R.id.editTextMessage);
			Editable message = txt.getText();
			if (message.length() > 0) {
				sendMessage(message.toString());

			}
		} else if (view.getId() == R.id.buttonRemoveAll)

			Helper.dialogMessage(this, R.string.remove_all_messages_,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							for (Message m : messages)
								m.removeFromDB(UserMessagesActivity.this);
							messages.clear();
							adapter.notifyDataSetChanged();
							refreshLabelVisibility();
						}
					}, null);

	}

	void sendMessage(final String message) {
		final ProgressDialog progressBar = new ProgressDialog(this);
		progressBar.setCancelable(true);
		progressBar.setMessage(getString(R.string.sending_message_));
		progressBar.setIndeterminate(true);
		progressBar.show();
		new AsyncTask<Void, Void, Void>() {
			protected void onPostExecute(Void result) {
				progressBar.dismiss();
			};

			@Override
			protected Void doInBackground(Void... params) {

				Credentials.testCredentials(UserMessagesActivity.this,
						new OnAsyncResponse() {

							@Override
							public void response(boolean success,
									String loginMessage) {
								if (success) {
									Credentials c = MySettings
											.readCredentials();
									final Message msg = new Message(Helper
											.getUnixTime(), c.getPhone(),
											user.phone, message);
									WebRequestResult result = HttpManager
											.messageToUser(msg);

									if (result.result) {
										runOnUiThread(new Runnable(){

											@Override
											public void run() {
												Helper.showMessage(
														UserMessagesActivity.this,
														getString(R.string.message_successfully_delivered));

												msg.saveToDB(UserMessagesActivity.this);
												addMessage(msg);
												EditText txt = (EditText) findViewById(R.id.editTextMessage);
												txt.setText("");
												
											}});
										

									} else
										Helper.showMessage(
												UserMessagesActivity.this,
												getString(
														R.string.message_not_delivered_s,
														result.message));
								} else {
									Helper.showMessage(
											UserMessagesActivity.this,
											getString(
													R.string.message_not_delivered_s,
													loginMessage));
								}

							}
						});

				return null;
			}

		}.execute(null, null, null);

	}

	public static void openMessages(Activity context, User selectedUser) {
		Intent intent = new Intent(context, UserMessagesActivity.class);
		intent.putExtra(Const.USER, selectedUser);
		context.startActivity(intent);

	}

	public void addMessage(Message msg) {
		messages.add(msg);
		adapter.notifyDataSetChanged();
		refreshLabelVisibility();
		
	}

	public User getUser() {
		return user;
	}

}

class MyMessageAdapter extends ArrayAdapter<Message> {

	private UserMessagesActivity context;

	public MyMessageAdapter(UserMessagesActivity context, int resource,
			ArrayList<Message> messages) {
		super(context, resource, messages);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		Message message = getItem(position);
		if (convertView == null) {
			LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(R.layout.mymessagerow, null);
		} else {
			view = convertView;
		}
		TextView tvName = (TextView) view.findViewById(R.id.textViewName);
		tvName.setText(message.getName());
		TextView tvMessage = (TextView) view.findViewById(R.id.textViewMessage);
		tvMessage.setText(message.getMessage());
		TextView tvDate = (TextView) view.findViewById(R.id.textViewDate);
		Date d = new Date(message.getTime() * 1000);

		java.text.DateFormat timeFormat = DateFormat
				.getTimeFormat(MyApplication.getInstance());
		java.text.DateFormat dateFormat = DateFormat
				.getDateFormat(MyApplication.getInstance());
		tvDate.setText("(" + dateFormat.format(d) + ", " + timeFormat.format(d)
				+ ")");
		
		LinearLayout messageContainer = (LinearLayout) view.findViewById(R.id.messageRow);
		messageContainer.setBackgroundResource(message.isReceived() ? R.drawable.speech_bubble_green : R.drawable.speech_bubble_orange);
		LayoutParams lp = (LayoutParams) messageContainer.getLayoutParams();
		lp.gravity = message.isReceived() ? Gravity.RIGHT : Gravity.LEFT;
		messageContainer.setLayoutParams(lp);
		//messageContainer.setTextColor(android.R.color.white);	
		return view;
	}

}
