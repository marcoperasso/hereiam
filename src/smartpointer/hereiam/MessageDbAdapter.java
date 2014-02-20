package smartpointer.hereiam;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class MessageDbAdapter {

	private Context context;
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;

	// Database fields
	static final String DATABASE_TABLE = "messages";

	static final String KEY_TIME = "time";
	static final String KEY_PHONEFROM = "phonefrom";
	static final String KEY_PHONETO = "phoneto";
	static final String KEY_MESSAGE = "message";

	public MessageDbAdapter(Context context) {
		this.context = context;
	}

	public MessageDbAdapter open() throws SQLException {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}

	private ContentValues createContentValues(Message message) {
		ContentValues values = new ContentValues();
		values.put(KEY_TIME, message.getTime());
		values.put(KEY_PHONEFROM, message.getPhoneFrom());
		values.put(KEY_PHONETO, message.getPhoneTo());
		values.put(KEY_MESSAGE, message.getMessage());

		return values;
	}

	public long createMessage(Message message) {
		ContentValues initialValues = createContentValues(message);
		return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
	}

	public boolean deleteMessage(Message message) {
		return database.delete(DATABASE_TABLE, KEY_TIME + "=" + message.getTime()
				+ " and " + KEY_PHONEFROM + "='" + message.getPhoneFrom() + "' and "
				+ KEY_PHONETO + "='" + message.getPhoneTo() + "'", null) > 0;
	}

	// fetch all users
	public ArrayList<Message> fetchMessages(User user) {
		Cursor cursor = database.query(DATABASE_TABLE, new String[] { KEY_TIME,
				KEY_PHONEFROM, KEY_PHONETO, KEY_MESSAGE }, KEY_PHONEFROM + "='" + user.phone + "' or " + KEY_PHONETO + "='" + user.phone + "'", null, null, null, KEY_TIME);
		ArrayList<Message> messages = new ArrayList<Message>();
		while (cursor.moveToNext()) {
			Message message = new Message(cursor.getLong(cursor
					.getColumnIndex(MessageDbAdapter.KEY_TIME)),
					cursor.getString(cursor
							.getColumnIndex(MessageDbAdapter.KEY_PHONEFROM)),
					cursor.getString(cursor
							.getColumnIndex(MessageDbAdapter.KEY_PHONETO)),
					cursor.getString(cursor
							.getColumnIndex(MessageDbAdapter.KEY_MESSAGE)));

			messages.add(message);
		}
		cursor.close();
		return messages;
	}
}
