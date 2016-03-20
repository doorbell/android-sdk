package io.doorbell.android;

import io.doorbell.android.callbacks.OnFeedbackSentCallback;
import io.doorbell.android.callbacks.OnShowCallback;
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

    private static final String PROPERTY_MODEL = "Model";
    private static final String PROPERTY_ANDROID_VERSION = "Android Version";
    private static final String PROPERTY_WI_FI_ENABLED = "WiFi enabled";
    private static final String PROPERTY_MOBILE_DATA_ENABLED = "Mobile Data enabled";
    private static final String PROPERTY_GPS_ENABLED = "GPS enabled";
    private static final String PROPERTY_SCREEN_RESOLUTION = "Screen Resolution";
    private static final String PROPERTY_ACTIVITY = "Activity";
    private static final String PROPERTY_APP_VERSION_NAME = "App Version Name";
    private static final String PROPERTY_APP_VERSION_CODE = "App Version Code";

    private static final String POWERED_BY_DOORBELL_TEXT = "Powered by <a href=\"https://doorbell.io\">Doorbell.io</a>";

    private Activity mActivity;
    private Context mContext;

    private OnFeedbackSentCallback mOnFeedbackSentCallback = null;
    private OnShowCallback mOnShowCallback = null;

    private String mName = "";

    private EditText mMessageField;
    private EditText mEmailField;
    private TextView mPoweredByField;

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

        this.setTitle(activity.getString(R.string.doorbell_title));

        this.setCancelable(true);

        this.buildProperties();


	    
        // Set app related properties
        PackageManager manager = activity.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(activity.getPackageName(), 0);

            this.addProperty(PROPERTY_APP_VERSION_NAME, info.versionName);
            this.addProperty(PROPERTY_APP_VERSION_CODE, info.versionCode);
        } catch (NameNotFoundException e) {

        }

        this.buildView();
    }

    private void buildProperties() {
        // Set phone related properties
        // this.addProperty("Brand", Build.BRAND); // mobile phone carrier
        this.addProperty(PROPERTY_MODEL, Build.MODEL);
        this.addProperty(PROPERTY_ANDROID_VERSION, Build.VERSION.RELEASE);

        try {
            SupplicantState supState;
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();

            this.addProperty(PROPERTY_WI_FI_ENABLED, supState);
        } catch (Exception e) {

        }


        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }
        this.addProperty(PROPERTY_MOBILE_DATA_ENABLED, mobileDataEnabled);

        try {
            final LocationManager manager = (LocationManager) this.mContext.getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            this.addProperty(PROPERTY_GPS_ENABLED, gpsEnabled);
        } catch (Exception e) {

        }

        try {
            DisplayMetrics metrics = new DisplayMetrics();

            this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            String resolution = Integer.toString(metrics.widthPixels) + "x" + Integer.toString(metrics.heightPixels);
            this.addProperty(PROPERTY_SCREEN_RESOLUTION, resolution);
        } catch (Exception e) {

        }

        try {
            String activityName = this.mActivity.getClass().getSimpleName();
            this.addProperty(PROPERTY_ACTIVITY, activityName);
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
        this.mEmailField.setVisibility(visibility);
        return this;
    }

    public Doorbell setPoweredByVisibility(int visibility) {
        this.mPoweredByField.setVisibility(visibility);
        return this;
    }

    public Doorbell setEmailHint(String emailHint) {
        this.mEmailField.setHint(emailHint);
        return this;
    }

    public Doorbell setEmailHint(int emailHintResId) {
        this.mEmailField.setHint(emailHintResId);
        return this;
    }

    public Doorbell setMessageHint(String messageHint) {
        this.mMessageField.setHint(messageHint);
        return this;
    }

    public Doorbell setMessageHint(int messageHintResId) {
        this.mMessageField.setHint(messageHintResId);
        return this;
    }

    public Doorbell setPositiveButtonText(String text) {
        this.setPositiveButton(text, null);
        return this;
    }

    public Doorbell setPositiveButtonText(int textResId) {
        this.setPositiveButton(textResId, null);
        return this;
    }

    public Doorbell setNegativeButtonText(String text) {
        this.setNegativeButton(text, null);
        return this;
    }

    public Doorbell setNegativeButtonText(int textResId) {
        this.setNegativeButton(textResId, null);
        return this;
    }

    public Doorbell setTitle(String title) {
        super.setTitle(title);
        return this;
    }

    public Doorbell setTitle(int titleResId) {
        super.setTitle(titleResId);
        return this;
    }

    public Doorbell setEmail(String email) {
        this.mEmailField.setText(email);
        return this;
    }

    public Doorbell setOnShowCallback(OnShowCallback onShowCallback) {
        this.mOnShowCallback = onShowCallback;
        return this;
    }

    public Doorbell setOnFeedbackSentCallback(OnFeedbackSentCallback onFeedbackSentCallback) {
        this.mOnFeedbackSentCallback = onFeedbackSentCallback;
        return this;
    }

    public Doorbell setName(String name) {
        this.mName = name;
        return this;
    }

    public Doorbell impression() {
        this.mApi.impression();

        return this;
    }

    private void buildView() {
        LinearLayout mainLayout = new LinearLayout(this.mContext);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        int padding = (int)this.mContext.getResources().getDimension(R.dimen.form_side_padding);
        mainLayout.setPadding(padding, 0, padding, 0);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        this.mMessageField = new EditText(this.mContext);
        this.mMessageField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mMessageField.setMinLines(2);
        this.mMessageField.setGravity(Gravity.TOP);
        this.mMessageField.setInputType(this.mMessageField.getInputType() | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        this.setMessageHint(this.mActivity.getString(R.string.doorbell_message_hint));
        mainLayout.addView(this.mMessageField);

        this.mEmailField = new EditText(this.mContext);
        this.mEmailField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mEmailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        this.setEmailHint(this.mActivity.getString(R.string.doorbell_email_hint));
        mainLayout.addView(this.mEmailField);

        this.mPoweredByField = new TextView(this.mContext);
        this.mPoweredByField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mPoweredByField.setText(Html.fromHtml(POWERED_BY_DOORBELL_TEXT));
        this.mPoweredByField.setPadding(7, 7, 7, 7);
        this.mPoweredByField.setMovementMethod(LinkMovementMethod.getInstance());
        mainLayout.addView(this.mPoweredByField);

        this.setView(mainLayout);

        this.setPositiveButtonText(this.mActivity.getString(R.string.doorbell_send));
        this.setNegativeButtonText(this.mActivity.getString(R.string.doorbell_cancel));
    }

    public AlertDialog show() {
        this.mApi.open();

        final AlertDialog dialog = super.show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Doorbell.this.mApi.setLoadingMessage(Doorbell.this.mActivity.getString(R.string.doorbell_sending));
                Doorbell.this.mApi.setCallback(new RestCallback() {
                    @Override
                    public void success(Object obj) {
                        if (Doorbell.this.mOnFeedbackSentCallback != null) {
                            Doorbell.this.mOnFeedbackSentCallback.handle(obj.toString());
                        } else {
                            Toast.makeText(Doorbell.this.mContext, obj.toString(), Toast.LENGTH_LONG).show();
                        }

                        Doorbell.this.mMessageField.setText("");
                        Doorbell.this.mProperties = new JSONObject();

                        dialog.hide();
                    }
                });
                Doorbell.this.mApi.sendFeedback(Doorbell.this.mMessageField.getText().toString(), Doorbell.this.mEmailField.getText().toString(), Doorbell.this.mProperties, Doorbell.this.mName);
            }
        });

        if (this.mOnShowCallback != null) {
            this.mOnShowCallback.handle();
        }

        return dialog;
    }

}