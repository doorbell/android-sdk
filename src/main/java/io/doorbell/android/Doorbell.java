package io.doorbell.android;

import androidx.appcompat.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import io.doorbell.android.callbacks.OnErrorCallback;
import io.doorbell.android.callbacks.OnFeedbackSentCallback;
import io.doorbell.android.callbacks.OnShowCallback;
import io.doorbell.android.manavo.rest.RestCallback;
import io.doorbell.android.shake.ShakeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

public class Doorbell {

    private static final String PROPERTY_MODEL = "Model";
    private static final String PROPERTY_ANDROID_VERSION = "Android Version";
    private static final String PROPERTY_WI_FI_ENABLED = "WiFi enabled";
    private static final String PROPERTY_MOBILE_DATA_ENABLED = "Mobile Data enabled";
    private static final String PROPERTY_GPS_ENABLED = "GPS enabled";
    private static final String PROPERTY_SCREEN_RESOLUTION = "Screen Resolution";
    private static final String PROPERTY_ACTIVITY = "Activity";
    private static final String PROPERTY_APP_VERSION_NAME = "App Version Name";
    private static final String PROPERTY_APP_VERSION_CODE = "App Version Code";
    private static final String PROPERTY_APP_LANGUAGE = "App Language";
    private static final String PROPERTY_DEVICE_LANGUAGE = "Device Language";

    private static final String POWERED_BY_DOORBELL_TEXT = "Powered by <a href=\"https://doorbell.io\">Doorbell.io</a>";

    private final Activity mActivity;
    private final Context mContext;

    private final AlertDialog.Builder mDialogBuilder;
    private AlertDialog mDialog;

    private OnFeedbackSentCallback mOnFeedbackSentCallback = null;
    private OnShowCallback mOnShowCallback = null;

    private String mName = "";

    private EditText mMessageField;
    private EditText mEmailField;
    private TextView mNPSLabel;
    private TextView mNPSScoreLabelLow;
    private TextView mNPSScoreLabelHigh;
    private SeekBar mNPSField;
    private LinearLayout mNPSScores;
    private TextView mPoweredByField;
    private Bitmap mScreenshot;

    private Boolean mNPSSelected;
    private int mSeekbarDefaultOffset;

    private JSONObject mProperties;

    private final DoorbellApi mApi;

    private final ShakeDetector shakeDetector;

    public Doorbell(Activity activity, long id, String privateKey) {
        this(activity, id, privateKey, new AlertDialog.Builder(activity));
    }

