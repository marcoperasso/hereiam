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
	private long getLatestPositionInterval = 60000; //il server fa pulizia ogni 15 minuti, io invio ogni 1 minuto
	private Runnable sendLatestPositionProcedureRunnable = new Runnable() {
		public void run() {
			sendLatestPositionProcedure();
		}
	};
	//se ho degli utenti connessi ma il gps non prende, ogni tot devo impostare la posizione
	//in base al network provider anche se non è cambiata, altrimenti
	//dopo un po' sul server viene cancellata
	private Runnable getLatestPositionRunnable = new Runnable() {
		public void run() {
			if (mLocation == null && users.size() > 0)
				mLocation = mlocManager
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);;
		}
	};

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
					if (mLocation == null)
					{
						mLocation = mlocManager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					}
					execute(command);
					mHandler.post(sendLatestPositionProcedureRunnable);
					
					Looper.loop();

					mlocManager.removeUpdates(ConnectorService.this);
					mHandler.removeCallbacks(getLatestPositionRunnable);
					mHandler.removeCallbacks(sendLatestPositionProcedureRunnable);
					mNotificationManager.cancel(Const.TRACKING_NOTIFICATION_ID);
					for (User user : users)
						MyApplication.getInstance().notifyUserDisconnection(
								user);
					MyApplication.getInstance().setPinnedUser(null);
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
		for (int i = 0; i < users.size(); i++) {
			User user2 = users.get(i);
			if (user2.equals(user)) {
				MyApplication.getInstance().setPinnedUser(user2);
				return;
			}
		}
		users.add(user);
		MyApplication.getInstance().setPinnedUser(user);
		
		setGPSOnNotification(silent);

	}

	boolean existUser(final User user) {
		for (int i = 0; i < users.size(); i++)
			if (users.get(i).phone.equals(user.phone)) {
				return true;
			}
		return false;

	}
	private void removeUser(final User user) {
		for (int i = 0; i < users.size(); i++)
			if (users.get(i).equals(user)) {
				users.remove(i);
				break;
			}
		if (users.isEmpty()) {
			stopSelf();
			MyApplication.getInstance().setPinnedUser(null);
		} else {
			setGPSOnNotification(false);
			MyApplication.getInstance().setPinnedUser(users.get(users.size() - 1));
		}

	}

	public void onCreate() {
		if (MyApplication.LogEnabled)
			Log.i(Const.LOG_TAG, "Starting connector service");

		
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
			mLocation = mlocManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			mLocation = mlocManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			break;
		}
	}

	private void sendLatestPositionProcedure() {
		sendLatestPosition();
		resetTimeoutForGettingPosition();
		mHandler.postDelayed(sendLatestPositionProcedureRunnable,
				sendLatestPositionInterval);

	}

	private void resetTimeoutForGettingPosition() {
		mHandler.removeCallbacks(getLatestPositionRunnable);
		mHandler.postDelayed(getLatestPositionRunnable,
				getLatestPositionInterval);
	}

	private void setGPSOnNotification(boolean silent) {
		String message = getString(R.string.sending_position, getUsersList());
		Intent intent = new Intent(this, TrackedUsersActivity.class);
		intent.putExtra(Const.USERS, users);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, // add
						// this
				PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getString(R.string.app_name))
				.setStyle(
						new NotificationCompat.BigTextStyle().bigText(message))
				.setContentText(message).setContentIntent(contentIntent);

		Notification notification = mBuilder.build();
		if (!silent)
			notification.defaults |= Notification.DEFAULT_ALL;
		mNotificationManager.notify(Const.TRACKING_NOTIFICATION_ID,
				notification);
	}

	private String getUsersList() {
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

		String[] phones = new String[users.size()];
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			phones[i] = user.phone;
		}

		Credentials currentCredentials = MySettings.readCredentials();
		final MyPosition loc = new MyPosition(currentCredentials == null ? ""
				: currentCredentials.getPhone(), phones,
				(int) (mLocation.getLatitude() * 1E6),
				(int) (mLocation.getLongitude() * 1E6),
				Helper.getUnixTime(),
				LocationManager.GPS_PROVIDER.equals(mLocation.getProvider()));
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

	public static void activate(Context context, User user, boolean activate,
			boolean silent) {
		Intent intent1 = new Intent(context, ConnectorService.class);
		intent1.putExtra(Const.COMMAND, new ConnectorServiceCommand(user,
				activate, silent));
		context.startService(intent1);

	}
}
