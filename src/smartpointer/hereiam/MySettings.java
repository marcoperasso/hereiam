package smartpointer.hereiam;

import java.util.Hashtable;

import android.content.Context;
import android.content.SharedPreferences;

public class MySettings {
	public static final String PREFS_NAME = "i";
	public static final String TRACK_GPS = "l";
	public static final String EMAIL = "o";
	public static final String PASSWORD = "p";
	public static final String PHONE = "ph";
	private static final String HIDDEN_MESSAGE_ = "hm_";
	private static final String HIDDEN_QUESTION = "hq_";
	private static final String ACCEPT_TIMEOUT = "at";
	private static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	static final int QUESTION_RESULT_UNDEFINED = 0;
	static final int QUESTION_RESULT_YES = 1;
	static final int QUESTION_RESULT_NO = 2;

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
			int version = settings.getInt(PROPERTY_APP_VERSION, 0);
			if (version == Helper.getAppVersion())
				credentials.setRegid(settings.getString(PROPERTY_REG_ID, ""));
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
		editor.putString(PROPERTY_REG_ID, c.getRegid());
		editor.putInt(PROPERTY_APP_VERSION, Helper.getAppVersion());
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

	public static int isHiddenQuestion(int messageId) {
		SharedPreferences settings = MyApplication.getInstance()
				.getSharedPreferences(PREFS_NAME, 0);
		return settings.getInt(HIDDEN_QUESTION + messageId, QUESTION_RESULT_UNDEFINED);
	}

	public static void setHiddenQuestion(int messageId, int set) {
		SharedPreferences settings = MyApplication.getInstance()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(HIDDEN_QUESTION + messageId, set);
		editor.commit();

	}

	public static int getAcceptTimeout() {
		SharedPreferences settings = MyApplication.getInstance()
				.getSharedPreferences(PREFS_NAME, 0);
		return settings.getInt(ACCEPT_TIMEOUT, 10);
	}
	
	public static void setAcceptTimeout(int timeout) {
		SharedPreferences settings = MyApplication.getInstance()
				.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(ACCEPT_TIMEOUT, timeout);
		editor.commit();
	}
}
