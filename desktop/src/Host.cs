// The desktop window around squib's engine. Each frame: the engine steps and draws into a Frame,
// then this see-through, click-through-where-empty window moves and sizes itself around whatever
// was drawn and shows it. It also does the desktop-only parts: the mouse (click, drag and throw,
// wiggle to pet), his menu and tray icon, app modes from the window in front, Minecraft's hotbar,
// napping while you're away, the focus timer, pause, and saving his stats.

using System;
using System.Collections.Generic;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using E = Squib.Engine;

namespace Squib
{
    sealed class Host : Window
    {
        readonly E.SharedPreferences prefs;
        E.SquibView v;
        readonly E.Frame frame = new E.Frame();
        readonly E.Canvas canvas;
        readonly Image img = new Image();
        WriteableBitmap wb;
        int[] buf;
        int winX, winY, winW, winH;           // the window, in screen pixels
        int areaX, areaY, areaW, areaH;       // the main screen's work area (above the taskbar), screen pixels
        double scale = 1;                     // screen pixels per WPF unit
        IntPtr hwnd;
        DispatcherTimer timer;
        readonly Menu menu = new Menu();
        Tray tray;
        readonly System.Threading.EventWaitHandle wake, quit;
        readonly E.Random rng = new E.Random();
        readonly bool demo;

        double now, last, floorCheckAt, modeCheckAt, saveAt, topmostAt, nextQuipAt = 20;
        bool paused, leaving, away, touching, wigglePet, menuMouseWasDown, shown;
        DateTime pauseUntil;
        double leaveAt, awayFor, petEnergy, cursorNearFor, focusLeft;
        float curX, curY, lastCurX, lastCurY;
        string pillText; E.Bitmap pill;

        public Host(string[] args)
        {
            demo = Array.IndexOf(args, "--demo") >= 0;
            Title = "Squib";
            WindowStyle = WindowStyle.None;
            AllowsTransparency = true;
            Background = Brushes.Transparent;
            ResizeMode = ResizeMode.NoResize;
            ShowInTaskbar = false;
            ShowActivated = false;
            Topmost = true;
            Width = Height = 1;
            UseLayoutRounding = true;
            RenderOptions.SetBitmapScalingMode(img, BitmapScalingMode.NearestNeighbor);
            img.Stretch = Stretch.Fill;
            Content = img;
            canvas = new E.Canvas(frame);

            string path = Path.Combine(Program.DataDir, "prefs.txt");
            bool existed = File.Exists(path);
            prefs = new E.SharedPreferences(path);
            if (!existed) Migrate();
            E.Context.Shared = prefs;
            E.Trace.Path = Path.Combine(Program.DataDir, "squib-log.txt");
            E.Trace.i("start", "squib " + E.Version.Name + " for windows");
            wake = new System.Threading.EventWaitHandle(false, System.Threading.EventResetMode.AutoReset, Program.WakeName);
            quit = new System.Threading.EventWaitHandle(false, System.Threading.EventResetMode.AutoReset, Program.QuitName);

            SourceInitialized += (s, e) =>
            {
                // keep him out of Alt+Tab and stop clicks from stealing focus
                hwnd = new WindowInteropHelper(this).Handle;
                SetClickThrough(prefs.getBoolean("clickThrough", false));
                scale = PresentationSource.FromVisual(this).CompositionTarget.TransformToDevice.M11;
                Remote.Start(Dispatcher);
                MeasureScreen();
                v = new E.SquibView(new E.Context((float)scale));
                v.onScreen(areaW, areaH, 0, 0);
                tray = new Tray(this);
                last = Clock();
                timer = new DispatcherTimer(DispatcherPriority.Render) { Interval = TimeSpan.FromMilliseconds(33) };
                timer.Tick += (s2, e2) => Tick();
                timer.Start();
            };
            SystemParameters.StaticPropertyChanged += (s, e) =>
            {
                if (e.PropertyName == "WorkArea" && v != null) { MeasureScreen(); v.onScreen(areaW, areaH, 0, 0); }
            };
            Closing += (s, e) => { prefs.Save(); E.Trace.i("start", "closing"); E.Trace.flush(); if (tray != null) tray.Dispose(); };
            // if Squib starts with Windows, keep that pointing at wherever Squib.exe is now
            try { if (Program.StartsWithWindows()) Program.SetStartsWithWindows(true); } catch { }
        }

