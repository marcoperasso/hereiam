package smartpointer.hereiam;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class UserPosition implements
Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6187571661244030130L;
	private User user;
	private GpsPoint position;
	private String address;
	public UserPosition(User user, GpsPoint position) {
		this.user = user;
		this.position = position;
	}
	public User getUser() {
		return user;
	}
	public GpsPoint getPosition() {
		return position;
	}
	
	public static UserPosition parseJSON(JSONObject jsonObject) throws JSONException {
		return new UserPosition(User.parseJSON(jsonObject), GpsPoint.parseJSON(jsonObject));
	}
	public boolean isGps() {
		return position.gps;
	}
	public void calculateAddress() {
		address = MyApplication.getInstance().reverseGeocode(position.lat, position.lon);	
	}
	public String getAddress() {
		return address;
	}
	
	
	
}
