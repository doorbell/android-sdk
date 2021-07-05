package io.doorbell.android.manavo.rest;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RestCache {

    public static boolean exists(RestApi api) throws UnsupportedEncodingException {
        String hash = RestCache.getRequestHash(api);
        if (hash == null) {
            return false;
        } else {
            File file = RestCache.getFile(api.activity, hash);
            return file.exists();
        }
    }

    public static String get(RestApi api) throws UnsupportedEncodingException {
        String hash = RestCache.getRequestHash(api);
        if (hash == null) {
            return null;
        } else {
            File file = RestCache.getFile(api.activity, hash);

            BufferedReader r;
            try {
                r = new BufferedReader(new FileReader(file));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
                r.close();

                return total.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    public static void save(RestApi api, String data) throws UnsupportedEncodingException {
        String hash = RestCache.getRequestHash(api);

        if (hash != null) {
            File file = RestCache.getFile(api.activity, hash);
            BufferedWriter out;
            try {
                out = new BufferedWriter(new FileWriter(file), 1024);
                out.write(data);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void clear(Context c) {
        // clear all the cache files
        File cacheDir = c.getCacheDir();
        for (File cacheFile : cacheDir.listFiles()) {
            if (".".equals(cacheFile.getName()) || "..".equals(cacheFile.getName())) {
                continue;  // Ignore the self and parent aliases.
            }
            cacheFile.delete();
        }
    }

    private static File getFile(Context c, String hash) {
        String filename = RestCache.getFilename(c, hash);
        if (filename != null) {
            return new File(filename);
        } else {
            return null;
        }
    }

    private static String getFilename(Context c, String hash) {
        File dir = c.getCacheDir();

        if (!dir.canWrite()) {
            return null;
        }

        String cachePath = dir.getAbsolutePath();

        return cachePath + "/" + hash;
    }

    private static String getRequestHash(RestApi api) throws UnsupportedEncodingException {
        Map<String, String> params = api.getParameters();

        if (api.endpoint == null) {
            return null;
        }

        StringBuilder query = new StringBuilder(api.endpoint);

        for (String key : params.keySet()) {
            query.append(URLEncoder.encode(key, "utf-8")).append("=").append(URLEncoder.encode(params.get(key), "utf-8")).append("&");
        }

        // get rid of the last ampersand
        if (query.length() > 0) {
            query.substring(0, query.length() - 1);
        }

        try {
            return RestCache.SHA1(query.toString());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes(StandardCharsets.ISO_8859_1), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    public static class CachePolicy {
        public static final int IGNORE_CACHE = 0;
        public static final int CACHE_THEN_NETWORK = 1;
        public static final int NETWORK_ONLY = 2;
        public static final int CACHE_ELSE_NETWORK = 3;
        public static final int UPDATE_CACHE = 4;
    }
}
