package smartpointer.hereiam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class AcceptConnectionActivity extends Activity implements
		OnClickListener {

	private static final int ACTIVATE_GPS_RESULT = 1;
	private User user;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accept_connection);
		user = (User) getIntent().getSerializableExtra(Const.USER);

		TextView tv = (TextView) findViewById(R.id.textViewQuestion);
		tv.setText(getString(R.string.wants_to_know_your_position_do_you_agree,
				user));

		findViewById(R.id.buttonYes).setOnClickListener(this);
		findViewById(R.id.buttonNo).setOnClickListener(this);
		findViewById(R.id.buttonAlways).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonYes:
			if (hasGPSOrDontWantToUseIt())
				acceptToSendMyPosition();
			break;
		case R.id.buttonAlways:
			// prendo l'equivalente utente nella lista, così non devo
			// rinfrescarla
			User fromPhone = MyApplication.getInstance().getUsers()
					.fromPhone(user.phone, true);

			fromPhone.trusted = true;
			fromPhone.saveToDb();

			if (hasGPSOrDontWantToUseIt())
				acceptToSendMyPosition();
			break;
		case R.id.buttonNo:
			rejectUser();
			break;
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVATE_GPS_RESULT) {
			acceptToSendMyPosition();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private boolean hasGPSOrDontWantToUseIt() {
		LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			return true;
		}
		Helper.hideableQuestion(this, new IFinishCallback() {
			@Override
			public void finished() {
				Intent myIntent = new Intent(
						Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivityForResult(myIntent, ACTIVATE_GPS_RESULT);

			}
		}, null, R.string.want_gps);
		return false;
	}

	private void acceptToSendMyPosition() {
		MyApplication.getInstance().respondToUser(user.phone,
				Const.MSG_ACCEPT_CONTACT);
		ConnectorService.activate(AcceptConnectionActivity.this, user, true,
				CommandType.START_SENDING_MY_POSITION);
		finish();

	}

	private void rejectUser() {
		MyApplication.getInstance().respondToUser(user.phone,
				Const.MSG_REJECT_CONTACT);
		finish();
	}

}
