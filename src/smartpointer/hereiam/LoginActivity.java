package smartpointer.hereiam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class LoginActivity extends Activity implements OnEditorActionListener,
		OnClickListener {

	private EditText mPassword;
	private EditText mUserid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		setTitle(R.string.insert_credential_title);

		mPassword = (EditText) findViewById(R.id.editTextPassword);
		mUserid = (EditText) findViewById(R.id.editTextUserId);

		Credentials c = MySettings.readCredentials();
		mUserid.setText(c.getUserId());
		mPassword.setText(c.getPassword());
		mPassword.setOnEditorActionListener(this);
		mPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);

		findViewById(R.id.ButtonOK).setOnClickListener(this);
		findViewById(R.id.buttonCancel).setOnClickListener(this);
		findViewById(R.id.ButtonRegister).setOnClickListener(this);
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE
				|| (event.getAction() == KeyEvent.ACTION_DOWN && event
						.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
			doLogin();
			return true;
		}
		return false;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Const.REGISTER_RESULT) {
			if (resultCode == RESULT_OK)
				onLogged();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onClick(View v) {
		if (v.getId() == R.id.ButtonOK) {
			doLogin();

		} else if (v.getId() == R.id.buttonCancel) {
			Intent returnIntent = new Intent();
			setResult(RESULT_CANCELED, returnIntent);
			finish();
		} else if (v.getId() == R.id.ButtonRegister) {
			Intent intent = new Intent(this, UserActivity.class);
			startActivityForResult(intent, Const.REGISTER_RESULT);
		}
	}

	private void onLogged() {
		Intent returnIntent = new Intent();
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	private void doLogin() {
		String userId = mUserid.getText().toString();
		String pwd = mPassword.getText().toString();
		if (Helper.isNullOrEmpty(userId) || Helper.isNullOrEmpty(pwd)) {
			return;
		}

		final Credentials credentials = new Credentials(userId, pwd);
		final ProgressDialog progressBar = new ProgressDialog(this);
		progressBar.setMessage(getString(R.string.verifying_credentials));
		progressBar.setCancelable(false);
		progressBar.setIndeterminate(true);
		progressBar.show();
		credentials.testLogin(this, new OnAsyncResponse() {

			public void response(boolean success, String message) {
				if (success) {
					MySettings.setCredentials(credentials);
					onLogged();
				} else {
					Helper.showMessage(LoginActivity.this, message);
				}
				progressBar.dismiss();
			}

		});

	}

}
