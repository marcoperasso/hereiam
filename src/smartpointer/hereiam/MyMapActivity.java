package smartpointer.hereiam;

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
	private HIALocationOverlay myLocationOverlay;
	private boolean mTrackGPSPosition;

	MenuItem mMenuItemTrackGpsPosition;

	private LocationManager mlocManager;

	private Animation mAnimation;

	private GoogleCloudMessaging gcm;
	private String regid;

	private GenericEventHandler mConnectorServiceChangedHandler = new GenericEventHandler() {

		@Override
		public void onEvent(Object sender, EventArgs args) {
			showTrackingButton();
		}
	};

	private GenericEventHandler mPinnedUserChangedHandler = new GenericEventHandler() {

		@Override
		public void onEvent(Object sender, EventArgs args) {
			if (MyApplication.getInstance().getPinnedUser() == null)
				mUsersOverlay.hideBalloon();
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
				if (userPosition.getUser().phone
						.equals(position.getUser().phone)
						|| Helper.getUnixTime()
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
				if (Helper.getUnixTime() - userPosition.getPosition().time > 900)
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
					if (Helper.isNullOrEmpty(regid))
						throw new Exception("Invalid GCM registration ID");
					sendRegistrationIdToBackend(new OnAsyncResponse() {

						@Override
						public void response(boolean success, String message) {
							if (success) {
								Credentials c = MySettings.readCredentials();
								c.setRegid(regid);
								MySettings.setCredentials(c);
							} else {
								Helper.showMessage(MyMapActivity.this, message);
							}

						}
					});

				} catch (Exception ex) {

					Helper.showMessage(
							MyMapActivity.this,
							getString(
									R.string.error_registering_to_google_play_services_s,
									ex.getMessage()));
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
					WebRequestResult resp = HttpManager.saveCredentials(c,
							false);
					onResponse.response(resp.result, resp.message);

				} else {
					Helper.showMessage(MyMapActivity.this, message);
				}

			}
		});
	}

	private void showTrackingButton() {
		Boolean show = MyApplication.getInstance().getConnectorService() != null;
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

		// testo le credenziali
		Credentials credential = MySettings.readCredentials();
		if (credential.isEmpty()) {
			login();

		} else {
			Credentials.testCredentials(this, new OnAsyncResponse() {

				@Override
				public void response(boolean success, String message) {
					if (!success) {
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

		myLocationOverlay = new HIALocationOverlay(this, mMap, mController,
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
			enableGPS();
			// testVersion();
			Helper.hideableMessage(this, R.string.warning_to_user);
		}

		mController.setZoom(zoomLevel);
		MyApplication.getInstance().ConnectorServiceChanged
				.addHandler(mConnectorServiceChangedHandler);
		MyApplication.getInstance().PinnedUserChanged
				.addHandler(mPinnedUserChangedHandler);
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

		showTrackingButton();

		// Look up the AdView as a resource and load a request.
		/*
		 * AdView adView = (AdView)this.findViewById(R.id.ad); AdRequest
		 * adRequest = new AdRequest();
		 * adRequest.addTestDevice("867101E88DC7B800CE0B950145A98812");
		 * adView.loadAd(adRequest);
		 */

		super.onCreate(savedInstanceState);
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
			regid = MySettings.readCredentials().getRegid();

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
		MyApplication.getInstance().PinnedUserChanged
				.removeHandler(mPinnedUserChangedHandler);
		super.onDestroy();
	}

	private void enableGPS() {
		if (!mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Helper.hideableQuestion(this, new IFinishCallback() {

				@Override
				public void finished() {
					Intent myIntent = new Intent(
							Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivityForResult(myIntent, Const.ACTIVATE_GPS_RESULT);

				}
			}, null, R.string.need_gps);

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Const.ACTIVATE_GPS_RESULT) {
			if (mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				Toast.makeText(this, R.string.gps_enabled, Toast.LENGTH_SHORT)
						.show();
			}
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
		if (connectorService != null && connectorService.existWatchedUser(user)) {
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
				try {
					progressBar.dismiss();
				} catch (Exception e) {

				}
			};
		}.execute(null, null, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mymap_menu, menu);
		mMenuItemTrackGpsPosition = menu.findItem(R.id.itemShowMyPosition);
		updateMenuTitle();

		return super.onCreateOptionsMenu(menu);
	}

	private void updateMenuTitle() {
		String string = getString(mTrackGPSPosition ? R.string.hide_my_position
				: R.string.show_my_position);
		mMenuItemTrackGpsPosition.setTitleCondensed(string);
		mMenuItemTrackGpsPosition.setTitle(string);
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
		case R.id.itemShowMyPosition: {
			toggleMyPositionVisibility();
			return true;
		}

		}
		return super.onOptionsItemSelected(item);
	}

	private void toggleMyPositionVisibility() {
		setTrackGPSPosition(!MySettings.getTrackGPSPosition(this));

	}

	private void startBook() {
		Intent intent = new Intent(this, BookActivity.class);
		startActivityForResult(intent, Const.BOOK_RESULT);
	}

	private void setTrackGPSPosition(boolean b) {
		mTrackGPSPosition = b;

		MySettings.setTrackGPSPosition(this, mTrackGPSPosition);
		updateMenuTitle();

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
		myLocationOverlay.disableCompass();
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
		myLocationOverlay.enableCompass();
		MyApplication.getInstance().registerForPositions(
				mPositionAvailableHandler, mPositionReceivedHandler,
				mPositionPurgeNeededHandler);

	}

	public ArrayList<User> getWatchedUsers() {
		ConnectorService connectorService = MyApplication.getInstance()
				.getConnectorService();
		return connectorService == null ? new ArrayList<User>()
				: connectorService.getWatchedUsers();
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
			if (MyApplication.getInstance().getConnectorService() != null)
				Helper.dialogMessage(this,
						R.string.do_you_want_to_stop_tracking_all_users,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// senza argomenti, stoppa il servizio
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