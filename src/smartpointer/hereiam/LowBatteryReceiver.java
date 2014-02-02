package smartpointer.hereiam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LowBatteryReceiver extends BroadcastReceiver {
	public LowBatteryReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (MyApplication.LogEnabled)
			Log.i(Const.LOG_TAG,
				"Low battery, stopping connector service.");
		Intent service = new Intent(context, ConnectorService.class);
		context.startService(service);
	}
}
