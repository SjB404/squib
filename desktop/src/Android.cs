// Stand-ins for the Android classes squib's engine draws and stores things with. A Canvas either
// paints into a Bitmap (speech bubbles, the menu) or records what's drawn on the screen into a
// Frame, which the desktop window then sizes itself around and paints.

using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;

namespace Squib.Engine
{
    public sealed class Bitmap
    {
        public enum Config { ARGB_8888 }
        public readonly int w, h;
        public readonly int[] px;

        Bitmap(int w, int h)
        {
            if (w <= 0 || h <= 0) throw new ArgumentException("width and height must be > 0: " + w + "x" + h);
            this.w = w; this.h = h; px = new int[w * h];
        }

        public static Bitmap createBitmap(int w, int h, Config c) { return new Bitmap(w, h); }
        public static Bitmap createBitmap(int[] colors, int w, int h, Config c) { var b = new Bitmap(w, h); Array.Copy(colors, b.px, w * h); return b; }
        public int getWidth() { return w; }
        public int getHeight() { return h; }
        public void setPixels(int[] p, int off, int stride, int x, int y, int ww, int hh) { for (int j = 0; j < hh; j++) Array.Copy(p, off + j * stride, px, (y + j) * w + x, ww); }
        public void eraseColor(int c) { for (int i = 0; i < px.Length; i++) px[i] = c; }
        public int getPixel(int x, int y) { return px[y * w + x]; }
        public void setPixel(int x, int y, int c) { px[y * w + x] = c; }
    }

    public sealed class Rect
    {
        public int left, top, right, bottom;
        public Rect() { }
        public Rect(int l, int t, int r, int b) { set(l, t, r, b); }
        public void set(int l, int t, int r, int b) { left = l; top = t; right = r; bottom = b; }
        public void set(Rect r) { set(r.left, r.top, r.right, r.bottom); }
        public int width() { return right - left; }
        public int height() { return bottom - top; }
        public bool contains(int x, int y) { return x >= left && x < right && y >= top && y < bottom; }
    }

    public sealed class Typeface
    {
        public static readonly Typeface DEFAULT = new Typeface(), MONOSPACE = new Typeface(), DEFAULT_BOLD = new Typeface();
        public const int NORMAL = 0, BOLD = 1;
    }

    public sealed class Paint
    {
        int color = unchecked((int)0xFF000000);
        float textSize = 12;
        public void setColor(int c) { color = c; }
        public int getColor() { return color; }
        public void setAlpha(int a) { color = (color & 0xFFFFFF) | ((a & 0xFF) << 24); }
        public int getAlpha() { return (int)((uint)color >> 24); }
        public void setTextSize(float s) { textSize = s; }
        public float getTextSize() { return textSize; }
        public float ascent() { return -0.928f * textSize; }
        public void setAntiAlias(bool b) { }
        public void setFilterBitmap(bool b) { }
        public void setSubpixelText(bool b) { }
        public void setDither(bool b) { }
        public Typeface setTypeface(Typeface t) { return t; }
    }

    // Everything drawn on the screen in one frame, in screen pixels.
    public sealed class Frame
    {
        struct Op { public int x0, y0, x1, y1, color; public Bitmap art; }
        readonly List<Op> ops = new List<Op>();
        public int Left, Top, Right, Bottom; // what was drawn, clipped to Clip
        public Rect Clip = new Rect(-100000, -100000, 100000, 100000);

        public void Clear() { ops.Clear(); Left = Top = int.MaxValue; Right = Bottom = int.MinValue; }
        public bool Empty { get { return Right <= Left || Bottom <= Top; } }

        internal void Add(int x0, int y0, int x1, int y1, int color, Bitmap art)
        {
            if (x1 <= x0 || y1 <= y0) return;
            if (x1 <= Clip.left || y1 <= Clip.top || x0 >= Clip.right || y0 >= Clip.bottom) return;
            ops.Add(new Op { x0 = x0, y0 = y0, x1 = x1, y1 = y1, color = color, art = art });
            Left = System.Math.Min(Left, System.Math.Max(x0, Clip.left));
            Top = System.Math.Min(Top, System.Math.Max(y0, Clip.top));
            Right = System.Math.Max(Right, System.Math.Min(x1, Clip.right));
            Bottom = System.Math.Max(Bottom, System.Math.Min(y1, Clip.bottom));
        }

