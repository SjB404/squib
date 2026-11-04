package com.squib.buddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Downloads squib.json from GitHub on start and every 6 hours, keeps a copy on the phone,
 * and applies it: line groups replace the built-in ones in {@link Lines}, "settings" sets
 * the matching static double in {@link Fun}, "apps" and "minecraft" add app packages.
 */
final class Remote {
    static final long EVERY_MS = 6L * 60 * 60 * 1000;
    static final int MAX_BYTES = 32 * 1024 * 1024;
    static final String URL_STR = "https://raw.githubusercontent.com/SjB404/squib/main/squib.json";

    static boolean cacheLoaded;
    static volatile boolean fetching;
    static final Handler main = new Handler(Looper.getMainLooper());
    static boolean started;
    static TextView status;

    private Remote() {
    }

    static void start(Context context) {
        final Context app = context.getApplicationContext();
        if (!started) {
            Fun.defaults();
        }
        loadCache(app);
        if (!started) {
            started = true;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        fetch(app);
                        try {
                            Thread.sleep(EVERY_MS);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            }, "squib-lines");
            t.setDaemon(true);
            t.start();
        }
    }

    static File cache(Context context) {
        return new File(context.getFilesDir(), "squib.json");
    }

    static void loadCache(Context context) {
        if (cacheLoaded) {
            return;
        }
        cacheLoaded = true;
        try {
            File f = cache(context);
            if (f.exists()) {
                apply(new String(readAll(new FileInputStream(f)), "UTF-8"));
            }
        } catch (Exception ignored) {
        }
    }

    static void fetch(Context context) {
        if (fetching) {
            return;
        }
        fetching = true;
        final SharedPreferences prefs = Prefs.get(context);
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(URL_STR).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setUseCaches(false);
            c.setRequestProperty("Cache-Control", "no-cache");
            int code = c.getResponseCode();
            if (code != 200) {
                prefs.edit().putString("remoteErr", "couldn't download (" + code + ")").apply();
                return;
            }
            final String json = new String(readAll(c.getInputStream()), "UTF-8");
            new JSONObject(json); // only keep it if it parses
            File tmp = new File(context.getFilesDir(), "squib.json.tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            out.write(json.getBytes("UTF-8"));
            out.close();
            tmp.renameTo(cache(context));
            main.post(new Runnable() {
                @Override
                public void run() {
                    prefs.edit().putLong("remoteAt", System.currentTimeMillis()).putInt("remoteCount", apply(json)).remove("remoteErr").apply();
                }
            });
        } catch (Exception e) {
            prefs.edit().putString("remoteErr", "couldn't download (" + e.getClass().getSimpleName() + ")").apply();
        } finally {
            fetching = false;
        }
    }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        try {
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BYTES) {
                    throw new Exception("too big");
                }
            }
        } finally {
            in.close();
        }
        return out.toByteArray();
    }

    /** Applies a squib.json document. Returns how many lines were loaded. */
    static int apply(String json) {
        int count = 0;
        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (Exception e) {
            return 0;
        }
        JSONObject lines = root.optJSONObject("lines");
        if (lines != null) {
            Iterator<String> keys = lines.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = lines.optJSONArray(key);
                if (arr == null) {
                    continue;
                }
                ArrayList<String> list = new ArrayList<String>();
                for (int i = 0; i < arr.length(); i++) {
                    String s = arr.optString(i, "").trim();
                    if (!s.isEmpty()) {
                        list.add(s);
                    }
                }
                if (!list.isEmpty() && Lines.set(key, list.toArray(new String[0]))) {
                    count += list.size();
                }
            }
        }
        JSONObject settings = root.optJSONObject("settings");
        if (settings != null) {
            Iterator<String> keys = settings.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.startsWith("bedtime")) {
                    continue; // bedtime is set on the phone now (older versions still read it from the file)
                }
                double v = settings.optDouble(key, Double.NaN);
                Field f = field(Fun.class, key);
                if (f != null && f.getType() == double.class && !Double.isNaN(v) && v >= 0) {
                    try {
                        f.setAccessible(true);
                        f.setDouble(null, v);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        JSONArray mc = root.optJSONArray("minecraft");
        if (mc != null) {
            for (int i = 0; i < mc.length(); i++) {
                String pkg = mc.optString(i, "").trim();
                if (!pkg.isEmpty()) {
                    Fun.MC.add(pkg);
                    Apps.put(pkg, Apps.Kind.GAME);
                }
            }
        }
        JSONObject apps = root.optJSONObject("apps");
        if (apps != null) {
            Iterator<String> keys = apps.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = apps.optJSONArray(key);
                Apps.Kind kind;
                try {
                    kind = Apps.Kind.valueOf(key);
                } catch (Exception e) {
                    continue;
                }
                if (arr == null) {
                    continue;
                }
                for (int i = 0; i < arr.length(); i++) {
                    String pkg = arr.optString(i, "").trim();
                    if (!pkg.isEmpty()) {
                        Apps.put(pkg, kind);
                    }
                }
            }
        }
        return count;
    }

    static Field field(Class<?> c, String name) {
        try {
            return c.getDeclaredField(name);
        } catch (Exception e) {
            return null;
        }
    }

    static void addRow(final MainActivity a, LinearLayout root) {
        status = a.label("", 14, MainActivity.CREAM, false);
        a.row(root, status, a.button("check now", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("checking...");
                final Context app = a.getApplicationContext();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        fetch(app);
                        main.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                refreshRow(a);
                            }
                        }, 300);
                    }
                }).start();
            }
        }));
    }

    static void refreshRow(MainActivity a) {
        if (status == null) {
            return;
        }
        SharedPreferences prefs = Prefs.get(a);
        long at = prefs.getLong("remoteAt", 0);
        String err = prefs.getString("remoteErr", null);
        String text;
        if (at == 0) {
            text = "voice lines: built-in" + (err != null ? ", " + err : "");
        } else {
            text = "voice lines: " + prefs.getInt("remoteCount", 0) + " from github, updated "
                    + new SimpleDateFormat("d MMM HH:mm", Locale.US).format(new Date(at)).toLowerCase(Locale.US)
                    + (err != null ? " (last check: " + err + ")" : "");
        }
        status.setText(text);
    }
}
