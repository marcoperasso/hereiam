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
				User u = new User(phoneNumber, name, dbUser.isTrustedUser(phoneNumber));
				add(u);
			}
		} finally {
			dbUser.close();
			phones.close();
		}

		Collections.sort(Users.this, new UserComparator());
		verifyRegistration();

		/*
		 * UserDbAdapter dbHelper = new UserDbAdapter(context); dbHelper.open();
		 * Cursor cursor = dbHelper.fetchAllUsers();
		 * 
		 * while (cursor.moveToNext()) { User user = new
		 * User(cursor.getInt(cursor .getColumnIndex(UserDbAdapter.KEY_ID)),
		 * cursor.getString(cursor .getColumnIndex(UserDbAdapter.KEY_USERID)),
		 * cursor.getString(cursor .getColumnIndex(UserDbAdapter.KEY_NAME)),
		 * cursor.getString(cursor .getColumnIndex(UserDbAdapter.KEY_SURNAME)));
		 * user.alwaysAcceptToSendPosition = cursor.getInt(cursor
		 * .getColumnIndex(UserDbAdapter.KEY_AUTOACCEPT)) == 1; add(user); }
		 * cursor.close(); dbHelper.close();
		 */

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
				for (int i = 0; i < size(); i++)
					Users.this.get(i).registered = result[i];

				Collections.sort(Users.this, new UserComparator());
			};
		}.execute(null, null, null);
	}

	/*
	 * public void removeUser(User selectedUser) { for (int i = 0; i < size();
	 * i++) { User u = get(i); if (u.id == selectedUser.id) { remove(i);
	 * UserDbAdapter dbHelper = new UserDbAdapter(context); dbHelper.open();
	 * dbHelper.deleteUser(u.id); dbHelper.close(); return; } }
	 * 
	 * }
	 * 
	 * public void addUser(User user) { add(user); UserDbAdapter dbHelper = new
	 * UserDbAdapter(context); dbHelper.open(); dbHelper.createUser(user);
	 * dbHelper.close();
	 * 
	 * }
	 * 
	 * public void updateUsers() { UserDbAdapter dbHelper = new
	 * UserDbAdapter(context); dbHelper.open(); for (User user : this) if
	 * (user.changed) { dbHelper.updateUser(user); user.changed = false; }
	 * dbHelper.close();
	 * 
	 * }
	 */
	public User fromPhone(String phone) {
		for (User user : this)
			if (user.phone.equals(phone))
				return user;
		return new User(phone, context.getString(R.string.unknown), false);
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
