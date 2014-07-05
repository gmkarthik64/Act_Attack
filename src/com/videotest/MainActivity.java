package com.videotest;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.Fragment;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends Activity {

	// Setup a result flag for your video capture
	int ACTION_TAKE_VIDEO = 100;
	String upLoadServerUri = null;
	String fPath = null;
	int serverResponseCode = 0;
	File loc_file = null;
	public final static String EXTRA_VIDEO = "video";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		upLoadServerUri = "http://agit.cloudapp.net/~karthik/UploadToServer.php";
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

	public void sendMessage(View view) {

		// Launch an intent to capture video from MediaStore
		Intent takeVideoIntent = new Intent(this, RecorderActivity.class);
		startActivityForResult(takeVideoIntent,1);
	}

	public void receiveMessage(View view) {
		Receiver runner = new Receiver();
		runner.execute("myvid.mp4");
		try {
			runner.get(100, TimeUnit.SECONDS);
		} catch (Exception e) {
		}
		Intent intent = new Intent(this, PlayVidActivity.class);
		intent.putExtra(EXTRA_VIDEO, loc_file.getPath());
		startActivity(intent);
	}

	// Obtain the file path to the video in onActivityResult
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			fPath = data.getStringExtra("result");
			Log.d("LOGCAT", "Video path is: " + fPath);
			AsyncTaskRunner runner = new AsyncTaskRunner();
			runner.execute(fPath);
		}
	}

	private String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		CursorLoader loader = new CursorLoader(getApplicationContext(),
				contentUri, proj, null, null, null);
		Cursor cursor = loader.loadInBackground();
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	private class AsyncTaskRunner extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			String sourceFileUri = params[0];
			String fileName = sourceFileUri;

			HttpURLConnection conn = null;
			DataOutputStream dos = null;
			String lineEnd = "\r\n";
			String twoHyphens = "--";
			String boundary = "*****";
			int bytesRead, bytesAvailable, bufferSize;
			byte[] buffer;
			int maxBufferSize = 1 * 1024 * 1024;
			File sourceFile = new File(sourceFileUri);

			if (!sourceFile.isFile()) {

				Log.e("uploadFile", "Source File not exist :" + fPath);

				return "0";

			} else {
				try {

					// open a URL connection to the Servlet
					FileInputStream fileInputStream = new FileInputStream(
							sourceFile);
					URL url = new URL(upLoadServerUri);

					// Open a HTTP connection to the URL
					conn = (HttpURLConnection) url.openConnection();
					conn.setDoInput(true); // Allow Inputs
					conn.setDoOutput(true); // Allow Outputs
					conn.setUseCaches(false); // Don't use a Cached Copy
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Connection", "Keep-Alive");
					conn.setRequestProperty("ENCTYPE", "multipart/form-data");
					conn.setRequestProperty("Content-Type",
							"multipart/form-data;boundary=" + boundary);
					conn.setRequestProperty("uploaded_file", fileName);

					dos = new DataOutputStream(conn.getOutputStream());

					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
							+ fileName + "\"" + lineEnd);

					dos.writeBytes(lineEnd);

					// create a buffer of maximum size
					bytesAvailable = fileInputStream.available();

					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					buffer = new byte[bufferSize];

					// read file and write it into form...
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);

					while (bytesRead > 0) {

						dos.write(buffer, 0, bufferSize);
						bytesAvailable = fileInputStream.available();
						bufferSize = Math.min(bytesAvailable, maxBufferSize);
						bytesRead = fileInputStream.read(buffer, 0, bufferSize);

					}

					// send multipart form data necesssary after file data...
					dos.writeBytes(lineEnd);
					dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

					// Responses from the server (code and message)
					serverResponseCode = conn.getResponseCode();
					String serverResponseMessage = conn.getResponseMessage();

					Log.i("uploadFile", "HTTP Response is : "
							+ serverResponseMessage + ": " + serverResponseCode);

					if (serverResponseCode == 200) {

					}

					// close the streams //
					fileInputStream.close();
					dos.flush();
					dos.close();

				} catch (MalformedURLException ex) {

					ex.printStackTrace();

					Log.e("Upload file to server", "error: " + ex.getMessage(),
							ex);
				} catch (Exception e) {

					e.printStackTrace();

					Log.e("Upload file to server Exception",
							"Exception : " + e.getMessage(), e);
				}
				sourceFile.delete();
				return "" + serverResponseCode;

			} // End else block
		}
	}

	private class Receiver extends AsyncTask<String, String, String> {
		int totalSize = 0;
		int downloadedSize = 0;

		@Override
		protected String doInBackground(String... params) {
			String fName = params[0];
			try {
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(
						"http://agit.cloudapp.net/~karthik/PrepFile.php");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("vid_id", fName));
				nameValuePairs.add(new BasicNameValuePair("del", "false"));
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				client.execute(post);
			} catch (Exception e) {
			}
			try {
				URL url = new URL("http://agit.cloudapp.net/~karthik/dloads/"
						+ fName);
				HttpURLConnection urlConnection = (HttpURLConnection) url
						.openConnection();

				urlConnection.setRequestMethod("GET");
				urlConnection.setDoOutput(true);

				// connect
				urlConnection.connect();

				// set the path where we want to save the file
				File SDCardRoot = new File(
						Environment
								.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
						"VideoTest");
				// create a new file, to save the downloaded file
				SDCardRoot.mkdirs();
				loc_file = new File(SDCardRoot, fName);

				FileOutputStream fileOutput = new FileOutputStream(loc_file);

				// Stream used for reading the data from the internet
				InputStream inputStream = urlConnection.getInputStream();

				// this is the total size of the file which we are downloading
				totalSize = urlConnection.getContentLength();

				// create a buffer...
				byte[] buffer = new byte[1024];
				int bufferLength = 0;

				while ((bufferLength = inputStream.read(buffer)) > 0) {
					fileOutput.write(buffer, 0, bufferLength);
					downloadedSize += bufferLength;
				}
				// close the output stream when complete //
				fileOutput.close();

			} catch (final MalformedURLException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (final Exception e) {
			}
			try {
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost(
						"http://agit.cloudapp.net/~karthik/PrepFile.php");
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("vid_id", fName));
				nameValuePairs.add(new BasicNameValuePair("del", "true"));
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				client.execute(post);
			} catch (Exception e) {
			}
			return "";
		}
	}
}