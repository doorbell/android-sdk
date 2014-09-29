package io.doorbell.android;

import io.doorbell.android.manavo.rest.RestCallback;

import java.lang.reflect.Method;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Doorbell extends AlertDialog.Builder {

	private Activity mActivity;
	private Context mContext;
	
	private String mMessageHint = "What's on your mind?";
	private String mEmailHint = "Your email address";
	private int mEmailFieldVisibility = View.VISIBLE;
	private int mPoweredByVisibility = View.VISIBLE;
	
	private String mEmail = "";
	
	private EditText mMessageField;
	private EditText mEmailField;
	
	private JSONObject mProperties;
	
	private DoorbellApi mApi;
	
	public Doorbell(Activity activity, long id, String privateKey) {
		super(activity);
		
		this.mApi = new DoorbellApi(activity);
		
		this.mProperties = new JSONObject();
		
		this.mActivity = activity;
		this.mContext = activity;
		this.setAppId(id);
		this.setApiKey(privateKey);
		
		this.setTitle("Feedback");
		this.setCancelable(true);
		
		this.buildProperties();
		
	    
	    
		// Set app related properties
		PackageManager manager = activity.getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(activity.getPackageName(), 0);
			
			this.addProperty("App Version Name", info.versionName);
			this.addProperty("App Version Code", info.versionCode);
		} catch (NameNotFoundException e) {
			
		}
	}
	
	private void buildProperties() {
		// Set phone related properties
		// this.addProperty("Brand", Build.BRAND); // mobile phone carrier
		this.addProperty("Model", Build.MODEL);
		this.addProperty("Android Version", Build.VERSION.RELEASE);
		
		try {
			SupplicantState supState; 
			WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			supState = wifiInfo.getSupplicantState();
			
			this.addProperty("WiFi enabled", supState);
		} catch (Exception e) {
			
		}


		boolean mobileDataEnabled = false; // Assume disabled
	    ConnectivityManager cm = (ConnectivityManager)this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	    try {
	        Class cmClass = Class.forName(cm.getClass().getName());
	        Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
	        method.setAccessible(true); // Make the method callable
	        // get the setting for "mobile data"
	        mobileDataEnabled = (Boolean)method.invoke(cm);
	    } catch (Exception e) {
	        // Some problem accessible private API
	        // TODO do whatever error handling you want here
	    }
	    this.addProperty("Mobile Data enabled", mobileDataEnabled);

	    try {
		    final LocationManager manager = (LocationManager) this.mContext.getSystemService( Context.LOCATION_SERVICE );
		    boolean gpsEnabled = manager.isProviderEnabled( LocationManager.GPS_PROVIDER );
		    
		    this.addProperty("GPS enabled", gpsEnabled);
	    } catch (Exception e) {
	    	
	    }
	    
	    

	    try {
	        DisplayMetrics metrics = new DisplayMetrics();
	 
	        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
	 
	        String resolution = Integer.toString(metrics.widthPixels) + "x" + Integer.toString(metrics.heightPixels);
	        this.addProperty("Screen Resolution", resolution);
	    } catch (Exception e) {
	    	
	    }
	    
	    try {
		    String activityName = this.mActivity.getClass().getSimpleName();
		    this.addProperty("Activity", activityName);
	    } catch (Exception e) {
	    	
	    }
	}
	
	public Doorbell addProperty(String key, Object value) {
		try {
			this.mProperties.put(key, value);
		} catch (JSONException e) {
			// caught
		}
		
		return this;
	}
	
	public Doorbell setAppId(long id) {
		this.mApi.setAppId(id);
		return this;
	}
	
	public Doorbell setApiKey(String key) {
		this.mApi.setApiKey(key);
		return this;
	}
	
	public Doorbell setEmailFieldVisibility(int visibility) {
		this.mEmailFieldVisibility = visibility;
		return this;
	}
	
	public Doorbell setPoweredByVisibility(int visibility) {
		this.mPoweredByVisibility = visibility;
		return this;
	}
	
	public Doorbell setEmailHint(String emailHint) {
		this.mEmailHint = emailHint;
		return this;
	}
	
	public Doorbell setEmail(String email) {
		this.mEmail = email;
		return this;
	}
	
	public Doorbell impression() {
		this.mApi.impression();
		
		return this;
	}
	
	public AlertDialog show() {
		this.mApi.open();
		
		LinearLayout mainLayout = new LinearLayout(this.mContext);
		mainLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		
		this.mMessageField = new EditText(this.mContext);
		this.mMessageField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		this.mMessageField.setHint(this.mMessageHint);
		this.mMessageField.setMinLines(2);
		this.mMessageField.setGravity(Gravity.TOP);
		this.mMessageField.setInputType(this.mMessageField.getInputType() | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		mainLayout.addView(this.mMessageField);
		
		this.mEmailField = new EditText(this.mContext);
		this.mEmailField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		this.mEmailField.setHint(this.mEmailHint);
		this.mEmailField.setText(this.mEmail);
		this.mEmailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		this.mEmailField.setVisibility(this.mEmailFieldVisibility);
		mainLayout.addView(this.mEmailField);
		
		TextView poweredBy = new TextView(this.mContext);
		poweredBy.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		poweredBy.setText(Html.fromHtml("Powered by <a href=\"https://doorbell.io\">Doorbell.io</a>"));
		poweredBy.setPadding(7, 7, 7, 7);
		poweredBy.setVisibility(this.mPoweredByVisibility);
		poweredBy.setMovementMethod(LinkMovementMethod.getInstance());
		mainLayout.addView(poweredBy);
		
		
		this.setView(mainLayout);
		
		this.setPositiveButton("Send", null);
		this.setNegativeButton("Cancel", null);
		
		final AlertDialog dialog = super.show();
		
		Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		positiveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Doorbell.this.mApi.setLoadingMessage("Sending...");
				Doorbell.this.mApi.setCallback(new RestCallback() {
					@Override
					public void success(Object obj) {
						Toast.makeText(Doorbell.this.mContext, obj.toString(), Toast.LENGTH_LONG).show();
						
						Doorbell.this.mMessageField.setText("");
						Doorbell.this.mProperties = new JSONObject();
						
						dialog.hide();
					}
				});
				Doorbell.this.mApi.sendFeedback(Doorbell.this.mMessageField.getText().toString(), Doorbell.this.mEmailField.getText().toString(), Doorbell.this.mProperties);
			}
		});
		
		return dialog;
	}
	
}
