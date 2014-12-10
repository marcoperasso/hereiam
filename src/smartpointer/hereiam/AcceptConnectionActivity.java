package smartpointer.hereiam;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class AcceptConnectionActivity extends Activity implements
		OnClickListener {

	private static final int ACTIVATE_GPS_RESULT = 1;
	private User user;
	private int timeout = 0;

	enum Response {
		NONE, ACCEPT, ACCEPT_TIMEOUT, REFUSE
	};

	Response response = Response.NONE;
	private EditText editTextTimeout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accept_connection);
		user = (User) getIntent().getSerializableExtra(Const.USER);

		TextView tv = (TextView) findViewById(R.id.textViewQuestion);
		tv.setText(getString(R.string.wants_to_know_your_position_do_you_agree,
				user));

		editTextTimeout = (EditText) findViewById(R.id.editTextMinutes);
		int minutes = MySettings.getAcceptTimeout();
		editTextTimeout.setText(Integer.toString(minutes));

		findViewById(R.id.buttonYes).setOnClickListener(this);
		findViewById(R.id.buttonNo).setOnClickListener(this);
		findViewById(R.id.buttonAlways).setOnClickListener(this);
		findViewById(R.id.buttonAcceptWithTimeout).setOnClickListener(this);
	}

	@Override
	protected void onDestroy() {
		switch (response) {
		case ACCEPT:
			MyApplication.getInstance().respondToUser(user.phone,
					Const.MSG_ACCEPT_CONTACT);
			ConnectorService.activate(AcceptConnectionActivity.this, user,
					true, CommandType.START_SENDING_MY_POSITION, -1);
			cancelNotification();

			break;
		case ACCEPT_TIMEOUT:
			MyApplication.getInstance().respondToUser(user.phone,
					Const.MSG_ACCEPT_CONTACT);
			ConnectorService.activate(AcceptConnectionActivity.this, user,
					true, CommandType.START_SENDING_MY_POSITION, timeout);
			cancelNotification();
			break;
		case NONE:
			break;
		case REFUSE:
			MyApplication.getInstance().respondToUser(user.phone,
					Const.MSG_REJECT_CONTACT);
			cancelNotification();
			break;
		default:
			break;
		}
		super.onDestroy();
	}

	private void cancelNotification() {
		Intent intent = getIntent();
		if (intent.hasExtra(Const.NOTIFICATION_CODE)) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(intent.getIntExtra(
					Const.NOTIFICATION_CODE, 0));
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonYes:
			hasGPSOrDontWantToUseIt();
			break;
		case R.id.buttonAlways:
			// prendo l'equivalente utente nella lista, così non devo
			// rinfrescarla
			User fromPhone = MyApplication.getInstance().getUsers()
					.fromPhone(user.phone, true);

			fromPhone.trusted = true;
			fromPhone.saveToDb();

			hasGPSOrDontWantToUseIt();
			break;
		case R.id.buttonNo:
			response = Response.REFUSE;
			finish();
			break;
		case R.id.buttonAcceptWithTimeout:
			timeout = -1;
			String s = editTextTimeout.getText().toString();
			try {
				timeout = Integer.parseInt(s);
			} catch (Exception ex) {
				break;
			}
			if (timeout <= 0)
				break;
			MySettings.setAcceptTimeout(timeout);
			response = Response.ACCEPT_TIMEOUT;
			finish();
			break;
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVATE_GPS_RESULT) {
			response = Response.ACCEPT;
			finish();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void hasGPSOrDontWantToUseIt() {
		LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			response = Response.ACCEPT;
			finish();
		}
		Helper.hideableQuestion(this, new IFinishCallback() {
			@Override
			public void finished() {
				Intent myIntent = new Intent(
						Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivityForResult(myIntent, ACTIVATE_GPS_RESULT);

			}
		}, new IFinishCallback() {

			@Override
			public void finished() {
				response = Response.ACCEPT;
				finish();
			}
		}, R.string.want_gps);
	}

}