    public Doorbell(Activity activity, long id, String privateKey, AlertDialog.Builder dialogBuilder) {
        this.mDialogBuilder = dialogBuilder;
        this.mApi = new DoorbellApi(activity);

        this.mProperties = new JSONObject();

        this.mActivity = activity;
        this.mContext = activity;
        this.setAppId(id);
        this.setApiKey(privateKey);

        this.mDialogBuilder.setTitle(activity.getString(R.string.doorbell_title));

        this.mDialogBuilder.setCancelable(true);

        this.buildProperties();

        // Set app related properties
        PackageManager manager = activity.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(activity.getPackageName(), 0);

            this.addProperty(PROPERTY_APP_VERSION_NAME, info.versionName);
            this.addProperty(PROPERTY_APP_VERSION_CODE, info.versionCode);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        this.buildView();

        this.shakeDetector = new ShakeDetector(new ShakeDetector.Listener() {
            @Override
            public void hearShake() {
                try {
                    Doorbell.this.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void patchTLSOrPromptUser() {
        this.mApi.patchTLSOrPromptUser();
    }

    public boolean isFullySupported() {
        return this.mApi.isFullySupported();
    }

    public AlertDialog.Builder getDialogBuilder() {
        return this.mDialogBuilder;
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
            e.printStackTrace();
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
        }
        this.addProperty(PROPERTY_MOBILE_DATA_ENABLED, mobileDataEnabled);

        try {
            final LocationManager manager = (LocationManager) this.mContext.getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            this.addProperty(PROPERTY_GPS_ENABLED, gpsEnabled);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            DisplayMetrics metrics = new DisplayMetrics();

            this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            String resolution = Integer.toString(metrics.widthPixels) + "x" + Integer.toString(metrics.heightPixels);
            this.addProperty(PROPERTY_SCREEN_RESOLUTION, resolution);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String activityName = this.mActivity.getClass().getSimpleName();
            this.addProperty(PROPERTY_ACTIVITY, activityName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.addProperty(PROPERTY_APP_LANGUAGE, this.mActivity.getResources().getConfiguration().locale.getDisplayName());
        this.addProperty(PROPERTY_DEVICE_LANGUAGE, Locale.getDefault().getDisplayName());
    }

    public Doorbell setApiLanguage(String language) {
        this.mApi.setLanguage(language);
        return this;
    }

    public Doorbell addProperty(String key, Object value) {
        try {
            this.mProperties.put(key, value);
        } catch (JSONException e) {
            // caught
        }

        return this;
    }

    public Doorbell setTags(ArrayList<String> tags) {
        this.mApi.setTags(tags);
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

    public EditText getMessageField() {
        return this.mMessageField;
    }

    public EditText getEmailField() {
        return this.mEmailField;
    }

    public TextView getNPSLabel() {
        return this.mNPSLabel;
    }

    public SeekBar getNPSField() {
        return this.mNPSField;
    }

    public TextView getNPSScoreLabelLow() {
        return this.mNPSScoreLabelLow;
    }

    public TextView getNPSScoreLabelHigh() {
        return this.mNPSScoreLabelHigh;
    }

    public TextView getPoweredByField() {
        return this.mPoweredByField;
    }

    public Doorbell setPositiveButtonText(String text) {
        this.mDialogBuilder.setPositiveButton(text, null);
        return this;
    }

    public Doorbell setPositiveButtonText(int textResId) {
        this.mDialogBuilder.setPositiveButton(textResId, null);
        return this;
    }

    public Doorbell setNegativeButtonText(String text) {
        this.mDialogBuilder.setNegativeButton(text, null);
        return this;
    }

    public Doorbell setNegativeButtonText(int textResId) {
        this.mDialogBuilder.setNegativeButton(textResId, null);
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

    public Doorbell setOnErrorCallback(OnErrorCallback onErrorCallback) {
        this.mApi.setOnErrorCallback(onErrorCallback);
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

    public Doorbell captureScreenshot() {
        View v = this.mActivity.getWindow().getDecorView().getRootView();
        v.setDrawingCacheEnabled(true);
        this.mScreenshot = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);

        return this;
    }

    private void buildView() {
        LinearLayout mainLayout = new LinearLayout(this.mContext);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        int padding = (int) this.mContext.getResources().getDimension(R.dimen.form_side_padding);
        mainLayout.setPadding(padding, 0, padding, 0);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        this.mMessageField = new EditText(this.mContext);
        this.mMessageField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mMessageField.setMinLines(2);
        this.mMessageField.setGravity(Gravity.TOP);
        this.mMessageField.setInputType(this.mMessageField.getInputType() | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        this.mMessageField.setHint(this.mActivity.getString(R.string.doorbell_message_hint));
        mainLayout.addView(this.mMessageField);

        this.mEmailField = new EditText(this.mContext);
        this.mEmailField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mEmailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        this.mEmailField.setHint(this.mActivity.getString(R.string.doorbell_email_hint));
        mainLayout.addView(this.mEmailField);

        this.mNPSLabel = new TextView(this.mContext);
        LinearLayout.LayoutParams npsLabelLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        npsLabelLayoutParams.setMargins(0, 10, 0, 0);
        this.mNPSLabel.setLayoutParams(npsLabelLayoutParams);
        this.mNPSLabel.setText(R.string.doorbell_nps_label);
        this.mNPSLabel.setVisibility(View.GONE);
        mainLayout.addView(this.mNPSLabel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.mNPSField = new SeekBar(this.mContext, null, 0, android.R.style.Widget_Material_SeekBar_Discrete);
        } else {
            this.mNPSField = new SeekBar(this.mContext);
        }
        this.mNPSField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mNPSField.setMax(10);
        this.mSeekbarDefaultOffset = this.mNPSField.getThumbOffset();
        this.resetNPS();
        this.mNPSField.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Doorbell.this.mNPSField.setThumbOffset(Doorbell.this.mSeekbarDefaultOffset);
                Doorbell.this.mNPSSelected = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mainLayout.addView(this.mNPSField);
        this.mNPSField.setVisibility(View.GONE);

        this.mNPSScores = new LinearLayout(this.mContext);
        LinearLayout.LayoutParams npsScoresLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        npsScoresLayoutParams.setMargins(0, 0, 0, 10);
        this.mNPSScores.setLayoutParams(npsScoresLayoutParams);
        this.mNPSScores.setOrientation(LinearLayout.HORIZONTAL);
        this.mNPSScores.setVisibility(View.GONE);
        this.mNPSScoreLabelLow = new TextView(this.mContext);
        this.mNPSScoreLabelLow.setText(R.string.doorbell_nps_score_low);
        this.mNPSScoreLabelLow.setGravity(Gravity.START);
        this.mNPSScoreLabelLow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        this.mNPSScores.addView(this.mNPSScoreLabelLow);
        this.mNPSScoreLabelHigh = new TextView(this.mContext);
        this.mNPSScoreLabelHigh.setText(R.string.doorbell_nps_score_high);
        this.mNPSScoreLabelHigh.setGravity(Gravity.END);
        this.mNPSScoreLabelHigh.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        this.mNPSScores.addView(this.mNPSScoreLabelHigh);
        mainLayout.addView(this.mNPSScores);

        this.mPoweredByField = new TextView(this.mContext);
        this.mPoweredByField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mPoweredByField.setText(Html.fromHtml(POWERED_BY_DOORBELL_TEXT));
        this.mPoweredByField.setPadding(7, 7, 7, 7);
        this.mPoweredByField.setMovementMethod(LinkMovementMethod.getInstance());
        mainLayout.addView(this.mPoweredByField);

        this.mDialogBuilder.setView(mainLayout);

        this.setPositiveButtonText(this.mActivity.getString(R.string.doorbell_send));
        this.setNegativeButtonText(this.mActivity.getString(R.string.doorbell_cancel));
    }

    private void resetNPS() {
        // To push the thumb off screen
        this.mNPSField.setThumbOffset(100000);
        this.mNPSField.setProgress(0);
        this.mNPSSelected = false;
    }

    public Doorbell enableNPSRatings() {
        this.mNPSLabel.setVisibility(View.VISIBLE);
        this.mNPSField.setVisibility(View.VISIBLE);
        this.mNPSScores.setVisibility(View.VISIBLE);

        return this;
    }

    public Doorbell disableNPSRatings() {
        this.mNPSLabel.setVisibility(View.GONE);
        this.mNPSField.setVisibility(View.GONE);
        this.mNPSScores.setVisibility(View.GONE);

        this.resetNPS();

        return this;
    }

    public Doorbell enableShowOnShake() {
        SensorManager sensorManager = (SensorManager)this.mActivity.getSystemService(Context.SENSOR_SERVICE);
        this.shakeDetector.start(sensorManager);

        return this;
    }

    public Doorbell disableShowOnShake() {
        this.shakeDetector.stop();

        SensorManager sensorManager = (SensorManager)this.mActivity.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this.shakeDetector);

        return this;
    }

    public Doorbell setShakeSensitivity(int sensitivity) {
        this.shakeDetector.setSensitivity(sensitivity);

        return this;
    }

    public Doorbell destroy() {
        this.disableShowOnShake();

        return this;
    }

    public Doorbell resetProperties() {
        this.mProperties = new JSONObject();
        this.buildProperties();

        return this;
    }

    public AlertDialog show() {
        this.mApi.open();

        if (this.mDialog == null) {
            this.mDialog = this.mDialogBuilder.create();
        }
        this.mDialog.show();

        Button positiveButton = this.mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Doorbell.this.mApi.setLoadingMessage(Doorbell.this.mActivity.getString(R.string.doorbell_sending));
                Doorbell.this.mApi.setCallback(new RestCallback() {
                    @Override
                    public void success(Object obj) {
                        try {
                            if (Doorbell.this.mOnFeedbackSentCallback != null) {
                                Doorbell.this.mOnFeedbackSentCallback.handle(obj.toString());
                            } else {
                                Toast.makeText(Doorbell.this.mContext, obj.toString(), Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            // Sometimes we get exceptions thrown, even from just the Toast message (https://stackoverflow.com/questions/48152659/toast-maketext-giving-error-inflating-class-textview-exception)
                            // so add some protection here
                            e.printStackTrace();
                        }

                        Doorbell.this.mMessageField.setText("");
                        Doorbell.this.resetNPS();

                        Doorbell.this.mDialog.hide();
                    }
                });

                if (Doorbell.this.mNPSSelected) {
                    Doorbell.this.mApi.setNPSRating(Doorbell.this.mNPSField.getProgress());
                } else {
                    Doorbell.this.mApi.setNPSRating(-1);
                }

                if (Doorbell.this.mScreenshot != null) {
                    Doorbell.this.mApi.sendFeedbackWithScreenshot(Doorbell.this.mMessageField.getText().toString(), Doorbell.this.mEmailField.getText().toString(), Doorbell.this.mProperties, Doorbell.this.mName, Doorbell.this.mScreenshot);
                } else {
                    Doorbell.this.mApi.sendFeedback(Doorbell.this.mMessageField.getText().toString(), Doorbell.this.mEmailField.getText().toString(), Doorbell.this.mProperties, Doorbell.this.mName);
                }
            }
        });

        if (this.mOnShowCallback != null) {
            this.mOnShowCallback.handle();
        }

        return this.mDialog;
    }

}