        static double Clock() { return E.SystemClock.elapsedRealtime() / 1000.0; }

        static void GetCursor(out float x, out float y) { Native.POINT p; Native.GetCursorPos(out p); x = p.X; y = p.Y; }

        void MeasureScreen()
        {
            Rect wa = SystemParameters.WorkArea;
            areaX = (int)Math.Round(wa.Left * scale); areaY = (int)Math.Round(wa.Top * scale);
            areaW = (int)Math.Round(wa.Width * scale); areaH = (int)Math.Round(wa.Height * scale);
            // things can be drawn anywhere on the screens, not just the work area
            frame.Clip = new E.Rect((int)Math.Round(SystemParameters.VirtualScreenLeft * scale) - areaX, (int)Math.Round(SystemParameters.VirtualScreenTop * scale) - areaY,
                (int)Math.Round((SystemParameters.VirtualScreenLeft + SystemParameters.VirtualScreenWidth) * scale) - areaX,
                (int)Math.Round((SystemParameters.VirtualScreenTop + SystemParameters.VirtualScreenHeight) * scale) - areaY);
        }

        // Stats and settings from the first desktop Squib (%APPDATA%\Squib\memory.txt), the first time this one runs.
        void Migrate()
        {
            string old = Path.Combine(Program.DataDir, "memory.txt");
            var ed = prefs.edit();
            if (File.Exists(old))
            {
                try
                {
                    foreach (string line in File.ReadAllLines(old))
                    {
                        int i = line.IndexOf('=');
                        if (i < 0) continue;
                        string k = line.Substring(0, i), val = line.Substring(i + 1);
                        int n; int.TryParse(val, out n);
                        DateTime d;
                        switch (k)
                        {
                            case "born":
                                if (DateTime.TryParseExact(val, "yyyy-MM-dd", System.Globalization.CultureInfo.InvariantCulture, System.Globalization.DateTimeStyles.None, out d))
                                    ed.putLong("born", (long)(d - new DateTime(1970, 1, 1)).TotalDays);
                                break;
                            case "fed": ed.putInt("fed", n); break;
                            case "pets": ed.putInt("pets", n); break;
                            case "boops": ed.putInt("boops", n); break;
                            case "chatty": ed.putBoolean("chatty", val != "0"); break;
                            case "reminders": ed.putBoolean("reminders", val != "0"); break;
                            case "hotbar": ed.putBoolean("hotbar", val != "0"); break;
                            case "size": ed.putInt("size", Math.Max(2, Math.Min(4, n))); break;
                            case "textsize": ed.putInt("textSize", Math.Max(0, Math.Min(2, n))); break;
                        }
                    }
                    ed.putBoolean("greeted", true); // you've met
                }
                catch { }
            }
            ed.apply();
            prefs.Save();
        }

        // ---------------------------------------------------------------- the frame

        void Tick()
        {
            now = Clock();
            double dt = Math.Min(0.1, now - last);
            last = now;
            if (quit.WaitOne(0)) { timer.Stop(); Close(); return; }
            if (wake.WaitOne(0)) { if (paused) Resume(); else if (v.speech == null) v.say("hi.", 0); }
            if (paused)
            {
                if (pauseUntil != DateTime.MaxValue && DateTime.Now >= pauseUntil) Resume();
                return;
            }

            GetCursor(out curX, out curY);
            curX -= areaX; curY -= areaY;
            double idle = Native.IdleSeconds();

            if (now >= floorCheckAt) { floorCheckAt = now + 0.2; UpdateFloor(); }
            if (now >= modeCheckAt) { modeCheckAt = now + 1; UpdateMode(); }
            UpdateAway(idle);
            UpdatePetting(dt);
            UpdateFocus(dt);
            UpdateQuips(dt);
            if (demo) Demo();
            v.cursor = new[] { curX, curY };
            v.chatty = prefs.getBoolean("chatty", true) && focusLeft <= 0;

            v.step();
            UpdateMenu();

            frame.Clear();
            v.onDraw(canvas);
            if (v.visible()) { DrawPill(); DrawMenu(); }
            Present();
            lastCurX = curX; lastCurY = curY;

            if (leaving)
            {
                double k = now - leaveAt;
                if (k > 2.4) Opacity = Math.Max(0, 1 - (k - 2.4) / 0.6);
                if (k > 3.0) { timer.Stop(); Close(); return; }
            }
            if (now >= saveAt) { saveAt = now + 5; prefs.Save(); E.Trace.flush(); }
            if (now >= topmostAt && hwnd != IntPtr.Zero)
            {
                topmostAt = now + 3;
                Native.SetWindowPos(hwnd, Native.HWND_TOPMOST, 0, 0, 0, 0, Native.SWP_NOMOVE | Native.SWP_NOSIZE | Native.SWP_NOACTIVATE);
            }
        }

