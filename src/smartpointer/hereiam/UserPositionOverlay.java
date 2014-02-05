package smartpointer.hereiam;

import java.util.ArrayList;
import java.util.Date;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class UserPositionOverlay extends BalloonItemizedOverlay<OverlayItem> {

	
	// private ImageView mImageView;
	private Paint pnt = new Paint();
	// Bitmap trackBitmap;
	GeoPoint trackRectOrigin;
	int currentZoomLevel = -1;
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private ArrayList<UserPosition> mPositions = new ArrayList<UserPosition>();
	private User pinnedUser;
	private MyMapActivity mContext;

	public UserPositionOverlay(Drawable defaultMarker, MyMapActivity context,
			MapView map) {
		super(boundCenterBottom(defaultMarker), map);
		super.setSnapToCenter(true);
		mContext = context;
		// mImageView = new ImageView(mContext);
		pnt.setStyle(Paint.Style.FILL);
		pnt.setStrokeWidth(4);
		pnt.setAntiAlias(true);


		setLastFocusedIndex(-1);

		populate();
	}

	@Override
	protected void onBalloonOpen(int index) {
		if (index < mPositions.size()) {
			pinnedUser = mPositions.get(index).getUser();

		}
		super.onBalloonOpen(index);
	}

	@Override
	protected void onBalloonHide() {
		pinnedUser = null;
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
			
			if (pinnedUser != null && pinnedUser.id == up.getUser().id)
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

	public void pinTo(User user) {
		pinnedUser = user;
		
	}

	User getPinnedUser() {
		return pinnedUser;
	}


}