        // Paints the frame into buf, a w x h window whose top-left is at (ox, oy) on the screen.
        public void Render(int[] buf, int ox, int oy, int w, int h)
        {
            Array.Clear(buf, 0, buf.Length);
            foreach (Op o in ops)
            {
                int x0 = System.Math.Max(o.x0, ox), y0 = System.Math.Max(o.y0, oy), x1 = System.Math.Min(o.x1, ox + w), y1 = System.Math.Min(o.y1, oy + h);
                if (x1 <= x0 || y1 <= y0) continue;
                if (o.art == null)
                {
                    int a = (int)((uint)o.color >> 24), c = o.color | unchecked((int)0xFF000000);
                    for (int y = y0; y < y1; y++)
                        for (int x = x0; x < x1; x++) Pixels.Blend(buf, (y - oy) * w + (x - ox), c, a);
                }
                else
                {
                    Bitmap b = o.art;
                    int dw = o.x1 - o.x0, dh = o.y1 - o.y0, a = o.color;
                    for (int y = y0; y < y1; y++)
                    {
                        int sy = (y - o.y0) * b.h / dh;
                        for (int x = x0; x < x1; x++) Pixels.Blend(buf, (y - oy) * w + (x - ox), b.px[sy * b.w + (x - o.x0) * b.w / dw], a);
                    }
                }
            }
        }
    }

    static class Pixels
    {
        // Source-over, straight (not premultiplied) alpha, like the phone.
        public static void Blend(int[] buf, int i, int c, int alpha)
        {
            int a = (int)((uint)c >> 24) * alpha / 255;
            if (a == 0) return;
            if (a == 255) { buf[i] = c; return; }
            int d = buf[i], da = (int)((uint)d >> 24);
            int oa = a + da * (255 - a) / 255;
            if (oa == 0) return;
            int r = (((c >> 16) & 255) * a + ((d >> 16) & 255) * da * (255 - a) / 255) / oa;
            int g = (((c >> 8) & 255) * a + ((d >> 8) & 255) * da * (255 - a) / 255) / oa;
            int bl = ((c & 255) * a + (d & 255) * da * (255 - a) / 255) / oa;
            buf[i] = (oa << 24) | (r << 16) | (g << 8) | bl;
        }
    }

    public sealed class Canvas
    {
        readonly Bitmap b;
        readonly Frame f;
        public Canvas(Bitmap b) { this.b = b; }
        public Canvas(Frame f) { this.f = f; }

        public void drawRect(float l, float t, float r, float bo, Paint p)
        {
            int x0 = Math.round(l), y0 = Math.round(t), x1 = Math.round(r), y1 = Math.round(bo);
            int color = p.getColor();
            if (f != null) { f.Add(x0, y0, x1, y1, color, null); return; }
            int a = (int)((uint)color >> 24), c = color | unchecked((int)0xFF000000);
            x0 = System.Math.Max(0, x0); y0 = System.Math.Max(0, y0); x1 = System.Math.Min(b.w, x1); y1 = System.Math.Min(b.h, y1);
            for (int y = y0; y < y1; y++)
                for (int x = x0; x < x1; x++) Pixels.Blend(b.px, y * b.w + x, c, a);
        }

        public void drawBitmap(Bitmap src, Rect s, Rect dst, Paint p)
        {
            if (s != null) throw new NotSupportedException("src rect");
            int alpha = p == null ? 255 : p.getAlpha();
            if (f != null) { f.Add(dst.left, dst.top, dst.right, dst.bottom, alpha, src); return; }
            int dw = dst.width(), dh = dst.height();
            if (dw <= 0 || dh <= 0) return;
            for (int y = System.Math.Max(0, dst.top); y < System.Math.Min(b.h, dst.bottom); y++)
            {
                int sy = (y - dst.top) * src.h / dh;
                for (int x = System.Math.Max(0, dst.left); x < System.Math.Min(b.w, dst.right); x++)
                    Pixels.Blend(b.px, y * b.w + x, src.px[sy * src.w + (x - dst.left) * src.w / dw], alpha);
            }
        }
    }

    public sealed class MotionEvent
    {
        public int action; public float x, y; public long time;
        public const int ACTION_DOWN = 0, ACTION_UP = 1, ACTION_MOVE = 2, ACTION_CANCEL = 3;
        public static MotionEvent obtain(long time, int action, float x, float y) { return new MotionEvent { time = time, action = action, x = x, y = y }; }
        public float getRawX() { return x; }
        public float getRawY() { return y; }
        public int getActionMasked() { return action; }
        public long getEventTime() { return time; }
    }

    public static class SystemClock
    {
        static readonly System.Diagnostics.Stopwatch clock = System.Diagnostics.Stopwatch.StartNew();
        // For simulations: when set, time only moves when Fake does.
        public static long? Fake;
        public static long elapsedRealtime() { return Fake ?? clock.ElapsedMilliseconds; }
    }

