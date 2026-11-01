package android.content;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import java.util.HashMap;
public class Context {
    public static float density = 2.75f;
    static final HashMap<String, SharedPreferences> prefs = new HashMap<>();
    final Resources res = new Resources();
    public Resources getResources() { return res; }
    public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferences p = prefs.get(name);
        if (p == null) { p = new SharedPreferences(); prefs.put(name, p); }
        return p;
    }
    public Context getApplicationContext() { return this; }
    public PackageManager getPackageManager() { return new PackageManager(); }
    public String getPackageName() { return "com.squib.buddy"; }
}
