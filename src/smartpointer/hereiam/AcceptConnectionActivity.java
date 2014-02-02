package smartpointer.hereiam;



import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class AcceptConnectionActivity extends Activity implements
		OnClickListener {

	private User user;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accept_connection);
		user = (User) getIntent().getSerializableExtra(Const.USER);

		TextView tv = (TextView) findViewById(R.id.textViewQuestion);
		tv.setText(getString(R.string.wants_to_know_your_position_do_you_agree,
				user));

		findViewById(R.id.buttonYes).setOnClickListener(this);
		findViewById(R.id.buttonNo).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonYes:
			acceptUser();
			ConnectorService.activate(AcceptConnectionActivity.this, user, true, false);
			break;
		case R.id.buttonNo:
			rejectUser();
			break;
		}

	}

	private void acceptUser() {
		MyApplication.getInstance().respondToUser(user.id, Const.MSG_ACCEPT_CONTACT);
		finish();

	}

	private void rejectUser() {
		MyApplication.getInstance().respondToUser(user.id, Const.MSG_REJECT_CONTACT);
		finish();
	}

	
}
