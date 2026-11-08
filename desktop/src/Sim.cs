// `Squib.exe --sim ...` runs the engine with a fake clock and no window, like tools/sim on the
// phone side:
//   --sim sheets [NAMES]   a picture of every activity -> desktop\build\sim\*.png ("sheets KITE MC HATS")
//   --sim picks            every activity and version through the picker, from the floor and a cloud
//   --sim stress HOURS     hours of random use; any exception fails it
// Prints to the console when started from one (Squib.exe is a windowed program, so use
// `start /wait Squib.exe --sim ...` or check sim.txt next to the pictures).

using System;
using System.Collections.Generic;
using System.IO;
using SD = System.Drawing;
using SDI = System.Drawing.Imaging;
using Squib.Engine;

namespace Squib
{
    static class Sim
    {
        const int W = 1920, H = 1040;
        static string outDir;
        static StringWriter log = new StringWriter();
        static readonly Frame frame = new Frame();
        static readonly Canvas canvas = new Canvas(frame);
        static readonly Context ctx = new Context(1.5f);

        public static int Run(string[] args)
        {
            outDir = Path.Combine(Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location), "build", "sim");
            Directory.CreateDirectory(outDir);
            SystemClock.Fake = 1000000;
            Calendar.Fake = DateTime.Today.AddHours(12); // midday, so bedtime never gets in the way
            Context.Shared = new SharedPreferences(null);
            Fun.defaults();
            int code = 0;
            try
            {
                string mode = args.Length > 1 ? args[1] : "sheets";
                if (mode == "sheets") Sheets(args.Length > 2 ? string.Join(" ", args, 2, args.Length - 2) : null);
                else if (mode == "picks") Picks();
                else if (mode == "modes") Modes_();
                else Stress(double.Parse(args[2], System.Globalization.CultureInfo.InvariantCulture));
            }
            catch (Exception e) { Say("FAILED: " + e); code = 1; }
            File.WriteAllText(Path.Combine(outDir, "sim.txt"), log.ToString());
            return code;
        }

        static void Say(string s) { log.WriteLine(s); Console.WriteLine(s); }

        static SquibView Fresh()
        {
            Fun.act = Fun.NONE;
            Fun.inMc = false;
            Fun.mcAct = 0;
            Fun.torches.clear();
            Fun.flowers.clear();
            Fun.oreOn = false;
            Fun.itemOverride = null;
            Fun.hat = null;
            Fun.seasonLines = null;
            Fun.nextSeasonCheck = 1e18;
            Fun.partyDay = false;
            Fun.night = false;
            Fun.december = false;
            Fun.bedtime = false;
            Fun.listening = false;
            Fun.listenOff = -1;
            Fun.texts.clear();
            Fun.autoText = false;
            Arrays.fill(Fun.recentActs, 0);
            var v = new SquibView(ctx);
            v.onScreen(W, H, 0, 0);
            v.greeted = true;
            RunFor(v, 4.0);
            Fun.nextAt = 1e18;
            Fun.nextFidgetAt = 1e18;
            v.nextChatAt = 1e18;
            v.speech = null;
            return v;
        }

        static void RunFor(SquibView v, double secs)
        {
            int frames = (int)System.Math.Round(secs * 30);
            for (int i = 0; i < frames; i++) { SystemClock.Fake += 33; v.step(); }
        }

        static SD.Bitmap Shot(SquibView v, int size)
        {
            frame.Clear();
            v.onDraw(canvas);
            int cx = (int)System.Math.Round(v.posX + v.S * 14), cy = (int)System.Math.Round(v.posY - v.S * 14);
            int ox = cx - size / 2, oy = cy - size * 2 / 3;
            var buf = new int[size * size];
            frame.Render(buf, ox, oy, size, size);
            var img = new SD.Bitmap(size * 2, size * 2, SDI.PixelFormat.Format32bppArgb);
            for (int y = 0; y < size * 2; y++)
                for (int x = 0; x < size * 2; x++)
                {
                    int sx = ox + x / 2, sy = oy + y / 2;
                    int c = buf[(y / 2) * size + x / 2];
                    int bg = (sx < 0 || sy < 0 || sx >= W || sy >= H) ? unchecked((int)0xFF555555) : unchecked((int)0xFFE9E5DD);
                    img.SetPixel(x, y, SD.Color.FromArgb(Over(c, bg)));
                }
            return img;
        }

