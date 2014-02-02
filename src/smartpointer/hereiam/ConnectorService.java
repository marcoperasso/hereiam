package smartpointer.hereiam;

import java.util.ArrayList;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * @author perasso
 * 
 */
public class ConnectorService extends Service implements LocationListener {

	public static final int DISTANCE_METERS = 40;

	private LocationManager mlocManager;
	private Thread mWorkerThread;
	private Handler mHandler;
	private ArrayList<User> users = new ArrayList<User>();

	// procedura di invio della posizione corrente
	private long sendLatestPositionInterval = 30000;// 30 secondi
	private Runnable sendLatestPositionProcedureRunnable;

	private Location mLocation;
	private NotificationManager mNotificationManager;

	public ConnectorService() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getExtras() == null) {
			if (MyApplication.LogEnabled)
				Log.d(Const.LOG_TAG,
						"Receiving connector service start command with no data, the service will be stopped");
			stopSelf();
			return super.onStartCommand(intent, flags, startId);
		}
		final ConnectorServiceCommand command = (ConnectorServiceCommand) intent
				.getSerializableExtra(Const.COMMAND);
		if (command == null) {
			if (MyApplication.LogEnabled)
				Log.d(Const.LOG_TAG,
						"Receiving connector service start command with no data, the service will be stopped");
			stopSelf();
			return super.onStartCommand(intent, flags, startId);
		}
		
		if (mWorkerThread == null) {
			mWorkerThread = new Thread(new Runnable() {
				public void run() {
					Looper.prepare();

					mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mHandler = new Handler();
					mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					execute(command);

					sendLatestPositionProcedure();
					Looper.loop();

					mlocManager.removeUpdates(ConnectorService.this);
					mNotificationManager.cancel(Const.TRACKING_NOTIFICATION_ID);
					for (User user : users)
						MyApplication.getInstance().notifyUserDisconnection(user);
					
					if (MyApplication.LogEnabled)
						Log.i(Const.LOG_TAG,
								"Finished connector service worker thread");

				}
			});
			mWorkerThread.setDaemon(true);
			mWorkerThread.setName("Connector Service Worker");
			mWorkerThread.start();
		} else {
			execute(command);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void execute(final ConnectorServiceCommand command) {
		mHandler.post(new Runnable() {
			public void run() {
				if (command.connect)
					addUser(command.user, command.silent);
				else
					removeUser(command.user);
			}
		});
	}

	private void addUser(final User user, boolean silent) {
		if (users.isEmpty()) {
			
			mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					6000, 5, ConnectorService.this);
			mlocManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 6000, 5,
					ConnectorService.this);
		}
		for (int i = 0; i < users.size(); i++)
			if (users.get(i).id == user.id) {
				return;
			}
		users.add(user);
		setGPSOnNotification(silent);

	}

	private void removeUser(final User user) {
		for (int i = 0; i < users.size(); i++)
			if (users.get(i).id == user.id) {
				users.remove(i);
				break;
			}
		if (users.isEmpty()) {
			mlocManager.removeUpdates(ConnectorService.this);
			stopSelf();
		}
		else
		{
			setGPSOnNotification(false);
		}

	}

	public void onCreate() {
		if (MyApplication.LogEnabled)
			Log.i(Const.LOG_TAG, "Starting connector service");

		sendLatestPositionProcedureRunnable = new Runnable() {
			public void run() {
				sendLatestPositionProcedure();
			}
		};
		MyApplication.getInstance().setConnectorService(this);
		super.onCreate();
	}

	public void onLocationChanged(Location location) {

		mLocation = location;
	}

	public void onProviderEnabled(String provider) {
		if (MyApplication.LogEnabled)
			Log.i(Const.LOG_TAG, "GPS enabled");
	}

	public void onProviderDisabled(String provider) {
		if (MyApplication.LogEnabled)
			Log.i(Const.LOG_TAG, "GPS disabled");
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {

		switch (status) {
		case LocationProvider.AVAILABLE:
			break;
		case LocationProvider.OUT_OF_SERVICE:
			mLocation = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			mLocation = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			break;
		}
	}

	private void sendLatestPositionProcedure() {
		sendLatestPosition();
		mHandler.postDelayed(sendLatestPositionProcedureRunnable,
				sendLatestPositionInterval);

	}

	private void setGPSOnNotification(boolean silent) {
		String message = getString(R.string.sending_position, getUsersList());
		Intent intent = new Intent(this, TrackedUsersActivity.class);
		intent.putExtra(Const.USERS, users);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, // add
																				// this
				PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_livetracking)
				.setContentTitle(getString(R.string.app_name))
				.setStyle(new NotificationCompat.BigTextStyle()
        .bigText(message))
				.setContentText(message).setContentIntent(contentIntent);

		Notification notification = mBuilder.build();
		if (!silent)
			notification.defaults |= Notification.DEFAULT_ALL;
		mNotificationManager.notify(Const.TRACKING_NOTIFICATION_ID,
				notification);
	}

	private Object getUsersList() {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			if (s.length() > 0)
				s.append(", ");
			s.append(user.toString());
		}
		return s.toString();
	}

	private void sendLatestPosition() {
		if (mLocation == null || users.size() == 0
				|| !Helper.isOnline(ConnectorService.this))
			return;

		Integer[] ids = new Integer[users.size()];
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			ids[i] = user.id;
		}
		
		Credentials currentCredentials = MySettings.CurrentCredentials;
		final MyPosition loc = new MyPosition(currentCredentials == null ? 0
				: currentCredentials.getId(), ids,
				(int) (mLocation.getLatitude() * 1E6),
				(int) (mLocation.getLongitude() * 1E6),
				(long) (System.currentTimeMillis() / 1E3),
				LocationManager.GPS_PROVIDER.equals(mLocation.getProvider())
				);
		new Thread() {
			@Override
			public void run() {
				try {
					if (HttpManager.sendPositionData(loc)) {
						if (MyApplication.LogEnabled)
							Log.d(Const.LOG_TAG,
									"Position data succesfully sent.");
						mLocation = null;
					}
				} catch (Exception e) {
					Log.e(Const.LOG_TAG, e.toString());
				}

				super.run();
			}
		}.start();

	}

	@Override
	public void onDestroy() {
		MyApplication.getInstance().setConnectorService(null);
		if (mHandler != null) {
			mHandler.post(new Runnable() {

				public void run() {
					Looper.myLooper().quit();

				}
			});
		}
		try {
			if (mWorkerThread != null)
				mWorkerThread.join();
		} catch (InterruptedException e) {
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static void activate(Context context, User user, boolean activate, boolean silent) {
		Intent intent1 = new Intent(context,
				ConnectorService.class);
		intent1.putExtra(Const.COMMAND,
				new ConnectorServiceCommand(user, activate, silent));
		context.startService(intent1);
		
	}

	public boolean isLiveTracking() {
		return users.size() > 0;
	}
	
	

}
