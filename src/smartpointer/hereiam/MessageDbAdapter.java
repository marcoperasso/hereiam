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
	static final String KEY_IDFROM = "idfrom";
	static final String KEY_IDTO = "idto";
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
		values.put(KEY_IDFROM, message.getIdFrom());
		values.put(KEY_IDTO, message.getIdTo());
		values.put(KEY_MESSAGE, message.getMessage());

		return values;
	}

	public long createMessage(Message message) {
		ContentValues initialValues = createContentValues(message);
		return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
	}

	public boolean deleteMessage(Message message) {
		return database.delete(DATABASE_TABLE, KEY_TIME + "=" + message.getTime()
				+ " and " + KEY_IDFROM + "=" + message.getIdFrom() + " and "
				+ KEY_IDTO + "=" + message.getIdTo(), null) > 0;
	}

	// fetch all users
	public ArrayList<Message> fetchMessages(User user, int limit, int start) {
		Cursor cursor = database.query(DATABASE_TABLE, new String[] { KEY_TIME,
				KEY_IDFROM, KEY_IDTO, KEY_MESSAGE }, user == null ? null
				: KEY_IDFROM + "=" + user.id, null, null, null, KEY_TIME);
		ArrayList<Message> messages = new ArrayList<Message>();
		while (cursor.moveToNext()) {
			Message message = new Message(cursor.getLong(cursor
					.getColumnIndex(MessageDbAdapter.KEY_TIME)),
					cursor.getInt(cursor
							.getColumnIndex(MessageDbAdapter.KEY_IDFROM)),
					cursor.getInt(cursor
							.getColumnIndex(MessageDbAdapter.KEY_IDTO)),
					cursor.getString(cursor
							.getColumnIndex(MessageDbAdapter.KEY_MESSAGE)));

			messages.add(message);
		}
		cursor.close();
		return messages;
	}
}
