package jp.bank;

//1) autocompletion cachée par le keyboard
//http://www.giantflyingsaucer.com/blog/?p=1342
//11) OK20140321.récupérer les valeurs d'autocompletion depuis le serveur
//et les sauvegarder dans un fichier local.
//2) rajouter le numero de cheque venant du serveur
//20140522 add an information about the number of pending entries
//20140522 add the possibility to scan entries from server
//20140625 modification du titre fenetre moyen de paiement
//         modification de la position des boutons qui 
//         surtout ENVOYER qui peut etre sollicité par erreur
//         en entrant le beneficiaire
// 20140701 layout changé pour tout mettre sur une seule ligne
//			modifié le lop time pour que ce soit plus rapide
//			apparition du champ cheque sur selection
// TBD: rajouter un log local et remote
// rajouter la possibilité de parcourir les transactions
// locales et remote
//20140901 add a menu item to update the list of "moyen de paiement"

//WARNING si beneficiaire 1&1 alors pb!!!!!
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Credentials;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BankActivity extends Activity implements OnClickListener {
	private static final String TAG = "com.BankActivity";
	private static final String DEBUG = "com.BankActivity.debug";
	public static final String INTENTERROR = "probleme d'acces serveur";
	public static final String INTENTSUCCESS = "Enregistrement effectué";
	public static final String INTENTRESET = "RESET";
	private static final String LIEUFILE = "jplieu.txt";
	private static final String BENEFICAIREFILE = "jpbene.txt";

	Button buttonStart, buttonStop;

	final int Date_Dialog_ID = 0;
	EditText montant, moyenPaiement, etDate, cheque;
	AutoCompleteTextView bene, lieu;
	String paiement;
	private String array_spinner[];
	private Spinner spinner1, spinner2;

	int cDay, cMonth, cYear; // this is the instances of the current date
	Calendar cDate;
	int sDay, sMonth, sYear; // this is the instances of the entered date

	// public void onWindowFocusChanged (boolean hasFocus) {
	// int x = buttonStop.getHeight();
	// Log.d(TAG, "buttonStop Height= " + x);
	// buttonStop.setWidth(x);
	//
	// }

	private String[] LIEUX = new String[] {};
	// private String[] BENEFICIAIRES = new String[] { "Belgium", "Belgarde",
	// "Bossi", "biture", "boo" };
	private String[] BENEFICIAIRES = new String[] {};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStop = (Button) findViewById(R.id.buttonStop);
		montant = (EditText) findViewById(R.id.montant);

		getAutocompletionData();
		bene = (AutoCompleteTextView) findViewById(R.id.bene);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.jpauto, BENEFICIAIRES);
		for (int i = 0; i < BENEFICIAIRES.length; i++) {
			Log.d(TAG, "BENE [" + i + "] " + BENEFICIAIRES[i]);
		}

		lieu = (AutoCompleteTextView) findViewById(R.id.lieu);
		ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
				R.layout.jpauto, LIEUX);
		for (int i = 0; i < LIEUX.length; i++) {
			Log.d(TAG, "LIEU [" + i + "] " + LIEUX[i]);
		}
		// the following is in error:
		// ArrayAdapter<String> adapter = new
		// ArrayAdapter<String>(this,android.R.layout.jpauto, COUNTRIES);
		AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.bene);
		textView.setAdapter(adapter);
		AutoCompleteTextView textView2 = (AutoCompleteTextView) findViewById(R.id.lieu);
		textView2.setAdapter(adapter2);
		// // mgr.hideSoftInputFromWindow(bene.getWindowToken(), 0);
		// bene.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

		cheque = (EditText) findViewById(R.id.cheque);
		// moyenPaiement = (EditText) findViewById(R.id.moyenPaiement);
		// lieu = (EditText) findViewById(R.id.lieu);
		etDate = (EditText) findViewById(R.id.date);
		// change_date = (Button) findViewById(R.id.buttonDate);
		addItemsOnSpinner2();
		addListenerOnSpinnerItemSelection();
		buttonStart.setOnClickListener(this);
		buttonStop.setOnClickListener(this);

		// DatePicker picker = (DatePicker) findViewById(R.id.datePicker1);
		//
		//
		// ViewGroup childpicker = (ViewGroup)
		// findViewById(Resources.getSystem()
		// .getIdentifier("month" /*
		// * rest is: day, year
		// */, "id", "android"));
		// EditText textview = (EditText) childpicker
		// .findViewById(Resources.getSystem().getIdentifier(
		// "timepicker_input", "id", "android"));
		// textview.setTextSize(10);
		// textview.setTextColor(Color.GREEN);
		// ViewGroup childpicker2 = (ViewGroup)
		// findViewById(Resources.getSystem()
		// .getIdentifier("year", "id", "android"));
		// EditText textview2 = (EditText)
		// childpicker2.findViewById(Resources.getSystem().getIdentifier("timepicker_input",
		// "id", "android"));
		// textview2.setTextSize(10);
		// textview2.setTextColor(Color.BLUE);
		// //change day
		// childpicker2 = (ViewGroup) findViewById(Resources.getSystem()
		// .getIdentifier("day", "id", "android"));
		// textview2 = (EditText)
		// childpicker2.findViewById(Resources.getSystem().getIdentifier("timepicker_input",
		// "id", "android"));
		// textview2.setTextSize(10);
		// textview2.setTextColor(Color.RED);
		// //change +
		// childpicker2 = (ViewGroup) findViewById(Resources.getSystem()
		// .getIdentifier("+", "id", "android"));
		// textview2 = (EditText)
		// childpicker2.findViewById(Resources.getSystem().getIdentifier("timepicker_input",
		// "id", "android"));
		// textview2.setTextSize(10);
		// textview2.setTextColor(Color.RED);

		updateDateDisplay();
		LocalBroadcastManager bManager = LocalBroadcastManager
				.getInstance(this);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(RECEIVE_JSON);
		bManager.registerReceiver(bReceiver, intentFilter);
		Intent aService = new Intent(this, BankService.class);
		// pass userid/pw parameter to service
		Bundle b = new Bundle();
		b.putString("montant", montant.getText().toString());
		// Log.d(TAG, "click montant = " + montant.getText().toString());
		b.putString("paiement", paiement);
		// Log.d(TAG, "click paiement = " + paiement);

		if (paiement == "Cheque") {
			b.putString("cheque", cheque.getText().toString());

		} else
			b.putString("cheque", "0");

		b.putString("date", etDate.getText().toString());
		// Log.d(TAG, "click date = " + etDate.getText().toString());
		b.putString("lieu", lieu.getText().toString());
		// Log.d(TAG, "click lieu = " + lieu.getText().toString());
		b.putString("bene", bene.getText().toString());
		// Log.d(TAG, "click bene = " + bene.getText().toString());
		aService.putExtras(b);
		startService(aService);
		TelephonyManager tManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		String uid = tManager.getDeviceId();
		Log.d(TAG, " le serial number est: " + uid);
		
		

	}
	//menu item
			@Override
			public boolean onCreateOptionsMenu(Menu menu) {
			    // Inflate the menu items for use in the action bar
			    MenuInflater inflater = getMenuInflater();
			    inflater.inflate(R.menu.activity_main, menu);
			    return super.onCreateOptionsMenu(menu);
			}
			@Override
			public boolean onOptionsItemSelected(MenuItem item) {
			    // Handle presses on the action bar items
			    switch (item.getItemId()) {
			        case R.id.action_search:
			            //openSearch();
			        	Log.d(DEBUG, "+++> search bouton appuyé");
			            return true;
			        case R.id.action_compose:
			            //composeMessage();
			        	Log.d(DEBUG, "+++> Compose bouton appuyé");
			            return true;
			        case R.id.menu_settings:
			            //composeMessage();
			        	Log.d(DEBUG, "+++> Menu settings appuyé");
			            return true;
			        default:
			            return super.onOptionsItemSelected(item);
			    }
			}

	private void updateDateDisplay() {
		// TODO Auto-generated method stub
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		cDate = Calendar.getInstance();
		String ladate = dateFormat.format(cDate.getTime());
		etDate.setText(ladate);
		// montant.setText("");
		lieu.setText("");
		bene.setText("");

	}

	private void getAutocompletionData() {

		// get autocompletion data
		LIEUX = jpreadFile(LIEUFILE);
		BENEFICIAIRES = jpreadFile(BENEFICAIREFILE);
		Log.d(TAG, " BENE = " + BENEFICIAIRES[0]);
	}

	public void addListenerOnSpinnerItemSelection() {
		spinner1 = (Spinner) findViewById(R.id.spinner1);

		// spinner1.setOnItemSelectedListener(new
		// CustomOnItemSelectedListener());
	}

	// Your activity will respond to this action String
	public static final String RECEIVE_JSON = "com.your.package.RECEIVE_JSON";
	// Your service will respond to this action String
	public static final String RECEIVE_JPJSON = "com.your.package.RECEIVE_JPJSON";

	// sendIntent to the service
	public void sendIntent(String aMsg) {
		// this code identified A
		Log.w(TAG, "ACTIVITY sends this message to SERVICE: " + aMsg);
		Intent RTReturn = new Intent(BankService.RECEIVE_JPJSON);
		RTReturn.putExtra("json", aMsg);
		LocalBroadcastManager.getInstance(this).sendBroadcast(RTReturn);

	}

	public void addItemsOnSpinner2() {

		spinner1 = (Spinner) findViewById(R.id.spinner1);
		List<String> list = new ArrayList<String>();
		list.add("CB La Poste achat");
		list.add("CB La Poste retrait");
		list.add("CB Oney achat");
		list.add("CB Oney retrait");
		list.add("Cash");
		list.add("Cheque");
		list.add("Virement");
		list.add("Prélèvement");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(dataAdapter);
		spinner1.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				Log.d(TAG, " SELECTION ======= " + spinner1.getSelectedItem());
				cheque = (EditText) findViewById(R.id.cheque);
				TextView chequeLabel = (TextView) findViewById(R.id.chequeText);

				if (spinner1.getSelectedItem() == "Cheque") {
					cheque.setVisibility(View.VISIBLE);
					chequeLabel.setVisibility(View.VISIBLE);
					Log.d(TAG, " cheque label ======= " + chequeLabel.getText());
				} else
					cheque.setVisibility(View.GONE);
				chequeLabel.setVisibility(View.GONE);
				// cheque.setVisibility(View.VISIBLE);

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		});
	}

	private boolean jpvalidate() {
		boolean validation = false;
		try {
			Float montantEF = Float.parseFloat(montant.getText().toString());
			validation = true;
			montant.setBackgroundColor(Color.WHITE);
			if (lieu.getText().length() < 1)
				validation = false;
			if (bene.getText().length() < 1)
				validation = false;

		} catch (NumberFormatException e) {
			validation = false;
			montant.setBackgroundColor(Color.RED);
			Log.d(TAG, "validation ERROR");
		}
		if (validation == false) {
			Toast.makeText(this, "validation ERROR", Toast.LENGTH_LONG).show();
			buttonStart.setBackgroundColor(Color.RED);
		}
		return validation;
	}

	// this code identified A
	private BroadcastReceiver bReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(RECEIVE_JSON)) {
				String serviceJsonString = intent.getStringExtra("json");
				Log.d(TAG, "--> intent received from service: "
						+ serviceJsonString);
				if (!serviceJsonString.contains(INTENTRESET))
					Toast.makeText(context, serviceJsonString,
							Toast.LENGTH_LONG).show();
				if (serviceJsonString.contains(INTENTSUCCESS)) {
					Log.d(TAG, "VERT" + INTENTSUCCESS);
					buttonStart.setBackgroundColor(Color.GREEN);
					updateDateDisplay();
				} else {
					if (serviceJsonString.contains(INTENTRESET)) {
						Log.d(TAG, "WHITE, reset button " + INTENTRESET);
						buttonStart.setBackgroundColor(Color.WHITE);
					} else {
						Log.d(TAG, "ROUGE " + INTENTERROR);
						buttonStart.setBackgroundColor(Color.RED);
					}
				}
			}
		}
	};

	@Override
	public void onClick(View src) {
		// Log.d(TAG,"---------2 eme onCLick ");
		buttonStart.setBackgroundColor(Color.CYAN);

		paiement = spinner1.getSelectedItem().toString();
		Log.d(TAG, "click paiement = " + paiement);

		switch (src.getId()) {
		case R.id.buttonStart:
			if (jpvalidate() == false)
				return;
			Log.d(TAG, "Start JpBank service");
			String msg = montant.getText().toString() + "&" + paiement + "&"
					+ cheque.getText().toString() + "&"
					+ etDate.getText().toString() + "&"
					+ lieu.getText().toString() + "&"
					+ bene.getText().toString();
			Log.d(TAG, "click bene = " + bene.getText().toString());

			// buttonStart.setEnabled(false);
			buttonStop.setEnabled(true);

			sendIntent(msg);
			updateDateDisplay();
			break;
		case R.id.buttonStop:
			Log.d(TAG, "onClick: stopping JpBank service");
			stopService(new Intent(this, BankService.class));
			buttonStart.setEnabled(true);
			buttonStop.setEnabled(false);

			// pour tuer l'application
			android.os.Process.killProcess(android.os.Process.myPid());
			break;
		}

		// finish();
	}

	private String[] jpreadFile(String thefile) {
		String[] returnedData;
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
		returnedData = aBuffer.split(",");
		return returnedData;
	}
}
