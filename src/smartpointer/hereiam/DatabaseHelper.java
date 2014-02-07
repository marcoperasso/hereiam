package smartpointer.hereiam;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "hereiam.db";
	private static final int DATABASE_VERSION = 1;

	// Lo statement SQL di creazione del database
	private static final String DATABASE_CREATE = "create table " + UserDbAdapter.DATABASE_TABLE + " (id integer primary key, name text not null, surname text not null, userid text not null, autoaccept integer not null);";

	// Costruttore
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Questo metodo viene chiamato durante la creazione del database
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	// Questo metodo viene chiamato durante l'upgrade del database, ad esempio
	// quando viene incrementato il numero di versione
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {

	}
}