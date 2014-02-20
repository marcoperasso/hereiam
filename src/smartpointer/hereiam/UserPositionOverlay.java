package smartpointer.hereiam;

import java.util.ArrayList;
import java.util.Date;

import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class UserPositionOverlay extends BalloonItemizedOverlay<OverlayItem> implements OnClickListener {

	
	// private ImageView mImageView;
	
	// Bitmap trackBitmap;
	GeoPoint trackRectOrigin;
	int currentZoomLevel = -1;
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private ArrayList<UserPosition> mPositions = new ArrayList<UserPosition>();
	
	private MyMapActivity mContext;

	public UserPositionOverlay(Drawable defaultMarker, MyMapActivity context,
			MapView map) {
		super(boundCenterBottom(defaultMarker), map);
		super.setSnapToCenter(true);
		mContext = context;
		// mImageView = new ImageView(mContext);
		

		setLastFocusedIndex(-1);

		populate();
	}

	@Override
	protected void onBalloonOpen(int index) {
		if (index < mPositions.size()) {
			MyApplication.getInstance().setPinnedUser(mPositions.get(index).getUser());

		}
		super.onBalloonOpen(index);
	}

	@Override
	protected void onBalloonHide() {
		MyApplication.getInstance().setPinnedUser(null);
		super.onBalloonHide();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	public void draw(final Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
	}

	public void setPositions(ArrayList<UserPosition> positions) {
		mPositions = positions;
		mOverlays.clear();

		OverlayItem itemToFocus = null;
		for (UserPosition up : positions) {
			GeoPoint point = new GeoPoint(up.getPosition().lat, up.getPosition().lon);
			java.text.DateFormat timeFormat = DateFormat
					.getTimeFormat(mContext);
			Date df = new java.util.Date(up.getPosition().time * 1000);
			String title = up.getUser().toString();
			String text = mContext.getString(R.string.balloon_text, timeFormat.format(df),
					up.getAddress(),
					mContext.getString(up.isGps() ? R.string.yes : R.string.no));
			OverlayItem overlayitem = new OverlayItem(point,
					title, text);
			mOverlays.add(overlayitem);
			
			User pinnedUser = MyApplication.getInstance().getPinnedUser();
			if (pinnedUser != null && up.getUser().phone.equals(pinnedUser.phone))
			{
				itemToFocus = overlayitem;
			}
		}
		// Workaround for bug that Google refuses to fix:
        // <a href="http://osdir.com/ml/AndroidDevelopers/2009-08/msg01605.html">http://osdir.com/ml/AndroidDevelopers/2009-08/msg01605.html</a>
        // <a href="http://code.google.com/p/android/issues/detail?id=2035">http://code.google.com/p/android/issues/detail?id=2035</a>
        setLastFocusedIndex(-1);
		populate();
		if (itemToFocus != null) //� cambiata la posizione dove visualizzare il balloon
			setFocus(itemToFocus);
		
	}

	public ArrayList<UserPosition> getPositions() {
		return mPositions;
	}

	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.balloon_disconnect)
		{
			Helper.dialogMessage(mContext, mContext.getString(R.string.do_you_want_to_stop_tracking_this_user, MyApplication.getInstance().getPinnedUser()),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							TrackedUsersActivity.disconnectUser(MyApplication.getInstance().getPinnedUser());
							hideBalloon();
						}					
					}, null);

		}
		if (v.getId() == R.id.balloon_message)
		{
			MessageActivity.sendMessageToUser(mContext, MyApplication.getInstance().getPinnedUser());
		}
		
		
	}


}
