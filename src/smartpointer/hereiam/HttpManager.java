package smartpointer.hereiam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.util.Log;

public class HttpManager {
	private static final int RES_SUCCESS = 0;
	private static final int RES_CUSTOM_ERROR = 1;
	private static final int RES_USER_NOT_LOGGED = 2;
	private static final int RES_NO_RECEIVER_IDS = 3;

	static String decodeMessage(int id) {
		switch (id) {
		case RES_USER_NOT_LOGGED: return MyApplication.getInstance().getString(R.string.you_are_not_logged);
		case RES_NO_RECEIVER_IDS: return MyApplication.getInstance().getString(R.string.no_registered_user_to_whom_send_your_message);
		default:
			return "";
		}
	}

	private static final int CONNECTION_TIMEOUT = 60000;
	private static String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*()";
	private static String cookie;
	private static final boolean debuggingServer = false;
	private static final String host = debuggingServer ? "http://10.0.2.2:8888/"
			: "http://www.ecommuters.com/";
	private static final String getUserRequest = host + "mitu/user";
	private static final String getUserLoggedRequest = host
			+ "mitu/user_logged";
	private static final String sendPositionDataRequest = host
			+ "mitu/update_position";
	private static final String getPositionsRequest = host
			+ "mitu/get_positions/";
	private static final String getUsers = host + "mitu/get_users?filter=";
	public static final String login_request = host + "mitu/login/";
	public static final String save_regid_request = host + "mitu/save_regid/";
	public static final String save_user_request = host + "mitu/save_user/";
	public static final String contact_user_request = host
			+ "mitu/contact_user";
	public static final String disconnect_user_request = host
			+ "mitu/disconnect_user/";
	public static final String respond_to_user_request = host
			+ "mitu/respond_to_user/";
	public static final String message_to_user_request = host
			+ "mitu/message_to_user/";

	private static String encodeURIComponent(String input) {
		if (Helper.isNullOrEmpty(input)) {
			return input;
		}

		int l = input.length();
		StringBuilder o = new StringBuilder(l * 3);
		try {
			for (int i = 0; i < l; i++) {
				String e = input.substring(i, i + 1);
				if (ALLOWED_CHARS.indexOf(e) == -1) {
					byte[] b = e.getBytes("utf-8");
					o.append(getHex(b));
					continue;
				}
				o.append(e);
			}
			return o.toString();
		} catch (UnsupportedEncodingException e) {
			Log.e(Const.LOG_TAG, Log.getStackTraceString(e));
		}
		return input;
	}

	private static String getHex(byte buf[]) {
		StringBuilder o = new StringBuilder(buf.length * 3);
		for (int i = 0; i < buf.length; i++) {
			int n = (int) buf[i] & 0xff;
			o.append("%");
			if (n < 0x10) {
				o.append("0");
			}
			o.append(Long.toString(n, 16).toUpperCase());
		}
		return o.toString();
	}

	static JSONObject sendRequestForObject(String reqString)
			throws JSONException, ClientProtocolException, IOException {
		StringBuilder result = sendRequest(reqString);
		return new JSONObject(result.toString());
	}

	static JSONArray sendRequestForArray(String reqString)
			throws JSONException, ClientProtocolException, IOException {
		StringBuilder result = sendRequest(reqString);
		return new JSONArray(result.toString());
	}

	private static StringBuilder sendRequest(String reqString)
			throws IOException, ClientProtocolException {
		StringBuilder result = new StringBuilder();
		HttpClient httpClient = new DefaultHttpClient();
		HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);

		HttpContext localContext = new BasicHttpContext();

		HttpGet httpGet = new HttpGet(reqString);
		httpGet.setHeader("Cookie", getCookie());
		HttpResponse response = null;
		response = httpClient.execute(httpGet, localContext);
		BufferedReader reader;
		reader = new BufferedReader(new InputStreamReader(response.getEntity()
				.getContent()), 8192);
		String line = null;
		while ((line = reader.readLine()) != null) {
			result.append(line);

		}
		reader.close();
		return result;
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

