package smartpointer.hereiam;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class User implements IJsonSerializable, Serializable {
	@Override
	public String toString() {
		return name + " " + surname + " (" + userId + ")";
	}

	int id;
	String userId;
	String name;
	String surname;
	boolean alwaysAcceptToSendPosition = false;
	
	private static final long serialVersionUID = -5703092633640293472L;

	public User(int id, String userId, String name, String surname) {
		this.id = id;
		this.userId = userId;
		this.name = name;
		this.surname = surname;
	}

	public User(int id, String userId) {
		this.id = id;
		this.userId = userId;
		this.name = null;
		this.surname = null;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("userid", userId);
		if (name != null)
			obj.put("name", name);
		if (surname != null)
			obj.put("surname", surname);

		return obj;
	}

	public static User parseJSON(String jsonString) throws JSONException {
		return parseJSON(new JSONObject(jsonString));

	}

	public static User parseJSON(JSONObject jsonObject) throws JSONException {
		User user = new User(jsonObject.getInt("id"),
				jsonObject.getString("userid"), jsonObject.getString("name"),
				jsonObject.getString("surname"));
		return user;
	}

}
