package com.perfectcorp.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.view.Gravity;
import android.widget.Toast;

public class Log {
	public static final String LOG_TAG = "LOG_TAG_BC";
	public static boolean IsDeveloperMode = false;
	private static final String TIMESTAMP = "timestamp";
	private static final String FOLDER_NAME = "Beauty Circle";
	private static final String LOG_FILE = "Log File";
	private static final String SEND_FILE = "Send %1$s file.";
	private static final String CHOOSE_EMAIL = "Choose an Email client.";
	// The log level will be changed by AP side to decide what level message is need to logged.
	public static int mDesiredLogLevel = -1;

	private static SimpleDateFormat mSDFTime = new SimpleDateFormat("MM/dd HH:mm:ss.S", Locale.US);
	private static SimpleDateFormat mSDFDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private static SimpleDateFormat mLogCatDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
	private static FileOutputStream mFileOutput;
	private static long WEEK_MILLIS = 604800000; //60*60*24*7*1000=604800000
	private static final String COLLECTED_DATA_ZIP_FILE_NAME = "CL_LOGCAT_DATA.zip";
	private static BlockingQueue<Runnable> mWorkQueue = new LinkedBlockingQueue<Runnable>();
	private static String mCurrentFilePath = null;
	private static ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(2, 4, 1, TimeUnit.SECONDS, mWorkQueue,
			new ThreadFactory() {
				int counter=0;
				@Override 
				public Thread newThread(Runnable runnable) {
					int thisIndex;
					synchronized (this) {
						thisIndex = counter++;
					}
					return new Thread(runnable, "BCLog #" + thisIndex);
				}
			});
	public static Context ApplicationContext;

	private static class LogInfo {
		public String Tag;
		public String Msg;
		public LogInfo(StackTraceElement ste, Object... args) {
			StringBuilder sb = new StringBuilder();
			sb.append('(').append(ste.getFileName()).append(':').append(ste.getLineNumber()).append(')');
			sb.append("[").append(ste.getMethodName()).append("] ");
			for (Object obj : args) {
				sb.append((obj == null) ? "null" : obj.toString());
			}
			Msg = sb.toString();
			Tag = ArrayUtils.last(ste.getClassName().split("\\.")).split("\\$")[0];
		}
	}

	public static int v(Object... args) {
		if (isLogShouldBeIgnore(android.util.Log.VERBOSE)) {
			return 0;
		}
		LogInfo li = new LogInfo(Thread.currentThread().getStackTrace()[3], args);
		writeLog(li.Msg, li.Tag);
		return android.util.Log.v(li.Tag, li.Msg);
	}

	public static int d(Object... args) {
		if (isLogShouldBeIgnore(android.util.Log.DEBUG)) {
			return 0;
		}
		LogInfo li = new LogInfo(Thread.currentThread().getStackTrace()[3], args);
		writeLog(li.Msg, li.Tag);
		return android.util.Log.d(li.Tag, li.Msg);
	}

	public static int i(Object... args) {
		if (isLogShouldBeIgnore(android.util.Log.INFO)) {
			return 0;
		}
		LogInfo li = new LogInfo(Thread.currentThread().getStackTrace()[3], args);
		writeLog(li.Msg, li.Tag);
		return android.util.Log.i(li.Tag, li.Msg);
	}

	public static int w(Object... args) {
		if (isLogShouldBeIgnore(android.util.Log.WARN)) {
			return 0;
		}
		LogInfo li = new LogInfo(Thread.currentThread().getStackTrace()[3], args);
		writeLog(li.Msg, li.Tag);
		return android.util.Log.w(li.Tag, li.Msg);
	}

	public static int e(Object... args) {
		if (isLogShouldBeIgnore(android.util.Log.ERROR)) {
			return 0;
		}
		LogInfo li = new LogInfo(Thread.currentThread().getStackTrace()[3], args);
		writeLog(li.Msg, li.Tag);
		return android.util.Log.e(li.Tag, li.Msg);
	}
	
	// Don't check Debuggable, dump log immediately.
	public static int bc_d(Object... args) {
		LogInfo li = new LogInfo(Thread.currentThread().getStackTrace()[3], args);
		writeLog(li.Msg, li.Tag);
		return android.util.Log.d(li.Tag, li.Msg);
	}
	
	// Don't check Debuggable, dump log immediately.
	public static int bc_e(Object... args) {
		LogInfo li = new LogInfo(Thread.currentThread().getStackTrace()[3], args);
		writeLog(li.Msg, li.Tag);
		return android.util.Log.e(li.Tag, li.Msg);
	}

	private static boolean isLogShouldBeIgnore(int level) {
		if (IsDeveloperMode) {
			return false;
		}

		// Check the desired log level
		if (mDesiredLogLevel != -1 && level >= mDesiredLogLevel) {
			return false;
		}

		// adb shell setprop log.tag.LOG_TAG_BC VERBOSE
		if (android.util.Log.isLoggable(LOG_TAG, android.util.Log.VERBOSE)) {
			return false;
		}

		return true;
	}

