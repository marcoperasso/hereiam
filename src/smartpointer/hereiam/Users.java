package smartpointer.hereiam;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;

public class Users extends ArrayList<User> implements IJsonSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3313480521664111183L;
	private Context context;

	public Users(Context context) {
		this.context = context;
		Cursor phones = context.getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,
				null, null);

		UserDbAdapter dbUser = null;
		try {
			dbUser = new UserDbAdapter(MyApplication.getInstance());
			dbUser.open();
			dbUser.fillUsersFromDB(this);
			while (phones.moveToNext()) {
				String name = phones
						.getString(phones
								.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
				String phoneNumber = phones
						.getString(phones
								.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				int t = Integer
						.parseInt(phones.getString(phones
								.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)));
				if (t != ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
					continue;
				phoneNumber = Helper.adjustPhoneNumber(phoneNumber);
				User u = fromPhone(phoneNumber, false);
				u.name = name;
			}
		} finally {
			dbUser.close();
			phones.close();
		}

		Collections.sort(Users.this, new UserComparator());
		verifyRegistration();

	}

	public void verifyRegistration() {
		AsyncTask<Void, Void, boolean[]> task = new AsyncTask<Void, Void, boolean[]>() {

			@Override
			protected boolean[] doInBackground(Void... params) {

				boolean[] res = new boolean[Users.this.size()];
				HttpManager.verifyRegistration(Users.this, res);
				return res;
			}

			protected void onPostExecute(boolean[] result) {
				UserDbAdapter dbUser = null;
				try {
					dbUser = new UserDbAdapter(MyApplication.getInstance());
					dbUser.open();
					for (int i = 0; i < size(); i++) {
						if (i < result.length) {
							User user = Users.this.get(i);
							boolean b = result[i];
							if (user.registered != b) {
								user.registered = b;
								dbUser.persist(user);
							}
						}
					}

				} finally {
					dbUser.close();
				}

				Collections.sort(Users.this, new UserComparator());
			};
		}.execute(null, null, null);
	}

	public User fromPhone(String phone, boolean setRegistered) {
		for (User user : this)
			if (user.phone.equals(phone))
				return user;
		User u = new User(phone, context.getString(R.string.unknown));
		if (setRegistered)
			u.registered = true;
		add(u);
		u.saveToDb();
		return u;
	}

	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject obj = new JSONObject();
		JSONArray users = new JSONArray();
		for (User u : this)
			users.put(u.phone);
		obj.put("users", users);
		return obj;
	}

}
