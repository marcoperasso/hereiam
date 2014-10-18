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
	private ArrayList<User> watchedUsers = new ArrayList<User>();// questa
	// lista va
	// sincronizzata
	// perché
	// acceduta
	// da più
	// thread
	private ArrayList<User> watchingUsers = new ArrayList<User>();// questa
																	// lista va
																	// sincronizzata
																	// perché
																	// acceduta
																	// da più
																	// thread

	// procedura di invio della posizione corrente
	private long sendLatestPositionInterval = 30000;// 30 secondi
	private Runnable sendLatestPositionProcedureRunnable = new Runnable() {
		public void run() {
			sendLatestPositionProcedure();
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
					if (mLocation == null) {
						mLocation = mlocManager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					}
					execute(command);
					mHandler.post(sendLatestPositionProcedureRunnable);

					Looper.loop();

					mlocManager.removeUpdates(ConnectorService.this);
					mHandler.removeCallbacks(sendLatestPositionProcedureRunnable);
					mNotificationManager.cancel(Const.TRACKING_NOTIFICATION_ID);
					synchronized (this) {
						for (User user : watchingUsers)
							MyApplication.getInstance()
									.notifyUserDisconnection(user);
						for (User user : watchedUsers)
							MyApplication.getInstance()
									.requestUserDisconnection(user);
					}
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
				switch (command.type) {
				case START_RECEIVING_USER_POSITION:
					addWatchedUser(command.user, command.silent);
					break;
				case START_SENDING_MY_POSITION:
					addWatchingUser(command.user, command.silent);
					break;
				case STOP_RECEIVING_USER_POSITION:
					removeWatchedUser(command.user);
					break;
				case STOP_SENDING_MY_POSITION:
					removeWatchingUser(command.user);
					break;
				default:
					break;
				}

			}
		});
	}

	private void addWatchedUser(final User user, boolean silent) {
		synchronized(this)
		{
		for (int i = 0; i < watchedUsers.size(); i++) {
			User user2 = watchedUsers.get(i);
			if (watchedUsers.equals(user)) {
				MyApplication.getInstance().setPinnedUser(user2);
				return;
			}
		}
		watchedUsers.add(user);
		}
		MyApplication.getInstance().setPinnedUser(user);
		
	}

	private void addWatchingUser(final User user, boolean silent) {
		synchronized (this) {
			if (watchingUsers.isEmpty()) {
				mlocManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 6000, 5,
						ConnectorService.this);
				mlocManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 6000, 5,
						ConnectorService.this);
			}

			watchingUsers.add(user);
		}
		setSendingPositionNotification(silent);

	}

	boolean existWatchedUser(final User user) {
		synchronized (this) {
		for (int i = 0; i < watchedUsers.size(); i++)
			if (watchedUsers.get(i).phone.equals(user.phone)) {
				return true;
			}
		return false;
		}
	}

	private void removeWatchedUser(final User user) {
		synchronized (this) {
			
		for (int i = 0; i < watchedUsers.size(); i++)
			if (watchedUsers.get(i).equals(user)) {
				watchedUsers.remove(i);
				break;
			}
		if (watchedUsers.isEmpty()) {
			MyApplication.getInstance().setPinnedUser(null);
			if (watchingUsers.isEmpty())// se anche l'altra lista di utenti è
										// vuota, posso spegnere il servizio
				stopSelf();
		} else {
			MyApplication.getInstance().setPinnedUser(
					watchedUsers.get(watchedUsers.size() - 1));
		}
		}
	}

	private void removeWatchingUser(final User user) {
		synchronized (this) {
			for (int i = 0; i < watchingUsers.size(); i++)
				if (watchingUsers.get(i).equals(user)) {
					watchingUsers.remove(i);
					break;
				}
			if (!watchingUsers.isEmpty())
			{
				//ho ancora utenti a cui mando la posizione:
				//aggiorno il messaggio di notifica
				setSendingPositionNotification(false);
			} else if (watchedUsers.isEmpty()) {// se non ho
												// più
												// utenti,
												// posso
												// spegnere
												// il
												// servizio
				stopSelf();
			}
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
		mHandler.postDelayed(sendLatestPositionProcedureRunnable,
				sendLatestPositionInterval);

	}

	private void setSendingPositionNotification(boolean silent) {
		String message = getString(R.string.sending_position,
				getWatchingUsersList());
		Intent intent = new Intent(this, WatchingUsersActivity.class);
		intent.putExtra(Const.USERS, getWatchingUsers());
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

	private String getWatchingUsersList() {
		synchronized (this) {
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < watchingUsers.size(); i++) {
				User user = watchingUsers.get(i);
				if (s.length() > 0)
					s.append(", ");
				s.append(user.toString());
			}
			return s.toString();
		}
	}

	private void sendLatestPosition() {
		String[] phones = null;
		synchronized (this) {

			if (watchingUsers.size() == 0
					|| !Helper.isOnline(ConnectorService.this))
				return;
			if (mLocation == null)
				mLocation = mlocManager
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

			if (mLocation == null)
				return;

			phones = new String[watchingUsers.size()];
			for (int i = 0; i < watchingUsers.size(); i++) {
				User user = watchingUsers.get(i);
				phones[i] = user.phone;
			}
		}
		Credentials currentCredentials = MySettings.readCredentials();
		final MyPosition loc = new MyPosition(currentCredentials == null ? ""
				: currentCredentials.getPhone(), phones,
				(int) (mLocation.getLatitude() * 1E6),
				(int) (mLocation.getLongitude() * 1E6), Helper.getUnixTime(),
				LocationManager.GPS_PROVIDER.equals(mLocation.getProvider()));
		mLocation = null;
		new Thread() {
			@Override
			public void run() {
				try {
					if (HttpManager.sendPositionData(loc)) {
						if (MyApplication.LogEnabled)
							Log.d(Const.LOG_TAG,
									"Position data succesfully sent.");
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

	public static void activate(Context context, User user, boolean silent,
			CommandType commandType) {
		Intent intent1 = new Intent(context, ConnectorService.class);
		intent1.putExtra(Const.COMMAND, new ConnectorServiceCommand(user,
				silent, commandType));
		context.startService(intent1);

	}

	public ArrayList<User> getWatchingUsers() {
		ArrayList<User> users = new ArrayList<User>();
		synchronized (this) {
			users.addAll(watchingUsers);
		}

		return users;
	}

	public ArrayList<User> getWatchedUsers() {
			
		ArrayList<User> users = new ArrayList<User>();
		synchronized (this) {
			users.addAll(watchedUsers);
		}

		return users;
		
	}
}
