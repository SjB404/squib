// Squib for Windows: the same squid as the phone app, living on top of your taskbar.
//
// Everything he does comes from the phone app's code (app/src/main/java), turned into C# by
// desktop/port when you build. This folder only has the desktop parts: the see-through window,
// mouse, his menu, the tray icon, app modes for Windows, and Minecraft's hotbar.
// Build with desktop\build.cmd (uses the C# compiler that ships with Windows).

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Windows;
using SD = System.Drawing;

namespace Squib
{
    static class Program
    {
        public const string MutexName = "Squib.DesktopBuddy", WakeName = "Squib.DesktopBuddy.Wake", QuitName = "Squib.DesktopBuddy.Quit";

        [STAThread]
        static int Main(string[] args)
        {
            if (Array.IndexOf(args, "--find-hotbar") >= 0) { GameWatch.Report(); return 0; }
            if (Array.IndexOf(args, "--find-chatbar") >= 0) { ChatBar.Report(); return 0; }
            if (args.Length > 0 && args[0] == "--sim") { Native.AttachConsole(-1); return Sim.Run(args); }
            bool fresh;
            var mutex = new System.Threading.Mutex(true, MutexName, out fresh);
            bool quit = Array.IndexOf(args, "--quit") >= 0;
            if (!fresh)
            {
                // already running: opening Squib again brings him back from a pause; --quit closes him (saving his stats)
                try { System.Threading.EventWaitHandle.OpenExisting(quit ? QuitName : WakeName).Set(); } catch { }
                return 0;
            }
            if (quit) return 0;
            var app = new Application();
            app.ShutdownMode = ShutdownMode.OnMainWindowClose;
            // if he ever crashes, say why in %APPDATA%\Squib\crash.txt
            app.DispatcherUnhandledException += (s, e) => Crashed(e.Exception);
            AppDomain.CurrentDomain.UnhandledException += (s, e) => Crashed(e.ExceptionObject as Exception);
            app.Run(new Host(args));
            GC.KeepAlive(mutex);
            return 0;
        }

        static void Crashed(Exception e)
        {
            try
            {
                Directory.CreateDirectory(DataDir);
                File.AppendAllText(Path.Combine(DataDir, "crash.txt"), DateTime.Now.ToString("s") + " " + Engine.Version.Name + "\r\n" + e + "\r\n\r\n");
            }
            catch { }
        }

        public static string DataDir
        {
            get { return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Squib"); }
        }

        // Starting with Windows is the usual per-user Run entry, so it also shows in Task Manager's startup apps.
        const string RunKey = @"Software\Microsoft\Windows\CurrentVersion\Run";

        public static bool StartsWithWindows()
        {
            using (var key = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(RunKey))
                return key != null && key.GetValue("Squib") != null;
        }

        public static void SetStartsWithWindows(bool on)
        {
            using (var key = Microsoft.Win32.Registry.CurrentUser.CreateSubKey(RunKey))
            {
                if (on) key.SetValue("Squib", "\"" + Process.GetCurrentProcess().MainModule.FileName + "\"");
                else key.DeleteValue("Squib", false);
            }
        }
    }