        static int Over(int c, int bg)
        {
            int a = (c >> 24) & 255;
            int r = (((c >> 16) & 255) * a + ((bg >> 16) & 255) * (255 - a)) / 255;
            int g = (((c >> 8) & 255) * a + ((bg >> 8) & 255) * (255 - a)) / 255;
            int b = ((c & 255) * a + (bg & 255) * (255 - a)) / 255;
            return unchecked((int)0xFF000000) | (r << 16) | (g << 8) | b;
        }

        static void Sheet(string name, List<SD.Bitmap> frames, List<string> labels)
        {
            int cols = 5, size = frames[0].Width;
            int rows = (frames.Count + cols - 1) / cols;
            using (var outImg = new SD.Bitmap(cols * size, rows * (size + 18) + 22))
            using (var g = SD.Graphics.FromImage(outImg))
            using (var big = new SD.Font("Segoe UI", 11, SD.FontStyle.Bold))
            using (var small = new SD.Font("Segoe UI", 9))
            {
                g.Clear(SD.Color.DimGray);
                g.DrawString(name, big, SD.Brushes.White, 4, 2);
                for (int i = 0; i < frames.Count; i++)
                {
                    int x = (i % cols) * size, y = 22 + (i / cols) * (size + 18);
                    g.DrawImage(frames[i], x, y + 18);
                    g.DrawString(labels[i], small, SD.Brushes.White, x + 4, y + 2);
                    frames[i].Dispose();
                }
                outImg.Save(Path.Combine(outDir, name + ".png"), SDI.ImageFormat.Png);
            }
        }

        static void Capture(string name, SquibView v, double total, int n, int size)
        {
            var frames = new List<SD.Bitmap>();
            var labels = new List<string>();
            double step = total / n;
            for (int i = 0; i < n; i++)
            {
                RunFor(v, step);
                frames.Add(Shot(v, size));
                labels.Add(string.Format(System.Globalization.CultureInfo.InvariantCulture, "{0:0.0}s act={1}", (i + 1) * step, Fun.act));
            }
            Sheet(name, frames, labels);
        }