        // Moves and sizes the window around what was drawn, then paints it.
        void Present()
        {
            if (frame.Empty) { if (shown) { Hide(); shown = false; } return; }
            int l = frame.Left + areaX - 6, t = frame.Top + areaY - 6, r = frame.Right + areaX + 6, b = frame.Bottom + areaY + 6;
            bool fits = buf != null && l >= winX && t >= winY && r <= winX + winW && b <= winY + winH
                        && (long)winW * winH <= 4L * Math.Max(1, (long)(r - l) * (b - t)) + 64 * 64 * 4;
            if (!fits)
            {
                const int grid = 32;
                int nl = (int)Math.Floor((l - 16) / (double)grid) * grid, nt = (int)Math.Floor((t - 16) / (double)grid) * grid;
                int nr = (int)Math.Ceiling((r + 16) / (double)grid) * grid, nb = (int)Math.Ceiling((b + 16) / (double)grid) * grid;
                if (nr - nl != winW || nb - nt != winH)
                {
                    winW = nr - nl; winH = nb - nt;
                    buf = new int[winW * winH];
                    wb = new WriteableBitmap(winW, winH, 96, 96, PixelFormats.Bgra32, null);
                    img.Source = wb;
                    img.Width = winW / scale; img.Height = winH / scale;
                    Width = winW / scale; Height = winH / scale;
                }
                winX = nl; winY = nt;
                Left = winX / scale; Top = winY / scale;
            }
            frame.Render(buf, winX - areaX, winY - areaY, winW, winH);
            wb.WritePixels(new Int32Rect(0, 0, winW, winH), buf, winW * 4, 0);
            if (!shown) { Show(); shown = true; topmostAt = 0; }
        }

        // ---------------------------------------------------------------- the world around him

        // A few times a second: stand on Minecraft's hotbar while the game is open and nothing covers it.
        void UpdateFloor()
        {
            E.Rect hb = null;
            if (prefs.getBoolean("hotbar", true))
            {
                IntPtr game = GameWatch.Find(now);
                if (game != IntPtr.Zero)
                {
                    var bar = GameWatch.Hotbar(game);
                    if (!GameWatch.Covered(game, bar)) hb = new E.Rect(bar.Left - areaX, bar.Top - areaY, bar.Right - areaX, bar.Bottom - areaY);
                }
            }
            E.Rect cur = v.hotbar;
            bool same = hb == null ? cur == null : cur != null && cur.left == hb.left && cur.top == hb.top && cur.right == hb.right;
            if (same) return;
            v.hotbar = hb;
            v.place();
            if (hb != null || cur != null) UpdateMode();
        }

        // Once a second: what's in front decides his mode (see Modes.cs).
        void UpdateMode()
        {
            IntPtr fg = Native.GetForegroundWindow();
            if (fg == IntPtr.Zero || fg == hwnd) return;
            string id;
            if (v.hotbar != null || GameWatch.IsMinecraft(fg)) id = "com.mojang.minecraftpe";
            else
            {
                string program = Native.ProcessName(fg);
                if (program == System.Diagnostics.Process.GetCurrentProcess().ProcessName) return; // his own tray menu
                string cls = Native.ClassName(fg);
                if (cls == "Progman" || cls == "WorkerW" || cls == "Shell_TrayWnd" || cls == "Shell_SecondaryTrayWnd") id = "HOME|desktop";
                else id = Modes.Classify(program, Native.Title(fg), Fullscreen(fg));
            }
            v.setApp(id);
            if (!v.visible() && menu.IsOpen) menu.Close();
        }

        static bool Fullscreen(IntPtr h)
        {
            Native.RECT r;
            if (!Native.GetWindowRect(h, out r)) return false;
            var mi = new Native.MONITORINFO { cbSize = System.Runtime.InteropServices.Marshal.SizeOf(typeof(Native.MONITORINFO)) };
            if (!Native.GetMonitorInfo(Native.MonitorFromWindow(h, 2), ref mi)) return false;
            return r.Left <= mi.rcMonitor.Left && r.Top <= mi.rcMonitor.Top && r.Right >= mi.rcMonitor.Right && r.Bottom >= mi.rcMonitor.Bottom;
        }

