package smartpointer.hereiam;

import org.json.JSONException;
import org.json.JSONObject;

interface IJsonSerializable {
	public JSONObject toJson() throws JSONException;
}

interface IFinishCallback {
	void finished();
}
