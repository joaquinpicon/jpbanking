package jp.bank;

//1)rajouter le feed back venant du serveur

//2)OK-resoudre le probleme de verification du montant recu lorsque le serveur renvoie du texte d'erreur
// float plante!!
//3)verifier les parametres d'entree 
//4) changer l'url local ou finoute en fonction de l'id de l'appli
//5) une nouvelle entrée émise par l"activité au lieu d'etre envoyée au serveur on l'enregistre dans le fichier pour que la boucle l'envoie au serveur

//6) 20140701 TBD en cas de serveur erreur, il faut mettre le bouton ENVOYER en rouge jusqu'à ce que le serveur redevienne disponible.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings.Secure;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

public class BankService extends Service {
	private static final String TAG = "com.BankService";
	private static final String TRANSACTIONFILE = "jpbank.txt";
	private static final String LIEUFILE = "jplieu.txt";
	private static final String BENEFICAIREFILE = "jpbene.txt";
	private static final String WAMPHOSTIP = "9.212.15.68";
	private static boolean stopThread = false;
	private String android_id = "";
	Intent anIntent;
	String acerid   = "6b1789aebe379078";
	String emulatorid = "58b3ec432ee476f1";
	// true = local, false = finoute
	boolean bureau = false;

	// MediaPlayer player;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void onCreate() {
		// context=this.context;

		// Toast.makeText(this, "Jp Bank Service Created", Toast.LENGTH_LONG)
		// .show();
		try {
			InetAddress address = InetAddress.getLocalHost();
			Log.d(TAG, "local IP address= " + address);
			Log.d(TAG, "local IP address= " + address.getHostAddress());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		android_id = Secure.getString(this.getContentResolver(),
				Secure.ANDROID_ID);
		if (android_id.equals(emulatorid)) {
			bureau = true;
			Log.d(TAG, "accessing localhost server");
		} else {
			bureau = false;
			Log.d(TAG, "accessing finoute server");
		}
		Log.d("Android", "Android ID : " + android_id);
		Log.d(TAG, "Bank service onCreate");
		

	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Jp Bank service Stopped", Toast.LENGTH_LONG)
				.show();
		Log.d(TAG, "onDestroy");
		stopThread = true;
	}

	public static final String INTENTERROR = "probleme d'acces serveur";
	public static final String INTENTSUCCESS = "Enregistrement effectué";
	public static final String INTENTRESET = "RESET";
	// Your service will respond to this action String
	public static final String RECEIVE_JPJSON = "com.your.package.RECEIVE_JPJSON";
	// Your activity will respond to this action String
	public static final String RECEIVE_JSON = "com.your.package.RECEIVE_JSON";

	@Override
	public void onStart(Intent intent, int startid) {
		
		Toast.makeText(this, "Jp Bank Service Started", Toast.LENGTH_LONG)
				.show();

		final Bundle b = intent.getExtras();
		final LocalBroadcastManager bManager = LocalBroadcastManager
				.getInstance(this);

		new Thread(new Runnable() {

			public void run() {

				IntentFilter intentFilter = new IntentFilter();
				intentFilter.addAction(RECEIVE_JPJSON);
				bManager.registerReceiver(bReceiver, intentFilter);
				int waitingTime = 1000;
				int maxTime=30000;
				// TODO Auto-generated method stub
				String jpsmstext = "message from service to activity -->";
				// String montant = b.getString("montant");
				// String paiement = b.getString("paiement");
				// String cheque = b.getString("cheque");
				// String date = b.getString("date");
				// String lieu = b.getString("lieu");
				// String bene = b.getString("bene");
				String dataFromFile = "";
				String[] records;
				// get autompletion data
				jpresetFile(LIEUFILE);
				jpresetFile(BENEFICAIREFILE);
				getAutocompletion("lieu");
				getAutocompletion("bene");
				while (true) {
					try {

						Log.d(TAG, "Service looping....wait time = " + waitingTime);
						// jpsmstext = postLoginData(montant, paiement, cheque,
						// date, lieu, bene);
						// sendIntent(jpsmstext);
						// getCurrentScreen();
						// sendJpSMS("0682855103",
						// "de la part de ton papa. Peux tu me dire si tu as reçu le sms? ");
						Thread.sleep(waitingTime);
						if(waitingTime < maxTime) waitingTime++;
						sendIntent(INTENTRESET);
						dataFromFile = jpreadFile("jpbank.txt");

						records = dataFromFile.split("EOR");
						int nbrecords = records.length;
						String[] bankData;
						for (int i = 0; i < (nbrecords - 1); i++) {
							Log.d(TAG, "record (" + i + ") over " + nbrecords
									+ ":" + records[i]);
							bankData = records[i].split("/");
							for (int j = 0; j < bankData.length; j++) {
								Log.d(TAG, "data (" + j + ") over "
										+ bankData.length + ":" + bankData[j]);
							}
							postLoginData(bankData[0], bankData[1],
									bankData[2], bankData[3], bankData[4],
									bankData[5]);
						}
						if (stopThread)
							break;

					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.d(TAG, "error during send SMS");
					}
					// REST OF CODE HERE//
				}

			}
		}).start();
		// Log.d(TAG, cread1);

	}

