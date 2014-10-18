package smartpointer.hereiam;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Application;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

public class MyApplication extends Application {

	private static final int MAX_POINTS = 1000;
	private static final int REMOVE_POINTS = (int) (MAX_POINTS * .2);
	public static final boolean LogEnabled = BuildConfig.DEBUG;

	private static MyApplication sInstance;

	private LinkedList<GeoAddress> points = new LinkedList<GeoAddress>();
	private ConnectorService connectorService;
	Event ConnectorServiceChanged = new Event();
	Event PinnedUserChanged = new Event();
	private Users users;
	private Object userTicket = new Object();
	private PositionsDownloader mPositionsDownloader = new PositionsDownloader(
			this);
	private PositionsReceivedEvent positionReceived = new PositionsReceivedEvent();
	private PositionsDownloadedEvent positionsDownloaded = new PositionsDownloadedEvent();
	private Event positionsPurgeNeeded = new Event();

	private User pinnedUser;
	private UserMessagesActivity userMessagesActivity;

	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;

	}

	@Override
	public void onTerminate() {
		sInstance = null;
		super.onTerminate();
	}

	public static MyApplication getInstance() {
		return sInstance;
	}

	public void setConnectorService(ConnectorService connectorService) {
		boolean changed = connectorService != this.connectorService;
		this.connectorService = connectorService;
		if (changed)
			ConnectorServiceChanged.fire(this, EventArgs.Empty);
	}

	public ConnectorService getConnectorService() {
		return this.connectorService;

	}
	
	public void notifyUserDisconnection(final User user) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				Credentials.testCredentials(MyApplication.this,
						new OnAsyncResponse() {
							@Override
							public void response(boolean success, String message) {
								if (success) {
									HttpManager.disconnectUser(user.phone);

								}
							}
						});

				return null;
			}
		}.execute(null, null, null);

	}
	
	public void requestUserDisconnection(final User user) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				Credentials.testCredentials(MyApplication.this,
						new OnAsyncResponse() {
							@Override
							public void response(boolean success, String message) {
								if (success) {
									HttpManager.requestUserDisconnection(user.phone);

								}
							}
						});

				return null;
			}
		}.execute(null, null, null);

	}

	void respondToUser(final String phone, final int response) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				Credentials.testCredentials(MyApplication.this,
						new OnAsyncResponse() {
							@Override
							public void response(boolean success, String message) {
								if (success) {
									HttpManager.respondToUser(phone, response);
								}
							}
						});

				return null;
			}
		}.execute(null, null, null);

	}

	void pingBack(final String phone) {
		new AsyncTask<Void, Void, WebRequestResult>() {

			@Override
			protected WebRequestResult doInBackground(Void... params) {
									return HttpManager.pingBack(phone);
			}
		}.execute(null, null, null);

	}
	public String reverseGeocode(double lat, double lon) {
		synchronized (points) {
			for (int i = points.size() - 1; i >= 0; i--) {
				GeoAddress geoAddress = points.get(i);
				if (geoAddress.isNear(lat, lon)) {
					if (i < points.size() - 1) {
						points.remove(i);
						points.add(geoAddress);
					}
					return geoAddress.getAddress();
				}
			}
			Geocoder geocoder = new Geocoder(this);

			List<Address> addresses;
			try {
				addresses = geocoder.getFromLocation(lat / 1000000,
						lon / 1000000, 1);

			} catch (Exception e) {
				return getString(R.string.cannot_get_address);
			}

			if (addresses != null) {
				Address returnedAddress = addresses.get(0);
				StringBuilder strReturnedAddress = new StringBuilder();
				for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
					if (i > 0)
						strReturnedAddress.append(" - ");
					strReturnedAddress
							.append(returnedAddress.getAddressLine(i));
				}

				String string = strReturnedAddress.toString();
				points.add(new GeoAddress(lat, lon, string));
				if (points.size() > MAX_POINTS) {
					for (int i = 0; i < REMOVE_POINTS; i++)
						points.removeFirst();
				}
				return string;
			}

			return getString(R.string.cannot_get_address);
		}
	}

	public Users getUsers() {
		synchronized (userTicket) {
			if (users == null) {
				users = new Users(this);
			}
			return users;
		}
	}

	public void invalidateUsers() {
		synchronized (userTicket) {
			users = null;
		}

	}

	public void unregisterForPositions(
			PositionsDownloadedEventHandler downloadHandler,
			PositionReceivedEventHandler receiveHandler,
			GenericEventHandler positionPurgeNeededHandler) {
		mPositionsDownloader.stop();

		positionsDownloaded.removeHandler(downloadHandler);

		positionReceived.addHandler(receiveHandler);

		positionsPurgeNeeded.addHandler(positionPurgeNeededHandler);
	}

	public void registerForPositions(
			PositionsDownloadedEventHandler downloadHandler,
			PositionReceivedEventHandler receiveHandler,
			GenericEventHandler positionPurgeNeededHandler) {
		mPositionsDownloader.start();
		positionsDownloaded.addHandler(downloadHandler);
		positionReceived.addHandler(receiveHandler);
		positionsPurgeNeeded.addHandler(positionPurgeNeededHandler);
	}

	public void receivedPosition(UserPosition pos) {
		positionReceived.fire(this, pos);
		mPositionsDownloader.restart();// fa ripartire il timer per downloadare
										// le posizioni
	}

	public void purgePositions() {
		positionsPurgeNeeded.fire(this, EventArgs.Empty);
	}

	public void downloadedPositions(ArrayList<UserPosition> positions) {
		positionsDownloaded.fire(this, positions);

	}

	public User getPinnedUser() {
		return pinnedUser;
	}

	public void setPinnedUser(User pinnedUser) {
		boolean changed = this.pinnedUser != pinnedUser;
		this.pinnedUser = pinnedUser;
		if (changed)
			PinnedUserChanged.fire(this, EventArgs.Empty);
	}

	public void setMessagesActivity(UserMessagesActivity userMessagesActivity) {
		this.userMessagesActivity = userMessagesActivity;
	}

	public UserMessagesActivity getMessagesActivity(User fromUser) {
		if (userMessagesActivity != null
				&& fromUser.equals(userMessagesActivity.getUser()))
			return userMessagesActivity;
		return null;
	}
}

class GeoAddress {

	private static final int MIN_DISTANCE_METRES = 5;
	private double lat;
	private double lon;
	private String address;

	public GeoAddress(double lat, double lon, String address) {
		this.lat = lat;
		this.lon = lon;
		this.address = address;
	}

	public boolean isNear(double lat2, double lon2) {
		double calculateGreatCircleDistance = GpsPoint
				.calculateGreatCircleDistance(lat, lon, lat2, lon2);
		return calculateGreatCircleDistance < MIN_DISTANCE_METRES; // metres;
	}

	public String getAddress() {
		return address;
	}
}