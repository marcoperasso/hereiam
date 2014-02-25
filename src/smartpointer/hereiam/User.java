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

	boolean trusted = false;
	boolean registered = false;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((phone == null) ? 0 : phone.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (phone == null) {
			if (other.phone != null)
				return false;
		} else if (!phone.equals(other.phone))
			return false;
		return true;
	}

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

	public void saveToDb() {
		UserDbAdapter dbUser = null;
		try {
			dbUser = new UserDbAdapter(MyApplication.getInstance());
			dbUser.open();
			dbUser.persist(this);
			
		} finally {
			dbUser.close();
		}
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
