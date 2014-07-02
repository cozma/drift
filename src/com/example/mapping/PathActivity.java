package com.example.mapping;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class PathActivity extends Activity implements SurfaceHolder.Callback,
		OnInitListener, Scanner.SyncListener, AutoScannerSession.Listener {

	/*
	 * Alex's Variables for Networking + MapQuest
	 */

	final static String URL_path = "http://54.198.34.151:7180/path";
	final static String URL_units = "http://54.198.34.151:7180/units";
	final static String URL_hazards = "http://54.198.34.151:7180/hazards";
	private TreeMap<Integer, Coordinate> coords;
	private List<GeoPoint> coordList;
	private HttpClient client;
	private JSONObject read_json;
	private HashMap<String, List<GeoPoint>> hazardList;
	private List<GeoPoint> Geopts_for_Hazards;

	private List<GeoPoint> lineData;
	LineOverlay lineOverlay;

	GeoPoint start;

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
	AnnotationView annotation;
	protected MapView map;
	private MyLocationOverlay myLocationOverlay;
	TextToSpeech tts;
	public GeoPoint currentLocation;
	private static final int SPEECH_REQUEST = 0;
	public final static String TAG = MainActivity.class.getSimpleName();

	// This is for the video recording
	private static final String TAGVID = "Recorder";
	public static SurfaceView mSurfaceView;
	public static SurfaceHolder mSurfaceHolder;
	public static Camera mCamera;
	public static boolean mPreviewRunning;
	private GestureDetector mGestureDetector;
	private static final int TAKE_VIDEO = 101;

	SeekBar zoomLvl;
	TextView zoomLevel;
	Pattern mPattern;

	int mapZoom = 0;

	/**
	 * These are the fields to setup the image scanner
	 */
	// Moodstocks API key/secret pair
	private static final String API_KEY = "ij3hks4yenn8abphhqpp";
	private static final String API_SECRET = "oJQ7dDkHU3ce6elQ";

	private boolean compatible = false;
	private Scanner scanner;

	private View resultView;
	private TextView resultID;

	private AutoScannerSession session = null;

	private static final int TYPES = Result.Type.IMAGE | Result.Type.QRCODE;

	/**
	 * Creates the activity for the smart path finder
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
		setContentView(R.layout.activity_map);

		Configuration.platform = Configuration.Platform.GOOGLE_GLASS;
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		coords = new TreeMap<Integer, Coordinate>();
		coordList = new ArrayList<GeoPoint>();
		client = new DefaultHttpClient();
		tts = new TextToSpeech(this, this);
		// This is everything for Smart Pathfinder
		// ----------------------------------------------------------------------

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		init();
		setupMyLocation();

		annotation = new AnnotationView(map);
		annotation.setScaleX(.35f);
		annotation.setScaleY(.35f);
		// this.map.setSatellite(true);
		start = new GeoPoint(38.8977, -77.0366);
		map.getController().animateTo(start);
		map.getController().setCenter(start);

		coordTextView = (TextView) this.findViewById(R.id.coordTextView);
		zoomLevel = (TextView) this.findViewById(R.id.zoomTex);

		mGestureDetector = createGestureDetector(this);

		/* Networking Threads */

		Threads();

		coords = new TreeMap<Integer, Coordinate>();
		coordList = new ArrayList<GeoPoint>();
		hazardList = new HashMap<String, List<GeoPoint>>();
		client = new DefaultHttpClient();

		zoomLvl = (SeekBar) findViewById(R.id.zoomLvl);

		zoomLvl.setMax(18);
		zoomLvl.setLeft(2);
		// zoomLvl.incrementProgressBy(1);
		zoomLvl.setProgress(map.getZoomLevel()); // Set it to zero so it will
		zoomLevel.setSingleLine();

		initScanner();

	}

	/**
	 * Initialize the scanner
	 */
	public void initScanner() {
		compatible = Scanner.isCompatible();
		if (compatible) {
			try {
				scanner = Scanner.get();
				String path = Scanner.pathFromFilesDir(this, "scanner.db");
				scanner.open(path, API_KEY, API_SECRET);
				scanner.setSyncListener(this);
				scanner.sync();
			} catch (MoodstocksError e) {
				e.printStackTrace();
			}

			SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
			preview.setZOrderOnTop(false); // necessary
			SurfaceHolder sfhTrackHolder = preview.getHolder();
			sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);

			try {
				session = new AutoScannerSession(this, Scanner.get(), this,
						preview);
				session.setResultTypes(TYPES);
			} catch (MoodstocksError e) {
				e.printStackTrace();
			}
		}

		resultView = findViewById(R.id.result);
		resultID = (TextView) findViewById(R.id.result_id);
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
			case R.id.zoom_in:
				tts.speak("zooming in", TextToSpeech.QUEUE_FLUSH, null);
				map.getController().zoomIn();
				mapZoom = map.getZoomLevel();
				zoomLvl.setProgress(map.getZoomLevel());
				zoomLevel.setText((map.getZoomLevel() * 7.14) + "%");

				break;
			case R.id.zoom_out:
				tts.speak("zooming out", TextToSpeech.QUEUE_FLUSH, null);
				map.getController().zoomOut();

				zoomLvl.setProgress(map.getZoomLevel());
				zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

				break;
			case R.id.overview_path:
				tts.speak("viewing full path", TextToSpeech.QUEUE_FLUSH, null);
				overview();
				zoomLvl.setProgress(map.getZoomLevel());
				zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");
				break;
			case R.id.rec_vid:
				tts.speak("now recording", TextToSpeech.QUEUE_FLUSH, null);

				Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

				startActivityForResult(videoIntent, TAKE_VIDEO);
				//
				// showMessageAndSpeak("Video saved to"
				// + CameraManager.EXTRA_VIDEO_FILE_PATH);
				break;
			case R.id.exit_screen:
				this.finish();
			case R.id.scan_start:
				// Intent scan = new Intent(this, ScanActivity.class);
				// this.startActivity(scan);
				return true;
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
						zoomLvl.setProgress(map.getZoomLevel());
						zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");
					} else {
						tts.speak("viewing full path",
								TextToSpeech.QUEUE_FLUSH, null);
						overview();
						zoomLvl.setProgress(map.getZoomLevel());
						zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

					}

					return true;
				} else if (gesture == Gesture.TWO_TAP) {
					// do something on two finger
					return true;
				} else if (gesture == Gesture.SWIPE_RIGHT) {
					// do something on right (forward) swipe
					map.getController().zoomIn();
					zoomLvl.setProgress(map.getZoomLevel());
					zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

					return true;
				} else if (gesture == Gesture.SWIPE_LEFT) {
					// do something on left (backwards) swipe
					map.getController().zoomOut();
					zoomLvl.setProgress(map.getZoomLevel());
					zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

					return true;
				} else if (gesture == Gesture.LONG_PRESS) {

				} else if (gesture == Gesture.SWIPE_UP) {

				} else if (gesture == Gesture.THREE_TAP) {

				} else if (gesture == Gesture.TWO_SWIPE_DOWN) {

				} else if (gesture == Gesture.TWO_SWIPE_LEFT) {
					map.getController().setZoom(6);
					zoomLvl.setProgress(map.getZoomLevel());
					zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

				} else if (gesture == Gesture.TWO_SWIPE_RIGHT) {
					map.getController().setZoom(14);
					zoomLvl.setProgress(map.getZoomLevel());
					zoomLevel.setText(map.getZoomLevel() * 7.14 + "%");

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

	public void DecimalDigitsInputFilter(int digitsBeforeZero,
			int digitsAfterZero) {
		mPattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1)
				+ "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?");
	}

	public CharSequence filter(CharSequence source, int start, int end,
			Spanned dest, int dstart, int dend) {

		Matcher matcher = mPattern.matcher(dest);
		if (!matcher.matches())
			return "";
		return null;
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
		if (compatible) {
			try {
				scanner.close();
				scanner.destroy();
				scanner = null;
			} catch (MoodstocksError e) {
				e.printStackTrace();
			}
		}
		android.os.Process.killProcess(android.os.Process.myPid());
		client.getConnectionManager().shutdown();
	}

	/**
	 * This provides a scope of the path on the screen displaying all points of
	 * the mission
	 * 
	 * @return true if the overview view is in place on the map
	 */
	public boolean overview() {
		map.getController().zoomToSpan(
				BoundingBox.calculateBoundingBoxGeoPoint(coordList));
		return true;
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
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.runOnFirstFix(new Runnable() {
			/**
			 * Runs the setup for my location
			 */
			public void run() {
				currentLocation = myLocationOverlay.getMyLocation();
				map.getController().animateTo(currentLocation);
				map.getController().setCenter(currentLocation);
				map.getController().setZoom(14);
				map.getOverlays().add(myLocationOverlay);
				myLocationOverlay.setFollowing(true);
			}
		});
	}

	/*
	 * On application resume enable features of the overlay
	 * 
	 * @see android.app.Activity#onResume()
	 */
	protected void onResume() {
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.enableCompass();
		super.onResume();
		session.start();
	}

	/*
	 * disable features of the overlay when in the background
	 * 
	 * @see android.app.Activity#onPause()
	 */
	protected void onPause() {
		super.onPause();
		myLocationOverlay.disableCompass();
		myLocationOverlay.disableMyLocation();
	}

	/*
	 * Send generic motion events to the gesture detector
	 */
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

	/*
	 * GET Request Methods
	 */

	public class ReadWrite extends AsyncTask<String, Integer, String> {
		protected String doInBackground(String... param) {
			try {

				/*
				 * READ HERE
				 */
				read_json = getCoords();
				JSONArray data = read_json.getJSONArray("team1");
				JSONObject info;

				coords.clear();

				coordList.clear();

				for (int i = 0; i < data.length(); i++) {
					info = data.getJSONObject(i);
					Coordinate pt = new Coordinate(info.getInt("point"),
							info.getString("name"), info.getDouble("lat"),
							info.getDouble("lng"));
					coords.put(pt.getPoint(), pt);
					coordList.add(new GeoPoint(pt.getLat(), pt.getLong()));
				}

				read_json = getHazards();
				data = read_json.getJSONArray("hazards");
				hazardList.clear();
				for (int i = 0; i < data.length(); i++) {
					info = data.getJSONObject(i);
					Geopts_for_Hazards = new ArrayList<GeoPoint>();
					Geopts_for_Hazards = placeGeo(info.getInt("descriptor"),
							Geopts_for_Hazards, info);
					hazardList.put(
							info.getString("lat1") + info.getString("lng1")
									+ info.getString("lat2")
									+ info.getString("lng2"),
							Geopts_for_Hazards); // hash

				}

				/*
				 * WRITE HERE
				 */

				sendPos();

				return "Success";

			} catch (Exception e) {
				return "Fail";
			}
		}

		protected void onPostExecute(String result) {

			PathActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					removeOverlay("paths");
					addLines();
					removeOverlay("points");
					addPoints();
					removeOverlay("hazards");
					addHazard();
					overview();
				}
			});

		}
	}

	private List<GeoPoint> placeGeo(int descriptor, List<GeoPoint> list,
			JSONObject info) throws JSONException {
		if (descriptor == 4) {
			descriptor = 2;
			for (int i = 1; i <= descriptor; i++) {
				list.add(new GeoPoint(
						info.getDouble("lat" + String.valueOf(i)), info
								.getDouble("lng" + String.valueOf(i)))); /*
																		 * ORDER
																		 * DEPENDENT
																		 */
			}
		}

		return list;
	}

	/*
	 * Returns the JSON file as an object
	 */

	public JSONObject getCoords() throws ClientProtocolException, IOException,
			JSONException {
		StringBuilder url = new StringBuilder(URL_path);

		HttpGet get = new HttpGet(url.toString());

		HttpResponse response = client.execute(get);
		/* A successful connection */
		if (response.getStatusLine().getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			String data = EntityUtils.toString(entity);
			JSONObject last = new JSONObject(data);
			return last;
		} else {
			return null;
		}
	}

	public JSONObject getHazards() throws ClientProtocolException, IOException,
			JSONException {
		StringBuilder url = new StringBuilder(URL_hazards);

		HttpGet get = new HttpGet(url.toString());

		HttpResponse response = client.execute(get);
		/* A successful connection */
		if (response.getStatusLine().getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			String data = EntityUtils.toString(entity);
			JSONObject last = new JSONObject(data);
			return last;
		} else {
			return null;
		}
	}

	/*
	 * The actual data that the device is POSTing to the server
	 */

	public void sendPos() throws ClientProtocolException, IOException,
			URISyntaxException {

		StringBuilder url = new StringBuilder(URL_units);
		HttpPost request = new HttpPost(url.toString());

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("ID", "Le Glass"));
		nameValuePairs.add(new BasicNameValuePair("bearing", truncateDecimal(
				azimut, 4).toString()));
		nameValuePairs.add(new BasicNameValuePair("lat", "38.902317"));
		nameValuePairs.add(new BasicNameValuePair("lng", "-77.038899"));
		// nameValuePairs.add(new BasicNameValuePair("lat",
		// truncateDecimal(currentLocation.getLatitude(),4).toString()));
		// nameValuePairs.add(new BasicNameValuePair("lng",
		// truncateDecimal(currentLocation.getLongitude(),4).toString()));
		nameValuePairs.add(new BasicNameValuePair("type", "neutral"));

		UrlEncodedFormEntity form = new UrlEncodedFormEntity(nameValuePairs,
				"UTF-8");
		request.setEntity(form);
		client.execute(request);
	}

	private static BigDecimal truncateDecimal(double x, int numberofDecimals) {
		if (x > 0) {
			return new BigDecimal(String.valueOf(x)).setScale(numberofDecimals,
					BigDecimal.ROUND_FLOOR);
		} else {
			return new BigDecimal(String.valueOf(x)).setScale(numberofDecimals,
					BigDecimal.ROUND_CEILING);
		}
	}

	/*
	 * UI Updating Methods
	 */

	private void addPoints() {

		// list of GeoPoint objects to be used to draw line
		Drawable icon = getResources().getDrawable(R.drawable.location_marker);
		final DefaultItemizedOverlay poiOverlay = new DefaultItemizedOverlay(
				icon);
		for (Coordinate coord : coords.values()) {
			if (!coord.getName().equals("NONE")) {
				poiOverlay.addItem(new OverlayItem(new GeoPoint(coord.getLat(),
						coord.getLong()), coord.getName(), "Checkpoint #"
						+ coord.getPoint()));
			}
		}

		poiOverlay.setTapListener(new ItemizedOverlay.OverlayTapListener() {
			public void onTap(GeoPoint pt, MapView mapView) {
				// when tapped, show the annotation for the overlayItem
				int lastTouchedIndex = poiOverlay.getLastFocusedIndex();
				if (lastTouchedIndex > -1) {
					OverlayItem tapped = poiOverlay.getItem(lastTouchedIndex);
					annotation.showAnnotationView(tapped);
				}
			}
		});
		poiOverlay.setKey("points");
		map.getOverlays().add(poiOverlay);
	}

	private void addLines() {

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(10);
		/* list of GeoPoint objects to be used to draw line */
		lineData = new ArrayList<GeoPoint>();
		for (Coordinate pt : coords.values()) {
			lineData.add(new GeoPoint(pt.getLat(), pt.getLong()));

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

		for (String key : hazardList.keySet()) {
			List<GeoPoint> hazard_values = hazardList.get(key);

			RectangleOverlay recOverlay = new RectangleOverlay(
					BoundingBox.calculateBoundingBoxGeoPoint(hazard_values),
					paint);
			recOverlay.setKey("hazards");
			map.getOverlays().add(recOverlay);

			// if(hazard_values.size() == 1){
			//
			// final int level =
			// Integer.parseInt(key.substring(0,key.indexOf(".")));
			// final String name = key.substring(key.indexOf(".")+1);
			// final double lat = hazard_values.get(0).getLatitude();
			// final double lng = hazard_values.get(0).getLongitude();
			// MainActivity.this.runOnUiThread(new Runnable() {
			// public void run() {
			// addSingularHazard(new Coordinate(level, name, lat, lng));
			// }
			// });
			// }else{

			// }
		}
	}

	protected boolean isRouteDisplayed() {
		return false;
	}

	/*
	 * ASync Threads for POST and GET requests
	 */

	private void Threads() {

		/*
		 * Polls the GET requests every 1 second(s), starting at runtime.
		 */

		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				long startTime = System.currentTimeMillis();

				new ReadWrite().execute("NIL");

				long stopTime = System.currentTimeMillis();
				long elapsedTime = stopTime - startTime;

				Log.d("THREAD TIME: ", String.valueOf(elapsedTime));
			}

		}, 6000, 4000);

	}

	private void removeOverlay(String key) {
		for (int i = 0; i < map.getOverlays().size(); i++) {
			if (map.getOverlays().get(i).getKey() == key) {
				map.getOverlays().remove(i);
			}
		}
	}

	/* Test for Hazard send */

	public void sendPosHazard(String lat1, String lng1)
			throws ClientProtocolException, IOException, URISyntaxException {

		StringBuilder url = new StringBuilder(URL_hazards);
		HttpPost request = new HttpPost(url.toString());

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("descriptor", "1"));
		nameValuePairs.add(new BasicNameValuePair("name", "MOCK"));
		nameValuePairs.add(new BasicNameValuePair("level", "2"));

		nameValuePairs.add(new BasicNameValuePair("lat1", lat1));
		nameValuePairs.add(new BasicNameValuePair("lng1", lng1));

		UrlEncodedFormEntity form = new UrlEncodedFormEntity(nameValuePairs,
				"UTF-8");
		request.setEntity(form);
		client.execute(request);
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
	 * ------------------------------------------------------------------//
	 * 
	 * 
	 * 
	 * These are the scanner methods
	 * 
	 * 
	 * 
	 * ------------------------------------------------------------------//
	 */

	/**
	 * Logging the start of the sync
	 */
	public void onSyncStart() {
		Log.d("Moodstocks SDK", "Sync will start.");
	}

	/**
	 * Logs the completion of the sync
	 */
	public void onSyncComplete() {
		try {
			Log.d("Moodstocks SDK", "Sync succeeded (" + scanner.count()
					+ " images)");
		} catch (MoodstocksError e) {
			e.printStackTrace();
		}
	}

	/**
	 * Logs the failure of the sync
	 */
	public void onSyncFailed(MoodstocksError e) {
		Log.d("Moodstocks SDK",
				"Sync error #" + e.getErrorCode() + ": " + e.getMessage());
	}

	/**
	 * Logs the progress of the sync
	 */
	public void onSyncProgress(int total, int current) {
		int percent = (int) ((float) current / (float) total * 100);
		Log.d("Moodstocks SDK", "Sync progressing: " + percent + "%");
	}

	/**
	 * This handles an exeption if the camera fails to open
	 */
	public void onCameraOpenFailed(Exception e) {
		// Implemented in a few steps
	}

	/**
	 * This prints a debug message if there is a problem presented
	 */
	public void onWarning(String debugMessage) {
		// Implemented in a few steps
	}

	/**
	 * These actions are taken when a hazard is read to execute further actions
	 * and restart the process
	 */
	public void onResult(Result result) {
		// resultID.setText(result.getValue());
		// resultView.setVisibility(View.VISIBLE);
		((AudioManager) getSystemService(Context.AUDIO_SERVICE))
				.playSoundEffect(Sounds.SUCCESS);
		try {
			sendPosHazard("38.9", "-77");
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tts.speak("Hazard Detected and Plotted", TextToSpeech.QUEUE_FLUSH, null);

		// resultView.setVisibility(View.INVISIBLE);
		session.resume();
	}

	/**
	 * The initiation procedures
	 */
	public void onInit(int status) {
		// TODO Auto-generated method stub

	}

}
