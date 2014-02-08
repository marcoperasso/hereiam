package smartpointer.hereiam;

import java.util.Hashtable;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class MySettings {
	public static final String PREFS_NAME = "i";
	public static final String TRACK_GPS = "l";
	public static final String MAX_TASK_ID = "m";
	public static final String VISIBLE_ROUTES = "r";
	public static final String LATEST_SYNC = "q";
	public static final String EMAIL = "o";
	public static final String PASSWORD = "p";
	public static final String USERID = "uid";
	public static final String ID = "id";
	private static final String HIDDEN_MESSAGE_ = "hm_";
	private static final String NAME = "n";
	private static final String SURNAME = "s";
	private static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final String PROPERTY_REG_ID_USER = "registration_user_id";

	private static Credentials credentials;

	public static boolean getTrackGPSPosition(Context context) {
		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		return settings.getBoolean(TRACK_GPS, true);
	}

	public static void setTrackGPSPosition(Context context, boolean b) {
		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(TRACK_GPS, b);
		editor.commit();
	}

	public static Credentials readCredentials() {
		if (credentials == null) {
			SharedPreferences settings = MyApplication.getInstance().getSharedPreferences(
					PREFS_NAME, 0);

			String pwd = settings.getString(PASSWORD, "");
			credentials = new Credentials(settings.getString(USERID, ""),
					Helper.isNullOrEmpty(pwd) ? "" : Helper.decrypt(pwd));
			credentials.setId(settings.getInt(ID, 0));
			credentials.setEmail(settings.getString(EMAIL, ""));
			credentials.setName(settings.getString(NAME, ""));
			credentials.setSurname(settings.getString(SURNAME, ""));
		}
		return credentials;
	}

	public static void setCredentials(Credentials c) {
		SharedPreferences settings = MyApplication.getInstance()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(ID, c.getId());
		editor.putString(USERID, c.getUserId());
		editor.putString(EMAIL, c.getEmail());
		editor.putString(PASSWORD, Helper.encrypt(c.getPassword()));
		editor.putString(NAME, c.getName());
		editor.putString(SURNAME, c.getSurname());
		editor.commit();
		credentials = c;
	}

	static Hashtable<String, Boolean> hiddenRoutes = new Hashtable<String, Boolean>();

	public static boolean isHiddenRoute(Context context, String routeName) {
		String key = VISIBLE_ROUTES + routeName;
		if (hiddenRoutes.containsKey(key))
			return hiddenRoutes.get(key);
		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		Boolean b = settings.getBoolean(key, false);
		hiddenRoutes.put(key, b);
		return b;
	}

	public static void setHiddenRoute(Context context, String routeName,
			boolean b) {
		String key = VISIBLE_ROUTES + routeName;
		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(key, b);
		editor.commit();
		hiddenRoutes.put(key, b);
	}

	public static long getLatestSyncDate(Context context) {
		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		return settings.getLong(LATEST_SYNC, 0L);
	}

	public static void setLatestSyncDate(Context context, long date) {
		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(LATEST_SYNC, date);
		editor.commit();
	}

	public static boolean isHiddenMessage(int messageId) {
		SharedPreferences settings = MyApplication.getInstance()
				.getSharedPreferences(PREFS_NAME, 0);
		return settings.getBoolean(HIDDEN_MESSAGE_ + messageId, false);
	}

	public static void setHiddenMessage(int messageId, boolean set) {
		SharedPreferences settings = MyApplication.getInstance()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(HIDDEN_MESSAGE_ + messageId, set);
		editor.commit();

	}

	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 * 
	 * @param context
	 *            application's context.
	 * @param regId
	 *            registration ID
	 */
	static void storeRegistrationId(String regId) {

		Context context = MyApplication.getInstance();
		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		int appVersion = Helper.getAppVersion();
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.putInt(PROPERTY_REG_ID_USER, credentials.getId());
		editor.commit();
	}

	/**
	 * Gets the current registration ID for application on GCM service.
	 * <p>
	 * If result is empty, the app needs to register.
	 * 
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	static String getRegistrationId() {
		Context context = MyApplication.getInstance();
		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		String registrationId = settings.getString(PROPERTY_REG_ID, "");
		if (Helper.isNullOrEmpty(registrationId)) {
			Log.i(Const.LOG_TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = settings.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = Helper.getAppVersion();
		if (registeredVersion != currentVersion) {
			Log.i(Const.LOG_TAG, "App version changed.");
			return "";
		}

		// Check if the user is changed
		int registeredUser = settings.getInt(PROPERTY_REG_ID_USER,
				Integer.MIN_VALUE);

		if (registeredUser != credentials.getId()) {
			Log.i(Const.LOG_TAG, "User changed.");
			return "";
		}
		return registrationId;
	}
}
