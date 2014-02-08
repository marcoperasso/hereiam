package smartpointer.hereiam;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class BookActivity extends ListActivity implements OnClickListener {

	private MyUserAdapter adapter;
	private ArrayList<User> users;
	private User selectedUser;
	private boolean usersChanged;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_book);
		registerForContextMenu(findViewById(android.R.id.list));
		users = MyApplication.getInstance().getUsers();

		adapter = new MyUserAdapter(this, R.layout.mymultichoicelistrow, users);
		setListAdapter(adapter);

		findViewById(R.id.buttonAdd).setOnClickListener(this);
		findViewById(R.id.buttonCancel).setOnClickListener(this);

		handleIntent(getIntent());
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
				}
			}

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == android.R.id.list) {
			createContextMenu(menu);
		} else if (v.getId() == R.id.btnOptions) {
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
		switch (item.getItemId()) {
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
			sendMessageToUser();

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

		return true;
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
		builder.setTitle(getString(R.string.set_user_password, selectedUser)).setView(input)
				.setPositiveButton(android.R.string.ok, null)
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

	private void sendMessageToUser() {

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_NORMAL);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.write_your_message).setView(input)
				.setPositiveButton(android.R.string.ok, null)
				.setNegativeButton(android.R.string.cancel, null);
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
				dialog.dismiss();
				sendMessage(selectedUser, input.getText().toString());
			}

		});
	}

	private void sendMessage(final User selectedUser, final String message) {
		final ProgressDialog progressBar = new ProgressDialog(this);
		progressBar.setCancelable(true);
		progressBar.setMessage(getString(R.string.sending_message_));
		progressBar.setIndeterminate(true);
		progressBar.show();
		new AsyncTask<Void, Void, Void>() {
			protected void onPostExecute(Void result) {
				progressBar.dismiss();
			};

			@Override
			protected Void doInBackground(Void... params) {

				Credentials.testCredentials(BookActivity.this,
						new OnAsyncResponse() {

							@Override
							public void response(boolean success, String message) {
								if (success) {
									WebRequestResult result = HttpManager
											.messageToUser(selectedUser.id,
													message);

									if (result.result)
										Helper.showMessage(
												BookActivity.this,
												getString(R.string.message_successfully_delivered));
									else
										Helper.showMessage(
												BookActivity.this,
												getString(
														R.string.message_not_delivered_s,
														result.message));
								}
								else
								{
									Helper.showMessage(
											BookActivity.this,
											getString(
													R.string.message_not_delivered_s,
													message));
								}

							}
						});

				return null;
			}

		}.execute(null, null, null);

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

	static class ViewHolder implements OnClickListener {
		TextView text;
		ImageButton button;
		BookActivity context;
		User user;

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.text1:
				context.contactUser(user, null);
				break;
			case R.id.btnOptions:
				context.setSelectedUser(user);
				context.openContextMenu(v);
				break;
			}
		}

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
			viewHolder.text.setOnClickListener(viewHolder);

			viewHolder.button = (ImageButton) view
					.findViewById(R.id.btnOptions);
			viewHolder.button.setOnClickListener(viewHolder);
			view.setTag(viewHolder);
			viewHolder.context = context;
		} else {
			view = convertView;
		}
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		viewHolder.setTextStyle();
		viewHolder.button.setOnClickListener(viewHolder);
		return view;
	}

	public ArrayList<User> getUsers() {
		return users;
	}

}
