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
		values.put(KEY_TRUSTED, user.alwaysAcceptToSendPosition);

		return values;
	}

	// create a user
	public long createUser(User user) {
		ContentValues initialValues = createContentValues(user);
		return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
	}

	// update a user
	public boolean updateUser(User user) {
		ContentValues updateValues = createContentValues(user);
		return database.update(DATABASE_TABLE, updateValues, KEY_ID + "='"
				+ user.phone + "'", null) > 0;
	}


	// fetch all users
	public boolean isTrustedUser(String phone) {
		Cursor cursor = null;
		try {
			cursor = database
					.query(DATABASE_TABLE,
							new String[] { KEY_ID, KEY_TRUSTED }, KEY_ID + "='"
									+ phone + "'", null, null, null, null);
			return cursor.moveToNext() ? cursor.getInt(cursor
					.getColumnIndex(KEY_TRUSTED)) == 1 : false;
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
}
