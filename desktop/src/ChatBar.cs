// Discord's message bar, so he can walk along the top of it while Discord is the window in front (like
// Minecraft's hotbar). Found through UI Automation, the accessibility API screen readers use: Discord
// (Chromium) switches its accessibility tree on the first time something asks, then reports the message
// box and the rounded bar around it with their real positions, whatever the theme, zoom or window size.
// It runs on its own thread, since asking another program can take a moment.

using System;
using System.Threading;
using System.Windows.Automation;
using SD = System.Drawing;
using E = Squib.Engine;

namespace Squib
{
    static class ChatBar
    {
        static readonly string[] programs = { "Discord", "DiscordPTB", "DiscordCanary" };
        static volatile object bar;      // SD.Rectangle in screen pixels, or null: none to stand on
        static volatile bool wanted;
        static Thread thread;

        /** The bar's rectangle (screen pixels) while Discord is in front with a message bar showing, else null. */
        public static SD.Rectangle? Bar { get { object b = bar; return b == null ? (SD.Rectangle?)null : (SD.Rectangle)b; } }

        /** Turns the watching on or off (the "walk on discord's message bar" setting). */
        public static void Watch(bool on)
        {
            wanted = on;
            if (!on) bar = null;
            if (on && thread == null)
            {
                thread = new Thread(Run) { IsBackground = true, Name = "chat bar" };
                thread.SetApartmentState(ApartmentState.MTA);
                thread.Start();
            }
        }

        public static bool IsDiscord(IntPtr h)
        {
            string name = Native.ProcessName(h);
            foreach (string p in programs) if (string.Equals(p, name, StringComparison.OrdinalIgnoreCase)) return true;
            return false;
        }

        static void Run()
        {
            IntPtr window = IntPtr.Zero;
            AutomationElement box = null;
            DateTime nextSearch = DateTime.MinValue;
            while (true)
            {
                Thread.Sleep(200);
                if (!wanted) { box = null; continue; }
                try
                {
                    IntPtr fg = Native.GetForegroundWindow();
                    if (fg == IntPtr.Zero || Native.IsIconic(fg) || !IsDiscord(fg)) { bar = null; continue; }
                    if (fg != window) { window = fg; box = null; nextSearch = DateTime.MinValue; }
                    SD.Rectangle r = SD.Rectangle.Empty;
                    if (box != null) r = Rect(box);
                    if (r.IsEmpty && DateTime.UtcNow >= nextSearch)
                    {
                        // not found yet, or gone (another channel, a different page): look again, at most once a second
                        nextSearch = DateTime.UtcNow.AddSeconds(1);
                        box = Find(fg);
                        if (box != null) r = Rect(box);
                        E.Trace.d("chatbar", box == null ? "no message bar in Discord" : "found Discord's message bar at " + r);
                    }
                    bar = r.IsEmpty ? null : (object)r;
                }
                catch (Exception e)
                {
                    // the element went away while we asked, or Discord closed: start over
                    box = null;
                    bar = null;
                    E.Trace.d("chatbar", "lost it: " + e.GetType().Name);
                }
            }
        }

        // `Squib.exe --find-chatbar` writes what he finds in the open Discord window (in front or not) to
        // %TEMP%\squib-chatbar.txt, for checking.
        public static string Report()
        {
            string result = "no discord window";
            foreach (string name in programs)
                foreach (var p in System.Diagnostics.Process.GetProcessesByName(name))
                {
                    IntPtr h = p.MainWindowHandle;
                    if (h == IntPtr.Zero) continue;
                    AutomationElement box = null;
                    for (int i = 0; i < 4 && box == null; i++) // the first ask switches Discord's accessibility on
                    {
                        box = Find(h);
                        if (box == null) Thread.Sleep(1000);
                    }
                    result = name + " window \"" + Native.Title(h) + "\": " + (box == null ? "no message bar found" : "message bar at " + Rect(box));
                }
            System.IO.File.WriteAllText(System.IO.Path.Combine(System.IO.Path.GetTempPath(), "squib-chatbar.txt"), result + "\r\n");
            return result;
        }

        static SD.Rectangle Rect(AutomationElement e)
        {
            System.Windows.Rect r = e.Current.BoundingRectangle;
            if (r.IsEmpty || r.Width < 40 || r.Height < 10 || e.Current.IsOffscreen) return SD.Rectangle.Empty;
            return new SD.Rectangle((int)Math.Round(r.X), (int)Math.Round(r.Y), (int)Math.Round(r.Width), (int)Math.Round(r.Height));
        }

        // The message box is an edit field in the chat column; the bar he stands on is its "channelTextArea"
        // parent (the rounded box with the + and emoji buttons). With more than one edit field (search, a
        // thread open beside the chat), the lowest one on screen is the message bar.
        static AutomationElement Find(IntPtr window)
        {
            AutomationElement root = AutomationElement.FromHandle(window);
            var edits = root.FindAll(TreeScope.Descendants, new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Edit));
            AutomationElement best = null;
            double bestBottom = double.MinValue;
            TreeWalker up = TreeWalker.RawViewWalker;
            foreach (AutomationElement edit in edits)
            {
                AutomationElement p = edit;
                for (int i = 0; i < 6 && p != null; i++, p = up.GetParent(p))
                {
                    if ((p.Current.ClassName ?? "").IndexOf("channelTextArea", StringComparison.Ordinal) < 0) continue;
                    System.Windows.Rect r = p.Current.BoundingRectangle;
                    if (!r.IsEmpty && r.Bottom > bestBottom) { bestBottom = r.Bottom; best = p; }
                    break;
                }
            }
            return best;
        }
    }
}