    static class Native
    {
        [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
        [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
        [StructLayout(LayoutKind.Sequential)] public struct LASTINPUTINFO { public uint cbSize; public uint dwTime; }
        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        public struct MONITORINFO { public int cbSize; public RECT rcMonitor, rcWork; public uint dwFlags; }

        [DllImport("kernel32.dll")] public static extern bool AttachConsole(int pid);
        [DllImport("user32.dll")] static extern short GetAsyncKeyState(int key);
        public static bool KeyDown(int key) { return (GetAsyncKeyState(key) & 0x8000) != 0; }
        [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
        [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, System.Text.StringBuilder s, int n);
        [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
        [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr h, ref POINT p);
        [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
        [DllImport("user32.dll")] public static extern bool IsWindow(IntPtr h);
        [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetClassName(IntPtr h, System.Text.StringBuilder s, int n);
        [DllImport("user32.dll")] public static extern IntPtr MonitorFromWindow(IntPtr h, uint flags);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern bool GetMonitorInfo(IntPtr m, ref MONITORINFO mi);
        [DllImport("user32.dll")] public static extern bool GetCursorPos(out POINT p);
        [DllImport("user32.dll")] public static extern bool GetLastInputInfo(ref LASTINPUTINFO lii);
        [DllImport("user32.dll")] public static extern int GetWindowLong(IntPtr h, int index);
        [DllImport("user32.dll")] public static extern int SetWindowLong(IntPtr h, int index, int value);
        [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);

        public const int GWL_EXSTYLE = -20, WS_EX_TOOLWINDOW = 0x80, WS_EX_TRANSPARENT = 0x20, WS_EX_NOACTIVATE = 0x08000000;
        public const uint SWP_NOSIZE = 0x1, SWP_NOMOVE = 0x2, SWP_NOACTIVATE = 0x10, SWP_NOZORDER = 0x4;
        public static readonly IntPtr HWND_TOPMOST = new IntPtr(-1);

        public static double IdleSeconds()
        {
            var lii = new LASTINPUTINFO();
            lii.cbSize = (uint)Marshal.SizeOf(lii);
            if (!GetLastInputInfo(ref lii)) return 0;
            return unchecked((uint)Environment.TickCount - lii.dwTime) / 1000.0;
        }

        static readonly Dictionary<uint, string> processNames = new Dictionary<uint, string>();

        public static string ProcessName(IntPtr h)
        {
            uint pid;
            GetWindowThreadProcessId(h, out pid);
            string name;
            if (!processNames.TryGetValue(pid, out name))
            {
                try { name = Process.GetProcessById((int)pid).ProcessName; } catch { name = ""; }
                if (processNames.Count > 500) processNames.Clear();
                processNames[pid] = name;
            }
            return name;
        }

        public static string Title(IntPtr h)
        {
            var s = new System.Text.StringBuilder(512);
            GetWindowText(h, s, 512);
            return s.ToString();
        }

        public static string ClassName(IntPtr h)
        {
            var s = new System.Text.StringBuilder(64);
            GetClassName(h, s, 64);
            return s.ToString();
        }
    }

    // Finds Minecraft and works out where its hotbar is. Minecraft always draws the hotbar 22 GUI pixels
    // tall, centred at the bottom of the game, so its top edge is the window's bottom minus 22 x GUI scale.
    // The GUI scale comes from the game's options.txt (0 there means "auto"). No screen reading needed.
    static class GameWatch
    {
        static readonly string[] gameProcesses = { "javaw", "java", "Minecraft.Windows" };
        static readonly string[] desktopClasses = { "Progman", "WorkerW", "Shell_TrayWnd", "Shell_SecondaryTrayWnd" };
        static readonly Dictionary<uint, string> optionsFiles = new Dictionary<uint, string>();
        static IntPtr game;
        static double nextSearch;
        static string optionsPath;
        static DateTime optionsWritten;
        static int guiScaleSetting;

        // The Minecraft window, if one is open and not minimised.
        public static IntPtr Find(double now)
        {
            IntPtr fg = Native.GetForegroundWindow();
            if (IsMinecraft(fg)) game = fg;
            else if (game != IntPtr.Zero && !IsMinecraft(game)) game = IntPtr.Zero;
            if (game == IntPtr.Zero && now >= nextSearch)
            {
                nextSearch = now + 3; // looking through processes isn't free, so not every time
                foreach (string name in gameProcesses)
                    foreach (var p in Process.GetProcessesByName(name))
                        if (IsMinecraft(p.MainWindowHandle)) game = p.MainWindowHandle;
            }
            return game != IntPtr.Zero && !Native.IsIconic(game) ? game : IntPtr.Zero;
        }

        public static bool IsMinecraft(IntPtr h)
        {
            if (h == IntPtr.Zero || !Native.IsWindow(h)) return false;
            if (Native.Title(h).IndexOf("Minecraft", StringComparison.OrdinalIgnoreCase) < 0) return false;
            string name = Native.ProcessName(h);
            foreach (string g in gameProcesses) if (string.Equals(g, name, StringComparison.OrdinalIgnoreCase)) return true;
            return false;
        }

        // The hotbar's rectangle in screen pixels, using Minecraft's own layout maths.
        public static SD.Rectangle Hotbar(IntPtr h)
        {
            Native.RECT r;
            var p = new Native.POINT();
            Native.GetClientRect(h, out r);
            Native.ClientToScreen(h, ref p);
            int w = r.Right, ht = r.Bottom;
            int scale = GuiScale(h, w, ht);
            int guiW = (w + scale - 1) / scale, guiH = (ht + scale - 1) / scale;
            return new SD.Rectangle(p.X + (guiW / 2 - 91) * scale, p.Y + (guiH - 22) * scale, 182 * scale, 22 * scale);
        }

        // Same rule as Minecraft: the biggest scale that keeps at least 320x240 GUI pixels, capped by the setting.
        static int GuiScale(IntPtr h, int w, int ht)
        {
            int setting = GuiScaleSetting(h), scale = 1;
            while (scale != setting && scale < w && scale < ht && w / (scale + 1) >= 320 && ht / (scale + 1) >= 240) scale++;
            return scale;
        }

        static int GuiScaleSetting(IntPtr h)
        {
            uint pid;
            Native.GetWindowThreadProcessId(h, out pid);
            string path;
            if (!optionsFiles.TryGetValue(pid, out path)) optionsFiles[pid] = path = FindOptionsFile(pid);
            try
            {
                if (path == null || !File.Exists(path)) return 0;
                DateTime written = File.GetLastWriteTimeUtc(path);
                if (path != optionsPath || written != optionsWritten)
                {
                    optionsPath = path;
                    optionsWritten = written;
                    guiScaleSetting = 0;
                    foreach (string line in File.ReadAllLines(path))
                        if (line.StartsWith("guiScale:")) int.TryParse(line.Substring(9), out guiScaleSetting);
                }
                return guiScaleSetting;
            }
            catch { return 0; }
        }

        // options.txt lives in the folder the game was started with (--gameDir), or the default .minecraft.
        static string FindOptionsFile(uint pid)
        {
            string dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
            try
            {
                using (var search = new System.Management.ManagementObjectSearcher("SELECT CommandLine FROM Win32_Process WHERE ProcessId = " + pid))
                    foreach (System.Management.ManagementObject process in search.Get())
                    {
                        var m = System.Text.RegularExpressions.Regex.Match((string)process["CommandLine"] ?? "", "--gameDir\\s+(?:\"([^\"]+)\"|(\\S+))");
                        if (m.Success) dir = m.Groups[1].Success ? m.Groups[1].Value : m.Groups[2].Value;
                    }
            }
            catch { }
            return Path.Combine(dir, "options.txt");
        }

        // Whether another window is in front of the hotbar (one on another monitor doesn't count).
        public static bool Covered(IntPtr h, SD.Rectangle hotbar)
        {
            IntPtr fg = Native.GetForegroundWindow();
            if (fg == IntPtr.Zero || fg == h) return false;
            uint pid;
            Native.GetWindowThreadProcessId(fg, out pid);
            if (pid == (uint)Process.GetCurrentProcess().Id) return false;
            if (Array.IndexOf(desktopClasses, Native.ClassName(fg)) >= 0) return false;
            Native.RECT r;
            Native.GetWindowRect(fg, out r);
            return SD.Rectangle.FromLTRB(r.Left, r.Top, r.Right, r.Bottom).IntersectsWith(hotbar);
        }

        // `Squib.exe --find-hotbar` writes what it works out to %TEMP%\squib-hotbar.txt, for checking.
        public static void Report()
        {
            IntPtr h = Find(0);
            string result = "no minecraft window";
            if (h != IntPtr.Zero)
            {
                int setting = GuiScaleSetting(h);
                result = "options " + optionsPath + "  guiScale:" + setting + "\r\nhotbar " + Hotbar(h);
            }
            File.WriteAllText(Path.Combine(Path.GetTempPath(), "squib-hotbar.txt"), result);
        }
    }
}
