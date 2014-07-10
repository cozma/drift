package com.bah.mapping;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

import com.example.mapping.R;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.mapquest.android.maps.AnnotationView;
import com.mapquest.android.maps.GeoPoint;
import com.moodstocks.android.AutoScannerSession;
import com.moodstocks.android.Configuration;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class VideoActivity extends Activity implements OnInitListener,
		Scanner.SyncListener, AutoScannerSession.Listener {

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
	TextToSpeech tts;

	int mapZoom = 0;

	private HazardAPI hazard_api;

	/**
	 * These are the fields to setup the image scanner
	 */
	// Moodstocks API key/secret pair
	private static final String API_KEY = "ij3hks4yenn8abphhqpp";
	private static final String API_SECRET = "oJQ7dDkHU3ce6elQ";

	public static final String ENDPOINT = "http://54.198.34.151:7180";
	public static final String UNIQUE_TEAM_ID = "/LeGlass"; // add a slash if
															// used

	private boolean compatible = false;
	private Scanner scanner;

	private View resultView;
	private TextView resultID;

	private AutoScannerSession session = null;

	private static final int TYPES = Result.Type.IMAGE | Result.Type.QRCODE;

	Hazard foundHazard;

	/**
	 * Creates the activity for the smart path finder
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
		setContentView(R.layout.activity_scan);
		Configuration.platform = Configuration.Platform.GOOGLE_GLASS;
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// This is everything for Smart Pathfinder
		// ----------------------------------------------------------------------

		RestAdapter adapter = new RestAdapter.Builder().setEndpoint(ENDPOINT)
				.build();

		hazard_api = adapter.create(HazardAPI.class);
		initScanner();

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
	 * These are the controls for the menu that is displayed under the Path
	 * finder screen
	 */
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {

			switch (item.getItemId()) {
			case R.id.rec_vid:
				tts.speak("now recording", TextToSpeech.QUEUE_FLUSH, null);

				Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

				startActivityForResult(videoIntent, TAKE_VIDEO);
				//
				// showMessageAndSpeak("Video saved to"
				// + CameraManager.EXTRA_VIDEO_FILE_PATH);
				break;

			case R.id.return_map:

				Intent myIntentClick = new Intent(this, PathActivity.class);
				startActivity(myIntentClick);
			case R.id.exit_screen:
				this.finish();
			}
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * These are the controls for the menu that is displayed under the Path
	 * finder screen
	 */
	public boolean onOptionsItemSelected(int featureId, MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {

			switch (item.getItemId()) {
			case R.id.rec_vid:
				tts.speak("now recording", TextToSpeech.QUEUE_FLUSH, null);

				Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

				startActivityForResult(videoIntent, TAKE_VIDEO);
				//
				// showMessageAndSpeak("Video saved to"
				// + CameraManager.EXTRA_VIDEO_FILE_PATH);
				break;
			case R.id.return_map:
				Intent myIntentClick = new Intent(this, PathActivity.class);
				startActivity(myIntentClick);

			case R.id.exit_screen:
				this.finish();

			}
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Creates the menu for voice recognized commands
	 */
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {

			// Add items using approved voice command constants,
			getMenuInflater().inflate(R.menu.video, menu);
			return true;
		}
		// Otherwise, hand off to super.
		return super.onCreatePanelMenu(featureId, menu);
	}

	/**
	 * Creates the menu for gesture controlled functions
	 */
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.video, menu);
		return super.onCreateOptionsMenu(menu);
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
	}

	/*
	 * On application resume enable features of the overlay
	 * 
	 * @see android.app.Activity#onResume()
	 */
	protected void onResume() {
		super.onResume();
		session.start();
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
	 * This handles an exception if the camera fails to open
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

		// sendPosHazard("38.9", "-77");
		switch (result.getValue()) {
		case "ToxicHazard":
			foundHazard = new Hazard("1", "ToxicHazard", "1", "38.9", "-77",
					"38.9", "-77", "0", "toxic");
			break;
		case "VoltageHazard":
			foundHazard = new Hazard("1", "VoltageHazard", "2", "38.9", "-77",
					"38.9", "-77", "0", "voltage");
			break;
		case "RestrictedHazard":
			foundHazard = new Hazard("1", "RestrictedArea", "3", "38.9", "-77",
					"38.9", "-77", "0", "restricted");
			break;
		case "BioHazard":
			foundHazard = new Hazard("1", "BioHazard", "4", "38.9", "-77",
					"38.9", "-77", "0", "bio");
			break;
		case "FireHazard":
			foundHazard = new Hazard("1", "FireHazard", "5", "38.9", "-77",
					"38.9", "-77", "0", "fire");
			break;
		case "AndrewHazard":
			foundHazard = new Hazard("1", "AndrewHazard", "5", "38.9", "-77",
					"38.9", "-77", "0", "andrew");
			break;
		}

		sendHazard(foundHazard);

		tts.speak("Hazard Detected and Plotted", TextToSpeech.QUEUE_FLUSH, null);

		// resultView.setVisibility(View.INVISIBLE);
		session.resume();
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

	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub

	}
}
