package android.graphics;
public class Canvas {
    final Bitmap b;
    public Canvas(Bitmap b) { this.b = b; }
    void blend(int x, int y, int c, int alpha) {
        if (x < 0 || y < 0 || x >= b.w || y >= b.h) return;
        int a = ((c >>> 24) * alpha) / 255; if (a == 0) return;
        int d = b.px[y * b.w + x]; int da = d >>> 24;
        int oa = a + da * (255 - a) / 255; if (oa == 0) return;
        int r = (((c >> 16) & 255) * a + ((d >> 16) & 255) * da * (255 - a) / 255) / oa;
        int g = (((c >> 8) & 255) * a + ((d >> 8) & 255) * da * (255 - a) / 255) / oa;
        int bl = ((c & 255) * a + (d & 255) * da * (255 - a) / 255) / oa;
        b.px[y * b.w + x] = (oa << 24) | (r << 16) | (g << 8) | bl;
    }
    public void drawRect(float l, float t, float r, float bo, Paint p) {
        int x0 = Math.round(l), y0 = Math.round(t), x1 = Math.round(r), y1 = Math.round(bo);
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) blend(x, y, p.color | 0xFF000000, p.color >>> 24);
    }
    public void drawBitmap(Bitmap src, Rect s, Rect dst, Paint p) {
        if (s != null) throw new RuntimeException("src rect not supported");
        int dw = dst.width(), dh = dst.height(); if (dw <= 0 || dh <= 0) return;
        int alpha = p == null ? 255 : p.getAlpha();
        for (int y = 0; y < dh; y++) { int sy = y * src.h / dh; for (int x = 0; x < dw; x++) { int sx = x * src.w / dw; blend(dst.left + x, dst.top + y, src.px[sy * src.w + sx], alpha); } }
    }
}
