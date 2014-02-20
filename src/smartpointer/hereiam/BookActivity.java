package smartpointer.hereiam;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class BookActivity extends ListActivity implements OnClickListener {

	private MyUserAdapter adapter;
	private Users users;
	private User selectedUser;
	private int requestedCommandId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestedCommandId = getIntent().getIntExtra(Const.COMMAND_ID, -1);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_book);
		registerForContextMenu(findViewById(android.R.id.list));
		users = MyApplication.getInstance().getUsers();
		// users.verifyRegistration();
		adapter = new MyUserAdapter(this, R.layout.mymultichoicelistrow, users);
		setListAdapter(adapter);

		findViewById(R.id.buttonCancel).setOnClickListener(this);
		refreshLabel();
	}

	private void refreshLabel() {
		int labelId = R.string.tap_an_user_for_options;
		if (users.isEmpty())
			labelId = R.string.no_user_in_book;
		else if (requestedCommandId == R.id.itemRequestUserPosition)
			labelId = R.string.tap_an_user_for_locate;
		else if (requestedCommandId == R.id.itemSendMessage)
			labelId = R.string.tap_an_user_for_message;
		((TextView) findViewById(R.id.textViewLabel)).setText(labelId);
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == android.R.id.list) {
			createContextMenu(menu);
		}
	}

	private void createContextMenu(ContextMenu menu) {
		if (selectedUser == null)
			return;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.book_context_menu, menu);
		menu.findItem(R.id.itemAutoAllow).setChecked(
				selectedUser.alwaysAcceptToSendPosition);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (selectedUser == null)
			return true;
		doAction(item.getItemId());

		return true;
	}

	private void doAction(int id) {
		switch (id) {
		case R.id.itemAutoAllow:

			selectedUser.alwaysAcceptToSendPosition = !selectedUser.alwaysAcceptToSendPosition;
			selectedUser.saveToDb();

			refreshRow();

			if (selectedUser.alwaysAcceptToSendPosition) {
				Helper.hideableMessage(this, R.string.warning_auto_accept,
						selectedUser);
			}
			break;
		case R.id.itemRequestUserPosition:
			contactUser(selectedUser, null);
			break;

		case R.id.itemForceGetUserPosition:
			askPasswordAndContactUser();

			break;

		case R.id.itemSendMessage:
			MessageActivity.sendMessageToUser(this, selectedUser);
			finish();
			break;
		case R.id.itemMessages:
			Intent intent = new Intent(this, UserMessagesActivity.class);
			intent.putExtra(Const.USER, selectedUser);
			startActivity(intent);
			break;

		}
	}

	private void refreshRow() {
		ListView list = getListView();
		int start = list.getFirstVisiblePosition();
		for (int i = start, j = list.getLastVisiblePosition(); i <= j; i++)
			if (selectedUser == list.getItemAtPosition(i)) {
				View view = list.getChildAt(i - start);
				list.getAdapter().getView(i, view, list);
				break;
			}
	}

	void contactUser(User user, String password) {
		Intent intent = new Intent();
		intent.putExtra(Const.USER, user);
		intent.putExtra(Const.PASSWORD, password);
		setResult(RESULT_OK, intent);
		finish();

	}

	private void askPasswordAndContactUser() {

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.set_user_password, selectedUser))
				.setView(input).setPositiveButton(android.R.string.ok, null)
				.setNegativeButton(android.R.string.cancel, null);
		final AlertDialog dialogPwd = builder.create();
		dialogPwd.show();

		Button cancelButton = dialogPwd
				.getButton(DialogInterface.BUTTON_NEGATIVE);
		cancelButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				dialogPwd.dismiss();

			}
		});

		Button okButton = dialogPwd.getButton(DialogInterface.BUTTON_POSITIVE);
		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dialogPwd.dismiss();
				contactUser(selectedUser, input.getText().toString());
			}
		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		selectedUser = users.get(position);
		if (!selectedUser.registered) {
			Helper.showMessage(
					this,
					getString(
							R.string._s_is_not_yet_registered_invite_her_him_to_register_,
							selectedUser));
			return;
		}
		if (requestedCommandId != -1)
			doAction(requestedCommandId);
		else
			openContextMenu(v);
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.buttonCancel) {
			Intent intent = new Intent();
			setResult(RESULT_CANCELED, intent);
			finish();
		}

	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	public void setSelectedUser(User user) {
		this.selectedUser = user;

	}

}

class MyUserAdapter extends ArrayAdapter<User> {

	private BookActivity context;
	private ArrayList<User> users;

	public MyUserAdapter(BookActivity context, int resource,
			ArrayList<User> users) {
		super(context, resource, users);
		this.context = context;
		this.users = users;
	}

	static class ViewHolder {
		TextView text;
		BookActivity context;
		public ImageView image;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		User user = users.get(position);
		ViewHolder viewHolder;
		if (convertView == null) {
			LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(R.layout.mymultichoicelistrow, null);
			viewHolder = new ViewHolder();
			viewHolder.text = (TextView) view.findViewById(R.id.text1);
			viewHolder.image = (ImageView) view
					.findViewById(R.id.imageRegistered);
			view.setTag(viewHolder);
		} else {
			view = convertView;
			viewHolder = (ViewHolder) view.getTag();
		}
		viewHolder.image.setVisibility(user.registered ? View.VISIBLE
				: View.INVISIBLE);
		viewHolder.text.setText(user.toString());
		if (user.alwaysAcceptToSendPosition)
			viewHolder.text.setTypeface(null, Typeface.BOLD_ITALIC);
		else
			viewHolder.text.setTypeface(null, Typeface.NORMAL);

		return view;
	}

	public ArrayList<User> getUsers() {
		return users;
	}

}
