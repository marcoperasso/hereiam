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
	private EditText mPhone;
	private EditText mPassword;
	private CountrySpinner mSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		setTitle(R.string.insert_credential_title);

		mPassword = (EditText) findViewById(R.id.editTextPassword);
		mPhone = (EditText) findViewById(R.id.editTextUserPhone);
		Credentials c = MySettings.readCredentials();
		mPassword.setText(c.getPassword());
		mPassword.setOnEditorActionListener(this);
		mPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);

		StringBuilder prefix = new StringBuilder();
		StringBuilder number = new StringBuilder();
		Helper.splitPhone(c.getPhone(), prefix, number);
		mPhone.setText(number);
		String sPrefix = prefix.length() == 0 ? Helper.getPrefix() : prefix
				.toString();
		mSpinner = (CountrySpinner) findViewById(R.id.spinnerPrefixes);
		mSpinner.setPrefix(sPrefix);
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
		String pwd = mPassword.getText().toString();
		String phone = mSpinner.getPrefix() + mPhone.getText().toString();
		phone = Helper.adjustPhoneNumber(phone);
		if (Helper.isNullOrEmpty(pwd) || Helper.isNullOrEmpty(phone)) {
			return;
		}

		final Credentials credentials = new Credentials(phone, pwd);
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
				try {
					progressBar.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});

	}

}
