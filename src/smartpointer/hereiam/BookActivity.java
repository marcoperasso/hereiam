package smartpointer.hereiam;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
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
import android.widget.ListView;
import android.widget.TextView;

public class BookActivity extends ListActivity implements OnClickListener {

	private MyUserAdapter adapter;
	private ArrayList<User> users;
	private User selectedUser;
	private boolean usersChanged;
	private int requestedCommandId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestedCommandId = getIntent().getIntExtra(Const.COMMAND_ID, -1);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_book);
		registerForContextMenu(findViewById(android.R.id.list));
		users = MyApplication.getInstance().getUsers();

		adapter = new MyUserAdapter(this, R.layout.mymultichoicelistrow, users);
		setListAdapter(adapter);

		findViewById(R.id.buttonAdd).setOnClickListener(this);
		findViewById(R.id.buttonCancel).setOnClickListener(this);
		refreshLabel();
		handleIntent(getIntent());
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

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		// Get the intent, verify the action and get the query
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			// manually launch the real search activity
			final Intent searchIntent = new Intent(getApplicationContext(),
					SearchActivity.class);
			// add query to the Intent Extras
			searchIntent.putExtra(SearchManager.QUERY, query);
			startActivityForResult(searchIntent, Const.SEARCH_ACTIVITY_RESULT);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Const.SEARCH_ACTIVITY_RESULT) {
			if (resultCode == RESULT_OK) {
				if (!data.hasExtra(Const.USER)) {
					// success but no user? means 'search again'
					onSearchRequested();
				} else {
					User user = (User) data.getSerializableExtra(Const.USER);

					for (User u : users)
						if (u.id == user.id) {
							Helper.showMessage(this,
									getString(R.string.user_already_in_book));
							super.onActivityResult(requestCode, resultCode,
									data);
							return;
						}
					MyApplication.getInstance().getUsers().addUser(user);
					adapter.notifyDataSetChanged();
					refreshLabel();
					Helper.showMessage(this,
							getString(R.string._s_has_been_added_to_your_users_book, user));
				}
			}

		}
		super.onActivityResult(requestCode, resultCode, data);
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
			selectedUser.changed = true;
			usersChanged = true;

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
		case R.id.itemRemoveUser:
			Helper.dialogMessage(this, BookActivity.this.getString(
					R.string.are_you_sure_to_remove_user_s_, selectedUser),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							removeUser();
						}
					}, null);

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

	private void removeUser() {
		MyApplication.getInstance().getUsers().removeUser(selectedUser);
		adapter.notifyDataSetChanged();
		refreshLabel();
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
		if (requestedCommandId != -1)
			doAction(requestedCommandId);
		else
			openContextMenu(v);
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.buttonAdd) {

			onSearchRequested();
		} else if (v.getId() == R.id.buttonCancel) {
			Intent intent = new Intent();
			setResult(RESULT_CANCELED, intent);
			finish();
		}

	}

	@Override
	protected void onPause() {

		if (usersChanged) {
			MyApplication.getInstance().getUsers().updateUsers();
			usersChanged = false;
		}
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
		User user;

		public void setTextStyle() {
			if (user.alwaysAcceptToSendPosition)
				text.setTypeface(null, Typeface.BOLD_ITALIC);
			else
				text.setTypeface(null, Typeface.NORMAL);

		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		User user = users.get(position);
		if (convertView == null) {
			LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(R.layout.mymultichoicelistrow, null);
			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.user = user;
			viewHolder.text = (TextView) view.findViewById(R.id.text1);
			viewHolder.text.setText(user.toString());

			view.setTag(viewHolder);
			viewHolder.context = context;
		} else {
			view = convertView;
		}
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		viewHolder.setTextStyle();
		return view;
	}

	public ArrayList<User> getUsers() {
		return users;
	}

}
