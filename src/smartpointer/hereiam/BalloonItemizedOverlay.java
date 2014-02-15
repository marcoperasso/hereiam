package smartpointer.hereiam;



import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public abstract class BalloonItemizedOverlay<Item extends OverlayItem> extends ItemizedOverlay<Item> implements OnClickListener {

	private static final long BALLOON_INFLATION_TIME = 300;
	private static Handler handler = new Handler();
	
	private MapView mapView;
	protected BalloonOverlayView balloonView;
	private View clickRegion;
	private View closeRegion;
	private int viewOffset;
	final MapController mc;
	private Item currentFocusedItem;
	protected int currentFocusedIndex;
	
	private boolean showClose = true;
	private boolean showDisclosure = false;
	private boolean snapToCenter = true;
	
	private static boolean isInflating = false;
	
	public BalloonItemizedOverlay(Drawable defaultMarker, MapView mapView) {
		super(defaultMarker);
		this.mapView = mapView;
		viewOffset = 0;
		mc = mapView.getController();
	}
	
	public void setBalloonBottomOffset(int pixels) {
		viewOffset = pixels;
	}
	public int getBalloonBottomOffset() {
		return viewOffset;
	}
	
	protected boolean onBalloonTap(int index, Item item) {
		return false;
	}
	
	protected void onBalloonOpen(int index) {}
	protected void onBalloonHide() {}
	@Override
	public final boolean onTap(int index) {
		
		handler.removeCallbacks(finishBalloonInflation);
		isInflating = true;
		handler.postDelayed(finishBalloonInflation, BALLOON_INFLATION_TIME);
		
		currentFocusedIndex = index;
		currentFocusedItem = createItem(index);
		setLastFocusedIndex(index);
		
		onBalloonOpen(index);
		createAndDisplayBalloonOverlay();
		
		if (snapToCenter) {
			animateTo(currentFocusedItem.getPoint());
		}
		
		return true;
	}

	protected void animateTo(GeoPoint center) {
		mc.animateTo(center);
	}

	protected BalloonOverlayView createBalloonOverlayView() {
		return new BalloonOverlayView(getMapView().getContext(), getBalloonBottomOffset());
	}
	
	protected MapView getMapView() {
		return mapView;
	}

	public void bringBalloonToFront() {
		if (balloonView != null) {
			balloonView.bringToFront();
		}
	}

	public void hideBalloon() {
		if (balloonView != null) {
			balloonView.setVisibility(View.GONE);
		}
		currentFocusedItem = null;
		onBalloonHide();
	}
	
	private void hideOtherBalloons(List<Overlay> overlays) {
		
		for (Overlay overlay : overlays) {
			if (overlay instanceof BalloonItemizedOverlay<?> && overlay != this) {
				((BalloonItemizedOverlay<?>) overlay).hideBalloon();
			}
		}
		
	}
	
	public void hideAllBalloons() {
		if (!isInflating) {
			List<Overlay> mapOverlays = mapView.getOverlays();
			if (mapOverlays.size() > 1) {
				hideOtherBalloons(mapOverlays);
			}
			hideBalloon();
		}
	}
	
	private OnTouchListener createBalloonTouchListener() {
		return new OnTouchListener() {
			
			float startX;
			float startY;
			
			public boolean onTouch(View v, MotionEvent event) {
				
				View l =  ((View) v.getParent()).findViewById(R.id.balloon_main_layout);
				Drawable d = l.getBackground();
				
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (d != null) {
						int[] states = {android.R.attr.state_pressed};
						if (d.setState(states)) {
							d.invalidateSelf();
						}
					}
					startX = event.getX();
					startY = event.getY();
					return true;
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					if (d != null) {
						int newStates[] = {};
						if (d.setState(newStates)) {
							d.invalidateSelf();
						}
					}
					if (Math.abs(startX - event.getX()) < 40 && 
							Math.abs(startY - event.getY()) < 40 ) {
						// call overridden method
						onBalloonTap(currentFocusedIndex, currentFocusedItem);
					}
					return true;
				} else {
					return false;
				}
				
			}
		};
	}
	
	@Override
	public Item getFocus() {
		return currentFocusedItem;
	}
	@Override
	public void setFocus(Item item) {
		super.setFocus(item);	
		currentFocusedIndex = getLastFocusedIndex();
		currentFocusedItem = item;
		if (currentFocusedItem == null) {
			hideBalloon();
		} else {
			createAndDisplayBalloonOverlay();
		}	
		
		if (snapToCenter && currentFocusedItem != null) {
			animateTo(currentFocusedItem.getPoint());
		}
	}
	private boolean createAndDisplayBalloonOverlay(){
		boolean isRecycled;
		if (balloonView == null) {
			balloonView = createBalloonOverlayView();
			clickRegion = (View) balloonView.findViewById(R.id.balloon_inner_layout);
			clickRegion.setOnTouchListener(createBalloonTouchListener());
			closeRegion = (View) balloonView.findViewById(R.id.balloon_close);
			
			View disconnect = (View) balloonView.findViewById(R.id.balloon_disconnect);
			disconnect.setOnClickListener(this);
			
			View msg = (View) balloonView.findViewById(R.id.balloon_message);
			msg.setOnClickListener(this);
			
			if (closeRegion != null) {
			if (!showClose) {
					closeRegion.setVisibility(View.GONE);
				} else {
					closeRegion.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							hideBalloon();	
						}
					});
				}
			}
			if (showDisclosure && !showClose) {
				View v = balloonView.findViewById(R.id.balloon_disclosure); 
				if (v != null) {
					v.setVisibility(View.VISIBLE);
				}
			}
			isRecycled = false;
		} else {
			isRecycled = true;
		}
	
		balloonView.setVisibility(View.GONE);
		
		List<Overlay> mapOverlays = mapView.getOverlays();
		if (mapOverlays.size() > 1) {
			hideOtherBalloons(mapOverlays);
		}
		
		if (currentFocusedItem != null)
			balloonView.setData(currentFocusedItem);
		
		GeoPoint point = currentFocusedItem.getPoint();
		MapView.LayoutParams params = new MapView.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, point,
				MapView.LayoutParams.BOTTOM_CENTER);
		params.mode = MapView.LayoutParams.MODE_MAP;
		
		balloonView.setVisibility(View.VISIBLE);
		
		if (isRecycled) {
			balloonView.setLayoutParams(params);
		} else {
			mapView.addView(balloonView, params);
		}
		
		return isRecycled;
	}
	
	public void setShowClose(boolean showClose) {
		this.showClose = showClose;
	}

	public void setShowDisclosure(boolean showDisclosure) {
		this.showDisclosure = showDisclosure;
	}

	public void setSnapToCenter(boolean snapToCenter) {
		this.snapToCenter = snapToCenter;
	}

	public static boolean isInflating() {
		return isInflating;
	}
	
	private static Runnable finishBalloonInflation = new Runnable() {
		public void run() {
			isInflating = false;
		}
	};
	
}