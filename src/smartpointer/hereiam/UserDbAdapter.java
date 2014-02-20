package smartpointer.hereiam;


public class UserDbAdapter {
/*
	private Context context;
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;

	// Database fields
	static final String DATABASE_TABLE = "users";

	static final String KEY_ID = "id";
	static final String KEY_NAME = "name";
	static final String KEY_SURNAME = "surname";
	static final String KEY_USERID = "userid";
	static final String KEY_AUTOACCEPT = "autoaccept";

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
		values.put(KEY_ID, user.id);
		values.put(KEY_NAME, user.name);
		values.put(KEY_SURNAME, user.surname);
		values.put(KEY_USERID, user.userId);
		values.put(KEY_AUTOACCEPT, user.alwaysAcceptToSendPosition);

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
		return database.update(DATABASE_TABLE, updateValues, KEY_ID + "=" + user.id,
				null) > 0;
	}

	// delete a user
	public boolean deleteUser(Integer id) {
		return database.delete(DATABASE_TABLE, KEY_ID + "=" + id, null) > 0;
	}

	// fetch all users
	public Cursor fetchAllUsers() {
		return database.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_NAME,
				KEY_SURNAME, KEY_USERID, KEY_AUTOACCEPT }, null, null, null,
				null, null);
	}
*/
}
