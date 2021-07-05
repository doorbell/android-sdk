package io.doorbell.android.manavo.rest;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public class RestRequest {

    private String username;
    private String password;
    private Handler handler;
    private final ExecutorService executorService;

    private boolean acceptAllSslCertificates = false;

    private Map<String, String> data;

    private String userAgent = null;

    private String contentType = null;

    public RestRequest() {
        this.executorService = Executors.newFixedThreadPool(1);
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public void authorize(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void acceptAllSslCertificates() {
        this.acceptAllSslCertificates = true;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public void setUserAgent(String agent) {
        this.userAgent = agent;
    }

    public void get(String url) throws IOException {
        if (this.data.size() > 0) {
            // if we don't already have some query string parameters, add a ?
            if (!url.contains("?")) {
                url += "?";
            } else { // if query sting parameter already exist, then keep adding to them
                url += "&";
            }

            StringBuilder urlBuilder = new StringBuilder(url);
            for (String key : this.data.keySet()) {
                try {
                    urlBuilder.append(URLEncoder.encode(key, "utf-8")).append("=").append(URLEncoder.encode(this.data.get(key), "utf-8")).append("&");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            url = urlBuilder.toString();
            url = url.substring(0, url.length() - 1);
        }

        HttpsURLConnection httpURLConnection = (HttpsURLConnection) (new URL(url)).openConnection();
        httpURLConnection.setRequestMethod("GET");

        this.prepareRequest(httpURLConnection);
    }

    public void post(String url) throws IOException {
        HttpsURLConnection httpURLConnection = (HttpsURLConnection) (new URL(url)).openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);

        this.prepareRequest(httpURLConnection);
    }

    public void put(String url) throws IOException {
        HttpsURLConnection httpURLConnection = (HttpsURLConnection) (new URL(url)).openConnection();
        httpURLConnection.setRequestMethod("PUT");
        httpURLConnection.setDoOutput(true);

        this.prepareRequest(httpURLConnection);
    }

    public void delete(String url) throws IOException {
        HttpsURLConnection httpURLConnection = (HttpsURLConnection) (new URL(url)).openConnection();
        httpURLConnection.setRequestMethod("DELETE");

        this.prepareRequest(httpURLConnection);
    }

    protected byte[] prepareData() {
        if (this.contentType != null && this.contentType.equalsIgnoreCase("application/json")) {
            JSONObject data = new JSONObject();
            try {
                for (String key : this.data.keySet()) {
                    data.put(key, this.data.get(key));
                }

                Log.d("RestRequestSending", data.toString());

                return data.toString().getBytes(StandardCharsets.UTF_8);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            try {
                StringBuilder data = new StringBuilder();

                for (String key : this.data.keySet()) {
                    data.append(URLEncoder.encode(key, "utf-8")).append("=").append(URLEncoder.encode(this.data.get(key), "utf-8")).append("&");
                }

                // get rid of the last ampersand
                if (data.length() > 0) {
                    data.substring(0, data.length() - 1);
                }

                return data.toString().getBytes(StandardCharsets.UTF_8);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private void prepareRequest(HttpsURLConnection request) {
        request.setDoInput(true);

        if (this.contentType != null) {
            request.setRequestProperty("Content-type", this.contentType);
        }

        request.setRequestProperty("Accept", "application/json");

        this.executorService.execute(() -> {
            Bundle b = RestRequest.this.executeRequest(request);

            Message m = new Message();
            m.setData(b);
            m.setTarget(RestRequest.this.handler);
            m.sendToTarget();
        });
    }

    public void cancelRequest() {
        this.executorService.shutdown();
    }

    private Bundle executeRequest(HttpsURLConnection request) {
        Bundle b = new Bundle();

        try {
            if (this.userAgent != null) {
                request.setRequestProperty("User-Agent", this.userAgent);
            }

            if (this.username != null && this.password != null) {
                String auth = this.username + ":" + this.password;
                byte[] encodedAuth = Base64.encode(auth.getBytes(StandardCharsets.UTF_8), 0);

                String authHeaderValue = "Basic " + new String(encodedAuth);
                request.setRequestProperty("Authorization", authHeaderValue);
            }
            request.addRequestProperty("Accept-Encoding", "gzip");

            if (request.getRequestMethod().equalsIgnoreCase("POST") || request.getRequestMethod().equalsIgnoreCase("PUT")) {
                byte[] input = this.prepareData();
                if (input.length > 0) {
                    try (OutputStream os = request.getOutputStream()) {
                        os.write(input, 0, input.length);
                    }
                }
            }

            int statusCode = request.getResponseCode();

            InputStream responseInputStream;
            if (request.getResponseCode() >= 200 && request.getResponseCode() < 300) {
                responseInputStream = request.getInputStream();
            } else {
                responseInputStream = request.getErrorStream();
            }

            StringBuilder responseDataBuilder;
            String responseData;
            String contentEncoding = request.getHeaderField("Content-Encoding");
            if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                GZIPInputStream gzipInputStream = new GZIPInputStream(responseInputStream);

                InputStreamReader reader = new InputStreamReader(gzipInputStream);
                BufferedReader in = new BufferedReader(reader);

                String line;
                responseDataBuilder = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    responseDataBuilder.append(line);
                }

                in.close();
                reader.close();
                gzipInputStream.close();
            } else {
                InputStreamReader reader = new InputStreamReader(responseInputStream);
                BufferedReader in = new BufferedReader(reader);

                String line;
                responseDataBuilder = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    responseDataBuilder.append(line);
                }

                in.close();
                reader.close();
            }

            responseData = responseDataBuilder.toString();

            Log.d("RestRequest", "ResponseData: "+responseData);

            responseInputStream.close();

            if (request.getResponseCode() >= 200 && request.getResponseCode() < 300) {
                b.putString("data", responseData);
            } else {
                b.putString("statusCodeError", responseData);
                b.putInt("statusCodeErrorNumber", request.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            b.putString("error", e.getMessage());
        }

        return b;
    }

}
