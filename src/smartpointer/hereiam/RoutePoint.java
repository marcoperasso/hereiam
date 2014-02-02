package smartpointer.hereiam;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class RoutePoint extends GpsPoint
		implements
			IJsonSerializable,
			Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4639726905468751875L;
	Integer normalColor;
	Integer followedRouteColor;

	public RoutePoint(int lat, int lon, long unixTime) {
		super(lat, lon, unixTime);
	}

	public JSONObject toJson() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("lat", lat);
		obj.put("lon", lon);
		obj.put("time", time);
		return obj;
	}

	public static RoutePoint parseJSON(JSONObject jsonObject)
			throws JSONException {
		return new RoutePoint(jsonObject.getInt("lat"), jsonObject.getInt("lon"),
				jsonObject.getLong("time"));
	}
	
}