        static void Sheets(string only)
        {
            for (int act = 1; act < Fun.NAMES.Length; act++)
            {
                string name = Fun.NAMES[act];
                if (only != null && !(" " + only + " ").Contains(" " + name + " ")) continue;
                SquibView v = Fresh();
                if (act == Fun.FISHING) { v.setApp("CHAT|discord"); RunFor(v, 1); }
                if (act == Fun.STARGAZE) Fun.night = true;
                if (act == Fun.CONFETTI) { Fun.partyDay = true; Fun.hat = "PARTY_HAT"; }
                v.speech = null;
                Fun.start(v, act);
                double dur = System.Math.Max(1.5, System.Math.Min(20, Fun.actEnd - v.t + 0.5));
                if (act == Fun.BALL || act == Fun.FISHING || act == Fun.SKATE || act == Fun.ICECREAM || act == Fun.FRIEND) dur = 16;
                if (act == Fun.BOOMERANG || act == Fun.BOWLING || act == Fun.BLOCKS || act == Fun.MAIL || act == Fun.WORKOUT) dur = 9;
                Capture(string.Format("{0:00}_{1}", act, name), v, dur, 15, 150);
                Say("drew " + name);
            }
            if (only == null || only.Contains("MC"))
            {
                for (int m = 1; m <= 5; m++)
                {
                    SquibView v = Fresh();
                    v.setApp("com.mojang.minecraftpe");
                    RunFor(v, 0.5);
                    Fun.nextMcAt = 1e18; Fun.nextItemAt = 1e18; Fun.helmet = true;
                    Fun.item = m == 3 ? 4 : 1;
                    Fun.startMc(v, v.t, m);
                    Capture("MC_" + m, v, System.Math.Max(1.5, Fun.mcEnd - v.t + 0.5), 10, 150);
                }
                Say("drew MC");
            }
            if (only == null || only.Contains("ANGER"))
            {
                // the anger mark: still and throbbing, on the floor, seated and on a cloud; then a boop while he sleeps
                var frames = new List<SD.Bitmap>(); var labels = new List<string>();
                foreach (string where in new[] { "floor", "seated", "cloud" })
                    foreach (double at in new[] { 0.4, 0.0 })
                    {
                        SquibView v = Fresh();
                        if (where == "cloud") { v.setApp("CHAT|discord"); Fun.trick(v, "FISHING", -1); Fun.end(v); v.perched = true; }
                        if (where == "seated") v.seated = true;
                        RunFor(v, 0.5);
                        double t0 = v.t - v.t % 1.0 + 1.0 + at; // throbs for the first third of each second
                        while (v.t < t0) { SystemClock.Fake += 33; v.annoyedFor = 1; v.step(); }
                        v.speech = null;
                        frames.Add(Shot(v, 150)); labels.Add(where + (at == 0 ? " throb" : ""));
                    }
                SquibView z = Fresh();
                z.setMotion(SquibView.Motion.SLEEP);
                RunFor(z, 1);
                frames.Add(Shot(z, 150)); labels.Add("asleep");
                float bx = z.posX + z.S * 14, by = z.posY - z.S * 6;
                z.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 0, bx, by));
                SystemClock.Fake += 80;
                z.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 1, bx, by));
                for (int i = 0; i < 6; i++) { RunFor(z, 0.8); frames.Add(Shot(z, 150)); labels.Add("booped +" + (0.8 * (i + 1)).ToString("0.0") + "s " + z.face); }
                Sheet("ANGER", frames, labels);
                Say("drew ANGER");
            }
            if (only == null || only.Contains("HATS"))
            {
                var frames = new List<SD.Bitmap>(); var labels = new List<string>();
                foreach (string hat in new[] { "PARTY_HAT", "WINTER_HAT", "WITCH_HAT" })
                    foreach (int facing in new[] { 1, -1 })
                    {
                        SquibView v = Fresh();
                        Fun.hat = hat; v.facing = facing;
                        RunFor(v, 0.2);
                        frames.Add(Shot(v, 150)); labels.Add(hat + " " + facing);
                    }
                SquibView s = Fresh();
                Fun.december = true; Fun.hat = "WINTER_HAT";
                RunFor(s, 12);
                frames.Add(Shot(s, 150)); labels.Add("snow");
                Sheet("HATS", frames, labels);
                Say("drew HATS");
            }
            if (only == null || only.Contains("MENU"))
            {
                // the main menu and the trick picker, as Host builds them (labels only)
                var main = new Menu.Page();
                main.Header("squib - day 6 - 9 shrimp");
                main.Header("version " + Engine.Version.Name + ", same as the phone app");
                main.Separator();
                foreach (string s in new[] { "feed a shrimp", "talk", "do a trick", "pick a trick...", "pick a fidget...", "start 25-min focus", "pause..." }) main.Add(s, null);
                main.Separator();
                main.Add("size: small   (click to change)", null);
                main.Add("text size: small   (click to change)", null);
                main.Toggle("app modes (what's on screen changes what he does)", () => true, null);
                main.Toggle("chatty", () => false, null);
                main.Add("lines: updated 4:05 pm (1199)   (click to check now)", null);
                main.Separator();
                main.Add("goodbye", null);
                var tricks = new Menu.Page { Rows = 14 };
                tricks.Header("pick a trick");
                tricks.Add("< back", null);
                for (int a = 1; a <= Fun.LAST_ACTIVITY; a++) tricks.Add(Fun.label(a) + (Fun.variantNames(a) != null ? " ..." : ""), null);
                var frames = new List<SD.Bitmap>(); var labels = new List<string>();
                foreach (int px in new[] { 9, 11 })
                    foreach (Menu.Page p in new[] { main, tricks })
                    {
                        var m = new Menu();
                        m.Open(p);
                        m.Hover = 4;
                        m.Render(px);
                        var img = new SD.Bitmap(m.Art.getWidth() * 2, m.Art.getHeight() * 2);
                        for (int y = 0; y < img.Height; y++)
                            for (int x = 0; x < img.Width; x++)
                                img.SetPixel(x, y, SD.Color.FromArgb(Over(m.Art.getPixel(x / 2, y / 2), unchecked((int)0xFF6E8FB0))));
                        img.Save(Path.Combine(outDir, "MENU_" + px + "_" + (p == main ? "main" : "tricks") + ".png"), SDI.ImageFormat.Png);
                    }
                Say("drew MENU");
            }
            if (only == null || only.Contains("TALK"))
            {
                var frames = new List<SD.Bitmap>(); var labels = new List<string>();
                SquibView v = Fresh();
                v.talk();
                for (int i = 0; i < 5; i++) { RunFor(v, 0.6); frames.Add(Shot(v, 150)); labels.Add("talk " + i); }
                v.feed();
                for (int i = 0; i < 5; i++) { RunFor(v, 0.7); frames.Add(Shot(v, 150)); labels.Add("feed " + i); }
                Sheet("TALK", frames, labels);
                Say("drew TALK");
            }
        }

        static void Picks()
        {
            int n = 0;
            foreach (string app in new[] { "HOME|explorer", "CHAT|discord" })
            {
                for (int act = 1; act < Fun.NAMES.Length; act++)
                {
                    string[] vs = Fun.variantNames(act);
                    for (int var = -1; var < (vs == null ? 0 : vs.Length); var++)
                    {
                        SquibView v = Fresh();
                        v.setApp(app);
                        RunFor(v, 1);
                        bool perched = v.perched;
                        Fun.trick(v, Fun.NAMES[act], var);
                        if (Fun.act != act) throw new Exception(Fun.NAMES[act] + " " + var + " didn't start in " + app);
                        double start = v.t;
                        while (Fun.act == act && v.t - start < 1500) { RunFor(v, 0.5); frame.Clear(); v.onDraw(canvas); }
                        if (Fun.act == act) throw new Exception(Fun.NAMES[act] + " " + var + " never ended");
                        RunFor(v, 0.5);
                        if (v.forcePlace != 0 || v.perched != perched) throw new Exception(Fun.NAMES[act] + " " + var + " didn't go back to its spot");
                        n++;
                    }
                }
            }
            Say("picked " + n + " tricks and versions, all started, ended and went back to their spot");
        }

        // On Windows he stays on the taskbar in every app mode, and switching windows or clicking him
        // never moves him somewhere else (it used to: chat apps sent him to a cloud in the top right).
        static void Modes_()
        {
            string[] ids = { "HOME|explorer", "WATCH|vlc", "MUSIC|spotify", "SCROLL|chrome|Reddit", "CHAT|discord", "GAME|REPO", "NAV|chrome|Google Maps", "READ|kindle" };
            int checks = 0;
            foreach (string from in ids)
                foreach (string to in ids)
                {
                    SquibView v = Fresh();
                    v.setApp(from);
                    RunFor(v, 0.5);
                    float lastX = v.posX;
                    Action watch = () =>
                    {
                        if (System.Math.Abs(v.posY - H) > 0.5f && v.motion != SquibView.Motion.JET)
                            throw new Exception("left the taskbar: " + from + " -> " + to + ", y " + v.posY);
                        if (System.Math.Abs(v.posX - lastX) > 12 * v.d)
                            throw new Exception("jumped " + (v.posX - lastX) + " px: " + from + " -> " + to);
                        lastX = v.posX;
                    };
                    v.setApp(to);
                    for (int i = 0; i < 30; i++) { SystemClock.Fake += 33; v.step(); watch(); }
                    float x = v.posX + v.S * 14, y = v.posY - v.S * 8;
                    v.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 0, x, y));
                    SystemClock.Fake += 80;
                    v.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 1, x, y));
                    for (int i = 0; i < 60; i++) { SystemClock.Fake += 33; v.step(); watch(); }
                    checks++;
                }
            // fishing needs a cloud: it goes just above him, and he comes back down after
            SquibView f = Fresh();
            float x0 = f.posX;
            Fun.trick(f, "FISHING", -1);
            if (!f.perched || System.Math.Abs(f.posX - x0) > 13 * f.d || f.posY > H - 30 * f.d) throw new Exception("fishing cloud isn't just above him: " + f.posX + "," + f.posY);
            while (Fun.act == Fun.FISHING) RunFor(f, 0.5);
            RunFor(f, 0.5);
            if (f.perched || System.Math.Abs(f.posY - H) > 0.5f) throw new Exception("didn't come back down after fishing");
            Say("modes: " + checks + " window switches and boops, he stayed put on the taskbar; fishing cloud is right above him");
            Clicks();
        }

        // Clicking him on Windows: WPF raises a mouse move when the window takes capture. That move
        // used to reach the engine before the touch-down, which teleported him and left him in DRAG.
        static void Clicks()
        {
            // the old order, straight into the engine: shows the bug is real
            SquibView o = Fresh();
            float ox = o.posX + o.S * 14, oy = o.posY - o.S * 8;
            o.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 0, ox, oy));
            o.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value + 80, 1, ox, oy));
            RunFor(o, 4);
            float bx = o.posX + o.S * 14 + 300 * o.d, by = o.posY - o.S * 8;
            o.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 2, bx, by - 400 * o.d)); // the capture move, stale start point
            o.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 0, bx, by));
            o.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value + 80, 1, bx, by));
            RunFor(o, 2);
            if (o.motion != SquibView.Motion.DRAG) throw new Exception("couldn't reproduce the old click bug (now " + o.motion + ")");
            new Touch(o).Unstick();
            RunFor(o, 2);
            if (o.motion == SquibView.Motion.DRAG) throw new Exception("Unstick didn't drop him out of DRAG");

            // the order the desktop sends now, through Touch
            SquibView v = Fresh();
            Touch touch = new Touch(v);
            float lastX = v.posX, lastY = v.posY;
            int clicks = 0;
            Action watch = () =>
            {
                if (v.motion == SquibView.Motion.DRAG && !touch.IsDown) throw new Exception("left in DRAG after click " + clicks);
                if (System.Math.Abs(v.posX - lastX) > 12 * v.d || (v.motion != SquibView.Motion.JET && System.Math.Abs(v.posY - lastY) > 12 * v.d))
                    throw new Exception("jumped from " + lastX + "," + lastY + " to " + v.posX + "," + v.posY + " on click " + clicks);
                lastX = v.posX; lastY = v.posY;
            };
            for (; clicks < 40; clicks++)
            {
                float x = v.posX + v.S * (8 + clicks % 12), y = v.posY - v.S * (4 + clicks % 9);
                touch.Move(SystemClock.Fake.Value, x, y - 400 * v.d); // a stray move before the press: ignored
                touch.Press(SystemClock.Fake.Value, x, y);
                touch.Move(SystemClock.Fake.Value, x, y);             // what CaptureMouse raises
                SystemClock.Fake += 80; v.step(); watch();
                touch.Release(SystemClock.Fake.Value, x, y, false);
                for (int i = 0; i < (clicks % 3 == 0 ? 150 : 20); i++) { SystemClock.Fake += 33; v.step(); touch.Unstick(); watch(); }
            }
            Say("clicks: the old order gets him stuck in DRAG; " + clicks + " clicks in the desktop's order never moved or stuck him");

            // booped or petted while asleep: wakes up annoyed (anger mark), no hearts while he is
            for (int how = 0; how < 2; how++)
            {
                SquibView w = Fresh();
                w.setMotion(SquibView.Motion.SLEEP);
                RunFor(w, 1);
                Touch tw = new Touch(w);
                float x = w.posX + w.S * 14, y = w.posY - w.S * 6;
                tw.Press(SystemClock.Fake.Value, x, y);
                if (how == 0) { SystemClock.Fake += 80; tw.Release(SystemClock.Fake.Value, x, y, false); }
                else w.startPetting();
                RunFor(w, 1);
                if (w.motion == SquibView.Motion.SLEEP || w.face != SquibView.Face.ANNOYED || w.speech == null)
                    throw new Exception((how == 0 ? "booped" : "petted") + " awake but not annoyed: " + w.motion + ", " + w.face);
                if (how == 1 && w.heartIn != 0) throw new Exception("hearts while annoyed at being woken");
                tw.Release(SystemClock.Fake.Value, x, y, false);
            }
            Say("woken: booped or petted while asleep, he wakes up annoyed with no hearts");
        }

        static void Stress(double hours)
        {
            var r = new Engine.Random(7);
            string[] apps = { "HOME|explorer", "WATCH|vlc", "MUSIC|spotify", "SCROLL|chrome", "CHAT|discord", "GAME|roblox",
                              "NAV|chrome", "READ|kindle", "HIDE|zoom", "com.mojang.minecraftpe" };
            Fun.activityGapMin = 25; Fun.activityGapMax = 70; Fun.fidgetGapMin = 12; Fun.fidgetGapMax = 35;
            SquibView v = Fresh();
            Fun.nextAt = v.t + 10; Fun.nextFidgetAt = v.t + 5; v.nextChatAt = v.t + 5;
            bool night = false;
            long frames = (long)(hours * 3600 * 30);
            var said = new Dictionary<string, int>();
            string last = null;
            var actCount = new int[Fun.NAMES.Length];
            int lastAct = 0;
            double nextEvent = v.t + 30;
            for (long i = 0; i < frames; i++)
            {
                SystemClock.Fake += 33;
                v.step();
                if (i % 3 == 0) { frame.Clear(); v.onDraw(canvas); }
                if (v.speech != null && !ReferenceEquals(v.speech, last)) { int c; said.TryGetValue(v.speech, out c); said[v.speech] = c + 1; }
                last = v.speech;
                if (Fun.act != lastAct && Fun.act != 0) actCount[Fun.act]++;
                lastAct = Fun.act;
                if (i % (30 * 60 * 90) == 0) { Fun.bedtime = night; night = !night; }
                if (i % (30 * 60 * 7) == 0 && Fun.bedtime) v.onScreenOn(r.nextBoolean() ? 30 : 900);
                if (v.t >= nextEvent)
                {
                    nextEvent = v.t + 20 + r.nextDouble() * 240;
                    double e = r.nextDouble();
                    float x = v.posX + v.S * 14, y = v.posY - v.S * 8;
                    long now = SystemClock.Fake.Value;
                    if (r.nextDouble() < 0.3) v.musicOn = !v.musicOn; // music starts or stops
                    if (e < 0.35) v.setApp(apps[r.nextInt(apps.Length)]);
                    else if (e < 0.5) { v.onHandleTouch(MotionEvent.obtain(now, 0, x, y)); v.onHandleTouch(MotionEvent.obtain(now + 50, 1, x, y)); }
                    else if (e < 0.58) { v.onHandleTouch(MotionEvent.obtain(now, 0, x, y)); RunFor(v, 1.5); v.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 1, x, y)); }
                    else if (e < 0.66)
                    {
                        v.onHandleTouch(MotionEvent.obtain(now, 0, x, y));
                        for (int k = 1; k <= 10; k++) { SystemClock.Fake += 33; v.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 2, x - k * 25, y - k * 40)); v.step(); }
                        v.onHandleTouch(MotionEvent.obtain(SystemClock.Fake.Value, 1, x - 250, y - 400));
                    }
                    else if (e < 0.74) v.feed();
                    else if (e < 0.86) v.trick();
                    else if (e < 0.92) v.talk();
                    else v.userHidden = !v.userHidden;
                }
            }
            int lines = 0;
            foreach (int c in said.Values) lines += c;
            Say(string.Format("simulated {0} h: {1} lines said, {2} distinct", hours, lines, said.Count));
            var b = new System.Text.StringBuilder("activities:");
            for (int k = 1; k < Fun.NAMES.Length; k++) b.Append(' ').Append(Fun.NAMES[k]).Append('=').Append(actCount[k]);
            Say(b.ToString());
        }
    }
}