        // Away from the computer for 3 minutes: he naps. Coming back is like the phone's screen coming on.
        void UpdateAway(double idle)
        {
            if (idle > 180)
            {
                away = true;
                awayFor = Math.Max(awayFor, idle);
                if ((v.motion == E.SquibView.Motion.IDLE || v.motion == E.SquibView.Motion.WANDER) && !v.shrimpOn && !touching && !leaving)
                    v.setMotion(E.SquibView.Motion.SLEEP);
            }
            else if (away && idle < 2)
            {
                away = false;
                v.onScreenOn(awayFor);
                awayFor = 0;
            }
        }

        // Wiggling the cursor over him pets him (holding the button down on him does too, like the phone).
        void UpdatePetting(double dt)
        {
            var hr = new E.Rect();
            bool over = !touching && v.handleRect(hr) && hr.contains((int)curX, (int)curY);
            double moved = (Math.Abs(curX - lastCurX) + Math.Abs(curY - lastCurY)) / scale;
            petEnergy = petEnergy * Math.Pow(0.25, dt) + (over ? Math.Min(moved, 40) : 0);
            if (over && petEnergy > 110 && !v.petting && v.motion != E.SquibView.Motion.DRAG && !leaving)
            {
                v.startPetting();
                wigglePet = true;
            }
            if (wigglePet && !over && petEnergy < 20)
            {
                wigglePet = false;
                if (!touching) v.petting = false;
            }
        }

        // ---------------------------------------------------------------- talking

        string Line(string group, string fallback)
        {
            string[] lines = E.Lines.get(group);
            return lines == null || lines.Length == 0 ? fallback : E.Pick.one(lines);
        }

        // Reactions to the cursor hanging around or flying past (desktop only).
        void UpdateQuips(double dt)
        {
            float cx = v.posX + v.S * 14, cy = v.posY - v.S * 10;
            bool near = Math.Sqrt((curX - cx) * (curX - cx) + (curY - cy) * (curY - cy)) < 140 * scale && !wigglePet;
            cursorNearFor = near ? cursorNearFor + dt : 0;
            bool free = v.chatty && v.speech == null && v.motion == E.SquibView.Motion.IDLE && E.Fun.act == E.Fun.NONE
                        && !E.Fun.bedtime && v.kindNow() == E.Apps.Kind.HOME && now >= nextQuipAt && !menu.IsOpen;
            if (!free) return;
            string group = null;
            if (cursorNearFor > 2.5) { cursorNearFor = -30; if (rng.nextDouble() < 0.5) group = "CURSOR"; }
            else if ((Math.Abs(curX - lastCurX) + Math.Abs(curY - lastCurY)) / scale > 700 && rng.nextDouble() < 0.3) group = "ZOOM";
            if (group == null) return;
            string s = Line(group, null);
            if (s == null) return;
            nextQuipAt = now + 25 + rng.nextDouble() * 80;
            v.say(s, 0);
        }

        // ---------------------------------------------------------------- focus timer

        void ToggleFocus()
        {
            if (v.motion == E.SquibView.Motion.SLEEP) v.setMotion(E.SquibView.Motion.IDLE);
            if (focusLeft > 0) { focusLeft = 0; v.say(Line("FOCUS_STOP", "timer off."), 0); return; }
            focusLeft = 25 * 60;
            v.say(Line("FOCUS_START", "focus timer on. 25 minutes."), 0);
            v.hop();
        }

        void UpdateFocus(double dt)
        {
            if (focusLeft <= 0) return;
            focusLeft -= dt;
            if (focusLeft > 0) return;
            focusLeft = 0;
            v.activeSecs = 0;
            if (v.motion == E.SquibView.Motion.SLEEP) v.setMotion(E.SquibView.Motion.IDLE);
            v.say(Line("FOCUS_DONE", "25 minutes. that's the timer."), 4);
            v.happyFor = 2.5;
            v.hop();
            for (int i = 0; i < 6; i++) v.heart();
        }

