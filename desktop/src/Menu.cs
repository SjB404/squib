// Squib's right-click menu, drawn in pixels with his own font, like his speech bubbles. A normal
// Windows menu misbehaves here: his window never takes focus (so it doesn't steal it from your
// game), and Windows menus rely on focus to close when you click away.

using System;
using System.Collections.Generic;
using Squib.Engine;

namespace Squib
{
    sealed class Menu
    {
        public sealed class Item
        {
            public Func<string> Label;
            public Func<bool> Checked;    // null: no checkbox
            public Action Run;
            public bool KeepOpen, Header, Separator;
            public int X, Y, W, H;        // in menu pixels, set when drawn
        }

        public sealed class Page
        {
            public readonly List<Item> Items = new List<Item>();
            public int Rows = 18;         // more than this and it wraps into columns

            public Item Add(Func<string> label, Action run) { var i = new Item { Label = label, Run = run }; Items.Add(i); return i; }
            public Item Add(string label, Action run) { return Add(() => label, run); }
            public void Header(Func<string> label) { Items.Add(new Item { Label = label, Header = true }); }
            public void Header(string label) { Header(() => label); }
            public void Separator() { Items.Add(new Item { Separator = true }); }
            public void Toggle(string label, Func<bool> on, Action flip) { var i = Add(label, flip); i.Checked = on; i.KeepOpen = true; }
        }

        const int Ink = unchecked((int)0xFF1F1E1D), Paper = unchecked((int)0xFFFAF9F5), Shadow = 0x55000000,
                  Coral = unchecked((int)0xFFD87656), HeaderInk = unchecked((int)0xFFB85A3C);

        public Page Current;
        public Bitmap Art;
        public int Hover = -1;
        public bool IsOpen { get { return Current != null; } }
        public float Left, Top, Scale = 2;     // where it's drawn, in screen pixels
        readonly Paint text = new Paint(), fill = new Paint();
        int fontPx;

        public void Open(Page p) { Current = p; Hover = -1; Art = null; }
        public void Close() { Current = null; Art = null; }

        public bool Contains(float x, float y)
        {
            return Art != null && x >= Left && y >= Top && x < Left + Art.getWidth() * Scale && y < Top + Art.getHeight() * Scale;
        }

        public int IndexAt(float x, float y)
        {
            if (!Contains(x, y)) return -1;
            int mx = (int)((x - Left) / Scale), my = (int)((y - Top) / Scale);
            for (int i = 0; i < Current.Items.Count; i++)
            {
                Item it = Current.Items[i];
                if (!it.Header && !it.Separator && mx >= it.X && mx < it.X + it.W && my >= it.Y && my < it.Y + it.H) return i;
            }
            return -1;
        }

        // Runs what was clicked. Returns false if the click wasn't on an item.
        public bool Click(float x, float y)
        {
            int i = IndexAt(x, y);
            if (i < 0) return Contains(x, y);
            Page before = Current;
            Item it = Current.Items[i];
            if (!it.KeepOpen) Close();
            it.Run();
            if (Current == before && Current != null) Art = null; // redraw: labels may have changed
            return true;
        }

        public void SetHover(int i) { if (i != Hover) { Hover = i; Art = null; } }

        // Draws the current page at 1:1 into a small bitmap (shown at Scale).
        public void Render(int px)
        {
            if (Current == null || (Art != null && px == fontPx)) return;
            fontPx = px;
            text.setTextSize(px);
            int lineH = PixelFont.H[PixelFont.pick(text)], rowH = lineH + 4, sepH = 5;
            var items = Current.Items;

            // headers across the top, then the rest in columns of up to Rows
            int y = 5, headerW = 0;
            var cols = new List<List<Item>>();
            foreach (Item it in items)
            {
                if (it.Header) { it.X = 4; it.Y = y; it.H = rowH; y += rowH; headerW = System.Math.Max(headerW, 8 + (int)PixelFont.measure(text, it.Label()) + 8); continue; }
                if (cols.Count == 0 || (cols[cols.Count - 1].Count >= Current.Rows && !it.Separator)) cols.Add(new List<Item>());
                cols[cols.Count - 1].Add(it);
            }
            int top = y, x = 4, bottom = y;
            foreach (List<Item> col in cols)
            {
                int w = 0;
                foreach (Item it in col) if (!it.Separator) w = System.Math.Max(w, 18 + (int)PixelFont.measure(text, it.Label()) + 8);
                y = top;
                foreach (Item it in col)
                {
                    it.X = x; it.Y = y; it.W = w; it.H = it.Separator ? sepH : rowH;
                    y += it.H;
                }
                bottom = System.Math.Max(bottom, y);
                x += w + 2;
            }
            int width = System.Math.Max(x + 2, headerW + 4), height = bottom + 5;
            foreach (Item it in items) if (it.Header || it.Separator) it.W = width - 8;

            var b = Bitmap.createBitmap(width + 2, height + 2, Bitmap.Config.ARGB_8888);
            var c = new Canvas(b);
            Box(c, Shadow, 2, 2, width, height, 2);
            Box(c, Ink, 0, 0, width, height, 2);
            Box(c, Paper, 2, 2, width - 4, height - 4, 2);
            int ascent = Engine.Math.round(-text.ascent());
            for (int i = 0; i < items.Count; i++)
            {
                Item it = items[i];
                if (it.Separator)
                {
                    fill.setColor(Ink);
                    for (int dx = it.X + 4; dx < it.X + it.W - 4; dx += 2) c.drawRect(dx, it.Y + 2, dx + 1, it.Y + 3, fill);
                    continue;
                }
                bool hot = i == Hover;
                if (hot) Box(c, Coral, it.X, it.Y, it.W, it.H, 1);
                int ink = it.Header ? HeaderInk : hot ? Paper : Ink;
                int tx = it.Header ? it.X + 4 : it.X + 16, ty = it.Y + 2 + (rowH - 4 - lineH) / 2;
                if (it.Checked != null)
                {
                    fill.setColor(ink);
                    int bx = it.X + 5, by = it.Y + (it.H - 7) / 2;
                    c.drawRect(bx, by, bx + 7, by + 1, fill); c.drawRect(bx, by + 6, bx + 7, by + 7, fill);
                    c.drawRect(bx, by, bx + 1, by + 7, fill); c.drawRect(bx + 6, by, bx + 7, by + 7, fill);
                    if (it.Checked()) c.drawRect(bx + 2, by + 2, bx + 5, by + 5, fill);
                }
                text.setColor(ink);
                string label = it.Label();
                PixelFont.draw(c, label, 0, label.Length, tx, ty + ascent - 3, text);
            }
            Art = b;
        }

        static readonly Paint boxPaint = new Paint();

        // A box with stepped pixel corners, like his speech bubbles.
        public static void Box(Canvas c, int color, int x, int y, int w, int h, int corner)
        {
            boxPaint.setColor(color);
            for (int i = 0; i <= corner; i++) c.drawRect(x + corner - i, y + i, x + w - (corner - i), y + h - i, boxPaint);
        }
    }
}
