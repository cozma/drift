package com.bah.mapping;

/**
 * @author Victor Kaiser-Pendergrast
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.bah.mapping.SleepDetector;

public class KeepAwakeService extends Service implements
		SleepDetector.SleepListener {
	private static final String TAG = "KeepAwakeService";
	private TextToSpeech mTTS;
	Boolean isAsleep = false;

	public class KeepAwakeBinder extends Binder {

		public void onUserPassedOut() {
			KeepAwakeService.this.onUserFallingAsleep();
			if (isAsleep) {
				Log.i(TAG, "User is falling asleep");

				// Start commands for alerting the system user and the server
				mTTS.speak("WAKE THE FUCK UP", TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	}

	private Context mContext;

	private SleepDetector mSleepDetector;

	@Override
	public void onCreate() {
		super.onCreate();

		mSleepDetector = new SleepDetector(mContext, this);
		mSleepDetector.setupReceiver();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {

		mSleepDetector.removeReceiver();

		super.onDestroy();
	}

	// @Override
	// public void onUserFallingAsleep() {
	// Log.i(TAG, "User is falling asleep");
	//
	// // Start commands for alerting the system user and the server
	// mTTS.speak(getString(R.string.speech_wake_up), TextToSpeech.QUEUE_FLUSH,
	// null);
	//
	// }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub

		return null;
	}

	@Override
	public void onUserFallingAsleep() {
		isAsleep = true;
	}

}
