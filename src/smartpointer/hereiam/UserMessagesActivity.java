package smartpointer.hereiam;

import java.util.ArrayList;
import java.util.Date;

import smartpointer.hereiam.MyUserAdapter.ViewHolder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class UserMessagesActivity extends ListActivity implements
		OnClickListener {

	private static final String MESSAGES = "msg";
	private ArrayList<Message> messages;
	private ArrayAdapter<Message> adapter;
	private User user;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_messages);
		user = (User) (getIntent().hasExtra(Const.USER) ? getIntent()
				.getSerializableExtra(Const.USER) : null);
		if (user == null) {
			finish();
			return;
		}
		if (savedInstanceState == null)
			messages = Message.getMessages(this, user);
		else
			messages = (ArrayList<Message>) savedInstanceState
					.getSerializable(MESSAGES);
		setTitle(getString(R.string.messages_from_to_s, user));
		adapter = new MyMessageAdapter(this, R.layout.mymessagerow, messages);
		setListAdapter(adapter);

		findViewById(R.id.buttonCancel).setOnClickListener(this);
		findViewById(R.id.buttonRemoveAll).setOnClickListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(MESSAGES, messages);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final int i = position;
		final Message message = (Message) l.getItemAtPosition(i);
		Helper.dialogMessage(this, R.string.delete_this_message_,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						messages.remove(message);
						message.removeFromDB(UserMessagesActivity.this);
						adapter.notifyDataSetChanged();
					}
				}, null);

		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.buttonCancel)
			finish();
		else if (view.getId() == R.id.buttonRemoveAll)

			Helper.dialogMessage(this, "Remove all messages?",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							for (Message m : messages)
								m.removeFromDB(UserMessagesActivity.this);
							messages.clear();
							adapter.notifyDataSetChanged();
						}
					}, null);

	}
}

class MyMessageAdapter extends ArrayAdapter<Message> {

	private ArrayList<Message> messages;
	private UserMessagesActivity context;

	public MyMessageAdapter(UserMessagesActivity context, int resource,
			ArrayList<Message> messages) {
		super(context, resource, messages);
		this.context = context;
		this.messages = messages;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		Message message = messages.get(position);
		if (convertView == null) {
			LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(R.layout.mymessagerow, null);
			TextView tvName = (TextView) view.findViewById(R.id.textViewName);
			tvName.setText(message.getName() + ":");
			TextView tvMessage = (TextView) view
					.findViewById(R.id.textViewMessage);
			tvMessage.setText(message.getMessage());
			TextView tvDate = (TextView) view.findViewById(R.id.textViewDate);
			Date d = new Date(message.getTime() * 1000);

			java.text.DateFormat timeFormat = DateFormat
					.getTimeFormat(MyApplication.getInstance());
			java.text.DateFormat dateFormat = DateFormat
					.getDateFormat(MyApplication.getInstance());
			tvDate.setText(dateFormat.format(d) + ", " + timeFormat.format(d));
			view.setBackgroundColor(message.isReceived() ? Color.YELLOW
					: Color.GREEN);

		} else {
			view = convertView;
		}

		return view;
	}

}
