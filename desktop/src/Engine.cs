// The desktop's versions of the engine pieces that are Android-specific on the phone (Apps, Prefs,
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
        /** Where the perch clouds go across the top, as fractions of the width (GAME on the left, the rest right). */
        public float perchLeft = 0.25f, perchRight = 0.75f;

        // The phone puts him by the nav bar and perches him in the top corners. On the desktop the
        // floor is the top of the taskbar (or Minecraft's hotbar), and the perches are further in
        // from the corners so he doesn't sit on windows' close buttons.
        bool deskPlace()
        {
            float edge = 8 * d;
            float left = edge - 2 * S, right = screenW - edge - 26 * S;
            float floor = screenH;
            float perchY = statusBar + 14 * d + 20 * S;
            perched = false;
            if (hotbar != null && forcePlace == 0)
            {
                setFloor(hotbar.top, hotbar.left - 2 * S, hotbar.right - 26 * S);
                return true;
            }
            int spot; // 0 floor, 1 perch right, 2 perch left, 3 bottom-right corner
            if (forcePlace != 0) spot = forcePlace == 1 ? 1 : 0;
            else
            {
                switch (kindNow())
                {
                    case Apps.Kind.WATCH: spot = 3; break;
                    case Apps.Kind.CHAT: case Apps.Kind.NAV: case Apps.Kind.READ: spot = 1; break;
                    case Apps.Kind.GAME: spot = 2; break;
                    default: spot = 0; break;
                }
            }
            if (spot == 1 || spot == 2)
            {
                perched = true;
                float mid = screenW * (spot == 1 ? perchRight : perchLeft) - 14 * S;
                setFloor(perchY, mid - 12 * d, mid + 12 * d);
            }
            else if (spot == 3) setFloor(floor, right - 12 * d, right);
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
