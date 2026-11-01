package android.content.pm;
import android.content.Intent;
public class PackageManager {
    public static class NameNotFoundException extends Exception {}
    public ApplicationInfo getApplicationInfo(String p, int f) throws NameNotFoundException { throw new NameNotFoundException(); }
    public ResolveInfo resolveActivity(Intent i, int f) { return null; }
}
