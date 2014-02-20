package smartpointer.hereiam;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "hereiam.db";
	private static final int DATABASE_VERSION = 1;

	private static final String CREATE_USERS = "create table "
			+ UserDbAdapter.DATABASE_TABLE
			+ " (phone text primary key, trusted integer not null, registered integer not null);";

	private static final String CREATE_MESSAGES = "create table "
			+ MessageDbAdapter.DATABASE_TABLE
			+ " (time integer, phonefrom text not null, phoneto text not null, message text not null, PRIMARY KEY (time, phonefrom, phoneto));";

	// Costruttore
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Questo metodo viene chiamato durante la creazione del database
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_USERS);
		database.execSQL(CREATE_MESSAGES);
	}

	// Questo metodo viene chiamato durante l'upgrade del database, ad esempio
	// quando viene incrementato il numero di versione
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {

	}
}