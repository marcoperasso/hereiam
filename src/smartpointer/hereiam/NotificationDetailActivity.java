package smartpointer.hereiam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class NotificationDetailActivity extends Activity implements
		OnClickListener {

	public static final String MESSAGE = "msg";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification_detail);

		findViewById(R.id.buttonOk).setOnClickListener(this);
		Intent intent = getIntent();
		if (intent.hasExtra(MESSAGE)) {
			TextView tv = (TextView) findViewById(R.id.textViewMessage);
			tv.setText(intent.getStringExtra(MESSAGE));
		}

	}

	@Override
	public void onClick(View arg0) {
		if (arg0.getId() == R.id.buttonOk)
			finish();

	}

}
