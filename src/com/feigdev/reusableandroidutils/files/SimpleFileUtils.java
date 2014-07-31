package com.feigdev.reusableandroidutils.files;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.*;

/**
 * Created by ejf3 on 3/9/14.
 */
public class SimpleFileUtils {
	private static final String TAG = "SimpleFileUtils";

	public static File getDir() {
		String sdDir = Environment.getExternalStorageDirectory().toString();
		File relativeDir = new File(sdDir, "FeigdevUtils");

		if (!relativeDir.exists())
			relativeDir.mkdirs();

		Log.d(TAG, relativeDir.toString() + " exists? " + relativeDir.exists());
		return relativeDir;
	}

	public static File getCacheDir(Context c) {
		File cacheDir = c.getExternalCacheDir();

		if (null == cacheDir)
			cacheDir = c.getCacheDir();

		if (!cacheDir.exists())
			cacheDir.mkdirs();

		return cacheDir;
	}

	public static String getSdDir() {
		return Environment.getExternalStorageDirectory().toString();
	}

	public static String write(String filename, byte[] data) {
		File pictureFile = new File(filename);

		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(data);
			fos.close();
			Log.d(TAG, "New Image saved:" + filename);
		} catch (Exception error) {
			Log.e(TAG, "File" + filename + "not saved: ", error);
		}
		return filename;
	}

	public static String writeRelative(String relativeFilename, byte[] data) {
		File relativeDir = getDir();

		if (!relativeDir.exists()) {
			Log.e(TAG, "Couldn't make directory");
			return null;
		}

		String filename = relativeDir.getPath() + File.separator
				+ relativeFilename;
		write(filename, data);
		return filename;
	}

	public static String writeCache(Context context, String relativeFilename,
			byte[] data) {
		File relativeDir = getCacheDir(context);

		if (!relativeDir.exists()) {
			Log.e(TAG, "Couldn't make directory");
			return null;
		}

		String filename = relativeDir.getPath() + File.separator
				+ relativeFilename;
		write(filename, data);
		return filename;
	}

	public static void copy(String src, String dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

}
