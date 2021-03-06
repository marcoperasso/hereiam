package smartpointer.hereiam;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GpsPoint implements Serializable {
	private static final double radius = 6378137.0;
	/**
	 * 
	 */
	private static final long serialVersionUID = -3295827706008323983L;
	int lat;
	int lon;
	long time;
	boolean gps;

	GpsPoint(int lat, int lon, long time, boolean gps) {
		this.lat = lat;
		this.lon = lon;
		this.time = time;
		this.gps = gps;
	}

	public GpsPoint() {

	}

	// /distanza in metri
	public double distance(GpsPoint pt) {
		return calculateGreatCircleDistance(radians(lat), radians(lon),
				radians(pt.lat), radians(pt.lon));
	}

	public double radians(int degreesE6) {
		return degreesE6 * Math.PI / 180e6;
	}

	// /distanza in metri
	public static double calculateGreatCircleDistance(double lat1, double long1,
			double lat2, double long2) {
		if (lat1 == lat2 && long1 == long2)
			return 0;
		return radius
				* Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1)
						* Math.cos(lat2) * Math.cos(long2 - long1));
	}

	public static GpsPoint parseJSON(JSONObject jsonObject) throws JSONException {
		return new GpsPoint(jsonObject.getInt("lat"), jsonObject.getInt("lon"), jsonObject.getInt("time"), jsonObject.getBoolean("gps"));
	}

}

class MyPosition extends GpsPoint implements IJsonSerializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3865710892328782713L;
	String phone;
	String[] connectedUsers;
	MyPosition(String phone, String[] connectedUsers, int lat, int lon, long time, boolean gps) {
		super(lat, lon, time, gps);
		this.phone = phone;
		this.connectedUsers = connectedUsers;
	}

	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject object = new JSONObject();
		object.put("phone", phone);
		object.put("lat", lat);
		object.put("lon", lon);
		object.put("time", time);
		object.put("gps", gps);
		JSONArray jsonArray = new JSONArray();
		for (String phone : connectedUsers)
			jsonArray.put(phone);
		object.put("users", jsonArray);
		return object;
	}

}
