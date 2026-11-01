package android.graphics;
public class Bitmap {
    public enum Config { ARGB_8888 }
    public final int w, h; public final int[] px;
    Bitmap(int w, int h) { if (w <= 0 || h <= 0) throw new IllegalArgumentException("width and height must be > 0: " + w + "x" + h); this.w = w; this.h = h; px = new int[w * h]; }
    public static Bitmap createBitmap(int w, int h, Config c) { return new Bitmap(w, h); }
    public static Bitmap createBitmap(int[] colors, int w, int h, Config c) { Bitmap b = new Bitmap(w, h); System.arraycopy(colors, 0, b.px, 0, w * h); return b; }
    public int getWidth() { return w; } public int getHeight() { return h; }
    public void setPixels(int[] p, int off, int stride, int x, int y, int ww, int hh) { for (int j = 0; j < hh; j++) System.arraycopy(p, off + j * stride, px, (y + j) * w + x, ww); }
    public void eraseColor(int c) { java.util.Arrays.fill(px, c); }
    public int getPixel(int x, int y) { return px[y * w + x]; }
}
