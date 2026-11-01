package android.content.res;
import android.util.DisplayMetrics;
public class Resources {
    public DisplayMetrics getDisplayMetrics() { DisplayMetrics m = new DisplayMetrics(); m.density = android.content.Context.density; return m; }
}
