package smartpointer.hereiam;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MessagesActivity extends ListActivity implements OnClickListener {

	private ArrayList<Message> messages;
	private ArrayAdapter<Message> adapter;
	private User user;
	int start = 0;
	int count = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_messages);
		user = (User) (getIntent().hasExtra(Const.USER) ? getIntent()
				.getSerializableExtra(Const.USER) : null);
		messages = Message.getMessages(this, user, count, start);

		adapter = new ArrayAdapter<Message>(this,
				R.xml.mylist, messages);
		setListAdapter(adapter);

		findViewById(R.id.buttonCancel).setOnClickListener(this);
		findViewById(R.id.buttonRemoveAll).setOnClickListener(this);
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
						message.removeFromDB(MessagesActivity.this);
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
								m.removeFromDB(MessagesActivity.this);
							messages.clear();
							adapter.notifyDataSetChanged();
						}
					}, null);

	}
}
