// Downloads squib.json from GitHub on start and every 6 hours, like the phone app, keeps a copy in
// %APPDATA%\Squib, and applies it: line groups replace the built-in ones, "settings" sets the
// matching numbers in Fun, "minecraft" and "apps" add phone apps, "desktopApps" adds Windows ones.

using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Reflection;
using System.Text;
using System.Threading;
using System.Web.Script.Serialization;
using System.Windows.Threading;
using Squib.Engine;

namespace Squib
{
    static class Remote
    {
        const string Url = "https://raw.githubusercontent.com/SjB404/squib/main/squib.json";
        const int EveryMs = 6 * 60 * 60 * 1000;
        static Dispatcher ui;
        static readonly AutoResetEvent now = new AutoResetEvent(false);
        static volatile bool fetching;
        public static string Status = "lines: built in";

        static string CachePath { get { return Path.Combine(Program.DataDir, "squib.json"); } }

        public static void Start(Dispatcher dispatcher)
        {
            ui = dispatcher;
            Fun.defaults();
            try { if (File.Exists(CachePath)) Apply(File.ReadAllText(CachePath, Encoding.UTF8), false); } catch { }
            var t = new Thread(() =>
            {
                while (true)
                {
                    Fetch();
                    now.WaitOne(EveryMs);
                }
            });
            t.IsBackground = true;
            t.Name = "squib-lines";
            t.Start();
        }

        // The "check now" button.
        public static void CheckNow()
        {
            Status = "lines: checking...";
            now.Set();
        }

        static void Fetch()
        {
            if (fetching) return;
            fetching = true;
            try
            {
                ServicePointManager.SecurityProtocol |= (SecurityProtocolType)3072; // TLS 1.2
                using (var web = new WebClient())
                {
                    web.Headers[HttpRequestHeader.CacheControl] = "no-cache";
                    web.Encoding = Encoding.UTF8;
                    string json = web.DownloadString(Url + "?t=" + DateTime.UtcNow.Ticks / TimeSpan.TicksPerMinute);
                    new JavaScriptSerializer { MaxJsonLength = int.MaxValue }.DeserializeObject(json); // only keep it if it parses
                    Directory.CreateDirectory(Program.DataDir);
                    File.WriteAllText(CachePath + ".tmp", json, new UTF8Encoding(false));
                    if (File.Exists(CachePath)) File.Replace(CachePath + ".tmp", CachePath, null); else File.Move(CachePath + ".tmp", CachePath);
                    ui.BeginInvoke(new Action(() => Apply(json, true)));
                }
            }
            catch (Exception e)
            {
                Status = "lines: couldn't download (" + e.GetType().Name + ")";
            }
            finally { fetching = false; }
        }

        // Applies a squib.json document. Returns how many lines were loaded.
        public static int Apply(string json, bool fresh)
        {
            Dictionary<string, object> root;
            try { root = (Dictionary<string, object>)new JavaScriptSerializer { MaxJsonLength = int.MaxValue }.DeserializeObject(json); }
            catch { return 0; }
            int count = 0;
            object o;
            if (root.TryGetValue("lines", out o) && o is Dictionary<string, object>)
                foreach (var g in (Dictionary<string, object>)o)
                {
                    var list = Strings(g.Value);
                    if (list.Count > 0 && Lines.set(g.Key, list.ToArray())) count += list.Count;
                }
            if (root.TryGetValue("settings", out o) && o is Dictionary<string, object>)
                foreach (var s in (Dictionary<string, object>)o)
                {
                    FieldInfo f = typeof(Fun).GetField(s.Key, BindingFlags.Static | BindingFlags.Public);
                    double v;
                    if (f == null || f.FieldType != typeof(double) || f.IsLiteral || f.IsInitOnly) continue;
                    try { v = Convert.ToDouble(s.Value, System.Globalization.CultureInfo.InvariantCulture); } catch { continue; }
                    if (!double.IsNaN(v) && v >= 0) f.SetValue(null, v);
                }
            if (root.TryGetValue("minecraft", out o))
                foreach (string pkg in Strings(o)) { Fun.MC.add(pkg); Apps.put(pkg, Apps.Kind.GAME); }
            if (root.TryGetValue("apps", out o) && o is Dictionary<string, object>)
                foreach (var a in (Dictionary<string, object>)o)
                {
                    Apps.Kind kind;
                    if (!Enum.TryParse(a.Key, out kind)) continue;
                    foreach (string pkg in Strings(a.Value)) Apps.put(pkg, kind);
                }
            if (root.TryGetValue("desktopApps", out o) && o is Dictionary<string, object>)
                foreach (var a in (Dictionary<string, object>)o)
                {
                    Apps.Kind kind;
                    if (!Enum.TryParse(a.Key, out kind)) continue;
                    foreach (string rule in Strings(a.Value)) Modes.Add(kind, rule);
                }
            Status = "lines: " + (fresh ? "updated " + DateTime.Now.ToString("h:mm tt", System.Globalization.CultureInfo.InvariantCulture).ToLowerInvariant() : "saved copy") + " (" + count + ")";
            return count;
        }

        static List<string> Strings(object o)
        {
            var list = new List<string>();
            var arr = o as object[];
            if (arr == null) return list;
            foreach (object x in arr)
            {
                string s = x as string;
                if (s != null && s.Trim().Length > 0) list.Add(s.Trim());
            }
            return list;
        }
    }
}
