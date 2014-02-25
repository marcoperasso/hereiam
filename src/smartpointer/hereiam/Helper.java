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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.widget.CheckBox;
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

	public static void hideableMessage(Context context, final int messageId,
			Object... formatArgs) {
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

	public static void hideableQuestion(Context context,
			final IFinishCallback yesCallback,
			final IFinishCallback noCallback, final int messageId,
			Object... formatArgs) {
		switch (MySettings.isHiddenQuestion(messageId)) {
		case MySettings.QUESTION_RESULT_YES:
			if (yesCallback != null)
				yesCallback.finished();
			return;
		case MySettings.QUESTION_RESULT_NO:
			if (noCallback != null)
				noCallback.finished();
			return;
		}
		Spanned msg = Html.fromHtml(context.getString(messageId, formatArgs));
		final CheckBox input = new CheckBox(context);
		input.setText(R.string.no_show_again);
		AlertDialog dialog = new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.app_name)
				.setMessage(msg)
				.setView(input)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (input.isChecked())
									MySettings.setHiddenQuestion(messageId,
											MySettings.QUESTION_RESULT_YES);
								if (yesCallback != null)
									yesCallback.finished();

							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (input.isChecked())
									MySettings.setHiddenQuestion(messageId,
											MySettings.QUESTION_RESULT_NO);
								if (noCallback != null)
									noCallback.finished();

							}
						}).show();
		TextView messageView = (TextView) dialog
				.findViewById(android.R.id.message);
		messageView.setLinksClickable(true);
		messageView.setMovementMethod(LinkMovementMethod.getInstance());
		input.setTextColor(messageView.getTextColors());

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

	static long getUnixTime() {
		return (long) (System.currentTimeMillis() / 1000);
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

	public static String adjustPhoneNumber(String number) {
		StringBuilder sb = new StringBuilder();
		String prefix = getPrefix();
		if (!number.startsWith("+", 0)) {
			sb.append(prefix);
		}
		for (int i = 0; i < number.length(); i++) {
			char c = number.charAt(i);

			if (Character.isDigit(c) || c == '-' || c == '+')
				sb.append(c);
		}

		return sb.toString();
	}

	public static String getPrefix() {
		Locale l = Locale.getDefault();
		String prefix = country2phone.get(l.getCountry());
		return prefix == null ? "" : prefix;

	}

	private static Map<String, String> country2phone = new HashMap<String, String>();
	static {
		country2phone.put("AF", "+93");
		country2phone.put("AL", "+355");
		country2phone.put("DZ", "+213");
		country2phone.put("AD", "+376");
		country2phone.put("AO", "+244");
		country2phone.put("AG", "+1-268");
		country2phone.put("AR", "+54");
		country2phone.put("AM", "+374");
		country2phone.put("AU", "+61");
		country2phone.put("AT", "+43");
		country2phone.put("AZ", "+994");
		country2phone.put("BS", "+1-242");
		country2phone.put("BH", "+973");
		country2phone.put("BD", "+880");
		country2phone.put("BB", "+1-246");
		country2phone.put("BY", "+375");
		country2phone.put("BE", "+32");
		country2phone.put("BZ", "+501");
		country2phone.put("BJ", "+229");
		country2phone.put("BT", "+975");
		country2phone.put("BO", "+591");
		country2phone.put("BA", "+387");
		country2phone.put("BW", "+267");
		country2phone.put("BR", "+55");
		country2phone.put("BN", "+673");
		country2phone.put("BG", "+359");
		country2phone.put("BF", "+226");
		country2phone.put("BI", "+257");
		country2phone.put("KH", "+855");
		country2phone.put("CM", "+237");
		country2phone.put("CA", "+1");
		country2phone.put("CV", "+238");
		country2phone.put("CF", "+236");
		country2phone.put("TD", "+235");
		country2phone.put("CL", "+56");
		country2phone.put("CN", "+86");
		country2phone.put("CO", "+57");
		country2phone.put("KM", "+269");
		country2phone.put("CD", "+243");
		country2phone.put("CG", "+242");
		country2phone.put("CR", "+506");
		country2phone.put("CI", "+225");
		country2phone.put("HR", "+385");
		country2phone.put("CU", "+53");
		country2phone.put("CY", "+357");
		country2phone.put("CZ", "+420");
		country2phone.put("DK", "+45");
		country2phone.put("DJ", "+253");
		country2phone.put("DM", "+1-767");
		country2phone.put("DO", "+1-809and1-829");
		country2phone.put("EC", "+593");
		country2phone.put("EG", "+20");
		country2phone.put("SV", "+503");
		country2phone.put("GQ", "+240");
		country2phone.put("ER", "+291");
		country2phone.put("EE", "+372");
		country2phone.put("ET", "+251");
		country2phone.put("FJ", "+679");
		country2phone.put("FI", "+358");
		country2phone.put("FR", "+33");
		country2phone.put("GA", "+241");
		country2phone.put("GM", "+220");
		country2phone.put("GE", "+995");
		country2phone.put("DE", "+49");
		country2phone.put("GH", "+233");
		country2phone.put("GR", "+30");
		country2phone.put("GD", "+1-473");
		country2phone.put("GT", "+502");
		country2phone.put("GN", "+224");
		country2phone.put("GW", "+245");
		country2phone.put("GY", "+592");
		country2phone.put("HT", "+509");
		country2phone.put("HN", "+504");
		country2phone.put("HU", "+36");
		country2phone.put("IS", "+354");
		country2phone.put("IN", "+91");
		country2phone.put("ID", "+62");
		country2phone.put("IR", "+98");
		country2phone.put("IQ", "+964");
		country2phone.put("IE", "+353");
		country2phone.put("IL", "+972");
		country2phone.put("IT", "+39");
		country2phone.put("JM", "+1-876");
		country2phone.put("JP", "+81");
		country2phone.put("JO", "+962");
		country2phone.put("KZ", "+7");
		country2phone.put("KE", "+254");
		country2phone.put("KI", "+686");
		country2phone.put("KP", "+850");
		country2phone.put("KR", "+82");
		country2phone.put("KW", "+965");
		country2phone.put("KG", "+996");
		country2phone.put("LA", "+856");
		country2phone.put("LV", "+371");
		country2phone.put("LB", "+961");
		country2phone.put("LS", "+266");
		country2phone.put("LR", "+231");
		country2phone.put("LY", "+218");
		country2phone.put("LI", "+423");
		country2phone.put("LT", "+370");
		country2phone.put("LU", "+352");
		country2phone.put("MK", "+389");
		country2phone.put("MG", "+261");
		country2phone.put("MW", "+265");
		country2phone.put("MY", "+60");
		country2phone.put("MV", "+960");
		country2phone.put("ML", "+223");
		country2phone.put("MT", "+356");
		country2phone.put("MH", "+692");
		country2phone.put("MR", "+222");
		country2phone.put("MU", "+230");
		country2phone.put("MX", "+52");
		country2phone.put("FM", "+691");
		country2phone.put("MD", "+373");
		country2phone.put("MC", "+377");
		country2phone.put("MN", "+976");
		country2phone.put("ME", "+382");
		country2phone.put("MA", "+212");
		country2phone.put("MZ", "+258");
		country2phone.put("MM", "+95");
		country2phone.put("NA", "+264");
		country2phone.put("NR", "+674");
		country2phone.put("NP", "+977");
		country2phone.put("NL", "+31");
		country2phone.put("NZ", "+64");
		country2phone.put("NI", "+505");
		country2phone.put("NE", "+227");
		country2phone.put("NG", "+234");
		country2phone.put("NO", "+47");
		country2phone.put("OM", "+968");
		country2phone.put("PK", "+92");
		country2phone.put("PW", "+680");
		country2phone.put("PA", "+507");
		country2phone.put("PG", "+675");
		country2phone.put("PY", "+595");
		country2phone.put("PE", "+51");
		country2phone.put("PH", "+63");
		country2phone.put("PL", "+48");
		country2phone.put("PT", "+351");
		country2phone.put("QA", "+974");
		country2phone.put("RO", "+40");
		country2phone.put("RU", "+7");
		country2phone.put("RW", "+250");
		country2phone.put("KN", "+1-869");
		country2phone.put("LC", "+1-758");
		country2phone.put("VC", "+1-784");
		country2phone.put("WS", "+685");
		country2phone.put("SM", "+378");
		country2phone.put("ST", "+239");
		country2phone.put("SA", "+966");
		country2phone.put("SN", "+221");
		country2phone.put("RS", "+381");
		country2phone.put("SC", "+248");
		country2phone.put("SL", "+232");
		country2phone.put("SG", "+65");
		country2phone.put("SK", "+421");
		country2phone.put("SI", "+386");
		country2phone.put("SB", "+677");
		country2phone.put("SO", "+252");
		country2phone.put("ZA", "+27");
		country2phone.put("ES", "+34");
		country2phone.put("LK", "+94");
		country2phone.put("SD", "+249");
		country2phone.put("SR", "+597");
		country2phone.put("SZ", "+268");
		country2phone.put("SE", "+46");
		country2phone.put("CH", "+41");
		country2phone.put("SY", "+963");
		country2phone.put("TJ", "+992");
		country2phone.put("TZ", "+255");
		country2phone.put("TH", "+66");
		country2phone.put("TL", "+670");
		country2phone.put("TG", "+228");
		country2phone.put("TO", "+676");
		country2phone.put("TT", "+1-868");
		country2phone.put("TN", "+216");
		country2phone.put("TR", "+90");
		country2phone.put("TM", "+993");
		country2phone.put("TV", "+688");
		country2phone.put("UG", "+256");
		country2phone.put("UA", "+380");
		country2phone.put("AE", "+971");
		country2phone.put("GB", "+44");
		country2phone.put("US", "+1");
		country2phone.put("UY", "+598");
		country2phone.put("UZ", "+998");
		country2phone.put("VU", "+678");
		country2phone.put("VA", "+379");
		country2phone.put("VE", "+58");
		country2phone.put("VN", "+84");
		country2phone.put("YE", "+967");
		country2phone.put("ZM", "+260");
		country2phone.put("ZW", "+263");
		country2phone.put("GE", "+995");
		country2phone.put("TW", "+886");
		country2phone.put("AZ", "+374-97");
		country2phone.put("CY", "+90-392");
		country2phone.put("MD", "+373-533");
		country2phone.put("SO", "+252");
		country2phone.put("GE", "+995");
		country2phone.put("AU", "");
		country2phone.put("CX", "+61");
		country2phone.put("CC", "+61");
		country2phone.put("AU", "");
		country2phone.put("HM", "");
		country2phone.put("NF", "+672");
		country2phone.put("NC", "+687");
		country2phone.put("PF", "+689");
		country2phone.put("YT", "+262");
		country2phone.put("GP", "+590");
		country2phone.put("GP", "+590");
		country2phone.put("PM", "+508");
		country2phone.put("WF", "+681");
		country2phone.put("TF", "");
		country2phone.put("PF", "");
		country2phone.put("BV", "");
		country2phone.put("CK", "+682");
		country2phone.put("NU", "+683");
		country2phone.put("TK", "+690");
		country2phone.put("GG", "+44");
		country2phone.put("IM", "+44");
		country2phone.put("JE", "+44");
		country2phone.put("AI", "+1-264");
		country2phone.put("BM", "+1-441");
		country2phone.put("IO", "+246");
		country2phone.put("", "+357");
		country2phone.put("VG", "+1-284");
		country2phone.put("KY", "+1-345");
		country2phone.put("FK", "+500");
		country2phone.put("GI", "+350");
		country2phone.put("MS", "+1-664");
		country2phone.put("PN", "");
		country2phone.put("SH", "+290");
		country2phone.put("GS", "");
		country2phone.put("TC", "+1-649");
		country2phone.put("MP", "+1-670");
		country2phone.put("PR", "+1-787and1-939");
		country2phone.put("AS", "+1-684");
		country2phone.put("UM", "");
		country2phone.put("GU", "+1-671");
		country2phone.put("UM", "");
		country2phone.put("UM", "");
		country2phone.put("UM", "");
		country2phone.put("UM", "");
		country2phone.put("UM", "");
		country2phone.put("UM", "");
		country2phone.put("UM", "");
		country2phone.put("VI", "+1-340");
		country2phone.put("UM", "");
		country2phone.put("HK", "+852");
		country2phone.put("MO", "+853");
		country2phone.put("FO", "+298");
		country2phone.put("GL", "+299");
		country2phone.put("GF", "+594");
		country2phone.put("GP", "+590");
		country2phone.put("MQ", "+596");
		country2phone.put("RE", "+262");
		country2phone.put("AX", "+358-18");
		country2phone.put("AW", "+297");
		country2phone.put("AN", "+599");
		country2phone.put("SJ", "+47");
		country2phone.put("AC", "+247");
		country2phone.put("TA", "+290");
		country2phone.put("AQ", "");
		country2phone.put("CS", "+381");
		country2phone.put("PS", "+970");
		country2phone.put("EH", "+212");
		country2phone.put("AQ", "");
		country2phone.put("AQ", "");
		country2phone.put("AQ", "");
		country2phone.put("AQ", "");
		country2phone.put("AQ", "");
	}
	public static String formatTimestamp(String unixTime) {
		try {
			Date d = new Date(Long.parseLong(unixTime)*1000);
			return DateFormat.getDateFormat(MyApplication.getInstance()).format(d) +" " + DateFormat.getTimeFormat(MyApplication.getInstance()).format(d);
		} catch (NumberFormatException e) {
			return "";
		}
	}

}