    public sealed class DisplayMetrics { public float density = 1; }

    public sealed class Resources
    {
        public readonly DisplayMetrics metrics = new DisplayMetrics();
        public DisplayMetrics getDisplayMetrics() { return metrics; }
    }

    public sealed class Context
    {
        readonly Resources res = new Resources();
        public Context(float density) { res.metrics.density = density; }
        public Resources getResources() { return res; }
        public Context getApplicationContext() { return this; }
        // Prefs.java asks for its prefs by name; on the desktop there's just the one file.
        public static SharedPreferences Shared;
        public SharedPreferences getSharedPreferences(string name, int mode) { return Shared; }
    }

    // Saved settings and stats, in a text file (one key=type:value per line).
    public sealed class SharedPreferences
    {
        readonly Dictionary<string, object> m = new Dictionary<string, object>();
        readonly string path;
        public bool Dirty;

        public SharedPreferences(string path)
        {
            this.path = path;
            if (path == null || !File.Exists(path)) return;
            try
            {
                foreach (string line in File.ReadAllLines(path, Encoding.UTF8))
                {
                    int eq = line.IndexOf('='), colon = eq < 0 ? -1 : line.IndexOf(':', eq);
                    if (eq < 0 || colon < 0) continue;
                    string k = line.Substring(0, eq), t = line.Substring(eq + 1, colon - eq - 1), v = line.Substring(colon + 1);
                    var ci = CultureInfo.InvariantCulture;
                    switch (t)
                    {
                        case "i": m[k] = int.Parse(v, ci); break;
                        case "l": m[k] = long.Parse(v, ci); break;
                        case "f": m[k] = float.Parse(v, ci); break;
                        case "b": m[k] = v == "1"; break;
                        case "s": m[k] = v.Replace("\\n", "\n").Replace("\\\\", "\\"); break;
                    }
                }
            }
            catch { }
        }

        public bool contains(string k) { return m.ContainsKey(k); }
        public HashMap<string, object> getAll() { var all = new HashMap<string, object>(); foreach (var kv in m) all[kv.Key] = kv.Value; return all; }
        public int getInt(string k, int d) { object o; return m.TryGetValue(k, out o) && o is int ? (int)o : d; }
        public long getLong(string k, long d) { object o; return m.TryGetValue(k, out o) ? (o is long ? (long)o : o is int ? (int)o : d) : d; }
        public float getFloat(string k, float d) { object o; return m.TryGetValue(k, out o) && o is float ? (float)o : d; }
        public bool getBoolean(string k, bool d) { object o; return m.TryGetValue(k, out o) && o is bool ? (bool)o : d; }
        public string getString(string k, string d) { object o; return m.TryGetValue(k, out o) && o is string ? (string)o : d; }
        public Editor edit() { return new Editor(this); }

        public void Save()
        {
            if (!Dirty || path == null) return;
            Dirty = false;
            try
            {
                var sb = new System.Text.StringBuilder();
                var keys = new List<string>(m.Keys);
                keys.Sort(StringComparer.Ordinal);
                foreach (string k in keys)
                {
                    object v = m[k];
                    string t = v is int ? "i" : v is long ? "l" : v is float ? "f" : v is bool ? "b" : "s";
                    string s = v is bool ? ((bool)v ? "1" : "0") : v is string ? ((string)v).Replace("\\", "\\\\").Replace("\n", "\\n") : Convert.ToString(v, CultureInfo.InvariantCulture);
                    sb.Append(k).Append('=').Append(t).Append(':').Append(s).Append("\r\n");
                }
                Directory.CreateDirectory(Path.GetDirectoryName(path));
                File.WriteAllText(path + ".tmp", sb.ToString(), new UTF8Encoding(false));
                if (File.Exists(path)) File.Replace(path + ".tmp", path, null); else File.Move(path + ".tmp", path);
            }
            catch { Dirty = true; }
        }

        public sealed class Editor
        {
            readonly SharedPreferences p;
            internal Editor(SharedPreferences p) { this.p = p; }
            public Editor putInt(string k, int v) { p.m[k] = v; return this; }
            public Editor putLong(string k, long v) { p.m[k] = v; return this; }
            public Editor putFloat(string k, float v) { p.m[k] = v; return this; }
            public Editor putBoolean(string k, bool v) { p.m[k] = v; return this; }
            public Editor putString(string k, string v) { p.m[k] = v; return this; }
            public Editor remove(string k) { p.m.Remove(k); return this; }
            public void apply() { p.Dirty = true; }
            public bool commit() { p.Dirty = true; return true; }
        }
    }
}
