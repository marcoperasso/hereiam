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
		NONE, ACCEPT, REFUSE
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

	}

	@Override
	protected void onDestroy() {
		switch (response) {
		case ACCEPT:
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
			if (!saveTimeout())
				return;
			response = Response.ACCEPT;
			hasGPSOrDontWantToUseIt();
			break;
		case R.id.buttonAlways:
			if (!saveTimeout())
				return;
			// prendo l'equivalente utente nella lista, così non devo
			// rinfrescarla
			User u = MyApplication.getInstance().getUsers()
					.fromPhone(user.phone, true);

			u.trusted = true;
			u.saveToDb();

			hasGPSOrDontWantToUseIt();
			break;
		case R.id.buttonNo:
			response = Response.REFUSE;
			finish();
			break;

		}

	}

	boolean saveTimeout() {
		timeout = -1;
		String s = editTextTimeout.getText().toString();
		try {
			timeout = Integer.parseInt(s);
		} catch (Exception ex) {
			int minutes = MySettings.getAcceptTimeout();
			editTextTimeout.setText(Integer.toString(minutes));
			return false;
		}
		if (timeout <= 0) {
			int minutes = MySettings.getAcceptTimeout();
			editTextTimeout.setText(Integer.toString(minutes));

			return false;
		}
		MySettings.setAcceptTimeout(timeout);
		return true;
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
