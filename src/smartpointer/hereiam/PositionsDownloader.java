package smartpointer.hereiam;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Handler;

public class PositionsDownloader implements Runnable{
	 final class DownloadPositionsAsyncTask extends
			AsyncTask<Void, Void, ArrayList<UserPosition>> {
		private DownloadPositionsAsyncTask() {
			
		}

		@Override
		protected ArrayList<UserPosition> doInBackground(
				Void... params) {
			Thread.currentThread().setName("Positions Downloader Worker");

			return HttpManager.getPositions();
		}

		@Override
		protected void onPostExecute(
				ArrayList<UserPosition> positions) {
			mRoutesOverlay.setPositions(positions);
			downloadPositionsTask = null;
			if (mHandler != null) {
				mHandler.postDelayed(PositionsDownloader.this, downloadPositionsInterval);
			}
			super.onPostExecute(positions);
		}
	}

	private static final int downloadPositionsInterval = 10000;

	private Handler mHandler;

	private AsyncTask<Void, Void, ArrayList<UserPosition>> downloadPositionsTask;

	private UserPositionOverlay mRoutesOverlay;

	private MyMapActivity activity;

	public PositionsDownloader(UserPositionOverlay overlay,
			MyMapActivity activity) {
		this.mRoutesOverlay = overlay;
		this.activity = activity;
	}

	public void start() {
		mHandler = new Handler();
		downloadPositions();
	}

	public void stop() {
		if (downloadPositionsTask != null)
			downloadPositionsTask.cancel(false);
		mHandler.removeCallbacks(this);
		mHandler = null;
	}
	private void downloadPositions() {
		if (Helper.isOnline(activity)) {
			downloadPositionsTask = new DownloadPositionsAsyncTask().execute(null, null, null);
		}
		else
		{
			if (mHandler != null) {
				mHandler.postDelayed(PositionsDownloader.this, downloadPositionsInterval);
			}
		}

	}


	public void run() {
		downloadPositions();
	}
}
