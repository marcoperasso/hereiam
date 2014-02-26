package smartpointer.hereiam;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class UserDbAdapter {

	private Context context;
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;

	// Database fields
	static final String DATABASE_TABLE = "trustedusers";

	static final String KEY_ID = "phone";
	static final String KEY_TRUSTED = "trusted";
	static final String KEY_REGISTERED = "registered";

	public UserDbAdapter(Context context) {
		this.context = context;
	}

	public UserDbAdapter open() throws SQLException {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}

	private ContentValues createContentValues(User user) {
		ContentValues values = new ContentValues();
		values.put(KEY_ID, user.phone);
		values.put(KEY_TRUSTED, user.trusted);
		values.put(KEY_REGISTERED, user.registered);
		return values;
	}

	// create a user
	private long createUser(User user) {
		ContentValues initialValues = createContentValues(user);
		return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
	}

	// update a user
	private boolean updateUser(User user) {
		ContentValues updateValues = createContentValues(user);
		return database.update(DATABASE_TABLE, updateValues, KEY_ID + "='"
				+ user.phone + "'", null) > 0;
	}

	// fetch all users
	public void fillUsersFromDB(Users users) {
		Cursor cursor = null;
		try {
			cursor = database.query(DATABASE_TABLE, new String[] { KEY_ID,
					KEY_TRUSTED, KEY_REGISTERED }, null, null,
					null, null, null);
			while (cursor.moveToNext()) {
				User u = new User(cursor
						.getString(cursor.getColumnIndex(KEY_ID)), context.getString(R.string.unknown));
				u.trusted = cursor
						.getInt(cursor.getColumnIndex(KEY_TRUSTED)) == 1;
				u.registered = cursor.getInt(cursor
						.getColumnIndex(KEY_REGISTERED)) == 1;
				users.add(u);
			}
		} finally {
			cursor.close();
		}
	}

	// fetch all users
	public boolean existUser(User user) {
		Cursor cursor = null;
		try

		{
			cursor = database.query(DATABASE_TABLE, new String[] { KEY_ID },
					KEY_ID + "='" + user.phone + "'", null, null, null, null);
			return cursor.moveToNext();
		} finally {
			cursor.close();
		}
	}

	public void persist(User user) {
		if (existUser(user))
			updateUser(user);
		else
			createUser(user);

	}
}
