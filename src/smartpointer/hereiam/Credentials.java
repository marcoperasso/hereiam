package smartpointer.hereiam;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;

public class Credentials {

	private String phone = "";
	private String email = "";
	private String password = "";
	private String regid = "";

	public Credentials(String phone, String pwd) {
		this.phone = phone;
		this.password = pwd;
	}

	public boolean isEmpty() {
		return Helper.isNullOrEmpty(phone) || Helper.isNullOrEmpty(password) ;
	}

	String getPassword() {
		return password;
	}

	void setPassword(String password) {
		this.password = password;
	}

	String getEmail() {
		return email;
	}

	void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}


	public static void testCredentials(final Context context,
			final OnAsyncResponse testResponse) {
		TestIfLoggedAsyncTask testIfLoggedAsyncTask = new TestIfLoggedAsyncTask(context, testResponse);
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			testIfLoggedAsyncTask.execute();
		}
		else
		{
			testIfLoggedAsyncTask.onPostExecute(testIfLoggedAsyncTask.doInBackground());
		}

	}

	public void testLogin(final Context context,
			final OnAsyncResponse onResponse) {

		if (!Helper.isOnline(context)) {
			onResponse.response(true, "");
		}
		LoginAsyncTask loginAsyncTask = new LoginAsyncTask(onResponse);
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			loginAsyncTask.execute(this);
		}
		else
		{
			loginAsyncTask.onPostExecute(loginAsyncTask.doInBackground(this));
		}
			

	}


	public String getRegid() {
		return regid;
	}

	public void setRegid(String regid) {
		this.regid = regid;
	}

}

class TestIfLoggedAsyncTask extends
		AsyncTask<Void, Void, Boolean> {
	private final Context context;
	private final OnAsyncResponse testResponse;

	TestIfLoggedAsyncTask(Context context, OnAsyncResponse testResponse) {
		this.context = context;
		this.testResponse = testResponse;
	}

	@Override
	protected Boolean doInBackground(Void... params) {

		return HttpManager.isLogged();
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			testResponse.response(true, "");
			return;
		}
		Credentials credential = MySettings.readCredentials();
		if (credential.isEmpty()) {
			testResponse.response(false, "");
			return;
		}
		credential.testLogin(context, testResponse);
		super.onPostExecute(result);
	}
}

class LoginAsyncTask extends
		AsyncTask<Credentials, Void, WebRequestResult> {
	private final OnAsyncResponse onResponse;

	LoginAsyncTask(OnAsyncResponse onResponse) {
		this.onResponse = onResponse;
	}

	@Override
	protected WebRequestResult doInBackground(Credentials... params) {
		Credentials c = params[0];
		String mail = c.getEmail();
		WebRequestResult res = HttpManager.login(c);
		if (res.result && !mail.equals(c.getEmail()))
		{
			MySettings.setCredentials(c);
		}
		return res;
	}

	@Override
	protected void onPostExecute(WebRequestResult result) {
		onResponse.response(result.result, result.message.toString());
		super.onPostExecute(result);
	}
}

