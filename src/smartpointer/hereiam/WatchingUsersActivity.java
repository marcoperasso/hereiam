package smartpointer.hereiam;

import java.util.ArrayList;


import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class WatchingUsersActivity extends ListActivity implements OnClickListener {

	private ArrayList<User> users;
	private ArrayAdapter<User> adapter;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tracked_users);

		users = (ArrayList<User>) getIntent().getSerializableExtra(Const.USERS);

		adapter = new ArrayAdapter<User>(this,
				R.xml.mylist, users);
		setListAdapter(adapter);
		
		findViewById(R.id.buttonCancel).setOnClickListener(this);
		findViewById(R.id.buttonRemoveAll).setOnClickListener(this);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final int i = position;
		final User user = (User) l.getItemAtPosition(i);
		Helper.dialogMessage(this, WatchingUsersActivity.this.getString(R.string.stop_sending_your_position_to_user, user),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ConnectorService.stopSendingMyPositionToUser(user);
						finish();
					}					
				}, null);

		super.onListItemClick(l, v, position, id);
	}
	
	
	

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.buttonCancel)
			finish();
		else if (view.getId() == R.id.buttonRemoveAll)
			Helper.dialogMessage(this, R.string.do_you_want_to_stop_tracking_all_users,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							for (User u : users)
								ConnectorService.stopSendingMyPositionToUser(u);
							finish();
						}					
					}, null);
			
		
	}
}