        void DrawPill()
        {
            if (focusLeft <= 0 || v.speech != null) return;
            int secs = (int)Math.Ceiling(focusLeft);
            string text = string.Format("focus {0}:{1:00}", secs / 60, secs % 60);
            if (text != pillText)
            {
                pillText = text;
                var p = new E.Paint();
                p.setTextSize(v.fontPx);
                int lineH = E.PixelFont.H[E.PixelFont.pick(p)];
                int w = (int)E.PixelFont.measure(p, text) + 8, h = lineH + 4;
                pill = E.Bitmap.createBitmap(w, h, E.Bitmap.Config.ARGB_8888);
                var c = new E.Canvas(pill);
                Menu.Box(c, unchecked((int)0xFF1F1E1D), 0, 0, w, h, 1);
                p.setColor(unchecked((int)0xFFFAF9F5));
                E.PixelFont.draw(c, text, 0, text.Length, 4, 2 + E.Math.round(-p.ascent()) - 3, p);
            }
            float T = v.T, x = v.posX + v.S * 14 - pill.getWidth() * T / 2, y = v.posY - v.S * 16 + v.lift() - pill.getHeight() * T - 4 * v.d;
            var dst = new E.Rect(E.Math.round(x / T) * (int)T, E.Math.round(y / T) * (int)T, 0, 0);
            dst.right = dst.left + (int)(pill.getWidth() * T); dst.bottom = dst.top + (int)(pill.getHeight() * T);
            canvas.drawBitmap(pill, null, dst, null);
        }

        // ---------------------------------------------------------------- menu

        void OpenMenu(Menu.Page p)
        {
            if (leaving || !v.visible()) return;
            v.speech = null;
            if (v.motion == E.SquibView.Motion.WANDER) v.setMotion(E.SquibView.Motion.IDLE); // stay put while it's open
            menu.Open(p);
            menuMouseWasDown = true;
        }

        public void ShowMenu() { if (paused) Resume(); OpenMenu(MainPage()); }

        void UpdateMenu()
        {
            if (!menu.IsOpen) return;
            bool down = Native.KeyDown(0x01) || Native.KeyDown(0x02);
            bool over = menu.Contains(curX, curY);
            if ((down && !menuMouseWasDown && !over) || Native.KeyDown(0x1B)) { menu.Close(); return; }
            menuMouseWasDown = down;
            menu.SetHover(menu.IndexAt(curX, curY));
            if (v.motion == E.SquibView.Motion.WANDER) v.setMotion(E.SquibView.Motion.IDLE);
        }

        void DrawMenu()
        {
            if (!menu.IsOpen) return;
            menu.Scale = v.T;
            menu.Render(v.fontPx);
            float T = v.T, w = menu.Art.getWidth() * T, h = menu.Art.getHeight() * T;
            float x = v.posX + v.S * 14 - w / 2;
            x = Math.Max(4 * v.d, Math.Min(areaW - 4 * v.d - w, x));
            float y = v.posY - v.S * 16 + v.lift() - h - 6 * v.d;
            if (y < 4 * v.d) y = Math.Min(v.posY + 6 * v.d, areaH - h - 4 * v.d); // no room above (on a cloud up top): below him
            menu.Left = (float)Math.Round(x / T) * T; menu.Top = (float)Math.Round(y / T) * T;
            var dst = new E.Rect((int)menu.Left, (int)menu.Top, (int)(menu.Left + w), (int)(menu.Top + h));
            canvas.drawBitmap(menu.Art, null, dst, null);
        }

        static readonly string[] SizeNames = { "", "", "small", "medium", "large" }, TextNames = { "small", "medium", "large" };

