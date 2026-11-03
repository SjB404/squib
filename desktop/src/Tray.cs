// Squib's tray icon: the desktop's version of his notification on the phone. It's how you reach
// him when click-through is on or he's paused.

using System;
using SD = System.Drawing;
using WF = System.Windows.Forms;
using E = Squib.Engine;

namespace Squib
{
    sealed class Tray : IDisposable
    {
        readonly WF.NotifyIcon icon = new WF.NotifyIcon();
        readonly Host host;
        readonly WF.ToolStripMenuItem pause, clickThrough;

        public Tray(Host host)
        {
            this.host = host;
            using (var b = Draw(null)) icon.Icon = SD.Icon.FromHandle(b.GetHicon());
            icon.Text = "squib";
            var m = new WF.ContextMenuStrip();
            m.Items.Add("squib's menu", null, (s, e) => host.ShowMenu());
            m.Items.Add("feed a shrimp", null, (s, e) => host.Feed());
            m.Items.Add("talk", null, (s, e) => host.Talk());
            m.Items.Add("do a trick", null, (s, e) => host.DoTrick());
            m.Items.Add(new WF.ToolStripSeparator());
            pause = new WF.ToolStripMenuItem("pause for 15 minutes", null, (s, e) => { if (host.Paused) host.Resume(); else host.Pause(15); });
            m.Items.Add(pause);
            clickThrough = new WF.ToolStripMenuItem("click-through", null, (s, e) => host.SetClickThrough(!host.ClickThrough));
            m.Items.Add(clickThrough);
            m.Items.Add(new WF.ToolStripSeparator());
            m.Items.Add("goodbye", null, (s, e) => host.Leave());
            m.Opening += (s, e) =>
            {
                pause.Text = host.Paused ? "unpause" : "pause for 15 minutes";
                clickThrough.Checked = host.ClickThrough;
            };
            icon.ContextMenuStrip = m;
            icon.MouseClick += (s, e) => { if (e.Button == WF.MouseButtons.Left) { if (host.Paused) host.Resume(); else host.ShowMenu(); } };
            icon.Visible = true;
        }

        public void Update(string status) { icon.Text = status ?? "squib"; }

        public void Dispose() { icon.Visible = false; icon.Dispose(); }

        // His face, 32 x 32, for the tray icon.
        public static SD.Bitmap Draw(E.SquibView v)
        {
            var b = new SD.Bitmap(32, 32, SD.Imaging.PixelFormat.Format32bppArgb);
            using (var g = SD.Graphics.FromImage(b))
            using (var coral = new SD.SolidBrush(SD.Color.FromArgb(0xD8, 0x76, 0x56)))
            using (var eye = new SD.SolidBrush(SD.Color.FromArgb(0x11, 0x11, 0x11)))
            {
                g.FillRectangle(coral, 6, 6, 20, 14);   // body
                g.FillRectangle(coral, 0, 10, 6, 5);    // arms
                g.FillRectangle(coral, 26, 10, 6, 5);
                foreach (int x in new[] { 6, 12, 18, 24 }) g.FillRectangle(coral, x, 20, 2, 7); // legs
                g.FillRectangle(eye, 10, 10, 3, 3);
                g.FillRectangle(eye, 19, 10, 3, 3);
            }
            return b;
        }
    }
}
