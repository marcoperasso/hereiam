package smartpointer.hereiam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HttpManager {
	private static final int RES_SUCCESS = 0;
	private static final int RES_CUSTOM_ERROR = 1;
	private static final int RES_USER_NOT_LOGGED = 2;
	private static final int RES_NO_RECEIVER_IDS = 3;

	static String decodeMessage(int id) {
		switch (id) {
		case RES_USER_NOT_LOGGED:
			return MyApplication.getInstance().getString(
					R.string.you_are_not_logged);
		case RES_NO_RECEIVER_IDS:
			return MyApplication.getInstance().getString(
					R.string.no_registered_user_to_whom_send_your_message);
		default:
			return "";
		}
	}

	private static final int CONNECTION_TIMEOUT = 60000;
	private static String cookie;
	private static final boolean debuggingServer = false;
	private static final String host = debuggingServer ? "http://10.0.2.2:8888/"
			: "http://www.smartpointer.it/";
	private static final String controllerUrl = host + "hereiam/";
	private static final String getUserLoggedRequest = controllerUrl + "user_logged/";
	private static final String sendPositionDataRequest = controllerUrl + "update_position/";
	private static final String getPositionsRequest = controllerUrl + "get_positions/";
	public static final String login_request = controllerUrl + "login/";
	public static final String save_user_request = controllerUrl + "save_user/";
	public static final String contact_user_request = controllerUrl + "contact_user";
	public static final String disconnect_user_request = controllerUrl + "disconnect_user/";
	public static final String respond_to_user_request = controllerUrl + "respond_to_user/";
	public static final String message_to_user_request = controllerUrl + "message_to_user/";
	public static final String verify_registratoin_request = controllerUrl + "verify_registration/";

	static JSONArray postRequestForArray(String reqString)
			throws ClientProtocolException, IOException, JSONException {
		return postRequestForArray(reqString, new ArrayList<NameValuePair>());
	}

	static JSONArray postRequestForArray(String reqString,
			IJsonSerializable data) throws ClientProtocolException,
			JSONException, IOException {
		return postRequestForArray(reqString, getJSONParameters(data));
	}

	static JSONArray postRequestForArray(String reqString,
			List<NameValuePair> parameters) throws ClientProtocolException,
			IOException, JSONException {
		return new JSONArray(postRequest(reqString, parameters));
	}

	static JSONObject postRequestForObject(String reqString)
			throws ClientProtocolException, JSONException, IOException {
		return new JSONObject(postRequest(reqString,
				new ArrayList<NameValuePair>()));
	}

	static JSONObject postRequestForObject(String reqString,
			IJsonSerializable data) throws ClientProtocolException,
			JSONException, IOException {
		return postRequestForObject(reqString, getJSONParameters(data));
	}

	static JSONObject postRequestForObject(String reqString,
			List<NameValuePair> params) throws JSONException,
			ClientProtocolException, IOException {
		return new JSONObject(postRequest(reqString, params));
	}

	private static List<NameValuePair> getJSONParameters(IJsonSerializable data)
			throws JSONException {
		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();

		String json = data.toJson().toString();
		postParameters.add(new BasicNameValuePair("data", json));

		return postParameters;
	}

	private static String postRequest(String reqString,
			List<NameValuePair> postParameters) throws ClientProtocolException,
			IOException, JSONException {
		StringBuilder result = new StringBuilder();
		HttpClient httpClient = new DefaultHttpClient();
		HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);

		HttpPost httpPost = new HttpPost(reqString);
		httpPost.setHeader("Cookie", getCookie());
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParameters);
		httpPost.setEntity(entity);
		HttpResponse response = null;
		response = httpClient.execute(httpPost, new BasicHttpContext());
		BufferedReader reader;
		reader = new BufferedReader(new InputStreamReader(response.getEntity()
				.getContent()), 8192);
		String line = null;
		while ((line = reader.readLine()) != null) {
			result.append(line);

		}
		reader.close();
		for (Header h : response.getHeaders("Set-Cookie")) {
			cookie = h.getValue();
		}
		return result.toString();
	}

	private static String getCookie() {
		StringBuilder sb = new StringBuilder();

		if (cookie != null)
			sb.append(cookie);
		if (debuggingServer)
			sb.append(";XDEBUG_SESSION=netbeans-xdebug");
		return sb.toString();
	}

	public static boolean isLogged() {

		JSONObject obj;
		try {
			obj = postRequestForObject(getUserLoggedRequest);
			return obj.getBoolean("logged");
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean sendPositionData(MyPosition position)
			throws JSONException, ClientProtocolException, IOException {
		JSONObject response = postRequestForObject(sendPositionDataRequest,
				position);
		return response.has("saved") && response.getBoolean("saved");

	}

	public static ArrayList<UserPosition> getPositions() {
		ArrayList<UserPosition> list = new ArrayList<UserPosition>();
		try {

			JSONArray points = postRequestForArray(getPositionsRequest);
			int length = points.length();
			for (int i = 0; i < length; i++) {
				UserPosition userPosition = UserPosition.parseJSON(points
						.getJSONObject(i));
				userPosition.calculateAddress();
				list.add(userPosition);
			}
			return list;
		} catch (Exception e) {
			return list;
		}

	}

	public static WebRequestResult login(Credentials c) {
		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("phone", c.getPhone()));
		postParameters.add(new BasicNameValuePair("pwd", Helper.md5(c
				.getPassword())));
		WebRequestResult result = new WebRequestResult();
		try {
			String postRequest = postRequest(login_request, postParameters);
			JSONObject obj = new JSONObject(postRequest);
			if (obj.has("message"))
				result.message = obj.getString("message");
			result.result = obj.has("success") && obj.getBoolean("success");
			if (obj.has("version")
					&& obj.getInt("version") != Const.PROTOCOL_VERSION) {
				result.message = MyApplication.getInstance().getString(
						R.string.wrong_version);
				result.result = false;
			}
			if (obj.has("mail"))
				c.setEmail(obj.getString("mail"));
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult saveCredentials(Credentials credentials,
			Boolean newUser) {
		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("pwd", Helper.md5(credentials
				.getPassword())));
		postParameters.add(new BasicNameValuePair("phone", credentials
				.getPhone()));
		postParameters.add(new BasicNameValuePair("regid", credentials
				.getRegid()));
		postParameters.add(new BasicNameValuePair("email", credentials
				.getEmail()));
		postParameters.add(new BasicNameValuePair("newuser", newUser ? "1"
				: "0"));
		WebRequestResult result = new WebRequestResult();
		try {
			String postRequest = postRequest(save_user_request, postParameters);
			JSONObject obj = new JSONObject(postRequest);
			if (obj.has("message"))
				result.message = obj.getString("message");
			result.result = obj.has("success") && obj.getBoolean("success");

		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult contactUser(String userPhone,
			String secureToken) {
		WebRequestResult result = new WebRequestResult();
		try {
			List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("userphone", userPhone));
			postParameters.add(new BasicNameValuePair("securetoken",
					secureToken));
			JSONObject obj = new JSONObject(postRequest(contact_user_request,
					postParameters));

			int res = obj.getInt("result");
			result.message = decodeMessage(res);

			result.result = res == RES_SUCCESS;
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult respondToUser(String userPhone,
			int responseCode) {
		WebRequestResult result = new WebRequestResult();
		try {
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("userphone", userPhone));
			params.add(new BasicNameValuePair("responsecode", Integer
					.toString(responseCode)));
			JSONObject obj = postRequestForObject(respond_to_user_request,
					params);

			int res = obj.getInt("result");
			result.message = decodeMessage(res);
			result.result = res == RES_SUCCESS;
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult disconnectUser(String userPhone) {
		WebRequestResult result = new WebRequestResult();
		try {
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("userphone", userPhone));
			JSONObject obj = postRequestForObject(disconnect_user_request,
					params);

			int res = obj.getInt("result");
			result.message = decodeMessage(res);

			result.result = res == RES_SUCCESS;
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult messageToUser(Message msg) {
		WebRequestResult result = new WebRequestResult();
		try {
			List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("userphone", msg
					.getPhoneTo()));
			postParameters.add(new BasicNameValuePair("message", msg
					.getMessage()));
			postParameters.add(new BasicNameValuePair("time", Long.toString(msg
					.getTime())));
			JSONObject obj = new JSONObject(postRequest(
					message_to_user_request, postParameters));

			int res = obj.getInt("result");
			result.message = decodeMessage(res);

			result.result = res == RES_SUCCESS;
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult verifyRegistration(Users users, boolean[] results) {
		WebRequestResult result = new WebRequestResult();
		try {
			
			JSONObject obj = postRequestForObject(verify_registratoin_request, users);
			
			int res = obj.getInt("result");
			result.message = decodeMessage(res);

			result.result = res == RES_SUCCESS;
			if(result.result) 
			{
				JSONArray ar = obj.getJSONArray("users");
				if (ar.length() != results.length)
					throw new JSONException("Invalid user array dimension");
				for (int i = 0; i < ar.length(); i++)
					results[i] = ar.getBoolean(i);
			}
			
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

}

class WebRequestResult {
	public WebRequestResult(boolean result, String message) {
		this.result = result;
		this.message = message;
	}

	public WebRequestResult() {
		this.result = false;
		this.message = "";
	}

	public boolean result;
	public String message;
}
