package smartpointer.hereiam;

import java.io.Serializable;
import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

public class User implements IJsonSerializable, Serializable {
	@Override
	public String toString() {
		return name + " " + " (" + phone + ")";
	}

	String phone;
	String name;

	boolean alwaysAcceptToSendPosition = false;
	boolean registered = false;
	transient boolean changed = false;

	private static final long serialVersionUID = -5703092633640293472L;

	public User(String phone, String name) {
		this.phone = phone;
		this.name = name;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("phone", phone);
		obj.put("name", name);

		return obj;
	}

}

class UserComparator implements Comparator<User> {
	@Override
	public int compare(User u1, User u2) {
		if (u1.registered && !u2.registered)
			return -1;
		if (u2.registered && !u1.registered)
			return 1;

		return u1.name.compareTo(u2.name);
	}
}
