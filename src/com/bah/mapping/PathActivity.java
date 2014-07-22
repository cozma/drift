package com.bah.mapping;

import java.util.ArrayList;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mapping.R;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.mapquest.android.maps.AnnotationView;
import com.mapquest.android.maps.BoundingBox;
import com.mapquest.android.maps.DefaultItemizedOverlay;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.ItemizedOverlay;
import com.mapquest.android.maps.LineOverlay;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.OverlayItem;
import com.mapquest.android.maps.RectangleOverlay;
import com.moodstocks.android.AutoScannerSession;
import com.moodstocks.android.Configuration;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;

public class PathActivity extends Activity implements SurfaceHolder.Callback {

	/*
	 * Alex's Variables for Networking + MapQuest
	 */

	public static final String ENDPOINT = "http://54.198.34.151:7180";
	public static final String UNIQUE_TEAM_ID = "/LeGlass"; // add a slash if
															// used

	List<Unit> units = null;
	List<Hazard> hazards = null;
	List<Path> paths = null;

	// TextView text;
	private Unit myself; // Hardcoded Coordinate unit

	/**
	 * Variables to set up mapview and location points for a user
	 */
	protected MapView map;
	private MyLocationOverlay myLocationOverlay;
	private List<GeoPoint> coordList;
	private GeoPoint currentLocation;
	private AnnotationView annotation;
	private List<GeoPoint> lineData;
	LineOverlay lineOverlay;

	float[] mGravity;
	float[] mGeomagnetic;
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;

	double azimut = 0;

	/*
	 * Dag's Code Zone All of your variables
	 */

	TextView coordTextView;
	View cardView;
	TextToSpeech tts;
	private static final int SPEECH_REQUEST = 0;
	public final static String TAG = MainActivity.class.getSimpleName();

	private GestureDetector mGestureDetector;

	/**
	 * Map objects that can be used for finalization but temporarily not used
	 * 
	 */
	SeekBar zoomLvl;
	TextView zoomLevel;
	Pattern mPattern;
	int mapZoom = 0;

	private UnitAPI unit_api;
	private HazardAPI hazard_api;
	private PathAPI path_api;

	/**
	 * Creates the activity for the path finder
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/**
		 * Setup the configurations to keep the screen on, feature voice
		 * commands on the activity and format the activity for glassware
		 */
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
		setContentView(R.layout.activity_map);
		Configuration.platform = Configuration.Platform.GOOGLE_GLASS;
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Checks the SDK version to avoid future errors
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		/*
		 * Initial Set Up
		 */
		RestAdapter adapter = new RestAdapter.Builder().setEndpoint(ENDPOINT)
				.build();
		unit_api = adapter.create(UnitAPI.class);
		hazard_api = adapter.create(HazardAPI.class);
		path_api = adapter.create(PathAPI.class);

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		coordList = new ArrayList<GeoPoint>();

		myself = new Unit("LeGlass", String.valueOf(azimut),
				String.valueOf(String.valueOf("33")), String.valueOf(String
						.valueOf("-77")), "friendly", "team1", "0");

		// This is everything for Smart Pathfinder
		// ----------------------------------------------------------------------
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		init(); // Initializes the path onto the map
		setupMyLocation(); // Sets up the user location (Mock Location)

		// Sets the scale of the map
		annotation = new AnnotationView(map);
		annotation.setScaleX(.35f);
		annotation.setScaleY(.35f);
		// this.map.setSatellite(true);

