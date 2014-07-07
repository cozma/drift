package com.bah.mapping;

/**
 * @author Victor Kaiser-Pendergrast
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.bah.mapping.SleepDetector;

public class KeepAwakeService extends Service implements
		SleepDetector.SleepListener {
	private static final String TAG = "KeepAwakeService";

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

	@Override
	public void onUserFallingAsleep() {
		Log.i(TAG, "User is falling asleep");

		// Start commands for alerting the system user and the server

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