	protected static void writeLog(final String message, final String title) {
		if (mExecutor == null) {
			return;
		}
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				writeLogImp(message, title);
			}
		});
		
	}

	protected static void closeFileOutputStream() throws IOException {
		if (mFileOutput != null) {
			mFileOutput.close();
			mFileOutput = null;
		}
	}

	private static void writeLogImp(String message, String title) {
		long localTimeMillis = System.currentTimeMillis();
		Date dt = new Date(localTimeMillis);
		String dts = mSDFTime.format(dt);
		if (mFileOutput == null) {
			File log = getLogFile();

			if (log == null) {
				return;
			}

			try {
				mFileOutput = new FileOutputStream(log, true);
			} catch (FileNotFoundException e) {
			}
		}

		if (mFileOutput == null)
			return;

		try {
			String s = "======== " + title + " ========\n";
			s += TIMESTAMP + ": " + dts + "\n";
			s += message + "\n\n";

			mFileOutput.write(s.getBytes());
			mFileOutput.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getBCExternalFolder() {
		// BC external path: /storage/emulated/0/BC/
		String externalFolder = "";
		try {
			externalFolder = Environment.getExternalStorageDirectory() + File.separator + FOLDER_NAME + File.separator;
			File dstFolder = new File(externalFolder);
			if (!dstFolder.exists()) {
				dstFolder.mkdir();
			}
		} catch (Exception e) {
			e.printStackTrace();
			externalFolder = "";
		}
		
		return externalFolder;
	}
	
	public static String getDebugLogFolder() {
		String debugFolder = "";
		if( ApplicationContext != null) {
			try {
				debugFolder = ApplicationContext.getCacheDir().toString() + File.separator + FOLDER_NAME + File.separator;
			} catch (Exception e) {
				e.printStackTrace();
				debugFolder = "";
			}
		}
		
		return debugFolder;
	}

	private static File getLogFile() {
		return getLogFile(getLogFolder(true).toString());
	}

	private static File getLogFile(String folder) {
		Date date = new Date();
		String fileName = "";
		synchronized( mSDFDate ) {
			fileName = mSDFDate.format(date) + ".log";
		}

		File file = new File(folder, fileName);
		mCurrentFilePath = file.getAbsolutePath();
		if (file.exists()) {
			return file;
		} else {
			try {
				file.createNewFile();
				return file;
			} catch (IOException e) {
				return null;
			}
		}
	}
	
	public static String getLogFolder(boolean isInternal) {
		File folder = new File(isInternal ? getDebugLogFolder(): getBCExternalFolder());
		if (!folder.exists()) {
			folder.mkdir();
		}
		
		return folder.toString();
	}

	public static void clearLogOutOfWeek() {
		long currentTimeMillis = System.currentTimeMillis();

		File logDir = new File(getLogFolder(true).toString());
		File[] fileList = logDir.listFiles();
		if (fileList != null) {
			for (File file:fileList) {
				if (currentTimeMillis - file.lastModified() > WEEK_MILLIS) {
					file.delete();
				}
			}
		}
	}

	/**
	 * Package specific log file to a Zip file
	 * 
	 * @param context context
	 */
	public static void packageLatestLogToZipFile(Context context) {
		ArrayList<File> srcList = new ArrayList<File>();
		srcList.add(new File(mCurrentFilePath));
		packageFilesToZipFile(context, srcList);
	}
	
	/**
	 * Package specific log file to a Zip file
	 * 
	 * @param context context
	 * @param srcList srcList
	 */
	public static String packageFilesToZipFile(Context context, ArrayList<File> srcList) {
		deleteZipFile(getCollectedAppDataZipFilePath());
		if (srcList == null || srcList.size() == 0)
			return null;
		
		PackageManager m = context.getPackageManager();
		String dataDir = context.getPackageName();
		PackageInfo pkgInfo;
		
		try {
			pkgInfo = m.getPackageInfo(dataDir, 0);
			dataDir = pkgInfo.applicationInfo.dataDir;
			
		} catch (NameNotFoundException e) {
			dataDir = null;
			e.printStackTrace();
		}
		
		if (dataDir == null)
			return null;
		
		String outputDir = getBCExternalFolder();
		try {
			File _file = new File(outputDir);
			if (!_file.canWrite())
				return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
				
		String zipFilePath = getCollectedAppDataZipFilePath();
		File zipFile = new File(zipFilePath);
		
		FileOutputStream fos = null;
		ZipOutputStream zos = null;

		try {
			if (zipFile.exists())
				zipFile.delete();
			
			fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);
			for (File src : srcList)
				addFileToZip(src, zos);
			zos.close();
			fos.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			zipFilePath = null;
		} catch (IOException e) {
			e.printStackTrace();
			zipFilePath = null;
		} finally {
			try {
				if (zos != null)
					zos.close();
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return zipFilePath;
	}
	
	/**
	 * Add a file to a Zip output stream
	 * 
	 * @param file
	 * @param zip
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void addFileToZip(File file, ZipOutputStream zip) 
			throws FileNotFoundException, IOException {
		addFileToZip(file, null, zip);
	}
	
	/**
	 * Add a file to a Zip output stream with a provided sub-folder name
	 * 
	 * @param file
	 * @param folder
	 * @param zip
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void addFileToZip(File file, String folder, ZipOutputStream zip)
			throws FileNotFoundException, IOException {
		
		byte[] buf = new byte[1024];
		int len = 0;
		
		FileInputStream in = new FileInputStream(file);
		
		if (folder != null) {
			zip.putNextEntry(new ZipEntry(folder + "/" + file.getName()));
		} else {
			zip.putNextEntry(new ZipEntry(file.getName()));
		}
		
		while ((len = in.read(buf)) > 0) {
			zip.write(buf, 0, len);
		}
		
		in.close();
	}
	
	public static String getCollectedAppDataZipFilePath() {
		return getBCExternalFolder() + COLLECTED_DATA_ZIP_FILE_NAME;
	}
	
	public static void deleteZipFile(String zipPath) {
		File zipFile = new File(zipPath);
		if (zipFile.exists()) {
			zipFile.delete();
		}
	}
	
	public static void sendMail(Activity activity) {
		if (activity == null)
			return;
		
		File zipFile = new File(Log.getCollectedAppDataZipFilePath());
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_SUBJECT, LOG_FILE)
		.putExtra(Intent.EXTRA_TEXT, String.format(Locale.getDefault(), SEND_FILE, zipFile.getName()))
		.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zipFile))
		.putExtra(Intent.EXTRA_EMAIL, Log.getMailList())
		.setType("application/zip");
		
		activity.startActivity(Intent.createChooser(intent, CHOOSE_EMAIL));
	}
	
	public static String[] getMailList() {
		return new String[] {"vince_huang@perfectcorp.com"};
	}
	
	public static String dumpLogcat(boolean bExternal) {
		Process process = null;
		InputStream input = null;
		OutputStream output = null;
		String filePath = null;
		try {
			process = Runtime.getRuntime().exec("logcat -d -v time");
			input = process.getInputStream();
			Date date = new Date();
			String fileName = "";
			synchronized( mLogCatDate ) {
				fileName = mLogCatDate.format(date) + "_logcat.log";
			}
		
			
			File file = new File((bExternal) ? getBCExternalFolder() : getDebugLogFolder(), fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			filePath = file.toString();
			
			output = new FileOutputStream(file);
			byte[] buffer = new byte[4 * 1024];
			int read;
			while ((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			output.flush();
		} catch (Exception e) {
			filePath = null;
		} finally {
			if (input != null)
				try { input.close(); } catch (IOException e) {}
			if (output != null)
				try { output.close(); } catch (IOException e) {}
		}
		return filePath;
	}

	public static String textToFile(boolean bExternal, String text, String fileName) {
		if (text == null || fileName == null) {
			return null;
		}
		OutputStream output = null;
		String filePath = null;
		try {
			File file = new File((bExternal) ? getBCExternalFolder() : getDebugLogFolder(), fileName);
			if (!file.exists()) {
				file.createNewFile();
			}

			output = new FileOutputStream(file);
			output.write(text.getBytes());
			output.flush();
			filePath = file.getPath();
		} catch (Exception e) {
			filePath = null;
		} finally {
			if (output != null)
				try { output.close(); } catch (IOException e) {}
		}
		return filePath;
	}

	public static String FileToText(boolean bExternal, String fileName) {
		if (fileName == null) {
			return null;
		}
		BufferedReader reader = null;
		String result = "";
		try {
			File file = new File((bExternal) ? getBCExternalFolder() : getDebugLogFolder(), fileName);
			reader = new BufferedReader(new FileReader(file));

			String text;
			while ((text = reader.readLine()) != null) {
				result += text + System.getProperty("line.separator");
			}
		} catch (Exception e) {
			result = null;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					Log.e(e);
				}
			}
		}
		return result;
	}

	public static void showCenterToast(Activity activity, String message) {
		Spannable centeredText = new SpannableString(message);
		centeredText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
				0, message.length() - 1,
				Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		Toast toast = Toast.makeText(activity, centeredText, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	public static int bear(Object... args) {
		LogInfo li = new LogInfo(Thread.currentThread().getStackTrace()[3], args);
		li.Tag = "Bear-" + li.Tag;
		return android.util.Log.d(li.Tag, li.Msg);
	}

	public static int bearS(Object... args) {
		StackTraceElement[] steList = Thread.currentThread().getStackTrace();
		LogInfo li = new LogInfo(steList[3], args);
		li.Tag = "Bear-" + li.Tag;
		int ret = android.util.Log.d(li.Tag, li.Msg);

		for (int i = 4; i < 13; ++i) {
			if (i >= steList.length)
				break;

			StackTraceElement ste = steList[i];
			android.util.Log.d(li.Tag, "\t - (" + ste.getFileName() + ":" + ste.getLineNumber() + ")[" + ste.getMethodName() + "]");
		}

		return ret;
	}
}
