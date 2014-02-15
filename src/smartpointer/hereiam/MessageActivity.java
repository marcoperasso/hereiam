package smartpointer.hereiam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class MessageActivity extends Activity implements OnClickListener {

	private User user;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		user = (User) getIntent().getSerializableExtra(Const.USER);
		setContentView(R.layout.activity_message);
		setTitle(user.toString());
		findViewById(R.id.buttonClose).setOnClickListener(this);
		findViewById(R.id.buttonSend).setOnClickListener(this);
	}

	static void sendMessageToUser(Activity launcher, final User user) {
		Intent intent = new Intent(launcher, MessageActivity.class);
		intent.putExtra(Const.USER, user);
		launcher.startActivityForResult(intent, Const.SEND_MESSAGE_RESULT);

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

				Credentials.testCredentials(MessageActivity.this,
						new OnAsyncResponse() {

							@Override
							public void response(boolean success,
									String loginMessage) {
								if (success) {
									Credentials c = MySettings
											.readCredentials();
									Message msg = new Message((long) (System
											.currentTimeMillis() / 1e3), c
											.getId(), user.id, message);
									WebRequestResult result = HttpManager
											.messageToUser(msg);

									if (result.result) {
										Helper.showMessage(
												MessageActivity.this,
												getString(R.string.message_successfully_delivered));

										msg.saveToDB(MessageActivity.this);
										Intent data = new Intent();
										data.putExtra(Const.MESSAGE, msg);
										setResult(RESULT_OK, data);
										finish();

									} else
										Helper.showMessage(
												MessageActivity.this,
												getString(
														R.string.message_not_delivered_s,
														result.message));
								} else {
									Helper.showMessage(
											MessageActivity.this,
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonClose:
			finish();
			break;
		case R.id.buttonSend:
			EditText txt = (EditText) findViewById(R.id.editTextMessage);
			Editable message = txt.getText();
			if (message.length() > 0)
			{
				sendMessage(message.toString());
				
			}
			break;
		}

	}

}
