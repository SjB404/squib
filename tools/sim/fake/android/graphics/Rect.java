package android.graphics;
public class Rect {
    public int left, top, right, bottom;
    public void set(int l, int t, int r, int b) { left = l; top = t; right = r; bottom = b; }
    public void set(Rect r) { set(r.left, r.top, r.right, r.bottom); }
    public int width() { return right - left; } public int height() { return bottom - top; }
}