        Menu.Page MainPage()
        {
            var p = new Menu.Page();
            p.Header(() => "squib - day " + v.day() + " - " + prefs.getInt("fed", 0) + " shrimp");
            p.Header("version " + E.Version.Name + ", same as the phone app");
            p.Separator();
            p.Add("feed a shrimp", () => v.feed());
            p.Add("talk", () => { if (v.motion == E.SquibView.Motion.SLEEP) v.setMotion(E.SquibView.Motion.IDLE); v.talk(); });
            p.Add("do a trick", () => v.trick());
            p.Add("pick a trick...", () => OpenMenu(TrickPage(1, E.Fun.LAST_ACTIVITY, "pick a trick")));
            p.Add("pick a fidget...", () => OpenMenu(TrickPage(E.Fun.FIRST_FIDGET, E.Fun.NAMES.Length - 1, "pick a fidget")));
            p.Add(() => focusLeft > 0 ? "stop focus timer" : "start 25-min focus", ToggleFocus);
            p.Add("pause...", () => OpenMenu(PausePage()));
            p.Separator();
            p.Add(() => "size: " + SizeNames[Math.Max(2, Math.Min(4, prefs.getInt("size", 2)))] + "   (click to change)", () =>
            {
                int s = prefs.getInt("size", 2);
                prefs.edit().putInt("size", s >= 4 ? 2 : s + 1).apply();
                v.reloadPrefs();
                v.kick(2);
            }).KeepOpen = true;
            p.Add(() => "text size: " + TextNames[Math.Max(0, Math.Min(2, prefs.getInt("textSize", 1)))] + "   (click to change)", () =>
            {
                prefs.edit().putInt("textSize", (prefs.getInt("textSize", 1) + 1) % 3).apply();
                v.reloadPrefs();
                pillText = null;
            }).KeepOpen = true;
            p.Toggle("app modes (what's on screen changes what he does)", () => prefs.getBoolean("appModes", true), () => Flip("appModes", true));
            p.Toggle("walk on minecraft's hotbar", () => prefs.getBoolean("hotbar", true), () => { Flip("hotbar", true); UpdateFloor(); });
            p.Toggle("chatty", () => prefs.getBoolean("chatty", true), () => Flip("chatty", true));
            p.Toggle("break reminders and check-ins", () => prefs.getBoolean("reminders", true), () => Flip("reminders", true));
            p.Toggle("click-through (use the tray icon for this menu)", () => prefs.getBoolean("clickThrough", false), () =>
            {
                Flip("clickThrough", false);
                SetClickThrough(prefs.getBoolean("clickThrough", false));
                if (prefs.getBoolean("clickThrough", false)) menu.Close();
            });
            p.Toggle("start with windows", Program.StartsWithWindows, () => { try { Program.SetStartsWithWindows(!Program.StartsWithWindows()); } catch { } });
            p.Add(() => Remote.Status + "   (click to check now)", Remote.CheckNow).KeepOpen = true;
            p.Separator();
            p.Add("goodbye", Leave);
            return p;
        }

        void Flip(string key, bool def)
        {
            prefs.edit().putBoolean(key, !prefs.getBoolean(key, def)).apply();
            v.reloadPrefs();
            v.kick(1.5);
        }

        Menu.Page TrickPage(int from, int to, string title)
        {
            var p = new Menu.Page { Rows = 14 };
            p.Header(title);
            p.Add("< back", () => OpenMenu(MainPage()));
            for (int a = from; a <= to; a++)
            {
                int act = a;
                string[] versions = E.Fun.variantNames(act);
                if (versions == null) p.Add(E.Fun.label(act), () => Trick(act, -1));
                else p.Add(E.Fun.label(act) + " ...", () => OpenMenu(VersionPage(act, versions, from, to, title)));
            }
            return p;
        }

        Menu.Page VersionPage(int act, string[] versions, int from, int to, string title)
        {
            var p = new Menu.Page { Rows = 14 };
            p.Header(E.Fun.label(act));
            p.Add("< back", () => OpenMenu(TrickPage(from, to, title)));
            p.Add("random", () => Trick(act, -1));
            for (int i = 0; i < versions.Length; i++) { int k = i; p.Add(versions[i], () => Trick(act, k)); }
            return p;
        }

        void Trick(int act, int version)
        {
            menu.Close();
            v.trick(E.Fun.NAMES[act], version);
        }

        Menu.Page PausePage()
        {
            var p = new Menu.Page();
            p.Header("pause: he's gone, nothing on screen");
            p.Add("< back", () => OpenMenu(MainPage()));
            foreach (int mins in new[] { 5, 15, 30, 60, 180 }) { int m = mins; p.Add(m < 60 ? m + " minutes" : m / 60 + (m == 60 ? " hour" : " hours"), () => Pause(m)); }
            p.Add("until i open squib again", () => Pause(0));
            return p;
        }

        // ---------------------------------------------------------------- pause, click-through, goodbye

        public bool Paused { get { return paused; } }

        public void Pause(int minutes)
        {
            menu.Close();
            paused = true;
            pauseUntil = minutes > 0 ? DateTime.Now.AddMinutes(minutes) : DateTime.MaxValue;
            if (shown) { Hide(); shown = false; }
            prefs.Save();
            if (tray != null) tray.Update(minutes > 0 ? "squib is paused until " + pauseUntil.ToString("h:mm tt", System.Globalization.CultureInfo.InvariantCulture).ToLowerInvariant() : "squib is paused");
        }