		/*
		 * Sets up the Speech Responses
		 */
		tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			/**
			 * TextToSpeech Initialization
			 */
			public void onInit(int status) {
				if (status == TextToSpeech.ERROR)
					Toast.makeText(getApplicationContext(), "TTS not working.",
							Toast.LENGTH_SHORT).show();
			}
		});

		coordTextView = (TextView) this.findViewById(R.id.coordTextView);
		// zoomLevel = (TextView) this.findViewById(R.id.zoomTex);

		mGestureDetector = createGestureDetector(this);

		// zoomLvl = (SeekBar) findViewById(R.id.zoomLvl);
		// zoomLvl.setMax(18);
		// zoomLvl.setLeft(2);
		// zoomLvl.incrementProgressBy(1);
		// zoomLvl.setProgress(map.getZoomLevel()); // Set it to zero so it will
		// zoomLevel.setSingleLine();

		/*
		 * Network Connection Polling
		 */

		// new Timer().scheduleAtFixedRate(new TimerTask() {
		// public void run() {
		// requestData();
		// }
		// }, 0, 1000);

		// // Eye Scanner
		// // Bind the service to get access to the getDirectionsToRestArea
		// method
		// bindService(new Intent(this, KeepAwakeService.class), mServiceConn,
		// 0);
		// if (mServiceBinder != null) {
		// mServiceBinder.onUserPassedOut();
		// ;
		// }

	}

	/**
	 * This creates the panel that appears after the "Ok Glass" command
	 * providing for a menu for voice control functions
	 */
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			// Add items using approved voice command constants,
			getMenuInflater().inflate(R.menu.map, menu);
			return true;
		}
		// Otherwise, hand off to super.
		return super.onCreatePanelMenu(featureId, menu);
	}

	/**
	 * This creates the options menu
	 */
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
		return true;
	}

	/**
	 * These are the controls for the menu that is displayed under the Path
	 * finder screen
	 */
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {

			switch (item.getItemId()) {
			case R.id.zoom_in: // Zooms in
				tts.speak("zooming in", TextToSpeech.QUEUE_FLUSH, null);
				map.getController().zoomIn();
				// mapZoom = map.getZoomLevel();
				// zoomLvl.setProgress(map.getZoomLevel());
				// zoomLevel.setText((map.getZoomLevel() * 7.14) + "%");
				break;
			case R.id.zoom_out: // Zooms out
				tts.speak("zooming out", TextToSpeech.QUEUE_FLUSH, null);
				map.getController().zoomOut();

				// zoomLvl.setProgress(map.getZoomLevel());
				// zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");
				break;
			case R.id.overview_path: // Shows an overview of the path
				tts.speak("viewing full path", TextToSpeech.QUEUE_FLUSH, null);
				overview();
				// zoomLvl.setProgress(map.getZoomLevel());
				// zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");
				break;
			case R.id.exit_screen:
				this.finish();
				break;
			case R.id.scan_start: // Starts Video Activity to start a scan
				Intent scan = new Intent(this, VideoActivity.class);
				this.startActivity(scan);
				break;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		// Create a base listener for generic gestures
		gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			/*
			 * Controls the responses to gesture commands
			 * 
			 * @see
			 * com.google.android.glass.touchpad.GestureDetector.BaseListener
			 * #onGesture(com.google.android.glass.touchpad.Gesture)
			 */
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TAP) {
					// do something on tap
					if (!overview()) {
						map.getController().setCenter(currentLocation);
						// zoomLvl.setProgress(map.getZoomLevel());
						// zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");
					} else {
						tts.speak("viewing full path",
								TextToSpeech.QUEUE_FLUSH, null);
						overview();
						// zoomLvl.setProgress(map.getZoomLevel());
						// zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

					}
					return true;
				} else if (gesture == Gesture.TWO_TAP) {
					// do something on two finger
					return true;
				} else if (gesture == Gesture.SWIPE_RIGHT) {
					// do something on right (forward) swipe
					map.getController().zoomIn();
					// zoomLvl.setProgress(map.getZoomLevel());
					// zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

					return true;
				} else if (gesture == Gesture.SWIPE_LEFT) {
					// do something on left (backwards) swipe
					map.getController().zoomOut();
					// zoomLvl.setProgress(map.getZoomLevel());
					// zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

					return true;
				} else if (gesture == Gesture.LONG_PRESS) {

				} else if (gesture == Gesture.SWIPE_UP) {

				} else if (gesture == Gesture.THREE_TAP) {

				} else if (gesture == Gesture.TWO_SWIPE_DOWN) {

				} else if (gesture == Gesture.TWO_SWIPE_LEFT) {
					map.getController().setZoom(6);
					// zoomLvl.setProgress(map.getZoomLevel());
					// zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

				} else if (gesture == Gesture.TWO_SWIPE_RIGHT) {
					map.getController().setZoom(14);
					// zoomLvl.setProgress(map.getZoomLevel());
					// zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

				} else if (gesture == Gesture.TWO_SWIPE_UP) {

				}
				return false;
			}
		});
		gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
			/**
			 * Counts fingers used
			 */
			public void onFingerCountChanged(int previousCount, int currentCount) {
				// do something on finger count changes
			}
		});
		gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
			/*
			 * Listens to scrolls
			 * 
			 * @see
			 * com.google.android.glass.touchpad.GestureDetector.ScrollListener
			 * #onScroll(float, float, float)
			 */
			public boolean onScroll(float displacement, float delta,
					float velocity) {
				// do something on scrolling
				return false;
			}
		});
		return gestureDetector;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#startActivityForResult(android.content.Intent,
	 * int, android.os.Bundle) Controls the start of the activity funcitons
	 */
	public void startActivityForResult(Intent intent, int requestCode,
			Bundle options) {
		super.startActivityForResult(intent, requestCode, options);
	}

	/**
	 * This is used to kill the application under the Exit command
	 */
	protected void onDestroy() {
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	/**
	 * This provides a scope of the path on the screen displaying all points of
	 * the mission
	 * 
	 * @return true if the overview view is in place on the map
	 */
	private boolean overview() {
		coordList.clear();
		for (Path p : paths) {
			coordList.add(new GeoPoint(p.getLat(), p.getLng()));
		}
		try {
			map.getController().zoomToSpan(
					BoundingBox.calculateBoundingBoxGeoPoint(coordList));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * This method provides a way to prompt the user with voice and text
	 * 
	 * @param message
	 *            the message that is being delivered to the user
	 */
	public void showMessageAndSpeak(String message) {
		((TextView) findViewById(R.id.coordTextView)).setText(message);
		tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
	}

	/**
	 * Controls the functionality of the buttons on click
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			Log.d(TAG, "Tapped (DPAD_CENTER)");
			// displaySpeechRecognizer();
			return true; // return true if you handled this event

		}
		return super.onKeyDown(keyCode, event);

	}

	/**
	 * Initialize the view.
	 */
	protected void init() {
		this.setupMapView(new GeoPoint(38.9, -77), 14);

	}

	/**
	 * This will set up a basic MapQuest map with zoom controls
	 */
	protected void setupMapView(GeoPoint pt, int zoom) {
		this.map = (MapView) findViewById(R.id.map);
		// set the zoom level
		map.getController().setZoom(zoom);
		// set the center point
		map.getController().setCenter(pt);
		// enable the zoom controls
		map.setBuiltInZoomControls(true);
	}

	/**
	 * Get the id of the layout file.
	 * 
	 * @return
	 */
	protected int getLayoutId() {
		return R.layout.activity_map;
	}

	/**
	 * Utility method for getting the text of an EditText, if no text was
	 * entered the hint is returned
	 * 
	 * @param editText
	 * @return the hint or the text value of EditText
	 */
	public String getText(EditText editText) {
		String s = editText.getText().toString();
		if ("".equals(s))
			s = editText.getHint().toString();
		return s;
	}

	/**
	 * Hides the soft keyboard
	 * 
	 * @param v
	 *            the current view
	 */
	public void hideSoftKeyboard(View v) {
		// hides soft keyboard
		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	// set up a MyLocationOverlay and execute the runnable once we have a
	// location fix
	private void setupMyLocation() {
		this.myLocationOverlay = new MyLocationOverlay(this, map);
		// myLocationOverlay.enableMyLocation();
		myLocationOverlay.runOnFirstFix(new Runnable() {
			/**
			 * Runs the setup for my location
			 */
			public void run() {
				currentLocation = new GeoPoint(38.9, -77);
				// myLocationOverlay.getMyLocation();
				map.getController().animateTo(currentLocation);
				map.getController().setCenter(currentLocation);
				map.getController().setZoom(14);
				map.getOverlays().add(myLocationOverlay);
				myLocationOverlay.setFollowing(true);
			}
		});
	}

	// /*
	// * On application resume enable features of the overlay
	// *
	// * @see android.app.Activity#onResume()
	// */
	protected void onResume() {
		// myLocationOverlay.enableMyLocation();
		// myLocationOverlay.enableCompass();
		super.onResume();
		// session.start();
	}

	/*
	 * disable features of the overlay when in the background
	 * 
	 * @see android.app.Activity#onPause()
	 */
	protected void onPause() {
		super.onPause();
		// myLocationOverlay.disableCompass();
		// myLocationOverlay.disableMyLocation();
	}

	// // /*
	// * Send generic motion events to the gesture detector
	// */
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}

	/*
	 * Provides a response to surface creation
	 * 
	 * @see
	 * android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder
	 * )
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

	/**
	 * Handles the change of the surface view
	 */
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	/**
	 * Handles cases of surfaces eliminated
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

	private void requestData() {
		RestAdapter adapter = new RestAdapter.Builder().setEndpoint(ENDPOINT)
				.build();

		UnitAPI unit_api = adapter.create(UnitAPI.class);
		HazardAPI hazard_api = adapter.create(HazardAPI.class);
		PathAPI path_api = adapter.create(PathAPI.class);

		/*
		 * GET All Unit Data
		 */

		unit_api.getUnits(new Callback<List<Unit>>() {

			@Override
			public void success(List<Unit> units, Response response) {
				PathActivity.this.units = units;
			}

			@Override
			public void failure(RetrofitError error) {
				// Log.d("Failure UnitGet()", error.getMessage());

			}
		});

		/*
		 * POST All Unit Data
		 */

		myself.setBearing(String.valueOf("25.53"));
		myself.setLat(String.valueOf("33"));
		myself.setLng(String.valueOf("-77"));

		unit_api.sendPos(myself, new Callback<Unit>() {

			@Override
			public void success(Unit t, Response response) {

			}

			@Override
			public void failure(RetrofitError error) {
				Log.d("Failure UnitPost()", "Unit Post Le Fail");
			}

		});

		/*
		 * GET Hazard Data
		 */

		hazard_api.getHazards(new Callback<List<Hazard>>() {

			@Override
			public void success(List<Hazard> hazards, Response response) {
				PathActivity.this.hazards = hazards;
			}

			@Override
			public void failure(RetrofitError error) {
				Log.d("Failure HazardGet()", error.getMessage());

			}
		});

		/*
		 * GET Path Data
		 */

		path_api.getPaths(new Callback<List<Path>>() {

			@Override
			public void success(List<Path> paths, Response response) {
				PathActivity.this.paths = paths;

			}

			@Override
			public void failure(RetrofitError error) {
				Log.d("Failure PathGet()", error.getMessage());

			}

		});

		/*
		 * Update Display with new Data
		 */

		PathActivity.this.runOnUiThread(new Runnable() {
			public void run() {
				updateDisplay();
			}
		});

	}

	private void updateDisplay() {
		removeOverlay();

		if (units != null) {
			addUnits();
		}
		if (hazards != null) {
			addHazard();
		}
		if (paths != null) {
			addLines();
		}

	}

	private void addUnits() {

		// list of GeoPoint objects to be used to draw line
		Drawable friendly = getResources().getDrawable(
				R.drawable.friendly_marker);
		Drawable neutral = getResources()
				.getDrawable(R.drawable.neutral_marker);
		Drawable enemy = getResources().getDrawable(R.drawable.enemy_marker);

		final DefaultItemizedOverlay friendlyPoi = new DefaultItemizedOverlay(
				friendly);
		for (Unit unit : units) {
			if (unit.getType().equals("friendly")) {
				friendlyPoi
						.addItem(new OverlayItem(new GeoPoint(unit.getLat(),
								unit.getLng()), unit.getID(), "Type: "
								+ unit.getType()));
			}

		}

		friendlyPoi.setTapListener(new ItemizedOverlay.OverlayTapListener() {
			public void onTap(GeoPoint pt, MapView mapView) {
				// when tapped, show the annotation for the overlayItem
				int lastTouchedIndex = friendlyPoi.getLastFocusedIndex();
				if (lastTouchedIndex > -1) {
					OverlayItem tapped = friendlyPoi.getItem(lastTouchedIndex);
					annotation.showAnnotationView(tapped);
				}
			}
		});
		friendlyPoi.setKey("friendlyPoi");
		map.getOverlays().add(friendlyPoi);

		final DefaultItemizedOverlay neutralPoi = new DefaultItemizedOverlay(
				neutral);
		for (Unit unit : units) {
			if (!(unit.getType().equals("enemy") || unit.getType().equals(
					"friendly"))) {
				neutralPoi
						.addItem(new OverlayItem(new GeoPoint(unit.getLat(),
								unit.getLng()), unit.getID(), "Type: "
								+ unit.getType()));
			}

		}

		neutralPoi.setTapListener(new ItemizedOverlay.OverlayTapListener() {
			public void onTap(GeoPoint pt, MapView mapView) {
				// when tapped, show the annotation for the overlayItem
				int lastTouchedIndex = neutralPoi.getLastFocusedIndex();
				if (lastTouchedIndex > -1) {
					OverlayItem tapped = neutralPoi.getItem(lastTouchedIndex);
					annotation.showAnnotationView(tapped);
				}
			}
		});
		neutralPoi.setKey("neutralPoi");
		map.getOverlays().add(neutralPoi);

		final DefaultItemizedOverlay enemyPoi = new DefaultItemizedOverlay(
				enemy);
		for (Unit unit : units) {
			if (unit.getType().equals("enemy")) {
				enemyPoi.addItem(new OverlayItem(new GeoPoint(unit.getLat(),
						unit.getLng()), unit.getID(), "Type: " + unit.getType()));
			}
		}

		enemyPoi.setTapListener(new ItemizedOverlay.OverlayTapListener() {
			public void onTap(GeoPoint pt, MapView mapView) {
				// when tapped, show the annotation for the overlayItem
				int lastTouchedIndex = enemyPoi.getLastFocusedIndex();
				if (lastTouchedIndex > -1) {
					OverlayItem tapped = enemyPoi.getItem(lastTouchedIndex);
					annotation.showAnnotationView(tapped);
				}
			}
		});
		enemyPoi.setKey("enemyPoi");
		map.getOverlays().add(enemyPoi);
	}

	private void addLines() {

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(10);
		/* list of GeoPoint objects to be used to draw line */
		lineData = new ArrayList<GeoPoint>();
		for (Path pt : paths) {
			lineData.add(new GeoPoint(pt.getLat(), pt.getLng()));
		}
		/* apply line style & data and add to map */
		lineOverlay = new LineOverlay(paint);
		lineOverlay.setKey("paths");
		lineOverlay.setData(lineData, true);
		map.getOverlays().add(lineOverlay);

	}

	private void addHazard() {

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLUE);
		paint.setStyle(Paint.Style.FILL);
		paint.setAlpha(40);
		for (Hazard hazard : hazards) {
			List<GeoPoint> hazard_values = new ArrayList<GeoPoint>();
			hazard_values.add(new GeoPoint(hazard.getLat1(), hazard.getLng1()));
			hazard_values.add(new GeoPoint(hazard.getLat2(), hazard.getLng2()));

			RectangleOverlay recOverlay = new RectangleOverlay(
					BoundingBox.calculateBoundingBoxGeoPoint(hazard_values),
					paint);
			recOverlay.setKey("hazards");

			map.getOverlays().add(recOverlay);
		}
	}

	private void removeOverlay() {
		map.getOverlays().clear();
	}

	protected boolean isRouteDisplayed() {
		return false;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;
		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
					mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				azimut = Math.toDegrees(orientation[0]); // orientation
															// contains: azimut,
															// pitch and roll
			}
		}
	}

	/**
	 * The initiation procedures
	 */
	public void onInit(int status) {
		// TODO Auto-generated method stub
	}

	private void sendHazard(Hazard hazard) {

		hazard_api.sendHazard(hazard, new Callback<Hazard>() {

			@Override
			public void success(Hazard t, Response response) {

			}

			@Override
			public void failure(RetrofitError error) {
				Log.d("Failure HazardPost()", error.getMessage().toString());
			}

		});
	}

}