	static JSONObject postRequestForObject(String reqString,
			IJsonSerializable data) throws ClientProtocolException,
			JSONException, IOException {
		return new JSONObject(postRequest(reqString, getJSONParameters(data)));
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

	public static void fillCredentialsData(Credentials c) {

		JSONObject obj;
		try {
			obj = sendRequestForObject(getUserRequest);
			c.setName(obj.getString("name"));
			c.setSurname(obj.getString("surname"));
			c.setUserId(obj.getString("userid"));
			c.setEmail(obj.getString("mail"));
			c.setId(obj.getInt("id"));
		} catch (Exception e) {
			Log.e(Const.LOG_TAG, Log.getStackTraceString(e));
		}
	}

	public static boolean isLogged() {

		JSONObject obj;
		try {
			obj = sendRequestForObject(getUserLoggedRequest);
			return obj.getBoolean("logged");
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean sendPositionData(MyPosition position) throws JSONException,
			ClientProtocolException, IOException {
		JSONObject response = postRequestForObject(sendPositionDataRequest,
				position);
		return response.has("saved") && response.getBoolean("saved");

	}

	public static ArrayList<UserPosition> getPositions() {
		ArrayList<UserPosition> list = new ArrayList<UserPosition>();
		try {
			
			JSONArray points = sendRequestForArray(getPositionsRequest);
			int length = points.length();
			for (int i = 0; i < length; i++) {
				UserPosition userPosition = UserPosition.parseJSON(points.getJSONObject(i));
				userPosition.calculateAddress();
				list.add(userPosition);
			}
			return list;
		} catch (Exception e) {
			return list;
		}

	}

	public static SearchPositionResult getUsers(String query) {
		SearchPositionResult res = new SearchPositionResult();
		try {
			JSONObject obj = sendRequestForObject(getUsers
					+ encodeURIComponent(query));
			JSONArray points = obj.getJSONArray("users");
			int length = points.length();
			for (int i = 0; i < length; i++)
				res.users.add(User.parseJSON(points.getJSONObject(i)));
			res.total = obj.getInt("total");
		} catch (Exception e) {
			res.error = e.getMessage();
		}
		return res;
	}

	public static WebRequestResult login(String userid, String pwd) {
		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("userid", userid));
		postParameters.add(new BasicNameValuePair("pwd", Helper.md5(pwd)));
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
		postParameters.add(new BasicNameValuePair("userid", credentials
				.getUserId()));
		postParameters
				.add(new BasicNameValuePair("name", credentials.getName()));
		postParameters.add(new BasicNameValuePair("surname", credentials
				.getSurname()));
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
			if (obj.has("id")) {
				credentials.setId(obj.getInt("id"));
			}
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult sendRegistrationIds(String regid) {
		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("regid", regid));
		WebRequestResult result = new WebRequestResult();
		try {
			String postRequest = postRequest(save_regid_request, postParameters);
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

	public static WebRequestResult contactUser(int usrId, String secureToken) {
		WebRequestResult result = new WebRequestResult();
		try {
			List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("userid", Integer.toString(usrId)));
			postParameters.add(new BasicNameValuePair("securetoken", secureToken));
			JSONObject obj = new JSONObject(postRequest(contact_user_request, postParameters));

			int res = obj.getInt("result");
			result.message = decodeMessage(res);

			result.result = res == RES_SUCCESS;
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult respondToUser(int usrId, int responseCode) {
		WebRequestResult result = new WebRequestResult();
		try {
			JSONObject obj = sendRequestForObject(respond_to_user_request + Integer.toString(usrId)
					+ "/" + responseCode);

			int res = obj.getInt("result");
			result.message = decodeMessage(res);
			result.result = res == RES_SUCCESS;
		} catch (Exception e) {
			result.message = e.toString();
			result.result = false;
		}
		return result;
	}

	public static WebRequestResult disconnectUser(int usrId) {
		WebRequestResult result = new WebRequestResult();
		try {
			JSONObject obj = sendRequestForObject(disconnect_user_request
					+ Integer.toString(usrId));

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
			postParameters.add(new BasicNameValuePair("userid", Integer.toString(msg.getIdTo())));
			postParameters.add(new BasicNameValuePair("message", msg.getMessage()));
			postParameters.add(new BasicNameValuePair("time", Long.toString(msg.getTime())));
			JSONObject obj = new JSONObject(postRequest(message_to_user_request, postParameters));

			int res = obj.getInt("result");
			result.message = decodeMessage(res);

			result.result = res == RES_SUCCESS;
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
