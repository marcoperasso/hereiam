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
			context.downloadedPositions(positions);
			
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

	private MyApplication context;
		
	
	
	public PositionsDownloader(MyApplication context) {
		this.context = context;
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
	public void restart()
	{
		if (mHandler != null) {
			mHandler.removeCallbacks(this);
			mHandler.postDelayed(this, downloadPositionsInterval);
		}
	}
	private void downloadPositions() {
		if (Helper.isOnline(context) && context.getConnectorService() != null ) {
			//per scaricare le posizioni devo essere online e il connector deve essere attivo (se non le mando, non devo neanche riceverle)
			downloadPositionsTask = new DownloadPositionsAsyncTask().execute(null, null, null);
		}
		else
		{
			if (mHandler != null) {
				mHandler.postDelayed(PositionsDownloader.this, downloadPositionsInterval);
			}
			context.purgePositions();
		}

	}


	public void run() {
		downloadPositions();
	}
}
