package com.bah.mapping;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mapping.R;
import com.example.mapping.R.id;
import com.example.mapping.R.layout;
import com.example.mapping.R.menu;
import com.example.mapping.R.raw;
import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.moodstocks.android.AutoScannerSession;
import com.moodstocks.android.Configuration;
import com.moodstocks.android.MoodstocksError;
import com.moodstocks.android.Result;
import com.moodstocks.android.Scanner;

public class MainActivity extends Activity {

	public final static String TAG = MainActivity.class.getSimpleName();
	TextView coordTextView;
	SoundPool soundPool;
	int soundID;
	TextToSpeech tts;
	GestureDetector gestureDetector;
	TextToSpeech ttsInit;

	/**
	 * Initialization of the class
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
		setContentView(R.layout.activity_main);

		coordTextView = (TextView) this.findViewById(R.id.coordTextView);

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// create tts
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

		ttsInit = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			/**
			 * TextToSpeech Initialization
			 */
			public void onInit(int status) {
				if (status == TextToSpeech.ERROR)
					Toast.makeText(getApplicationContext(), "TTS not working.",
							Toast.LENGTH_SHORT).show();
			}
		});

		// test("Alex This is what I am sending to Alex.");
		// Card menu = new Card(this);
		// menu.setText("Choose Guide");
		// menu.setFootnote("This is the menu.");

		// play opening sound
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		soundID = soundPool.load(this, R.raw.shot, 1);
		soundPool
				.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
					@Override
					public void onLoadComplete(SoundPool arg0, int arg1,
							int arg2) {
						soundPool.play(soundID, 1, 1, 1, 0, 1);
					}
				});

		coordTextView.setText("drift");
		coordTextView.setTypeface(null, Typeface.BOLD);

	}

	/**
	 * Creates the menu for voice recognized commands
	 */
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {

			// Add items using approved voice command constants,
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		}
		// Otherwise, hand off to super.
		return super.onCreatePanelMenu(featureId, menu);
	}

	/**
	 * Creates the menu for gesture controlled functions
	 */
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
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
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Response to the selection of a menu item
	 */
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			switch (item.getItemId()) {
			case R.id.menu_item_path:
				// showMessageAndSpeak("Guided Path");

				Intent myIntent = new Intent(this, PathActivity.class);
				// myIntent.putExtra("key", null); //Optional parameters
				this.startActivity(myIntent);
				return true;
			case R.id.menu_item_vid:
				startActivity(new Intent(this, VideoActivity.class));
			}
		}

		return onOptionsItemSelected(item); // Calls the gesture version

	}

	/**
	 * Response to a selection of a option item
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_path:
			// showMessageAndSpeak("Guided Path");

			Intent myIntentClick = new Intent(this, PathActivity.class);
			// myIntent.putExtra("key", null); //Optional parameters
			this.startActivity(myIntentClick);
			return true;
		case R.id.menu_item_vid:
			startActivity(new Intent(this, VideoActivity.class));
		}
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

	/*
	 * Has the input read aloud and shown on screen simultaneously
	 */
	public void showMessageAndSpeak(String message) {
		coordTextView.setText(message);
		tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
	}

}
