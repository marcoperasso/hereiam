package smartpointer.hereiam;

import android.content.Context;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class HIALocationOverlay extends MyLocationOverlay {

	private MapController mController;
	private UserPositionOverlay mUsersOverlay;

	public HIALocationOverlay(Context context, MapView map,
			MapController controller, UserPositionOverlay routesOverlay) {
		super(context, map);
		this.mController = controller;
		this.mUsersOverlay = routesOverlay;
	}

	@Override
	public synchronized void onLocationChanged(Location loc) {
		// se ho il balloon aperto, seguo il balloon e non la mia posizione
		if (mUsersOverlay.getFocus() == null) {
			mController.animateTo(new GeoPoint((int) (loc.getLatitude() * 1e6),
					(int) (loc.getLongitude() * 1e6)));
		}
		super.onLocationChanged(loc);
	}
}
