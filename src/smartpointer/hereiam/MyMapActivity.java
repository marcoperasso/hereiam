package smartpointer.hereiam;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MyMapActivity extends MapActivity implements OnClickListener {

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	private MapView mMap;

	private MapController mController;

	private UserPositionOverlay mUsersOverlay;
	private MITULocationOverlay myLocationOverlay;
	private boolean mTrackGPSPosition;
	private boolean invalidGCMStauts;

	MenuItem mMenuItemTrackGpsPosition;

	private LocationManager mlocManager;

	private Animation mAnimation;

	private GoogleCloudMessaging gcm;
	private String regid;

	private GenericEventHandler mConnectorServiceChangedHandler = new GenericEventHandler() {

		@Override
		public void onEvent(Object sender, EventArgs args) {
			showTrackingButton(isLiveTracking());
		}
	};

	PositionsDownloadedEventHandler mPositionAvailableHandler = new PositionsDownloadedEventHandler() {

		@Override
		public void onEvent(Object sender, ArrayList<UserPosition> positions) {
			mUsersOverlay.setPositions(positions);

		}
	};

	PositionReceivedEventHandler mPositionReceivedHandler = new PositionReceivedEventHandler() {

		@Override
		public void onEvent(Object sender, UserPosition position) {

			ArrayList<UserPosition> positions = mUsersOverlay.getPositions();
			for (int i = positions.size() - 1; i >= 0; i--) {
				UserPosition userPosition = positions.get(i);
				// tolgo le posizioni più vecchie di 15 minuti e quella che sto
				// per aggiungere
				if (userPosition.getUser().phone.equals(position.getUser().phone)
						|| (long) (System.currentTimeMillis() / 1E3)
								- userPosition.getPosition().time > 900)
					positions.remove(i);
			}
			mUsersOverlay.setPositions(positions);
		}
	};

	GenericEventHandler mPositionPurgeNeededHandler = new GenericEventHandler() {

		@Override
		public void onEvent(Object sender, EventArgs args) {
			ArrayList<UserPosition> positions = mUsersOverlay.getPositions();

			for (int i = positions.size() - 1; i >= 0; i--) {
				UserPosition userPosition = positions.get(i);
				// tolgo le posizioni più vecchie di 15 minuti e quella che sto
				// per aggiungere
				if ((long) (System.currentTimeMillis() / 1E3)
						- userPosition.getPosition().time > 900)
					positions.remove(i);
			}
			mUsersOverlay.setPositions(positions);
		}
	};

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(Const.LOG_TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging
								.getInstance(MyMapActivity.this);
					}
					regid = gcm.register(Const.SENDER_ID);

					sendRegistrationIdToBackend(new OnAsyncResponse() {

						@Override
						public void response(boolean success, String message) {
							if (success)
								MySettings.storeRegistrationId(regid);
							else {
								Helper.showMessage(MyMapActivity.this, message);
								invalidGCMStauts = true;
							}

						}
					});

				} catch (IOException ex) {

					Helper.showMessage(
							MyMapActivity.this,
							getString(
									R.string.error_registering_to_google_play_services_s,
									ex.getMessage()));
					invalidGCMStauts = true;
				}
				return null;
			}

		}.execute(null, null, null);
	}

	/**
	 * Sends the registration ID to your server over HTTP, so it can use
	 * GCM/HTTP or CCS to send messages to your app. Not needed for this demo
	 * since the device sends upstream messages to a server that echoes back the
	 * message using the 'from' address in the message.
	 */
	private void sendRegistrationIdToBackend(final OnAsyncResponse onResponse) {
		Credentials.testCredentials(this, new OnAsyncResponse() {

			@Override
			public void response(boolean success, String message) {
				if (success) {
					Credentials c = MySettings.readCredentials();
					c.setRegid(regid);
					WebRequestResult resp = HttpManager
							.saveCredentials(c, false);
					onResponse.response(resp.result, resp.message);

				} else {
					Helper.showMessage(MyMapActivity.this, message);
				}

			}
		});
	}

	private void showTrackingButton(Boolean show) {
		Button btn = (Button) findViewById(R.id.buttonLiveTrackingOff);
		if (show) {
			btn.setVisibility(View.VISIBLE);
			btn.setAnimation(mAnimation);
			mAnimation.start();
		} else {
			btn.setVisibility(View.GONE);
			btn.setAnimation(null);
			mAnimation.cancel();
		}

	}

	/** Called when the activity is first created. */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mymap);
		findViewById(R.id.buttonLiveTrackingOn).setOnClickListener(this);
		findViewById(R.id.buttonLiveTrackingOff).setOnClickListener(this);
		findViewById(R.id.buttonBook).setOnClickListener(this);
		findViewById(R.id.buttonMessage).setOnClickListener(this);
		findViewById(R.id.buttonOther).setOnClickListener(this);
		mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		enableGPS();

		// testo le credenziali
		Credentials credential = MySettings.readCredentials();
		if (credential.isEmpty()) {
			login();

		} else {
			Credentials.testCredentials(this, new OnAsyncResponse() {

				@Override
				public void response(boolean success, String message) {
					if (!success)
					{
						Helper.showMessage(MyMapActivity.this, message);
						login();
					}

				}
			});
			registerForGCM();
		}
		mTrackGPSPosition = MySettings.getTrackGPSPosition(this);

		mMap = (MapView) this.findViewById(R.id.mapview1);
		mController = mMap.getController();
		mMap.setSatellite(false);
		mMap.displayZoomControls(true);
		int zoomLevel = 15;

		List<Overlay> mapOverlays = mMap.getOverlays();
		Drawable drawable = this.getResources().getDrawable(R.drawable.marker);
		mUsersOverlay = new UserPositionOverlay(drawable, this, mMap);
		mapOverlays.add(mUsersOverlay);

		myLocationOverlay = new MITULocationOverlay(this, mMap, mController,
				mUsersOverlay);
		myLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				try {
					mController.animateTo(myLocationOverlay.getMyLocation());
				} catch (Exception ex) {
					Log.e(Const.LOG_TAG, Log.getStackTraceString(ex));
				}
			}

		});

		mapOverlays.add(myLocationOverlay);
		if (savedInstanceState != null) {

			int late6 = savedInstanceState.getInt(Const.MAPLATITUDE);
			int lone6 = savedInstanceState.getInt(Const.MAPLONGITUDE);
			mController.animateTo(new GeoPoint(late6, lone6));

			zoomLevel = savedInstanceState.getInt(Const.ZoomLevel, 15);

			Serializable positions = savedInstanceState
					.getSerializable(Const.POSITIONS);
			if (positions != null)
				mUsersOverlay.setPositions((ArrayList<UserPosition>) positions);

		} else {
			// testVersion();
			Helper.hideableMessage(this, R.string.warning_to_user);
		}

		mController.setZoom(zoomLevel);
		MyApplication.getInstance().ConnectorServiceChanged
				.addHandler(mConnectorServiceChangedHandler);

		mAnimation = new AlphaAnimation(1, 0.5f);
		// from
		// fully
		// visible
		// to
		// invisible
		mAnimation.setDuration(300);
		mAnimation.setInterpolator(new LinearInterpolator()); // do not alter
																// animation
																// rate
		mAnimation.setRepeatCount(Animation.INFINITE); // Repeat animation
														// infinitely
		mAnimation.setRepeatMode(Animation.REVERSE); // Reverse animation at the
														// end so the button
														// will
														// fade back in

		showTrackingButton(isLiveTracking());
	}

	private void login() {
		// non ho le credenziali: le chiedo e contestualmente le valido (se
		// sono online
		// facendo una login, altrimenti controllando che non siano vuote),
		// se non sono buone esco
		Intent intent = new Intent(MyMapActivity.this, LoginActivity.class);
		startActivityForResult(intent, Const.LOGIN_RESULT);
	}

	private void registerForGCM() {
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(this);
			regid = MySettings.getRegistrationId();

			if (Helper.isNullOrEmpty(regid)) {
				registerInBackground();
			}
		} else {
			Helper.showMessage(this,
					getString(R.string.no_valid_google_play_services_apk_found));
		}
	}

	@Override
	protected void onDestroy() {
		MyApplication.getInstance().ConnectorServiceChanged
				.removeHandler(mConnectorServiceChangedHandler);
		super.onDestroy();
	}

	private void enableGPS() {
		if (!mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

			Helper.dialogMessage(this, R.string.need_gps, R.string.app_name,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							Intent myIntent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							startActivityForResult(myIntent,
									Const.ACTIVATE_GPS_RESULT);
							return;

						}
					}, null);

			return;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Const.ACTIVATE_GPS_RESULT) {
			Toast.makeText(this, R.string.gps_enabled, Toast.LENGTH_SHORT)
					.show();
		} else if (requestCode == Const.LOGIN_RESULT) {
			if (resultCode == RESULT_OK) {
				
				registerForGCM();
			} else
				finish();

		} else if (requestCode == Const.SEARCH_ACTIVITY_RESULT
				|| requestCode == Const.BOOK_RESULT) {
			if (resultCode == RESULT_OK) {
				if (!data.hasExtra(Const.USER)) {
					// success but no user? means 'search again'
					onSearchRequested();
				} else {
					contactUser((User) data.getSerializableExtra(Const.USER),
							data.getStringExtra(Const.PASSWORD));
				}
			}

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void contactUser(final User user, final String pwd) {
		ConnectorService connectorService = MyApplication.getInstance()
				.getConnectorService();
		if (connectorService != null && connectorService.existUser(user)) {
			Helper.showMessage(this,
					getString(R.string._s_has_already_been_connected, user));
			return;
		}
		final ProgressDialog progressBar = new ProgressDialog(this);
		progressBar.setCancelable(true);
		progressBar.setMessage(getString(R.string.sending_request_));
		progressBar.setIndeterminate(true);
		progressBar.show();
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				Credentials.testCredentials(MyMapActivity.this,
						new OnAsyncResponse() {

							@Override
							public void response(boolean success, String message) {
								if (success) {
									WebRequestResult contactUser = HttpManager.contactUser(
											user.phone,
											Helper.isNullOrEmpty(pwd) ? Const.NULL_TOKEN
													: Helper.encrypt(pwd));
									if (contactUser.result) {
										Helper.showMessage(
												MyMapActivity.this,
												getString(
														R.string.your_request_to_s_has_been_sent,
														user));
										mUsersOverlay.pinTo(user);
										//se non posso ricevere messaggi, mi connetto subito, 
										//altrimenti mi connetter?ando ricevo conferma
										if (invalidGCMStauts) {
											ConnectorService.activate(
													MyMapActivity.this, user,
													true, false);
										}

									} else {
										Helper.showMessage(MyMapActivity.this,
												contactUser.message);
									}
								} else
									Helper.showMessage(MyMapActivity.this,
											message);
							}
						});

				return null;
			}

			protected void onPostExecute(Void result) {
				progressBar.dismiss();
			};
		}.execute(null, null, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mymap_menu, menu);
		// mMenuItemTrackGpsPosition = menu.findItem(R.id.itemTrackGpsPosition);
		// mMenuItemTrackGpsPosition
		// .setTitleCondensed(getString(mTrackGPSPosition ?
		// R.string.hide_position_menu
		// : R.string.show_position_menu));

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemRequestUserPosition:
			startBookContactUser();
		 break;
		case R.id.itemAccount: {
			Intent intent = new Intent(this, UserActivity.class);
			intent.putExtra(UserActivity.REGISTER_USER, false);
			startActivity(intent);
			return true;
		}
		case R.id.itemNewAccount: {
			Intent intent = new Intent(this, UserActivity.class);
			intent.putExtra(UserActivity.REGISTER_USER, true);
			startActivityForResult(intent, Const.LOGIN_RESULT);
			return true;
		}
		case R.id.itemLogin: {
			login();
			return true;
		}
		
		case R.id.itemBook: {
			startBook();
			return true;
		}
		case R.id.itemSendMessage: {
			startBookMessage();
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	private void startBook() {
		Intent intent = new Intent(this, BookActivity.class);
		startActivityForResult(intent, Const.BOOK_RESULT);
	}

	@SuppressWarnings("unused")
	private void setTrackGPSPosition(boolean b) {
		mTrackGPSPosition = b;

		MySettings.setTrackGPSPosition(this, mTrackGPSPosition);
		//mMenuItemTrackGpsPosition
		//		.setTitleCondensed(getString(mTrackGPSPosition ? R.string.hide_position_menu
		//				: R.string.show_position_menu));

		if (mTrackGPSPosition)
			myLocationOverlay.enableMyLocation();
		else
			myLocationOverlay.disableMyLocation();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		try {
			outState.putInt(Const.MAPLATITUDE, mMap.getMapCenter()
					.getLatitudeE6());
			outState.putInt(Const.MAPLONGITUDE, mMap.getMapCenter()
					.getLongitudeE6());
			outState.putInt(Const.ZoomLevel, mMap.getZoomLevel());
			outState.putSerializable(Const.POSITIONS,
					mUsersOverlay.getPositions());
		} catch (Exception e) {
			Log.e(Const.LOG_TAG, Log.getStackTraceString(e));
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
		// myLocationOverlay.disableCompass();
		MyApplication.getInstance().unregisterForPositions(
				mPositionAvailableHandler, mPositionReceivedHandler,
				mPositionPurgeNeededHandler);

	}

	@Override
	protected void onResume() {
		super.onResume();
		checkPlayServices();
		if (mTrackGPSPosition)
			myLocationOverlay.enableMyLocation();
		// myLocationOverlay.enableCompass();
		MyApplication.getInstance().registerForPositions(
				mPositionAvailableHandler, mPositionReceivedHandler,
				mPositionPurgeNeededHandler);

	}

	public boolean isLiveTracking() {
		ConnectorService connectorService = MyApplication.getInstance()
				.getConnectorService();
		return connectorService != null;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.buttonLiveTrackingOn) {

			startBookContactUser();

		} else if (v.getId() == R.id.buttonLiveTrackingOff) {
			if (isLiveTracking())
				Helper.dialogMessage(this,
						R.string.do_you_want_to_stop_tracking_all_users,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								startService(new Intent(MyMapActivity.this,
										ConnectorService.class));
							}
						}, null);
		} else if (v.getId() == R.id.buttonMessage) {
			startBookMessage();
		} else if (v.getId() == R.id.buttonBook) {
			startBook();
		} else if (v.getId() == R.id.buttonOther) {
			openOptionsMenu();
		}
	}

	private void startBookContactUser() {
		Intent intent = new Intent(this, BookActivity.class);
		intent.putExtra(Const.COMMAND_ID, R.id.itemRequestUserPosition);
		startActivityForResult(intent, Const.BOOK_RESULT);
	}

	private void startBookMessage() {
		Intent intent = new Intent(this, BookActivity.class);
		intent.putExtra(Const.COMMAND_ID, R.id.itemSendMessage);
		startActivity(intent);
	}

}