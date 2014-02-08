package smartpointer.hereiam;

import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.text.format.DateFormat;

public class Message {
	private long time;
	private int idFrom;
	private int idTo;
	private String message;
	
	private String name;
	private boolean received;

	public Message(long time, int idFrom, int idTo, String message) {
		this.time = time;
		this.idFrom = idFrom;
		this.idTo = idTo;
		this.message = message;
		
		calculateAuxData();
	}
	void calculateAuxData()
	{
		Credentials c = MySettings.readCredentials();
		received = (c.getId() == idTo);
		int otherId = received ? idFrom : idTo;
		User u = MyApplication.getInstance().getUsers().fromId(otherId);
		name = (u == null) ? "Unknown" :  u.toString();
		
		
	}
	@Override
	public String toString() {
		Date d = new Date(time * 1000);
		StringBuilder sb = new StringBuilder();
		java.text.DateFormat timeFormat = DateFormat
				.getTimeFormat(MyApplication.getInstance());
		java.text.DateFormat dateFormat = DateFormat
				.getDateFormat(MyApplication.getInstance());
		sb.append(dateFormat.format(d));
		sb.append(" - ");
		sb.append(timeFormat.format(d));
		sb.append(" - ");
		sb.append(name);
		sb.append("\r\n");
		sb.append(message);
		return sb.toString();
	}

	public void saveToDB(Context context) {
		MessageDbAdapter db = new MessageDbAdapter(context);
		try {
			db.open();
			db.createMessage(this);
		} finally {
			db.close();
		}

	}

	public static ArrayList<Message> getMessages(Context context, User user,
			int count, int start) {
		MessageDbAdapter db = new MessageDbAdapter(context);
		try {
			db.open();
			return db.fetchMessages(user, count, start);
		} finally {
			db.close();
		}
	}

	public void removeFromDB(Context context) {
		MessageDbAdapter db = new MessageDbAdapter(context);
		try {
			db.open();
			db.deleteMessage(this);
		} finally {
			db.close();
		}

	}
	public long getTime() {
		return time;
	}
	
	public int getIdFrom() {
		return idFrom;
	}
	
	public int getIdTo() {
		return idTo;
	}
	
	public String getMessage() {
		return message;
	}
	

}
