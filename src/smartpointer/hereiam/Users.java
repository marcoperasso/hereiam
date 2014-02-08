package smartpointer.hereiam;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;

public class Users extends ArrayList<User> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3313480521664111183L;
	private Context context;

	public Users(Context context) {
		this.context = context;
		UserDbAdapter dbHelper = new UserDbAdapter(context);
		dbHelper.open();
		Cursor cursor = dbHelper.fetchAllUsers();

		while (cursor.moveToNext()) {
			User user = new User(cursor.getInt(cursor
					.getColumnIndex(UserDbAdapter.KEY_ID)),
					cursor.getString(cursor
							.getColumnIndex(UserDbAdapter.KEY_USERID)),
					cursor.getString(cursor
							.getColumnIndex(UserDbAdapter.KEY_NAME)),
					cursor.getString(cursor
							.getColumnIndex(UserDbAdapter.KEY_SURNAME)));
			user.alwaysAcceptToSendPosition = cursor.getInt(cursor
					.getColumnIndex(UserDbAdapter.KEY_AUTOACCEPT)) == 1;
			add(user);
		}
		cursor.close();
		dbHelper.close();

	}

	public void removeUser(User selectedUser) {
		for (int i = 0; i < size(); i++) {
			User u = get(i);
			if (u.id == selectedUser.id) {
				remove(i);
				UserDbAdapter dbHelper = new UserDbAdapter(context);
				dbHelper.open();
				dbHelper.deleteUser(u.id);
				dbHelper.close();
				return;
			}
		}

	}

	public void addUser(User user) {
		add(user);
		UserDbAdapter dbHelper = new UserDbAdapter(context);
		dbHelper.open();
		dbHelper.createUser(user);
		dbHelper.close();

	}

	public void updateUsers() {
		UserDbAdapter dbHelper = new UserDbAdapter(context);
		dbHelper.open();
		for (User user : this)
			if (user.changed) {
				dbHelper.updateUser(user);
				user.changed = false;
			}
		dbHelper.close();

	}

	public User fromId(int id) {
		for (User user : this)
			if (user.id == id)
				return user;
		return null;
	}

}
