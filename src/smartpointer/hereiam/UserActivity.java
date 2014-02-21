package smartpointer.hereiam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UserActivity extends Activity implements OnClickListener {

	static final String REGISTER_USER = "ru";
	private EditText mPassword;
	private EditText mUserPhone;
	private EditText mMail;
	private boolean newUser;
	private EditText mPassword1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user);

		mUserPhone = (EditText) findViewById(R.id.editTextUserPhone);
		mPassword = (EditText) findViewById(R.id.editTextPassword);
		mPassword1 = (EditText) findViewById(R.id.editTextRepeatPassword);
		mMail = (EditText) findViewById(R.id.editTextMail);
		newUser = getIntent().getBooleanExtra(REGISTER_USER, true);
		if (!newUser) {
			Credentials c = MySettings.readCredentials();
			mUserPhone.setText(c.getPhone());
			mUserPhone.setEnabled(false);

			mPassword.setText(c.getPassword());
			mPassword1.setText(c.getPassword());
			mMail.setText(c.getEmail());
		}
		Button btnOk = (Button) findViewById(R.id.ButtonOK);
		btnOk.setOnClickListener(this);

		Button btnCancel = (Button) findViewById(R.id.buttonCancel);
		btnCancel.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v.getId() == R.id.ButtonOK) {
			doRegister();

		} else if (v.getId() == R.id.buttonCancel) {
			Intent returnIntent = new Intent();
			setResult(RESULT_CANCELED, returnIntent);
			finish();
		}

	}

	private String checkField(int id) {
		EditText t = (EditText) findViewById(id);
		String s = t.getText().toString();
		if (Helper.isNullOrEmpty(s)) {
			Helper.showMessage(UserActivity.this,
					getString(R.string.the_field_s_is_obligatory_, t.getHint()));
			return null;
		}
		return s;
	}

	private void doRegister() {
		String userId = checkField(R.id.editTextUserPhone);
		if (userId == null)
			return;

		String pwd = checkField(R.id.editTextPassword);
		if (pwd == null)
			return;
		String pwd1 = checkField(R.id.editTextRepeatPassword);
		if (pwd1 == null)
			return;

		if (!pwd.equals(pwd1)) {
			Toast.makeText(this, R.string.passwords_do_not_match_,
					Toast.LENGTH_LONG).show();
			return;
		}
		String mail = mMail.getText().toString();

		String phone = mUserPhone.getText().toString();
		phone = Helper.adjustPhoneNumber(phone);
		final Credentials credentials = new Credentials(phone, pwd);
		credentials.setEmail(mail);

		final ProgressDialog progressBar = new ProgressDialog(this);
		progressBar.setMessage(getString(R.string.saving_user_data_));
		progressBar.setCancelable(false);
		progressBar.setIndeterminate(true);
		progressBar.show();
		new AsyncTask<Void, Void, WebRequestResult>() {

			@Override
			protected WebRequestResult doInBackground(Void... params) {

				WebRequestResult res = HttpManager.saveCredentials(credentials,
						newUser);
				if (res.result) {
					MySettings.setCredentials(credentials);
				}
				return res;
			}

			protected void onPostExecute(WebRequestResult result) {
				progressBar.dismiss();
				if (result.result) {
					Intent returnIntent = new Intent();
					setResult(RESULT_OK, returnIntent);
					finish();
				} else {
					Helper.showMessage(UserActivity.this, result.message);
				}
			};

		}.execute(null, null, null);
	}

}
