package com.videotest;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.Fragment;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends Activity {

	// Setup a result flag for your video capture
	public final static String EXTRA_MESSAGE = "MESSAGE";
	int ACTION_TAKE_VIDEO = 100;
	String upLoadServerUri = null;
	String fPath = null;
	int serverResponseCode = 0;
	
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
	
	public void sendMessage(View view)
	{

		// Launch an intent to capture video from MediaStore
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
	}

		// Obtain the file path to the video in onActivityResult
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (resultCode == RESULT_OK) {

		    if (requestCode == ACTION_TAKE_VIDEO) {

		        Uri videoUri = data.getData();
		        fPath = getRealPathFromURI(videoUri);
		        Log.d("LOGCAT", "Video path is: " + fPath);
		        uploadFile(fPath);
		    }
		}
	}
		 public int uploadFile(String sourceFileUri) {
	           
	           
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
	               
	               Log.e("uploadFile", "Source File not exist :"
	                                   +fPath);
	                
	               runOnUiThread(new Runnable() {
	                   public void run() {
	                      
	                   }
	               }); 
	                
	               return 0;
	            
	          }
	          else
	          {
	               try { 
	                    
	                     // open a URL connection to the Servlet
	                   FileInputStream fileInputStream = new FileInputStream(sourceFile);
	                   URL url = new URL(upLoadServerUri);
	                    
	                   // Open a HTTP  connection to  the URL
	                   conn = (HttpURLConnection) url.openConnection(); 
	                   conn.setDoInput(true); // Allow Inputs
	                   conn.setDoOutput(true); // Allow Outputs
	                   conn.setUseCaches(false); // Don't use a Cached Copy
	                   conn.setRequestMethod("POST");
	                   conn.setRequestProperty("Connection", "Keep-Alive");
	                   conn.setRequestProperty("ENCTYPE", "multipart/form-data");
	                   conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
	                   conn.setRequestProperty("uploaded_file", fileName); 
	                    
	                   dos = new DataOutputStream(conn.getOutputStream());
	          
	                   dos.writeBytes(twoHyphens + boundary + lineEnd); 
	                   dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
	                                             + fileName +"\"" + lineEnd);
	                    
	                   dos.writeBytes(lineEnd);
	          
	                   // create a buffer of  maximum size
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
	                    
	                   if(serverResponseCode == 200){
	                        
	                       runOnUiThread(new Runnable() {
	                            public void run() {
	                                 
	                            }
	                        });                
	                   }    
	                    
	                   //close the streams //
	                   fileInputStream.close();
	                   dos.flush();
	                   dos.close();
	                     
	              } catch (MalformedURLException ex) {
	                   
	                  ex.printStackTrace();
	                   
	                  runOnUiThread(new Runnable() {
	                      public void run() {

	                      }
	                  });
	                   
	                  Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
	              } catch (Exception e) {
	                   
	                  e.printStackTrace();
	                   
	                  runOnUiThread(new Runnable() {
	                      public void run() {
	                      }
	                  });
	                  Log.e("Upload file to server Exception", "Exception : "
	                                                   + e.getMessage(), e);  
	              }      
	              return serverResponseCode; 
	               
	           } // End else block 
	         } 
		 private String getRealPathFromURI(Uri contentUri) {
			    String[] proj = { MediaStore.Images.Media.DATA };
			    CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
			    Cursor cursor = loader.loadInBackground();
			    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			    cursor.moveToFirst();
			    return cursor.getString(column_index);
			}	 
}