        public void Resume()
        {
            paused = false;
            last = Clock();
            if (tray != null) tray.Update(null);
        }

        public bool ClickThrough { get { return prefs.getBoolean("clickThrough", false); } }

        public void SetClickThrough(bool on)
        {
            prefs.edit().putBoolean("clickThrough", on).apply();
            if (hwnd == IntPtr.Zero) return;
            int ex = Native.GetWindowLong(hwnd, Native.GWL_EXSTYLE) | Native.WS_EX_TOOLWINDOW | Native.WS_EX_NOACTIVATE;
            Native.SetWindowLong(hwnd, Native.GWL_EXSTYLE, on ? ex | Native.WS_EX_TRANSPARENT : ex & ~Native.WS_EX_TRANSPARENT);
        }

        public void Leave()
        {
            if (leaving) return;
            if (paused) Resume();
            menu.Close();
            leaving = true;
            leaveAt = now;
            v.shrimpOn = false;
            if (v.motion == E.SquibView.Motion.SLEEP) v.setMotion(E.SquibView.Motion.IDLE);
            v.say(Line("GOODBYE", "bye. open squib again whenever."), 0);
            v.happyFor = 3;
            v.hop();
            prefs.Save();
        }

        public void Feed() { if (paused) Resume(); v.feed(); }
        public void Talk() { if (paused) Resume(); v.talk(); }
        public void DoTrick() { if (paused) Resume(); v.trick(); }
        public System.Drawing.Bitmap Portrait() { return Tray.Draw(v); }

        // ---------------------------------------------------------------- mouse

        protected override void OnMouseLeftButtonDown(System.Windows.Input.MouseButtonEventArgs e)
        {
            e.Handled = true;
            if (leaving) return;
            if (menu.IsOpen && menu.Contains(curX, curY)) { menu.Click(curX, curY); return; }
            if (menu.IsOpen) { menu.Close(); return; }
            var hr = new E.Rect();
            if (v.handleRect(hr) && hr.contains((int)curX, (int)curY))
            {
                touching = true;
                CaptureMouse();
                v.onHandleTouch(E.MotionEvent.obtain(E.SystemClock.elapsedRealtime(), E.MotionEvent.ACTION_DOWN, curX + 0f, curY + 0f));
                return;
            }
            if (v.speech != null) v.speech = null; // clicked his speech bubble
        }

        protected override void OnMouseMove(System.Windows.Input.MouseEventArgs e)
        {
            if (!touching) return;
            float x, y;
            GetCursor(out x, out y);
            v.onHandleTouch(E.MotionEvent.obtain(E.SystemClock.elapsedRealtime(), E.MotionEvent.ACTION_MOVE, x - areaX, y - areaY));
        }

        protected override void OnMouseLeftButtonUp(System.Windows.Input.MouseButtonEventArgs e)
        {
            e.Handled = true;
            if (!touching) return;
            touching = false;
            ReleaseMouseCapture();
            float x, y;
            GetCursor(out x, out y);
            v.onHandleTouch(E.MotionEvent.obtain(E.SystemClock.elapsedRealtime(), E.MotionEvent.ACTION_UP, x - areaX, y - areaY));
        }

        protected override void OnLostMouseCapture(System.Windows.Input.MouseEventArgs e)
        {
            if (!touching) return;
            touching = false;
            v.onHandleTouch(E.MotionEvent.obtain(E.SystemClock.elapsedRealtime(), E.MotionEvent.ACTION_CANCEL, curX, curY));
        }

        protected override void OnMouseRightButtonUp(System.Windows.Input.MouseButtonEventArgs e)
        {
            e.Handled = true;
            if (leaving) return;
            if (menu.IsOpen) { if (!menu.Contains(curX, curY)) menu.Close(); else menu.Click(curX, curY); return; }
            ShowMenu();
        }

        // `Squib.exe --demo` goes through a few things without the mouse, for checking how they look.
        int demoStep;
        void Demo()
        {
            double[] at = { 6, 12, 20, 28 };
            if (demoStep >= at.Length || now < at[demoStep]) return;
            switch (demoStep++)
            {
                case 0: v.talk(); break;
                case 1: v.feed(); break;
                case 2: v.trick("SPINNER", -1); break;
                case 3: ShowMenu(); break;
            }
        }
    }
}