	private BroadcastReceiver bReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(RECEIVE_JPJSON)) {
				String serviceJsonString = intent.getStringExtra("json");

				Log.d(TAG, "======> intent received from ACTIVITY: "
						+ serviceJsonString);
				String[] parts = serviceJsonString.split("&");
//				postLoginData(parts[0], parts[1], parts[2], parts[3], parts[4],
//						parts[5]);
				String data2commit = parts[0] + "/" + parts[1] + "/" + parts[2] + "/"
						+ parts[3] + "/" + parts[4] + "/" + parts[5] + "EOR";
				jpwriteFile(TRANSACTIONFILE, data2commit);
				Log.d(TAG, "======> service received intent from activity, wait for service to post data");
			}
		}
	};

	// sendIntent to the activity
	public void sendIntent(String aMsg) {
		// this code identified A
		Log.w(TAG, "service sends this message to activity: " + aMsg);
		Intent RTReturn = new Intent(BankActivity.RECEIVE_JSON);
		RTReturn.putExtra("json", aMsg);
		LocalBroadcastManager.getInstance(this).sendBroadcast(RTReturn);

	}

	public String postLoginData(String montant, String paiement, String cheque,
			String date, String lieu, String bene) {
		// Create a new HttpClient and Post Header
		String dataBank = "error";
		String str = "sms text error";
		// DefaultHttpClient httpclient = new DefaultHttpClient();
		// timeout settings
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used.
		int timeoutConnection = 3000;
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 5000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
		// UsernamePasswordCredentials creds = (UsernamePasswordCredentials) new
		// UsernamePasswordCredentials(
		// username, password);
		// httpclient.getCredentialsProvider().setCredentials(
		// new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);
		/* login.php returns true if username and password is equal to saranga */
		String data2commit = montant + "/" + paiement + "/" + cheque + "/"
				+ date + "/" + lieu + "/" + bene + "EOR";

		try {
			// Add user name and password
			Log.w(TAG, "post montant: " + montant + " paiement: " + paiement
					+ " cheque: " + cheque + " date: " + date + " lieu: "
					+ lieu + " bene: " + bene);
			String jphttp = "";
			if (bureau)
				jphttp = "http://"+ WAMPHOSTIP+ "/bank/dataentry.php";
			else {
				jphttp = "http://www.finoute.com/bank/dataentry.php";
				UsernamePasswordCredentials creds = (UsernamePasswordCredentials) new UsernamePasswordCredentials(
						"joaquinpicon", "a1joa1a");
				httpclient.getCredentialsProvider().setCredentials(
						new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
						creds);
			}
			HttpPost httppost = new HttpPost(jphttp);
			Log.w(TAG, "ready to start http request: " + jphttp);
			// "http://www.finoute.com/bank/dataentry.php");
			// "http://9.215.15.35/bank/dataentry.php");

			List<NameValuePair> data = new ArrayList<NameValuePair>(2);
			data.add(new BasicNameValuePair("montant", montant));
			// Date dt = new Date(date);
			// SimpleDateFormat dateFormat = new
			// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// String ladate = dateFormat.format(dt);
			data.add(new BasicNameValuePair("date", date));
			data.add(new BasicNameValuePair("lieu", lieu));
			data.add(new BasicNameValuePair("bene", bene));
			data.add(new BasicNameValuePair("paiement", paiement));
			data.add(new BasicNameValuePair("cheque", cheque));
			httppost.setEntity(new UrlEncodedFormEntity(data));
			// Execute HTTP Post Request
			Log.w(TAG, "Execute HTTP Post Request");

			HttpResponse response = httpclient.execute(httppost);
			final int statusCode = response.getStatusLine().getStatusCode();

			str = inputStreamToString(response.getEntity().getContent())
					.toString();

			if (statusCode != HttpStatus.SC_OK) {
				Log.w(TAG, "postLoginData HTTP ERROR " + statusCode);
				Log.w(TAG, str);
				jpwriteFile(TRANSACTIONFILE, data2commit);
			} else {
				// the server may successfully answer but the answer may contain
				// an error
				// like mysql error because the DB has not yet started and
				// refused the connection.
				// to be solved!! Check that the montant is
				Log.w(TAG, "Split of :" + str);
				String[] dataReturned = str.split("/");
				if (dataReturned[0].equals("SUCCESS")) {
					dataBank = new String("montant: " + dataReturned[6]
							+ " date: " + dataReturned[3] + " solde: "
							+ dataReturned[7]);
					// Toast.makeText(this, dataBank, Toast.LENGTH_LONG).show();
					Float montantSent = Float.parseFloat(montant);
					Float epsilon = new Float(0.0001);
					Float montantReceived = Float.parseFloat(dataReturned[6]);
					Log.w(TAG, "montantSent : " + montantSent
							+ " montantReceived : " + montantReceived);
					if ((montantSent / montantReceived - 1) < epsilon) {
						Log.w(TAG, "Success: " + str);
						Log.w(TAG, "Success ?: " + dataBank);
						Log.w(TAG, "---->: solde= " + dataReturned[7]);
						sendIntent(INTENTSUCCESS + " solde= " + dataReturned[7]);
					}
				} else {
					Log.w(TAG, "FAILURE: " + str);
					Log.w(TAG, "FAILURE ?: " + dataBank);
					sendIntent(INTENTERROR);
					if (str.indexOf("Duplicate entry") > 1) {
						Log.w(TAG,
								"FAILURE due to duplicate entry. Don't send this entry again: "
										+ dataBank);
					} else {
						Log.w(TAG,
								"Seems to be a server access problem, retry sending this record: ");
						jpwriteFile(TRANSACTIONFILE, data2commit);
					}
				}

			}

		} catch (ClientProtocolException e) {
			// e.printStackTrace();
			Log.w(TAG, "error in postLoginData client protocol");
			jpwriteFile(TRANSACTIONFILE, data2commit);
		} catch (IOException e) {

			jpwriteFile(TRANSACTIONFILE, data2commit);
			Log.w(TAG, "error in postLoginData IOexception ");
			e.printStackTrace();
		}
		return str;
	}

	public String getAutocompletion(String champ) {
		// Create a new HttpClient and Post Header
		String dataBank = "error";
		String str = "sms text error";
		// DefaultHttpClient httpclient = new DefaultHttpClient();
		// timeout settings
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used.
		int timeoutConnection = 3000;
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 5000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
		// UsernamePasswordCredentials creds = (UsernamePasswordCredentials) new
		// UsernamePasswordCredentials(
		// username, password);
		// httpclient.getCredentialsProvider().setCredentials(
		// new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);
		/* login.php returns true if username and password is equal to saranga */

		try {
			// Add user name and password
			Log.w(TAG, "autocompletion for: " + champ);
			String jphttp = "";
			if (bureau)
				jphttp = "http://"+ WAMPHOSTIP+ "/bank/autocompletion.php";
			else {
				jphttp = "http://www.finoute.com/bank/autocompletion.php";
				UsernamePasswordCredentials creds = (UsernamePasswordCredentials) new UsernamePasswordCredentials(
						"joaquinpicon", "a1joa1a");
				httpclient.getCredentialsProvider().setCredentials(
						new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
						creds);
			}
			HttpPost httppost = new HttpPost(jphttp);
			Log.w(TAG, "ready to start http request: " + jphttp);
			// "http://www.finoute.com/bank/dataentry.php");
			// "http://9.215.15.35/bank/dataentry.php");

			List<NameValuePair> data = new ArrayList<NameValuePair>(2);

			data.add(new BasicNameValuePair("champ", champ));

			httppost.setEntity(new UrlEncodedFormEntity(data));
			// Execute HTTP Post Request
			Log.w(TAG, "Execute HTTP Post Request for autocompletion data");

			HttpResponse response = httpclient.execute(httppost);
			final int statusCode = response.getStatusLine().getStatusCode();

			str = inputStreamToString(response.getEntity().getContent())
					.toString();

			if (statusCode != HttpStatus.SC_OK) {
				Log.w(TAG, "postLoginData HTTP ERROR for: " + champ + " rc= "
						+ statusCode);
				Log.w(TAG, str);

			} else {
				// the server may successfully answer but the answer may contain
				// an error
				// like mysql error because the DB has not yet started and
				// refused the connection.
				// to be solved!! Check that the montant is
				Log.w(TAG, "Split of :" + str);
				String[] dataReturned = str.split(",");
				if (dataReturned[0].equals("SUCCESS")) {
					if (champ.equals("lieu"))
						jpwriteFile(LIEUFILE, str);
					else
						jpwriteFile(BENEFICAIREFILE, str);

				} else {
					Log.w(TAG, "FAILURE: " + str);
					Log.w(TAG, "FAILURE ?: " + dataBank);
					sendIntent(INTENTERROR);
					if (str.indexOf("Duplicate entry") > 1) {
						Log.w(TAG,
								"FAILURE due to duplicate entry. Don't send this entry again: "
										+ dataBank);
					} else {
						Log.w(TAG,
								"Seems to be a server access problem, retry sending this record: ");

					}
				}

			}

		} catch (ClientProtocolException e) {
			// e.printStackTrace();
			Log.w(TAG, "error in postLoginData client protocol");

		} catch (IOException e) {

			Log.w(TAG, "error in postLoginData IOexception ");
			e.printStackTrace();
		}
		return str;
	}

	private String jpreadFile(String thefile) {
		String aDataRow = "";
		String aBuffer = "";
		try {
			File myFile = new File("/mnt/sdcard/" + thefile);
			FileInputStream fIn = new FileInputStream(myFile);
			BufferedReader myReader = new BufferedReader(new InputStreamReader(
					fIn));

			while ((aDataRow = myReader.readLine()) != null) {
				aBuffer += aDataRow + "\n";
			}

			myReader.close();

			Log.w(TAG, "Done reading SD.......... " + aBuffer);
			// Toast.makeText(getBaseContext(), "Done reading SD " +aBuffer,
			// Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.w(TAG, "Error reading file: " + thefile);
			// Toast.makeText(getBaseContext(), e.getMessage(),
			// Toast.LENGTH_SHORT)
			// .show();
		}
		jpresetFile(thefile);
		return aBuffer;
	}

	private void jpresetFile(String thefile) {
		try {
			// data has been read, reset the file
			File myFile = new File("/mnt/sdcard/" + thefile);
			FileOutputStream fOut = new FileOutputStream(myFile);
			myFile.createNewFile();
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.close();
			fOut.close();
			Log.w(TAG, "Reset file executed");
			// Toast.makeText(getBaseContext(), "Done reading SD " +aBuffer,
			// Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.w(TAG, "Error reading file: " + thefile);
			// Toast.makeText(getBaseContext(), e.getMessage(),
			// Toast.LENGTH_SHORT)
			// .show();
		}

	}

	private void jpwriteFile(String filename, String txtData) {
		// write on SD card file data in the text box
		try {
//			sendIntent(INTENTERROR);
			File myFile = new File("/mnt/sdcard/"+ filename);
			myFile.createNewFile();
			FileOutputStream fOut = new FileOutputStream(myFile, true);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append(txtData);
			myOutWriter.close();
			fOut.close();
			// Toast.makeText(getBaseContext(),
			// "Done writing SD 'mysdfile.txt'",
			// Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			// Toast.makeText(getBaseContext(), e.getMessage(),
			// Toast.LENGTH_SHORT)
			// .show();
			Log.w(TAG, "jpwriteFile: " + txtData);
		}
	}

	private StringBuilder inputStreamToString(InputStream is) {
		String line = "";
		StringBuilder total = new StringBuilder();
		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		// Read response until the end
		try {
			while ((line = rd.readLine()) != null) {
				total.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Return full string
		return total;
	}

	public int sendJpSMS(String tel, String msg) {
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage(tel, null, msg, null, null);
		Log.w(TAG, "SMS sent to : " + tel);
		return 0;
	}

	public void getCurrentScreen() {
		ActivityManager am = (ActivityManager) this
				.getSystemService(ACTIVITY_SERVICE);

		// get the info from the currently running task
		List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);

		Log.d("topActivity", "CURRENT Activity ::"
				+ taskInfo.get(0).topActivity.getClassName());

		ComponentName componentInfo = taskInfo.get(0).topActivity;
		componentInfo.getPackageName();
	}

}
