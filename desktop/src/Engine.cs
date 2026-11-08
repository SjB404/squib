// The desktop's versions of the engine pieces that are Android-specific on the phone (Apps, Trace,
// Lines lookups), plus the hooks the generated SquibView calls (see desktop/port/patches.txt).

using System;
using System.IO;
using System.Reflection;

namespace Squib.Engine
{
    // App modes. On the phone these come from the app in front; on the desktop the host works
    // the mode out from the window in front (see Modes.cs) and hands SquibView an id "KIND|what".
    public static class Apps
    {
        public enum Kind { HOME, WATCH, MUSIC, SCROLL, CHAT, GAME, NAV, READ, HIDE }

        static readonly HashMap<string, Kind?> KNOWN = new HashMap<string, Kind?>();

        // Phone packages from squib.json ("apps", "minecraft") and Fun.defaults(). The Minecraft ones
        // matter on the desktop too: the host reports Minecraft as a Minecraft package.
        public static void put(string pkg, Kind kind) { KNOWN.put(pkg, kind); }

        public static Kind kindOf(object context, string id)
        {
            Kind? known = KNOWN.get(id);
            if (known != null) return known.Value;
            int bar = id.IndexOf('|');
            Kind k;
            if (bar > 0 && Enum.TryParse(id.Substring(0, bar), out k)) return k;
            return Kind.HOME;
        }
    }

    // The log, like the phone's Trace.java: what he noticed and decided, and why.
    // On Windows: %APPDATA%\Squib\squib-log.txt (the last ~1 MB; the one before is squib-log.old.txt).
    public static class Trace
    {
        public static string Path;
        static readonly System.Text.StringBuilder pending = new System.Text.StringBuilder();

        public static void t(string tag, string msg) { log("T", tag, msg); }
        public static void d(string tag, string msg) { log("D", tag, msg); }
        public static void i(string tag, string msg) { log("I", tag, msg); }
        public static void w(string tag, string msg) { log("W", tag, msg); }
        public static void e(string tag, string msg, Exception err) { log("E", tag, msg + (err == null ? "" : ": " + err)); }

        static void log(string level, string tag, string msg)
        {
            if (Path == null) return;
            lock (pending)
            {
                pending.Append(DateTime.Now.ToString("MM-dd HH:mm:ss.fff", System.Globalization.CultureInfo.InvariantCulture))
                       .Append(' ').Append(level).Append(' ').Append(tag).Append(": ").Append(msg).Append("\r\n");
                if (pending.Length > 64 * 1024) flush();
            }
        }

        public static void flush()
        {
            if (Path == null) return;
            lock (pending)
            {
                if (pending.Length == 0) return;
                try
                {
                    if (File.Exists(Path) && new FileInfo(Path).Length > 1024 * 1024)
                    {
                        string old = System.IO.Path.ChangeExtension(Path, ".old.txt");
                        if (File.Exists(old)) File.Delete(old);
                        File.Move(Path, old);
                    }
                    File.AppendAllText(Path, pending.ToString());
                }
                catch { }
                pending.Length = 0;
            }
        }
    }


    public static partial class Lines
    {
        /** A line group by name, or null if there's no such group. */
        public static string[] get(string name)
        {
            FieldInfo f = typeof(Lines).GetField(name, BindingFlags.Static | BindingFlags.Public);
            return f == null ? null : f.GetValue(null) as string[];
        }

        /** Replaces a group. Returns false if there's no such group. */
        public static bool set(string name, string[] lines)
        {
            FieldInfo f = typeof(Lines).GetField(name, BindingFlags.Static | BindingFlags.Public);
            if (f == null || f.FieldType != typeof(string[])) return false;
            f.SetValue(null, lines);
            Pick.reset();
            return true;
        }
    }

    public partial class Pick
    {
        // the Java's anonymous Comparator: lines said longest ago first
        sealed class ByRecent : Comparator<string>
        {
            public int compare(string a, string b) { return recent.lastIndexOf(a.hashCode()) - recent.lastIndexOf(b.hashCode()); }
        }
    }

    // The mouse, handed to the engine as touches in the order a phone sends them. WPF raises a
    // mouse move as soon as the window takes mouse capture, and it used to reach the engine before
    // the touch-down: with the last click's start point still stored, that looked like a long drag,
    // so he jumped across the screen and was left in DRAG (arms wiggling) with no drag to end it.
    public sealed class Touch
    {
        readonly SquibView v;
        bool down;
        public Touch(SquibView v) { this.v = v; }
        public bool IsDown { get { return down; } }

        public void Press(long time, float x, float y)
        {
            down = true;
            v.onHandleTouch(MotionEvent.obtain(time, MotionEvent.ACTION_DOWN, x, y));
        }

        // Moves only count between a press and its release.
        public void Move(long time, float x, float y)
        {
            if (down) v.onHandleTouch(MotionEvent.obtain(time, MotionEvent.ACTION_MOVE, x, y));
        }

        public void Release(long time, float x, float y, bool cancel)
        {
            if (!down) return;
            down = false;
            v.onHandleTouch(MotionEvent.obtain(time, cancel ? MotionEvent.ACTION_CANCEL : MotionEvent.ACTION_UP, x, y));
            Unstick();
        }

        // Nothing holds him, yet he's shown as carried: let him drop instead of hanging there.
        public void Unstick()
        {
            if (down || v.dragging || v.motion != SquibView.Motion.DRAG) return;
            Trace.w("touch", "in DRAG with nothing holding him: drops");
            v.setMotion(SquibView.Motion.FALL);
        }
    }

    public partial class SquibView
    {
        Context context;

        // what the Java's super(context) did
        void super_(Context c) { context = c; }
        public Context getContext() { return context; }
        public void invalidate() { }

        // ---- set by the desktop host ----

        /** Minecraft's hotbar while you play, in screen pixels: he stands on it instead of the taskbar. */
        public Rect hotbar;
        /** Where the cursor is, so his eyes can follow it (null: don't). */
        public float[] cursor;
        // The phone moves him to a spot for each app mode (a cloud in a top corner for chat, maps,
        // reading and games, the bottom-right corner for videos). On a computer the window in front
        // changes all the time, so that had him jumping around the screen every few seconds. Here he
        // stays where he is, on the taskbar (or Minecraft's hotbar), in every mode; the modes only
        // change what he does. A trick that needs a cloud (fishing) puts one just above him.
        bool deskPlace()
        {
            float edge = 8 * d;
            float left = edge - 2 * S, right = screenW - edge - 26 * S;
            float floor = screenH;
            perched = false;
            if (hotbar != null && forcePlace == 0)
            {
                setFloor(hotbar.top, hotbar.left - 2 * S, hotbar.right - 26 * S);
                return true;
            }
            if (forcePlace == 1)
            {
                perched = true;
                float x = clamp(posX, left + 12 * d, right - 12 * d);
                setFloor(floor - 40 * d, x - 12 * d, x + 12 * d);
            }
            else setFloor(floor, left, right);
            return true;
        }

        // The phone has no cursor, so he looks straight ahead; on the desktop he watches the cursor.
        bool deskLook()
        {
            if (cursor == null) return false;
            double dx = cursor[0] - (posX + 14 * S), dy = cursor[1] - (posY - 13 * S);
            lookX = dx < -40 * d ? -1 : dx > 40 * d ? 1 : 0;
            lookY = dy > 30 * d ? 1 : 0;
            return true;
        }
    }
}
