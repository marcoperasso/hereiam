package smartpointer.hereiam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Helper {

	private static final char[] PASSWORD = "enfldsgbnlsngdlksdsgm"
			.toCharArray();
	private static final byte[] SALT = { (byte) 0xde, (byte) 0x33, (byte) 0x10,
			(byte) 0x12, (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, };

	static String encrypt(String property) {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory
					.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT,
					20));
			return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private static String base64Encode(byte[] bytes) {
		// NB: This class is internal, and you probably should use another impl
		return Base64.encodeToString(bytes, Base64.DEFAULT);
	}

	static String decrypt(String property) {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory
					.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT,
					20));
			return new String(pbeCipher.doFinal(base64Decode(property)),
					"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private static byte[] base64Decode(String property) throws IOException {
		// NB: This class is internal, and you probably should use another impl
		return Base64.decode(property, Base64.DEFAULT);
	}

	static boolean isOnline(Context context) {
		try {
			ConnectivityManager cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			return cm.getActiveNetworkInfo().isConnected();
		} catch (Exception e) {
			return false;
		}

	}

	public static boolean isNullOrEmpty(String s) {
		return s == null || s.length() == 0;
	}

	public static void dialogMessage(final Context context, String message,
			String title, DialogInterface.OnClickListener okListener,
			DialogInterface.OnClickListener cancelListener) {
		new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert).setTitle(title)
				.setMessage(message)
				.setPositiveButton(R.string.yes, okListener)
				.setNegativeButton(R.string.no, cancelListener).show();

	}

	public static void dialogMessage(final Context context, String message,
			DialogInterface.OnClickListener okListener,
			DialogInterface.OnClickListener cancelListener) {
		dialogMessage(context, message, context.getString(R.string.app_name),
				okListener, cancelListener);

	}

	public static void dialogMessage(final Context context, int message,
			DialogInterface.OnClickListener okListener,
			DialogInterface.OnClickListener cancelListener) {
		dialogMessage(context, context.getString(message), okListener,
				cancelListener);

	}

	public static void dialogMessage(final Context context, int message,
			int title, DialogInterface.OnClickListener okListener,
			DialogInterface.OnClickListener cancelListener) {
		dialogMessage(context, context.getString(message),
				context.getString(title), okListener, cancelListener);

	}

	
	public static void hideableMessage(Context context, final int messageId, Object... formatArgs) {
		if (MySettings.isHiddenMessage(messageId))
			return;
		Spanned msg = Html.fromHtml(context.getString(messageId, formatArgs));
		AlertDialog dialog = new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.app_name)
				.setMessage(msg)
				.setPositiveButton(android.R.string.ok, null)
				.setNegativeButton(R.string.no_show_again,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								MySettings.setHiddenMessage(messageId, true);
								dialog.dismiss();
							}
						}).show();
		TextView messageView = (TextView) dialog
				.findViewById(android.R.id.message);
		messageView.setLinksClickable(true);
		messageView.setMovementMethod(LinkMovementMethod.getInstance());
	}

	public static List<File> getFiles(Context context, String ext) {
		File dir = context.getFilesDir();
		final List<File> files = new ArrayList<File>();
		File[] subFiles = dir.listFiles();
		if (subFiles != null) {
			for (File file : subFiles) {
				if (file.isFile() && file.getName().endsWith(ext)) {
					files.add(file);
				}
			}
		}
		return files;
	}

	public static void copyFile(File aSourceFile, File aTargetFile,
			boolean aAppend) throws IOException {
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		try {
			inStream = new FileInputStream(aSourceFile);
			inChannel = inStream.getChannel();
			outStream = new FileOutputStream(aTargetFile, aAppend);
			outChannel = outStream.getChannel();
			long bytesTransferred = 0;
			// defensive loop - there's usually only a single iteration :
			while (bytesTransferred < inChannel.size()) {
				bytesTransferred += inChannel.transferTo(0, inChannel.size(),
						outChannel);
			}
		} finally {
			// being defensive about closing all channels and streams
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
			if (inStream != null)
				inStream.close();
			if (outStream != null)
				outStream.close();
		}
	}

	public static final String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String formatElapsedTime(long totalTimeSeconds) {
		int seconds = (int) (totalTimeSeconds) % 60;
		int minutes = (int) ((totalTimeSeconds / (60)) % 60);
		int hours = (int) ((totalTimeSeconds / (60 * 60)) % 24);
		return String.format("%dh,  %dm, %ds", hours, minutes, seconds);
	}

	public static void saveObject(Context context, String fileName, Object obj)
			throws IOException {
		FileOutputStream fos = context.openFileOutput(fileName,
				Context.MODE_PRIVATE);
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(fos);
			out.writeObject(obj);
			out.flush();
		} finally {
			out.close();
			fos.close();
		}

	}

	public static Object readObject(Context context, String fileName) {
		File file = context.getFileStreamPath(fileName);
		if (file.exists()) {
			try {
				FileInputStream fis = context.openFileInput(fileName);
				ObjectInput in = null;
				try {
					in = new ObjectInputStream(fis);
					try {
						return in.readObject();
					} catch (Exception ex) {
						Log.e(Const.LOG_TAG, Log.getStackTraceString(ex));
					}
				} catch (Exception e) {
					Log.e(Const.LOG_TAG, Log.getStackTraceString(e));
				} finally {
					in.close();
					fis.close();
				}
			} catch (Exception e) {
				Log.e(Const.LOG_TAG, Log.getStackTraceString(e));
			}

		}
		return null;
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	static int getAppVersion() {
		try {
			Context context = MyApplication.getInstance();
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	public static void showMessage(final Activity context, final String message) {
		context.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			}
		});

	}

	static void sendMessageToUser(final Activity context, final User user) {

		// Set an EditText view to get user input
		final EditText input = new EditText(context);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_NORMAL 
				| InputType.TYPE_TEXT_FLAG_MULTI_LINE);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.write_your_message).setView(input)
		.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.send, null);
		final AlertDialog dialog = builder.create();
		dialog.show();

		Button cancelButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		cancelButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				dialog.dismiss();

			}
		});

		Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Editable text = input.getText();
				if (text.length() == 0)
					return;
				dialog.dismiss();
				sendMessage(context, user, text.toString());
			}

		});
	}

	static void sendMessage(final Activity context, final User selectedUser, final String message) {
		final ProgressDialog progressBar = new ProgressDialog(context);
		progressBar.setCancelable(true);
		progressBar.setMessage(context.getString(R.string.sending_message_));
		progressBar.setIndeterminate(true);
		progressBar.show();
		new AsyncTask<Void, Void, Void>() {
			protected void onPostExecute(Void result) {
				progressBar.dismiss();
			};

			@Override
			protected Void doInBackground(Void... params) {

				Credentials.testCredentials(context,
						new OnAsyncResponse() {

							@Override
							public void response(boolean success, String loginMessage) {
								if (success) {
									Credentials c = MySettings.readCredentials();
									Message msg = new Message((long) (System.currentTimeMillis() / 1e3), c.getId(), selectedUser.id, message);
									WebRequestResult result = HttpManager
											.messageToUser(msg);

									if (result.result)
									{
										Helper.showMessage(
												context,
												context.getString(R.string.message_successfully_delivered));
										
										
										msg.saveToDB(context);
										
									}
									else
										Helper.showMessage(
												context,
												context.getString(
														R.string.message_not_delivered_s,
														result.message));
								}
								else
								{
									Helper.showMessage(
											context,
											context.getString(
													R.string.message_not_delivered_s,
													loginMessage));
								}

							}
						});

				return null;
			}

		}.execute(null, null, null);

	}
}
