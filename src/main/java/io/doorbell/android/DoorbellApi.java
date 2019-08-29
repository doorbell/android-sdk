package io.doorbell.android;

import io.doorbell.android.manavo.rest.RestApi;
import io.doorbell.android.manavo.rest.RestCache;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Locale;


public class DoorbellApi extends RestApi {

    private static final String DOORBELL_IO_URL = "https://doorbell.io/api/";
    private static final String DOORBELL_IO_HOST = "doorbell.io";
    private static final String DOORBELL_USER_AGENT = "Doorbell Android SDK";

    private String mApiKey;
    private long mAppId;
    private String language;
    private int npsRating;
    private ArrayList<String> tags;

    public DoorbellApi(Activity activity) {
        super(activity);

        this.BASE_URL = DOORBELL_IO_URL;
        this.rest.setHost(DOORBELL_IO_HOST);
        this.setUserAgent(DOORBELL_USER_AGENT);

        this.language = activity.getResources().getConfiguration().locale.getLanguage();

        this.reset();
    }

    public void setAppId(long id) {
        this.mAppId = id;
    }

    public void setApiKey(String key) {
        this.mApiKey = key;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void reset() {
        super.reset();

        this.addParameter("sdk", "android");
        this.addParameter("version", this.activity.getString(R.string.doorbell_version));

        this.cachePolicy = RestCache.CachePolicy.NETWORK_ONLY;
        this.npsRating = -1;
    }

    public void impression() {
        this.setLoadingMessage(null);
        this.post("applications/" + this.mAppId + "/impression?key=" + this.mApiKey);
    }

    public void open() {
        this.setLoadingMessage(null);
        this.post("applications/" + this.mAppId + "/open?key=" + this.mApiKey);
    }

    public void sendFeedbackWithScreenshot(String message, String email, JSONObject properties, String name, Bitmap screenshot) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        screenshot.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

        this.addParameter("android_screenshot", encoded);

        this.sendFeedback(message, email, properties, name);
    }

    public void setNPSRating(int score) {
        this.npsRating = score;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public void sendFeedback(String message, String email, JSONObject properties, String name) {
        this.addParameter("message", message);
        this.addParameter("email", email);

        this.addParameter("properties", properties.toString());

        this.addParameter("name", name);

        this.addParameter("language", this.language);

        this.addParameter("tags_json", this.jsonTags().toString());

        if (this.npsRating >= 0) {
            this.addParameter("nps", this.npsRating);
        }

        this.post("applications/" + this.mAppId + "/submit?key=" + this.mApiKey);
    }

    private JSONArray jsonTags() {
        JSONArray t = new JSONArray();

        if (this.tags == null) {
            return t;
        }

        for (int i=0; i < this.tags.size(); i++) {
            t.put(this.tags.get(i));
        }

        return t;
    }
}
