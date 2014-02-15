package smartpointer.hereiam;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;

public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2514749928792720911L;
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
		if (received)
		{
		User u = MyApplication.getInstance().getUsers().fromId(idFrom);
		name = (u == null) ? "Unknown" :  u.name;
		}
		else
		{
			name = "Me";
		}
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

	public static ArrayList<Message> getMessages(Context context, User user) {
		MessageDbAdapter db = new MessageDbAdapter(context);
		try {
			db.open();
			return db.fetchMessages(user);
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
	public String getName() {
		return name;
	}
	public boolean isReceived() {
		return received;
	}
	

}
