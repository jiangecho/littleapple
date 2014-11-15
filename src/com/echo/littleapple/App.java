package com.echo.littleapple;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.ByteArrayBuffer;

import com.echo.littleapple.Util.PostResultCallBack;

import android.app.Application;
import android.content.pm.PackageManager.NameNotFoundException;

public class App extends Application {

	public boolean showInterstitialAd = false;
	public boolean autoDownloadAd = false;
	public boolean newVersionAvailable = false;

	// ad related
	// TODO important: update every version
	private static final String AUTO_DOWNLOAD_AD = "auto3.0";
	// only for xiaomi: xiaomi do not allow use interstitial ad
	private static final String SHOW_INTERSTITIAL_AD = "3.0ad";

	private static final String SUBMIT_SCORE_URL = "http://littleappleapp.sinaapp.com/submit_score.php";

	@Override
	public void onCreate() {
		super.onCreate();

		asyncGetOnlineConfig();
		initAd();
	}

	private void asyncGetOnlineConfig() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				getOnlineConfig();
			}
		}).start();
	}

	private void getOnlineConfig() {
		try {
			URL url = new URL("http://littleappleapp.sinaapp.com/config.txt");
			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();
			InputStream inputStream = urlConnection.getInputStream();
			byte[] bytes = new byte[1024];
			int count;
			ByteArrayBuffer byteArrayBuffer = new ByteArrayBuffer(1024);
			count = inputStream.read(bytes);
			while (count != -1) {
				byteArrayBuffer.append(bytes, 0, count);
				count = inputStream.read(bytes);
			}
			String config = new String(byteArrayBuffer.toByteArray());
			if (config.contains(SHOW_INTERSTITIAL_AD)) {
				showInterstitialAd = true;
			}

			if (config.contains(AUTO_DOWNLOAD_AD)) {
				autoDownloadAd = true;
			}

			String[] tmp = config.split("\\s+");

			for (String str : tmp) {
				if (str.startsWith("version")) {
					int versionCode = Integer.parseInt(str.substring("version"
							.length()));
					checkUpdate(versionCode);
					break;
				}
			}

			urlConnection.disconnect();
			inputStream.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkUpdate(int versionCode) {
		try {
			int currentVersionCode = getPackageManager().getPackageInfo(
					getPackageName(), 0).versionCode;
			if (versionCode > currentVersionCode) {
				newVersionAvailable = true;
			}

		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void initAd() {
		// TODO depend on different ad platform
	}

	public static void submitScore(final String nickyName,
			final String scoreString, final int type,
			final PostResultCallBack callBack) {
		if (nickyName == null || nickyName.trim().equals("")
				|| scoreString == null || scoreString.trim().equals("")) {
			return;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO the uri should base on the mode
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("nickyname",
						nickyName));
				nameValuePairs
						.add(new BasicNameValuePair("score", scoreString));
				nameValuePairs.add(new BasicNameValuePair("type", type + ""));
				Util.httpPost(SUBMIT_SCORE_URL, nameValuePairs, callBack);
			}
		}).start();
	}

	public static void submitScore(final String nickyName,
			final String scoreString, final int type) {
		submitScore(nickyName, scoreString, type, null);
	}

}
