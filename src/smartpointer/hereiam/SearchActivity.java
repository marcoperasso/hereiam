package smartpointer.hereiam;

import java.util.ArrayList;


import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SearchActivity extends ListActivity implements OnClickListener {

	private ArrayAdapter<User> adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);

		Intent intent = getIntent();
		String query = intent.getStringExtra(SearchManager.QUERY);
		doSearch(query);
		findViewById(R.id.buttonCancel).setOnClickListener(this);
		findViewById(R.id.buttonSearch).setOnClickListener(this);
		
		registerForContextMenu(findViewById(android.R.id.list));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		User u = (User) l.getItemAtPosition(position);
		contactUser(u, null);
		super.onListItemClick(l, v, position, id);
	}

	private void contactUser(User u, String pwd) {
		Intent intent = new Intent();
		intent.putExtra(Const.USER, u);
		intent.putExtra(Const.PASSWORD, pwd);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	
	private void doSearch(String query) {
		final ProgressDialog progressBar = new ProgressDialog(this);
		progressBar.setCancelable(true);
		progressBar.setMessage(getString(R.string.searching));
		progressBar.setIndeterminate(true);
		progressBar.show();
		new AsyncTask<String, Void, SearchPositionResult>() {

			
			@Override
			protected SearchPositionResult doInBackground(String... params) {
				String query = params[0];
				return HttpManager.getUsers(query);
			}

			@Override
			protected void onPostExecute(SearchPositionResult result) {

				adapter = new ArrayAdapter<User>(SearchActivity.this,
						R.xml.mylist, result.users);
				setListAdapter(adapter);
				
				TextView tv = (TextView) findViewById(R.id.textViewLabel);
				StringBuilder sb = new StringBuilder();
				if (result.users.size() > 0)
					sb.append(getString(R.string.found_user, result.users.size()));
				else
					sb.append(getString(R.string.no_user));
				
				if (result.total > result.users.size())
				{
					sb.append("\r\n");
					sb.append(getString(R.string.refine_query, result.users.size(), result.total));
				}
				tv.setText(sb.toString());
				progressBar.dismiss();
				if (!Helper.isNullOrEmpty(result.error))
						Helper.showMessage(SearchActivity.this, result.error);
				super.onPostExecute(result);
			}
		}.execute(query);
		
	}

	public void onClick(View v) {
		if (v.getId() == R.id.buttonCancel)
		{
			finish();
		}
		else if (v.getId() == R.id.buttonSearch)
		{
			Intent intent = new Intent();
			setResult(RESULT_OK, intent);
			finish();
		}
		
	}

}
class SearchPositionResult
{
	public ArrayList<User> users = new ArrayList<User>();
	public int total = 0;
	public String error;
	}
