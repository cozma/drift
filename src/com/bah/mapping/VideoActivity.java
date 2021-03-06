package com.bah.mapping;

import java.io.File;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

import com.example.mapping.R;
import com.feigdev.reusableandroidutils.graphics.PhotoCallback;
import com.feigdev.reusableandroidutils.graphics.PhotoHandler;
import com.google.android.glass.media.CameraManager;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.moodstocks.android.AutoScannerSession;
import com.moodstocks.android.Configuration;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.ShareCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bah.mapping.EyeGesture;
import com.bah.mapping.EyeGestureManager;
import com.bah.mapping.EyeGestureManager.Listener;

public class VideoActivity extends Activity implements OnInitListener,
		Scanner.SyncListener, AutoScannerSession.Listener, PhotoCallback {

	public final static String TAG = MainActivity.class.getSimpleName();

	// This is for the video recording
	// private static final String TAGVID = "Recorder";
	public static SurfaceView mSurfaceView;
	public static SurfaceHolder mSurfaceHolder;
	public static Camera mCamera;
	public static boolean mPreviewRunning;
	private static final int TAKE_PICTURE_REQUEST = 1;
	// private GestureDetector mGestureDetector;
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
	 * Eye Gesture Setup
	 */
	// private EyeGestureManager mEyeGestureManager;
	// private EyeGestureListener mEyeGestureListener;
	//
	// private EyeGesture distress = EyeGesture.DOUBLE_BLINK;

	private AudioManager success;

	// Variables for Light Sensor
	TextView lightAmountLevel;
	TextView tiltAmountLevel;
	private SensorManager sensorManager;
	private SensorEventListener sensorListener;

	private Camera camera;
	private String pictureFile;
	private String pictureSavePath;

	/**
	 * Creates the activity for the backlog and scanner
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
		setContentView(R.layout.activity_scan);
		Configuration.platform = Configuration.Platform.GOOGLE_GLASS;
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// This is everything for Smart Path Finder
		// ----------------------------------------------------------------------

		RestAdapter adapter = new RestAdapter.Builder().setEndpoint(ENDPOINT)
				.build();

		hazard_api = adapter.create(HazardAPI.class);

		// Light sensor
		lightAmountLevel = (TextView) findViewById(R.id.Light_Level);

		// Accelerometer
		tiltAmountLevel = (TextView) findViewById(R.id.Tilt_level);

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

		sensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);
		sensorListener = new SensorEventListener() {

			/**
			 * This will run a response of extremely high light levels & Sends a
			 * Hazard to the server
			 */
			public void onSensorChanged(SensorEvent event) {

				/**
				 * Sets up the Accelerometer for Immobility Checks
				 */
				if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
					// Check to see if user is tilted in a direction
					double tiltValue = event.values[0];
					DecimalFormat tiltLimit = new DecimalFormat("#.000");
					String tiltFormatted = tiltLimit.format(tiltValue);
					tiltAmountLevel.setText("TILT: " + tiltFormatted + "°");
					if ((tiltValue < -9) || (tiltValue > 9)) {
						// Send Hazard to map'
						setVisible();
						resultID.setText("Immobile Hazard");
						foundHazard = new Hazard("1", "ImmobileHazard", "8",
								"38.9", "-77", "38.9", "-77", "0", "8");
					}
					// else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
					// lightAmountLevel.setText("LIGHT: " + event.values[0]
					// + " SI Lux");
					// // tts.speak("Light Level " + event.values.toString(),
					// // TextToSpeech.QUEUE_FLUSH, null);
					// int maxLightLevel = 13000;
					// if (event.values[0] > 13000) {
					// // Sends a Flash Hazard off a large Light committed
					// ((AudioManager) getSystemService(Context.AUDIO_SERVICE))
					// .playSoundEffect(Sounds.ERROR);
					// resultID.setText("Flash Hazard");
					// foundHazard = new Hazard("1", "FlashHazard", "7",
					// "38.9", "-77", "38.9", "-77", "0", "7");
					//
					// }
					// }

				}

				// sendHazard(foundHazard); //Sends the Hazard to the server

			}

			/**
			 * If the accuracy changes
			 */
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub

			}

		};

		sensorManager.registerListener(sensorListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				sensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(sensorListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
				sensorManager.SENSOR_DELAY_GAME);

		new DriveUpload().execute();

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
			// preview.setZOrderOnTop(false); // necessary
			// SurfaceHolder sfhTrackHolder = preview.getHolder();
			// sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);

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
	 * Initiates an action for a tap of the glass gesture
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			Log.d(TAG, "Tapped (DPAD_CENTER)");
			openOptionsMenu(); // open the option menu on tap
			return true; // return true if you handled this event
		}
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			// Stop the preview and release the camera.
			// Execute your logic as quickly as possible
			// so the capture happens quickly.
			session.stop();
			//pictureTaken(pictureSavePath);
			//takePicture();
			return false;
		} else {
			return super.onKeyDown(keyCode, event);
		}

	}

	/**
	 * Provides the menu commands for the activity
	 */
	public void openMenu(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
		case R.id.rec_vid:
			tts.speak("now recording", TextToSpeech.QUEUE_FLUSH, null);

			session.stop();
			Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

			startActivityForResult(videoIntent, TAKE_VIDEO);

			// File directory = new File(
			// Environment
			// .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
			// "Video");
			//
			// File videoPath = new File(directory.getPath());
			//
			// tts.speak("file saved to " + videoPath, TextToSpeech.QUEUE_FLUSH,
			// null);

			break;
		case R.id.return_map:
			Intent myIntentClick = new Intent(this, PathActivity.class);
			startActivity(myIntentClick);

		case R.id.exit_screen:
			this.finish();
		}
		// session.start();
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

			openMenu(item);
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * These are the controls for the menu that is displayed under the Path
	 * finder screen
	 */
	public boolean onOptionsItemSelected(int featureId, MenuItem item) {
		openMenu(item);
		return super.onOptionsItemSelected(item);
	}

	/*
	 * Calls general motion events on the glass UNSTABLE
	 */
	public boolean onGenericMotionEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG,
					String.format("Motion ACTION_DOWN: %.1f / %.1f",
							event.getX(), event.getY()));
			// return true if you handled this event
			break;
		case MotionEvent.ACTION_MOVE:
			Log.d(TAG,
					String.format("Motion ACTION_MOVE: %.1f / %.1f",
							event.getX(), event.getY()));
			// return true if you handled this event
			break;
		case MotionEvent.ACTION_UP:
			Log.d(TAG, String.format("Motion ACTION_UP: %.1f / %.1f",
					event.getX(), event.getY()));
			// return true if you handled this event
			break;
		}

		return super.onGenericMotionEvent(event);
	}

	/**
	 * Manual Version of Picture Capture
	 */
	// private void takePicture() {
	// Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	// startActivityForResult(intent, TAKE_PICTURE_REQUEST);
	// }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
			String picturePath = data
					.getStringExtra(CameraManager.EXTRA_PICTURE_FILE_PATH);
			processPictureWhenReady(picturePath);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void processPictureWhenReady(final String picturePath) {
		final File pictureFile = new File(picturePath);
		pictureSavePath = picturePath;

		if (pictureFile.exists()) {
			// The picture is ready; process it.
		} else {
			// The file does not exist yet. Before starting the file observer,
			// you
			// can update your UI to let the user know that the application is
			// waiting for the picture (for example, by displaying the thumbnail
			// image and a progress indicator).

			final File parentDirectory = pictureFile.getParentFile();
			FileObserver observer = new FileObserver(parentDirectory.getPath(),
					FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
				// Protect against additional pending events after CLOSE_WRITE
				// or MOVED_TO is handled.
				private boolean isFileWritten;

				@Override
				public void onEvent(int event, String path) {
					if (!isFileWritten) {
						// For safety, make sure that the file that was created
						// in
						// the directory is actually the one that we're
						// expecting.
						File affectedFile = new File(parentDirectory, path);
						isFileWritten = affectedFile.equals(pictureFile);

						if (isFileWritten) {
							stopWatching();

							// Now that the file is ready, recursively call
							// processPictureWhenReady again (on the UI thread).
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									processPictureWhenReady(picturePath);
								}
							});
						}
					}
				}
			};
			observer.startWatching();
		}
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
		return true;
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
		Log.d("INIT:", "Sync will start.");
	}

	/**
	 * Logs the completion of the sync
	 */
	public void onSyncComplete() {
		try {
			Log.d("Sync", "Sync Complete (" + scanner.count() + " images)");
		} catch (MoodstocksError e) {
			e.printStackTrace();
		}
	}

	/**
	 * Logs the failure of the sync
	 */
	public void onSyncFailed(MoodstocksError e) {
		Log.d("ERROR",
				"Sync error #" + e.getErrorCode() + ": " + e.getMessage());
	}

	/**
	 * Logs the progress of the sync
	 */
	public void onSyncProgress(int total, int current) {
		int percent = (int) ((float) current / (float) total * 100);
		Log.d("CONT..", "Sync progressing: " + percent + "%");
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
		Log.d("Warning", "Sync is experiencing difficulties");

	}

	/**
	 * Set the visibility for Hazard prompt
	 */
	public void setVisible() {
		resultView.setVisibility(View.VISIBLE);
		new CountDownTimer(5000, 1000) {

			public void onTick(long millisUntilFinished) {
				// Do nothing
			}

			public void onFinish() {
				resultView.setVisibility(View.INVISIBLE);
			}
		}.start();
	}

	/**
	 * These actions are taken when a hazard is read to execute further actions
	 * and restart the process
	 */
	public void onResult(Result result) {
		// resultID.setText(result.getValue());
		setVisible();
		((AudioManager) getSystemService(Context.AUDIO_SERVICE))
				.playSoundEffect(Sounds.SUCCESS);

		// sendPosHazard("38.9", "-77");
		switch (result.getValue()) {
		case "ToxicHazard":
			resultID.setText("Toxic Hazard");
			foundHazard = new Hazard("1", "ToxicHazard", "1", "38.9", "-77",
					"38.9", "-77", "0", "1");
			break;
		case "VoltageHazard":
			resultID.setText("Voltage Hazard");
			foundHazard = new Hazard("1", "VoltageHazard", "2", "38.9", "-77",
					"38.9", "-77", "0", "2");
			break;
		case "RestrictedHazard":
			resultID.setText("Restricted Hazard");
			foundHazard = new Hazard("1", "RestrictedArea", "3", "38.9", "-77",
					"38.9", "-77", "0", "3");
			break;
		case "BioHazard":
			resultID.setText("Bio Hazard");
			foundHazard = new Hazard("1", "BioHazard", "4", "38.9", "-77",
					"38.9", "-77", "0", "4");
			break;
		case "FireHazard":
			resultID.setText("Fire Hazard");
			foundHazard = new Hazard("1", "FireHazard", "5", "38.9", "-77",
					"38.9", "-77", "0", "5");
			break;
		case "MikeHazard":
			resultID.setText("Enemy Target: Mike");
			foundHazard = new Hazard("1", "MikeHazard", "6", "38.9", "-77",
					"38.9", "-77", "0", "6");
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

	/**
	 * Needed to keep Activity from being abstract
	 */
	public void onInit(int status) {
		// Do nothing
	}

	@Override
	public void pictureTaken(String lastFile) {
		pictureFile = lastFile;
		//camera.startPreview();
	}

	/**
	 * Take a picture
	 */
	public void takePicture() {
		Log.d(TAG, "takePicture");
		camera.takePicture(null, null, new PhotoHandler(this));

	}

	/**
	 * Uploads the image captured to a Google Drive account
	 *
	 */
	private class DriveUpload extends AsyncTask<Void, Void, Void> {
		/**
		 * Background Actions
		 */
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "GlassPhotoDelay");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * Post Execution of picture taken
		 */
		protected void onPostExecute(Void params) {
			try {
				//

				// http://divingintoglass.blogspot.com/2014/01/how-to-upload-files-using-google-drive.html
				Uri imageUri = Uri.fromFile(new File(pictureSavePath));
				Intent shareIntent = ShareCompat.IntentBuilder
						.from(getParent()).setText("#glassgif #throughglass")
						.setType("image/gif").setStream(imageUri).getIntent()
						.setPackage("com.google.android.apps.docs");

				startActivity(shareIntent);

				getParent().finish();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

}
