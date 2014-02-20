package smartpointer.hereiam;

import java.util.Hashtable;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class MySettings {
	public static final String PREFS_NAME = "i";
	public static final String TRACK_GPS = "l";
	public static final String EMAIL = "o";
	public static final String PASSWORD = "p";
	public static final String PHONE = "ph";
	private static final String HIDDEN_MESSAGE_ = "hm_";
	private static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";

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
			credentials = new Credentials(settings.getString(PHONE, ""),
					Helper.isNullOrEmpty(pwd) ? "" : Helper.decrypt(pwd));
			credentials.setEmail(settings.getString(EMAIL, ""));
		}
		return credentials;
	}

	public static void setCredentials(Credentials c) {
		SharedPreferences settings = MyApplication.getInstance()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PHONE, c.getPhone());
		editor.putString(EMAIL, c.getEmail());
		editor.putString(PASSWORD, Helper.encrypt(c.getPassword()));
		editor.commit();
		credentials = c;
	}

	static Hashtable<String, Boolean> hiddenRoutes = new Hashtable<String, Boolean>();

	
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

		
		return registrationId;
	}
}
