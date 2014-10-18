package smartpointer.hereiam;

import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmBroadcastReceiver extends BroadcastReceiver {
	public GcmBroadcastReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Bundle extras = intent.getExtras();
			GoogleCloudMessaging gcm = GoogleCloudMessaging
					.getInstance(context);
			// The getMessageType() intent parameter must be the intent you
			// received
			// in your BroadcastReceiver.
			String messageType = gcm.getMessageType(intent);

			if (!extras.isEmpty()) { // has effect of unparcelling Bundle
				/*
				 * Filter messages based on message type. Since it is likely
				 * that GCM will be extended in the future with new message
				 * types, just ignore any message types you're not interested
				 * in, or that you don't recognize.
				 */
				if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
						.equals(messageType)) {
					sendNotification(context,
							context.getString(R.string.send_error, extras),
							null, "");
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
						.equals(messageType)) {
					sendNotification(context, context.getString(
							R.string.deleted_messages_on_server_s, extras),
							null, "");
					// If it's a regular GCM message, do some work.
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
						.equals(messageType)) {

					int msgtype = Integer.parseInt(extras.getString("msgtype"));
					String touserphone = extras.getString("touserphone");
					Credentials c = MySettings.readCredentials();
					// if i registered the phone for multiple users, the message
					// could arrive
					// even if it's not for me!
					if (c == null || !touserphone.equals(c.getPhone()))
						return;
					String fromUserPhone = extras.containsKey("fromuserphone") ? extras.getString("fromuserphone") : null;
					User fromUser = Helper.isNullOrEmpty(fromUserPhone) ? null :  MyApplication.getInstance().getUsers()
							.fromPhone(fromUserPhone, true);

					switch (msgtype) {
					case Const.MSG_REQUEST_CONTACT: {
						String secureToken = extras.getString("securetoken");
						if (!Helper.isNullOrEmpty(secureToken)
								&& !Const.NULL_TOKEN.equals(secureToken)) {
							if (c.getPassword().equals(
									Helper.decrypt(secureToken))) {
								ConnectorService.activate(context, fromUser,
										true, CommandType.START_SENDING_MY_POSITION); // do not notify in case
														// the phpne has been
														// stolen
								MyApplication.getInstance().respondToUser(
										fromUser.phone,
										Const.MSG_ACCEPT_CONTACT);
							} else {
								MyApplication.getInstance().respondToUser(
										fromUser.phone,
										Const.MSG_WRONG_PASSWORD);
							}

						} else if (fromUser.trusted) {
							ConnectorService.activate(context, fromUser, false,
									CommandType.START_SENDING_MY_POSITION);
							MyApplication.getInstance().respondToUser(
									fromUser.phone, Const.MSG_ACCEPT_CONTACT);
						} else {

							Intent intent2 = new Intent(context,
									AcceptConnectionActivity.class);
							intent2.putExtra(Const.USER, fromUser);

							String time = extras.getString("time");
							sendNotification(context, context.getString(
									R.string.s_wants_to_know_your_position,
									Helper.formatTimestamp(time), fromUser),
									intent2, fromUser.phone);
						}
						break;
					}
					case Const.MSG_ACCEPT_CONTACT: {
						String time = extras.getString("time");
						sendNotification(
								context,
								context.getString(
										R.string.s_has_accepted_to_let_you_know_her_its_position,
										Helper.formatTimestamp(time), fromUser),
								null, fromUser.phone);
						ConnectorService.activate(MyApplication.getInstance(), fromUser, true, CommandType.START_RECEIVING_USER_POSITION);
						break;
					}
					case Const.MSG_REJECT_CONTACT: {
						String time = extras.getString("time");
						sendNotification(
								context,
								context.getString(
										R.string.s_has_refused_to_let_you_know_her_its_position,
										Helper.formatTimestamp(time), fromUser),
								null, fromUser.phone);
						break;
					}
					case Const.MSG_REMOVE_CONTACT: {
						String time = extras.getString("time");
						sendNotification(context, context.getString(
								R.string.s_stopped_sending_her_its_position,
								Helper.formatTimestamp(time), fromUser), null,
								fromUser.phone);
						ConnectorService.activate(context, fromUser, false, CommandType.STOP_RECEIVING_USER_POSITION);

						break;
					}
					case Const.MSG_REQUEST_TO_REMOVE_CONTACT: {
						String time = extras.getString("time");
						sendNotification(context, context.getString(
								R.string.s_requested_stop_sending_your_position,
								Helper.formatTimestamp(time), fromUser), null,
								fromUser.phone);
						WatchingUsersActivity.stopSendingMyPositionToUser(fromUser);

						break;
					}
					case Const.MSG_WRONG_PASSWORD: {
						sendNotification(context, context.getString(
								R.string.wrong_password_specified_for_user_s,
								fromUser), null, fromUser.phone);
						break;
					}
					case Const.MSG_MESSAGE: {
						String msg = extras.getString("message");
						long time = Long.parseLong(extras.getString("time"));
						Message message = new Message(time, fromUser.phone,
								c.getPhone(), msg);
						message.saveToDB(MyApplication.getInstance());
						UserMessagesActivity activity = MyApplication
								.getInstance().getMessagesActivity(fromUser);
						if (activity == null) {
							Intent intent2 = new Intent(context,
									UserMessagesActivity.class);
							intent2.putExtra(Const.USER, fromUser);

							sendNotification(context, context.getString(
									R.string._s_says, fromUser, msg), intent2,
									fromUser.phone);
						} else {
							activity.addMessage(message);
						}
						break;
					}
					case Const.MSG_POSITION: {
						JSONObject jsonObject = new JSONObject(
								extras.getString("position"));
						UserPosition pos = new UserPosition(fromUser,
								GpsPoint.parseJSON(jsonObject));
						MyApplication.getInstance().receivedPosition(pos);
						break;
					}
					case Const.MSG_PING: {
						MyApplication.getInstance().pingBack(c.getPhone());
						break;
					}
					}
				}
			}
		} catch (Exception e) {

		}
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	private void sendNotification(Context context, String msg, Intent intent,
			String fromUserPhone) {
		int code = Math.abs(fromUserPhone.hashCode());
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(context, code,
				intent == null ? new Intent() : intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.message)
				.setContentTitle(context.getString(R.string.app_name))
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
				.setContentText(msg).setContentIntent(contentIntent);
		Notification notification = mBuilder.build();
		notification.defaults |= Notification.DEFAULT_ALL;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(Const.TRACE_REQUEST_NOTIFICATION_ID + code,
				notification);

	}
}
