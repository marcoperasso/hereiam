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
	private String phoneFrom;
	private String phoneTo;
	private String message;
	
	private String name;
	private boolean received;

	public Message(long time, String phoneFrom, String phoneTo, String message) {
		this.time = time;
		this.phoneFrom = phoneFrom;
		this.phoneTo  = phoneTo;
		this.message = message;
		
		calculateAuxData();
	}
	void calculateAuxData()
	{
		Credentials c = MySettings.readCredentials();
		received = (c.getPhone().equals(phoneTo));
		if (received)
		{
		User u = MyApplication.getInstance().getUsers().fromPhone(phoneFrom);
		name = (u == null) ? MyApplication.getInstance().getString(R.string.unknown) :  u.name;
		}
		else
		{
			name =  MyApplication.getInstance().getString(R.string.me);
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
	
	public String getPhoneFrom() {
		return phoneFrom;
	}
	
	public String getPhoneTo() {
		return phoneTo;
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
