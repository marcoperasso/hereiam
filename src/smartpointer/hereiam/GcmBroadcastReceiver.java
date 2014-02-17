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
							null, 0);
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
						.equals(messageType)) {
					sendNotification(context, context.getString(
							R.string.deleted_messages_on_server_s, extras),
							null, 0);
					// If it's a regular GCM message, do some work.
				} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
						.equals(messageType)) {

					int msgtype = Integer.parseInt(extras.getString("msgtype"));
					Integer touserid = Integer.parseInt(extras
							.getString("touserid"));
					Credentials c = MySettings.readCredentials();
					// if i registered the phone for multiple users, the message
					// could arrive
					// even if it's not for me!
					//if (c == null || !touserid.equals(c.getId()))
					//	return;
					User fromUser = User.parseJSON(extras.getString("user"));
					boolean knownUser = false;
					// replace request user from book user if available
					for (User u : MyApplication.getInstance().getUsers())
						if (u.id == fromUser.id) {
							fromUser = u;
							knownUser = true;
							break;
						}
					if (!knownUser)
					{
						MyApplication.getInstance().getUsers().addUser(fromUser);
					}
					switch (msgtype) {
					case Const.MSG_REQUEST_CONTACT: {
						String secureToken = extras.getString("securetoken");

						if (!Helper.isNullOrEmpty(secureToken)
								&& !Const.NULL_TOKEN.equals(secureToken)) {
							if (c.getPassword().equals(
									Helper.decrypt(secureToken))) {
								ConnectorService.activate(context, fromUser,
										true, true); // do not notify in case
														// the phpne has been
														// stolen
								MyApplication.getInstance().respondToUser(
										fromUser.id, Const.MSG_ACCEPT_CONTACT);
							} else {
								MyApplication.getInstance().respondToUser(
										fromUser.id, Const.MSG_WRONG_PASSWORD);
							}

						} else if (fromUser.alwaysAcceptToSendPosition) {
							ConnectorService.activate(context, fromUser, true,
									false);
							MyApplication.getInstance().respondToUser(
									fromUser.id, Const.MSG_ACCEPT_CONTACT);
						} else {

							Intent intent2 = new Intent(context,
									AcceptConnectionActivity.class);
							intent2.putExtra(Const.USER, fromUser);

							sendNotification(context, context.getString(
									R.string.s_wants_to_know_your_position,
									fromUser), intent2, fromUser.id);
						}
						break;
					}
					case Const.MSG_ACCEPT_CONTACT: {
						sendNotification(
								context,
								context.getString(
										R.string.s_has_accepted_to_let_you_know_her_its_position,
										fromUser), null, fromUser.id);
						ConnectorService.activate(context, fromUser, true,
								false);
						break;
					}
					case Const.MSG_REJECT_CONTACT: {
						sendNotification(
								context,
								context.getString(
										R.string.s_has_refused_to_let_you_know_her_its_position,
										fromUser.id), null, fromUser.id);
						ConnectorService.activate(context, fromUser, false,
								false);
						break;
					}
					case Const.MSG_REMOVE_CONTACT: {
						sendNotification(context, context.getString(
								R.string.s_stopped_sending_her_its_position,
								fromUser), null, fromUser.id);
						ConnectorService.activate(context, fromUser, false,
								false);

						break;
					}
					case Const.MSG_WRONG_PASSWORD: {
						sendNotification(context, context.getString(
								R.string.wrong_password_specified_for_user_s,
								fromUser), null, fromUser.id);
						break;
					}
					case Const.MSG_MESSAGE: {
						String msg = extras.getString("message");
						long time = Long.parseLong(extras.getString("time"));
						Message message = new Message(time, fromUser.id,
								c.getId(), msg);
						message.saveToDB(MyApplication.getInstance());
						Intent intent2 = new Intent(context,
								UserMessagesActivity.class);
						intent2.putExtra(Const.USER, fromUser);

						sendNotification(context, context.getString(
								R.string._s_says, fromUser, msg), intent2,
								fromUser.id);

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
			int fromUserId) {
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
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
		mNotificationManager.notify(Const.TRACE_REQUEST_NOTIFICATION_ID
				+ fromUserId, notification);

	}
}
