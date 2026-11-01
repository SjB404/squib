package com.squib.buddy;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Everything squib does besides walking and talking: activities, quick fidgets,
 * Minecraft gear, seasonal hats, and scroll check-ins.
 *
 * Grid coordinates: squib's sprite is 28 x 20 "grid pixels", each S screen pixels wide.
 * (0,0) is the sprite's top-left; the floor is at y = 20; his body is roughly x 6..21.
 * {@link #sx} and {@link #sy} turn grid coordinates into screen pixels.
 */
final class Fun {
    private Fun() {
    }

    // ---------- activity ids ----------
    static final int NONE = 0, BALL = 1, PLANE = 2, BUBBLES = 3, BOOK = 4, YOYO = 5, DANCE = 6, WAVE = 7,
            BUTTERFLY = 8, NAP = 9, TEA = 10, GUITAR = 11, JUGGLE = 12, KITE = 13, FISHING = 14, RAIN = 15,
            SKATE = 16, ICECREAM = 17, BALLOON = 18, MAGIC = 19, STARGAZE = 20, SNEEZE = 21, JUMPROPE = 22,
            PAINT = 23, SELFIE = 24, CONFETTI = 25,
            // quick fidgets (their own, shorter timer)
            STRETCH = 26, SPIN = 27, TRIP = 28, SCRATCH = 29, YAWN = 30;

    /** Name of each activity. Also the name of its line group in squib.json. */
    static final String[] NAMES = {"NONE", "BALL", "PLANE", "BUBBLES", "BOOK", "YOYO", "DANCE", "WAVE",
            "BUTTERFLY", "NAP", "TEA", "GUITAR", "JUGGLE", "KITE", "FISHING", "RAIN", "SKATE", "ICECREAM",
            "BALLOON", "MAGIC", "STARGAZE", "SNEEZE", "JUMPROPE", "PAINT", "SELFIE", "PARTY",
            "STRETCH", "SPIN", "TRIP", "SCRATCH", "YAWN"};

    static final int ANY = 0, FLOOR = 1, PERCH = 2;
    /** Where each activity can happen: anywhere, only on the floor, or only perched on a cloud. */
    static final int[] WHERE = {ANY, FLOOR, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, FLOOR, PERCH,
            ANY, FLOOR, ANY, ANY, ANY, FLOOR, ANY, FLOOR, ANY, ANY, ANY, ANY, ANY, FLOOR, ANY, ANY};
    /** How likely each activity is, before app-mode and time-of-day adjustments. */
    static final int[] WEIGHT = {0, 3, 2, 3, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 6, 1, 2, 2, 2, 2, 5, 1, 2, 2, 2, 25,
            3, 3, 2, 3, 2};

    // ---------- tunable from squib.json "settings" ----------
    static double scrollCheckMinutes = 15;
    static double activityGapMin = 25;
    static double activityGapMax = 70;
    static double fidgetGapMin = 12;
    static double fidgetGapMax = 35;
    /** Seconds between snowflakes near squib in December. 0 turns snow off. */
    static double snowEvery = 0.9;

    static final Set<String> MC = new HashSet<String>(Arrays.asList(
            "com.mojang.minecraftpe", "com.mojang.minecraftedu", "net.kdt.pojavlaunch", "net.kdt.pojavlaunch.debug",
            "git.artdeell.mojo", "com.movtery.zalithlauncher", "com.tungsten.fcl"));

    static final Random rng = new Random();
    static final Paint paint = new Paint();
    static final Paint ink = new Paint();
    static final Rect dst = new Rect();
    static final HashMap<String, Bitmap[]> cache = new HashMap<String, Bitmap[]>();

    static {
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        ink.setAntiAlias(false);
        ink.setColor(0xFF1D1D1D);
    }

    // ---------- activity state ----------
    static int act = NONE;
    static int variant;
    static int phase;
    static final int[] recentActs = new int[3];
    static double lastT, actStart, actEnd, nextAt = -1, nextFidgetAt = -1, nextEvent, flipUntil, sipAt, mark;
    static int kicks, kicksWanted, side, count;
    static float bx, bh, bvx, bvh, alpha;
    static float ox, oy, ovx, ovy;
    static boolean objOn;
    static int[] order;

    // ---------- minecraft state ----------
    static final String[] ITEMS = {"SWORD", "PICK", "AXE", "SHOVEL", "GEM"};
    static final int MC_SWING = 1, MC_MINE = 2, MC_SHOW = 3, MC_ORE = 4, MC_TORCH = 5;
    static boolean inMc, helmet;
    static int item, mcAct, itemMode, savedItem; // itemMode 0 rest, 1 raised, 2 strike
    static String itemOverride;
    static double nextItemAt, nextMcAt, mcStart, mcEnd, mcBeat;
    static int crack;
    static boolean oreOn;
    static final ArrayList<float[]> torches = new ArrayList<float[]>(); // {x, y, goneAt}

    // ---------- seasons ----------
    static String hat;
    static String[] seasonLines;
    static double nextSeasonCheck, nextSnow;
    static boolean partyDay, december, night;

    // ---------- scroll check-ins ----------
    static double lastScroll;
    static int scrollN;

    static void defaults() {
        for (String pkg : MC) {
            Apps.put(pkg, Apps.Kind.GAME);
        }
    }

    // ================= drawing helpers =================

    static Bitmap[] bmp(String key, String[] rows) {
        Bitmap[] b = cache.get(key);
        if (b != null) {
            return b;
        }
        int w = 0;
        for (String r : rows) {
            w = Math.max(w, r.length());
        }
        int h = rows.length;
        int[] normal = new int[w * h], flip = new int[w * h], cw = new int[w * h], ccw = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = x < rows[y].length() ? Art.color(rows[y].charAt(x)) : 0;
                normal[y * w + x] = c;
                flip[y * w + (w - 1 - x)] = c;
                cw[x * h + (h - 1 - y)] = c;
                ccw[x * h + y] = c;
            }
        }
        b = new Bitmap[]{
                Bitmap.createBitmap(normal, w, h, Bitmap.Config.ARGB_8888),
                Bitmap.createBitmap(flip, w, h, Bitmap.Config.ARGB_8888),
                Bitmap.createBitmap(cw, h, w, Bitmap.Config.ARGB_8888),
                Bitmap.createBitmap(ccw, h, w, Bitmap.Config.ARGB_8888)};
        cache.put(key, b);
        return b;
    }

    static String[] rows(String name) {
        try {
            return (String[]) Art.class.getDeclaredField(name).get(null);
        } catch (Exception e) {
            return Art.GEM;
        }
    }

    static Bitmap[] bmp(String name) {
        return bmp(name, rows(name));
    }

    static int[] grip(String name) {
        try {
            return (int[]) Art.class.getDeclaredField(name + "_GRIP").get(null);
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    static float center(SquibView v) {
        return v.posX + v.S * 14;
    }

    /** Grid row of the top of his head. */
    static int top(SquibView v) {
        return (v.seated ? 3 : 0) + v.squashLevel * 2 + 4;
    }

    /** Grid row where his arms attach. */
    static int armRow(SquibView v) {
        int top = top(v);
        return ((((v.seated ? 18 : 15) - top) + 1) / 3) + top;
    }

    static boolean right(SquibView v) {
        return v.facing >= 0;
    }

    static int handPx(SquibView v, boolean right) {
        return right ? v.squashLevel + 25 : 2 - v.squashLevel;
    }

    static int handPy(SquibView v, boolean right) {
        return armRow(v) - (right ? v.armLiftR : v.armLiftL) + 1;
    }

    /** Mirrors a grid x for something w pixels wide when he faces left. */
    static float mx(boolean right, float x, float w) {
        return right ? x : 28 - x - w;
    }

    static float sx(SquibView v, float gx) {
        return Math.round(v.posX) + v.S * gx;
    }

    static float sy(SquibView v, float gy, float lift) {
        return Math.round((v.posY - v.S * 20) + lift) + v.S * gy;
    }

    static void arm(SquibView v, boolean right, int lift) {
        if (right) {
            v.armLiftR = lift;
        } else {
            v.armLiftL = lift;
        }
    }

    static void drawBmp(Canvas c, Bitmap b, float x, float y, float scale, int a) {
        dst.set(Math.round(x), Math.round(y), Math.round(x + b.getWidth() * scale), Math.round(y + b.getHeight() * scale));
        paint.setAlpha(Math.max(0, Math.min(255, a)));
        c.drawBitmap(b, null, dst, paint);
        paint.setAlpha(255);
    }

    /** Draws art at a grid position on squib's sprite. */
    static void put(SquibView v, Canvas c, String name, int index, float gx, float gy, float lift, int a) {
        drawBmp(c, bmp(name)[index], sx(v, gx), sy(v, gy, lift), v.S, a);
    }

    /** Draws a prop held in one hand. artFacesIn: the art points toward his body (like the mug). */
    static void held(SquibView v, Canvas c, float lift, String name, boolean right, boolean rotated, boolean artFacesIn) {
        String[] r = rows(name);
        Bitmap[] b = bmp(name, r);
        int[] g = grip(name);
        int w = r[0].length(), h = r.length;
        boolean mirror = artFacesIn ? right : !right;
        int index = (rotated ? 2 : 0) + (mirror ? 1 : 0);
        int gx, gy;
        if (!rotated) {
            gx = mirror ? (w - 1) - g[0] : g[0];
            gy = g[1];
        } else {
            gx = mirror ? g[1] : (h - 1) - g[1];
            gy = g[0];
        }
        drawBmp(c, b[index], sx(v, handPx(v, right) - gx), sy(v, handPy(v, right) - gy, lift), v.S, 255);
    }

    /** A thin line in screen pixels, for strings and fishing line. */
    static void line(Canvas c, float x0, float y0, float x1, float y1, float width, int color) {
        ink.setColor(color);
        int n = (int) Math.max(1, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / width));
        for (int i = 0; i <= n; i++) {
            float x = x0 + (x1 - x0) * i / n, y = y0 + (y1 - y0) * i / n;
            c.drawRect(Math.round(x - width / 2), Math.round(y - width / 2), Math.round(x + width / 2), Math.round(y + width / 2), ink);
        }
        ink.setColor(0xFF1D1D1D);
    }

    /** A line of grid pixels, for chunky things like the telescope. */
    static void gline(SquibView v, Canvas c, float x0, float y0, float x1, float y1, char color, float lift) {
        ink.setColor(Art.color(color));
        int n = (int) Math.max(1, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= n; i++) {
            float gx = Math.round(x0 + (x1 - x0) * i / n), gy = Math.round(y0 + (y1 - y0) * i / n);
            float x = sx(v, gx), y = sy(v, gy, lift);
            c.drawRect(x, y, x + v.S, y + v.S, ink);
        }
        ink.setColor(0xFF1D1D1D);
    }

    static float thin(SquibView v) {
        return Math.max(1, Math.round(v.S / 3));
    }

    static void puff(SquibView v, String name, float x, float y, float vx, float vy, float life, float gravity, float scale) {
        v.spawn(bmp(name)[0], x, y, vx, vy, life, gravity, 0, 0.6f, scale, true);
    }

    static void maybeSay(SquibView v, String[] lines, double chance) {
        if (lines == null || lines.length == 0 || !v.chatty || v.speech != null || v.kindNow() == Apps.Kind.CHAT
                || rng.nextDouble() >= chance) {
            return;
        }
        v.say(Pick.one(lines), 0);
    }

    static void maybeSay(SquibView v, String group, double chance) {
        maybeSay(v, Lines.get(group), chance);
    }

    static float clamp(float f, float lo, float hi) {
        return f < lo ? lo : f > hi ? hi : f;
    }

    static double rand(double lo, double hi) {
        return lo + rng.nextDouble() * (hi - lo);
    }

    // ================= main tick =================

    static void pose(SquibView v) {
        double t = v.t;
        double dt = Math.min(0.05, Math.max(0, t - lastT));
        lastT = t;
        if (nextAt < 0) {
            nextAt = t + rand(15, 35);
            nextFidgetAt = t + rand(6, 14);
        }
        season(v, t, dt);
        scroll(v);
        Apps.Kind kind = v.kindNow();
        SquibView.Motion motion = v.motion;
        boolean busy = v.dragging || v.petting || v.touching || v.shrimpOn || motion == SquibView.Motion.DRAG
                || motion == SquibView.Motion.FALL || motion == SquibView.Motion.JET || motion == SquibView.Motion.CHASE;
        boolean mc = v.appModes && v.pkg != null && MC.contains(v.pkg);
        if (mc != inMc) {
            inMc = mc;
            end(v);
            torches.clear();
            oreOn = false;
            itemOverride = null;
            if (mc) {
                helmet = rng.nextDouble() < 0.6;
                item = rng.nextInt(ITEMS.length);
                nextItemAt = t + rand(20, 45);
                nextMcAt = t + 2;
                mcAct = 0;
            }
        }
        if (inMc) {
            mcTick(v, t, busy);
            return;
        }
        if (act != NONE) {
            if (busy || kind == Apps.Kind.HIDE || kind == Apps.Kind.WATCH || (act != NAP && motion == SquibView.Motion.SLEEP)) {
                end(v);
            } else {
                update(v, t, dt);
            }
            return;
        }
        if (busy || motion != SquibView.Motion.IDLE || v.seated || kind == Apps.Kind.WATCH || kind == Apps.Kind.HIDE) {
            return;
        }
        if (t >= nextAt) {
            start(v, pick(v, kind));
        } else if (t >= nextFidgetAt) {
            start(v, pickFidget(v));
        }
    }

    static boolean allowed(SquibView v, int a) {
        int where = WHERE[a];
        if (where == FLOOR && v.perched) {
            return false;
        }
        if (where == PERCH && !v.perched) {
            return false;
        }
        if (a == STARGAZE && !night) {
            return false;
        }
        if (a == CONFETTI && !partyDay) {
            return false;
        }
        for (int r : recentActs) {
            if (r == a) {
                return false;
            }
        }
        return true;
    }

    static int pick(SquibView v, Apps.Kind kind) {
        int[] w = new int[CONFETTI + 1];
        int total = 0;
        for (int a = 1; a <= CONFETTI; a++) {
            if (!allowed(v, a)) {
                continue;
            }
            int weight = WEIGHT[a];
            if (kind == Apps.Kind.MUSIC && (a == DANCE || a == GUITAR)) {
                weight *= 5;
            }
            if (kind == Apps.Kind.READ && (a == BOOK || a == TEA)) {
                weight *= 4;
            }
            if (night && a == NAP) {
                weight *= 3;
            }
            w[a] = weight;
            total += weight;
        }
        if (total == 0) {
            return WAVE;
        }
        int n = rng.nextInt(total);
        for (int a = 1; a <= CONFETTI; a++) {
            n -= w[a];
            if (n < 0) {
                return a;
            }
        }
        return WAVE;
    }

    static int pickFidget(SquibView v) {
        int[] options = {STRETCH, SPIN, TRIP, SCRATCH, YAWN};
        int total = 0;
        for (int a : options) {
            total += allowed(v, a) ? WEIGHT[a] : 0;
        }
        if (total == 0) {
            return SPIN;
        }
        int n = rng.nextInt(total);
        for (int a : options) {
            n -= allowed(v, a) ? WEIGHT[a] : 0;
            if (n < 0) {
                return a;
            }
        }
        return SPIN;
    }

    /** The "trick" button: do something right now. */
    static void trick(SquibView v) {
        if (!v.visible()) {
            return;
        }
        if (inMc) {
            mcAct = 0;
            nextMcAt = 0;
            startMc(v, v.t, rng.nextBoolean() ? MC_ORE : MC_TORCH);
            return;
        }
        if (v.kindNow() == Apps.Kind.WATCH) {
            v.say("shh. i'm watching.", 0);
            return;
        }
        end(v);
        if (v.motion == SquibView.Motion.SLEEP || v.motion == SquibView.Motion.WANDER) {
            v.setMotion(SquibView.Motion.IDLE);
        }
        v.seated = false;
        start(v, pick(v, v.kindNow()));
    }

    static void start(SquibView v, int a) {
        double t = v.t;
        float d = v.d;
        act = a;
        actStart = t;
        objOn = false;
        phase = 0;
        count = 0;
        variant = 0;
        boolean quick = a >= STRETCH;
        if (!quick) {
            System.arraycopy(recentActs, 0, recentActs, 1, recentActs.length - 1);
            recentActs[0] = a;
        }
        if (a != SNEEZE) {
            maybeSay(v, NAMES[a], a == WAVE || a == CONFETTI ? 0.8 : quick ? 0.12 : 0.45);
        }
        switch (a) {
            case BALL:
                bx = clamp(center(v) + (rng.nextBoolean() ? -1 : 1) * (40 + rng.nextFloat() * 60) * d,
                        v.minX + v.S * 14, v.maxX + v.S * 14);
                bh = 170 * d;
                bvh = 0;
                bvx = 0;
                kicks = 0;
                kicksWanted = rng.nextInt(4) + 3;
                alpha = 255;
                actEnd = t + 30;
                break;
            case PLANE:
                actEnd = t + 10;
                break;
            case BUBBLES:
                actEnd = t + rand(7, 11);
                nextEvent = t + 0.6;
                break;
            case BOOK:
                actEnd = t + rand(12, 22);
                nextEvent = t + 3;
                flipUntil = 0;
                break;
            case YOYO:
                actEnd = t + rand(7, 11);
                break;
            case DANCE:
                actEnd = t + rand(6, 9);
                nextEvent = t;
                break;
            case WAVE:
                actEnd = t + 2.6;
                break;
            case BUTTERFLY:
                side = rng.nextBoolean() ? -1 : 1;
                ox = side < 0 ? -10 * d : v.screenW + 10 * d;
                oy = v.posY - (60 + rng.nextFloat() * 60) * d;
                objOn = true;
                actEnd = t + 18;
                nextEvent = t + 1.5;
                break;
            case NAP:
                v.setMotion(SquibView.Motion.SLEEP);
                actEnd = t + rand(20, 45);
                break;
            case TEA:
                actEnd = t + rand(9, 14);
                nextEvent = t + 0.5;
                sipAt = t + 2;
                break;
            case GUITAR:
                actEnd = t + rand(7, 11);
                nextEvent = t + 0.4;
                break;
            case JUGGLE:
                actEnd = t + rand(6, 9);
                variant = rng.nextDouble() < 0.3 ? 1 : 0; // 1: drops them at the end
                break;
            case KITE:
                v.facing = center(v) < v.screenW / 2f ? 1 : -1;
                actEnd = t + rand(10, 16);
                break;
            case FISHING:
                v.facing = center(v) < v.screenW / 2f ? 1 : -1; // cast toward the middle of the screen
                variant = rng.nextDouble() < 0.55 ? 0 : rng.nextDouble() < 0.67 ? 1 : 2; // shrimp, fish, boot
                bh = (float) rand(90, 150) * d; // line length
                mark = t + 0.8 + 1.5 + rand(3, 7); // when he gets a bite
                actEnd = t + 30;
                break;
            case RAIN:
                actEnd = t + rand(9, 12);
                nextEvent = t + 0.6;
                break;
            case SKATE:
                count = rng.nextInt(2) + 2; // passes
                phase = 0;
                pickSkateTarget(v);
                actEnd = t + 30;
                break;
            case ICECREAM:
                count = 3; // bites left
                nextEvent = t + 1.4;
                sipAt = t + 1.0;
                actEnd = t + 30;
                break;
            case BALLOON:
                variant = rng.nextInt(5);
                actEnd = t + rand(7, 11);
                nextEvent = t + 1.5;
                break;
            case MAGIC:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                variant = rng.nextInt(4);
                actEnd = t + 5;
                break;
            case STARGAZE:
                actEnd = t + rand(10, 14);
                nextEvent = t + rand(1.5, 2.5);
                break;
            case SNEEZE:
                actEnd = t + 1.5;
                break;
            case JUMPROPE:
                actEnd = t + rand(6, 9);
                count = -1;
                break;
            case PAINT:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                variant = rng.nextInt(Art.PAINTINGS.length);
                order = new int[48];
                for (int i = 0; i < 48; i++) {
                    order[i] = i;
                }
                for (int i = 47; i > 0; i--) {
                    int j = rng.nextInt(i + 1), tmp = order[i];
                    order[i] = order[j];
                    order[j] = tmp;
                }
                mark = t + rand(6, 8); // done painting
                actEnd = mark + 2.5;
                nextEvent = t;
                break;
            case SELFIE:
                actEnd = t + 5;
                break;
            case CONFETTI:
                actEnd = t + 4;
                nextEvent = t + 0.2;
                break;
            case STRETCH:
                v.kick(-2.5);
                actEnd = t + 1.6;
                break;
            case SPIN:
                v.hop();
                actEnd = t + 1.2;
                nextEvent = t;
                break;
            case TRIP:
                v.kick(4);
                v.surpriseFor = 0.6;
                actEnd = t + 1.6;
                break;
            case SCRATCH:
                actEnd = t + 1.8;
                break;
            case YAWN:
                v.kick(-1.5);
                actEnd = t + 2.2;
                break;
        }
    }

    static void end(SquibView v) {
        if (act == NAP && v.motion == SquibView.Motion.SLEEP) {
            v.setMotion(SquibView.Motion.IDLE);
        }
        if (act == TRIP) {
            v.seated = false;
        }
        v.raise = 0;
        if (act != NONE) {
            double t = v.t;
            if (act >= STRETCH) {
                nextFidgetAt = t + rand(fidgetGapMin, Math.max(fidgetGapMin, fidgetGapMax));
                nextAt = Math.max(nextAt, t + 4);
            } else {
                nextAt = t + rand(activityGapMin, Math.max(activityGapMin, activityGapMax));
                nextFidgetAt = Math.max(nextFidgetAt, t + rand(6, 12));
            }
            v.nextDecision = rand(2, 5);
        }
        act = NONE;
        objOn = false;
    }

    static void pickSkateTarget(SquibView v) {
        float span = v.maxX - v.minX;
        float far = Math.min(span * 0.5f, 120 * v.d);
        for (int i = 0; i < 8; i++) {
            float x = v.minX + rng.nextFloat() * span;
            if (Math.abs(x - v.posX) >= far) {
                v.targetX = x;
                return;
            }
        }
        v.targetX = v.posX < v.minX + span / 2 ? v.maxX : v.minX;
    }

    static void update(SquibView v, double t, double dt) {
        float d = v.d;
        float S = v.S;
        float f = (float) dt;
        boolean right = right(v);
        double el = t - actStart;
        v.nextDecision = 5;
        switch (act) {
            case BALL: {
                bvh -= 900 * d * f;
                bh += bvh * f;
                if (bh <= 0) {
                    bh = 0;
                    bvh = bvh < -70 * d ? -bvh * 0.45f : 0;
                }
                bx += bvx * f;
                bvx *= (float) Math.pow(bh == 0 ? 0.3 : 0.85, dt);
                float lo = v.minX + 14 * S, hi = v.maxX + 14 * S;
                if (bx < lo) {
                    bx = lo;
                    bvx = Math.abs(bvx) * 0.6f;
                }
                if (bx > hi) {
                    bx = hi;
                    bvx = -Math.abs(bvx) * 0.6f;
                }
                float gap = bx - center(v);
                if (Math.abs(gap) > 6 * d && v.motion == SquibView.Motion.IDLE) {
                    v.lookX = gap > 0 ? 1 : -1;
                }
                if (t > actEnd) {
                    kicksWanted = kicks;
                }
                if (kicks < kicksWanted) {
                    if (Math.abs(gap) <= 11 * d) {
                        if (bh < 8 * d && Math.abs(bvx) < 40 * d) {
                            int dir = bx < v.screenW / 2f ? 1 : -1;
                            if (rng.nextDouble() < 0.3) {
                                dir = -dir;
                            }
                            bvx = dir * (150 + rng.nextFloat() * 200) * d;
                            bvh = (140 + rng.nextFloat() * 200) * d;
                            v.facing = dir;
                            v.kick(2.5);
                            v.happyFor = 0.6;
                            kicks++;
                            if (v.motion == SquibView.Motion.WANDER) {
                                v.setMotion(SquibView.Motion.IDLE);
                            }
                        }
                    } else {
                        v.targetX = clamp(bx - 14 * S - Math.signum(gap) * 7 * d, v.minX, v.maxX);
                        if (Math.abs(v.targetX - v.posX) > 2 && v.motion == SquibView.Motion.IDLE) {
                            v.setMotion(SquibView.Motion.WANDER);
                        }
                    }
                } else if (Math.abs(bvx) < 15 * d && bh == 0) {
                    alpha -= 300 * f;
                    if (alpha <= 0) {
                        end(v);
                    }
                }
                break;
            }
            case PLANE: {
                if (!objOn) {
                    int dir = center(v) < v.screenW / 2f ? 1 : -1;
                    v.facing = dir;
                    arm(v, dir > 0, 2);
                    v.lookX = dir;
                    if (el > 0.7) {
                        objOn = true;
                        ox = sx(v, handPx(v, dir > 0));
                        oy = sy(v, handPy(v, dir > 0) - 2, 0);
                        ovx = dir * 130 * d;
                        ovy = -70 * d;
                        v.kick(1.5);
                    }
                } else {
                    ovy += 30 * d * f;
                    oy += (ovy + (float) Math.sin(el * 4) * 40 * d) * f;
                    ox += ovx * f;
                    float gap = ox - center(v);
                    v.lookX = Math.abs(gap) < 8 * d ? 0 : gap > 0 ? 1 : -1;
                    v.lookY = oy < v.posY - 22 * S ? -1 : 0;
                    if (ox < -40 * d || ox > v.screenW + 40 * d || oy < -40 * d || t > actEnd) {
                        end(v);
                    }
                }
                break;
            }
            case BUBBLES: {
                arm(v, right, 2);
                v.lookX = v.facing;
                if (t >= nextEvent) {
                    nextEvent = t + rand(0.3, 0.6);
                    int[] g = Art.WAND_GRIP;
                    float x = sx(v, (right ? 1.5f - g[0] : g[0] - 1.5f) + handPx(v, right) + 0.5f);
                    float y = sy(v, handPy(v, right) - g[1] + 1, v.lift());
                    float scale = rng.nextDouble() < 0.3 ? S * 1.5f : S;
                    puff(v, "BUBBLE", x, y, v.facing * (15 + rng.nextFloat() * 30) * d, -(15 + rng.nextFloat() * 20) * d,
                            rng.nextFloat() * 1.5f + 2.5f, -6 * d, scale);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case BOOK: {
                v.armLiftL = 1;
                v.armLiftR = 1;
                v.lookX = 0;
                v.lookY = 1;
                if (t >= nextEvent) {
                    flipUntil = t + 0.35;
                    nextEvent = t + rand(3, 6);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case YOYO: {
                arm(v, right, Math.sin(el * 4.4) > 0.7 ? 1 : 0);
                v.lookX = v.facing;
                v.lookY = 1;
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case DANCE: {
                int beat = (int) (el * 3);
                v.armLiftL = beat % 2 == 0 ? 2 : 0;
                v.armLiftR = 2 - v.armLiftL;
                v.lookX = (beat / 2) % 2 == 0 ? -1 : 1;
                v.face = SquibView.Face.HAPPY;
                if (t >= nextEvent) {
                    nextEvent = t + 0.66;
                    v.hop();
                    if (rng.nextDouble() < 0.6) {
                        v.note();
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case WAVE: {
                arm(v, center(v) < v.screenW / 2f, ((int) (el * 5)) % 2 == 0 ? 2 : 1);
                v.lookX = 0;
                v.lookY = 0;
                v.face = SquibView.Face.HAPPY;
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case BUTTERFLY: {
                float cx = center(v);
                float cy = v.posY - 24 * S;
                if (phase == 0) {
                    float dx = cx - ox, dy = cy - oy;
                    float dist = (float) Math.hypot(dx, dy);
                    float step = 80 * d * f;
                    if (dist < 14 * d) {
                        phase = 1;
                        actStart = t;
                    } else {
                        ox += dx / dist * step;
                        oy += dy / dist * step + f * (float) Math.sin(t * 9) * 30 * d;
                    }
                } else if (phase == 1) {
                    float tx = cx + (float) Math.cos(el * 2.2) * 22 * d;
                    float ty = cy - 6 * d + (float) Math.sin(el * 4.4) * 9 * d;
                    ox += (tx - ox) * Math.min(1, 4 * f);
                    oy += (ty - oy) * Math.min(1, 4 * f);
                    if (t >= nextEvent) {
                        nextEvent = t + rand(1.2, 2.7);
                        v.hop();
                        flipUntil = t + 0.35;
                    }
                    if (t < flipUntil) {
                        v.armLiftL = 2;
                        v.armLiftR = 2;
                    }
                    if (el > 6) {
                        phase = 2;
                        side = rng.nextBoolean() ? -1 : 1;
                    }
                } else {
                    oy -= 70 * d * f;
                    ox += side * 50 * d * f + f * (float) Math.sin(t * 9) * 20 * d;
                    if (oy < -30 * d || ox < -30 * d || ox > v.screenW + 30 * d) {
                        end(v);
                    }
                }
                float gap = ox - center(v);
                v.lookX = Math.abs(gap) < 8 * d ? 0 : gap > 0 ? 1 : -1;
                v.lookY = oy < v.posY - 20 * S ? -1 : 0;
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case NAP: {
                if (v.motion != SquibView.Motion.SLEEP) {
                    end(v);
                } else if (t > actEnd) {
                    v.setMotion(SquibView.Motion.IDLE);
                    end(v);
                }
                break;
            }
            case TEA: {
                boolean sip = t >= sipAt && t < sipAt + 1;
                if (t >= sipAt + 1) {
                    sipAt = t + rand(2.5, 4.5);
                }
                arm(v, right, sip ? 3 : 0);
                if (sip) {
                    v.face = SquibView.Face.HAPPY;
                }
                if (t >= nextEvent) {
                    nextEvent = t + 0.5;
                    puff(v, "STEAM", sx(v, (right ? 3 : -3) + handPx(v, right)), sy(v, handPy(v, right) - 3, v.lift()),
                            (rng.nextFloat() - 0.5f) * 10 * d, -20 * d, 1.4f, -4 * d, S);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case GUITAR: {
                arm(v, right, ((int) (el * 6)) % 2);
                arm(v, !right, 0);
                if (((int) (el / 2)) % 2 == 0) {
                    v.lookX = v.facing;
                    v.lookY = 1;
                } else {
                    v.lookX = 0;
                    v.lookY = 0;
                    v.face = SquibView.Face.HAPPY;
                }
                if (t >= nextEvent) {
                    nextEvent = t + rand(0.45, 0.85);
                    v.note();
                    if (rng.nextDouble() < 0.5) {
                        v.kick(1);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case JUGGLE: {
                int beat = (int) (el * 0.9 * 2 * 3);
                v.armLiftL = beat % 2 == 0 ? 3 : 1;
                v.armLiftR = 4 - v.armLiftL;
                v.lookX = 0;
                v.lookY = -1;
                if (t > actEnd) {
                    if (variant == 1) {
                        for (int i = 0; i < 3; i++) {
                            float[] p = juggle(v, i, el, v.lift());
                            v.spawn(bmp("JUGGLE" + (i + 1))[0], p[0] + S, p[1] + S, (rng.nextFloat() - 0.5f) * 60 * d,
                                    -40 * d, 1.1f, 600 * d, 0, 0.8f, S, true);
                        }
                        v.surpriseFor = 0.8;
                        v.kick(2);
                        maybeSay(v, "JUGGLE_DROP", 0.7);
                    } else {
                        v.happyFor = 0.8;
                    }
                    end(v);
                }
                break;
            }
            case KITE: {
                arm(v, right, 2);
                v.lookX = v.facing;
                v.lookY = -1;
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case FISHING: {
                double castEnd = actStart + 0.8, sinkEnd = castEnd + 1.5;
                v.lookX = v.facing;
                v.lookY = 1;
                if (phase == 0) { // cast
                    arm(v, right, el < 0.55 ? 2 : 0);
                    if (t >= castEnd) {
                        phase = 1;
                        v.kick(1);
                    }
                } else if (phase == 1 || phase == 2) { // sink, then wait
                    arm(v, right, 0);
                    if (t >= sinkEnd) {
                        phase = 2;
                    }
                    if (t >= mark) {
                        phase = 3;
                        nextEvent = t;
                        v.surpriseFor = 0.8;
                        maybeSay(v, "FISHING_BITE", 0.4);
                    }
                } else if (phase == 3) { // bite
                    arm(v, right, 1);
                    if (t - mark > 0.9) {
                        phase = 4;
                        ovy = 0; // reeled in so far
                    }
                } else if (phase == 4) { // reel in
                    arm(v, right, ((int) (t * 10)) % 2 + 1);
                    ovy += 230 * d * f;
                    if (ovy >= bh) {
                        phase = 5;
                        mark = t;
                        if (variant == 0) {
                            v.happyFor = 1.5;
                            for (int i = 0; i < 3; i++) {
                                v.heart();
                            }
                            maybeSay(v, "FISH_SHRIMP", 0.9);
                        } else if (variant == 1) {
                            v.happyFor = 1.0;
                            maybeSay(v, "FISH_FISH", 0.9);
                        } else {
                            v.annoyedFor = 1.2;
                            maybeSay(v, "FISH_BOOT", 0.9);
                        }
                    }
                } else { // show the catch, then let it go
                    arm(v, right, 2);
                    v.lookY = -1;
                    if (t - mark > 1.3) {
                        float[] tip = rodTip(v, v.lift());
                        if (variant != 0) {
                            v.spawn(bmp(variant == 1 ? "FISH" : "BOOT")[right ? 0 : 1], tip[0], tip[1] + 3 * S,
                                    (right ? 1 : -1) * 40 * d, -80 * d, 1.6f, 420 * d, 0, 1, S, true);
                        }
                        end(v);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case RAIN: {
                double umbrellaAt = actStart + 1.6, stopAt = actEnd - 2.6;
                boolean umbrella = t >= umbrellaAt && t < stopAt + 0.4;
                if (t < umbrellaAt) {
                    v.lookY = -1;
                    if (el > 0.7) {
                        v.face = SquibView.Face.ANNOYED;
                    }
                } else if (umbrella) {
                    v.armLiftL = 2;
                    v.armLiftR = 2;
                    v.lookY = 0;
                    v.lookX = 0;
                } else {
                    v.lookY = -1;
                    v.face = SquibView.Face.HAPPY;
                    if (phase == 0) {
                        phase = 1;
                        maybeSay(v, "RAINBOW", 0.6);
                    }
                }
                if (t >= nextEvent && t < stopAt) {
                    nextEvent = t + 0.05;
                    float lift = v.lift();
                    float x = sx(v, 8 + rng.nextFloat() * 12);
                    float y0 = sy(v, top(v) - 17, lift);
                    float y1 = umbrella ? sy(v, top(v) - 8, lift) : (rng.nextBoolean() ? sy(v, top(v), lift) : v.posY);
                    float speed = 140 * d;
                    v.spawn(bmp("DROP")[0], x, y0, 0, speed, Math.max(0.05f, (y1 - y0) / speed), 0, 0, 1, S, true);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case SKATE: {
                v.raise = 3;
                v.face = SquibView.Face.HAPPY;
                if (phase == 0) { // ride
                    float gap = (float) (v.targetX - v.posX);
                    int dir = gap > 0 ? 1 : -1;
                    v.facing = dir;
                    v.lookX = dir;
                    float step = Math.min(Math.abs(gap), 115 * d * f);
                    v.posX += dir * step;
                    v.armLiftL = 1;
                    v.armLiftR = 1;
                    if (Math.abs(gap) < 2) {
                        phase = 1;
                        mark = t;
                        v.hop();
                        v.kick(-1.5);
                    }
                } else { // kickflip, then a short pause
                    v.armLiftL = 2;
                    v.armLiftR = 2;
                    if (t - mark > 1.0) {
                        count--;
                        if (count <= 0) {
                            end(v);
                        } else {
                            phase = 0;
                            pickSkateTarget(v);
                        }
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case ICECREAM: {
                boolean lick = count > 0 && t >= sipAt && t < sipAt + 0.6;
                arm(v, right, lick ? 3 : 0);
                if (lick) {
                    v.face = SquibView.Face.HAPPY;
                }
                if (count > 0 && t >= sipAt + 0.6) {
                    count--;
                    sipAt = t + rand(1.0, 1.8);
                    if (count == 0) {
                        mark = t;
                        v.happyFor = 1.2;
                        v.heart();
                        v.kick(2);
                    }
                }
                if (count > 0 && t >= nextEvent) {
                    nextEvent = t + rand(0.9, 1.6);
                    puff(v, "DRIP", sx(v, handPx(v, right) + 0.5f), sy(v, handPy(v, right) - 1, v.lift()), 0, 10 * d, 0.9f, 300 * d, S);
                }
                if (count == 0 && t - mark > 1.2) {
                    end(v);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case BALLOON: {
                arm(v, right, 2);
                v.lookX = v.facing;
                v.lookY = -1;
                if (t >= nextEvent) {
                    nextEvent = t + rand(1.4, 2.4);
                    v.hop();
                }
                if (t > actEnd) {
                    float[] p = balloon(v, el, v.lift());
                    if (rng.nextDouble() < 0.6) {
                        for (int i = 0; i < 8; i++) {
                            double a = i * Math.PI / 4;
                            puff(v, "SWOOSH", p[0] + 2.5f * S, p[1] + 2.5f * S, (float) Math.cos(a) * 90 * d,
                                    (float) Math.sin(a) * 90 * d, 0.35f, 0, S);
                        }
                        v.surpriseFor = 0.9;
                        v.kick(3);
                        maybeSay(v, "BALLOON_POP", 0.8);
                    } else {
                        v.spawn(bmp(balloonKey(), balloonRows())[0], p[0] + 2.5f * S, p[1] + 3 * S, 15 * d, -55 * d, 5, -4 * d, 0, 1, S, true);
                        maybeSay(v, "BALLOON_BYE", 0.8);
                    }
                    end(v);
                }
                break;
            }
            case MAGIC: {
                boolean r = side > 0;
                v.facing = side;
                v.lookX = side;
                v.lookY = 1;
                float hx = sx(v, mx(r, 28, 9) + 4.5f), hy = sy(v, 15, 0);
                if (phase == 0 && el > 0.1) {
                    phase = 1;
                    poof(v, hx, hy + 2 * S);
                }
                if (el < 2.6) {
                    int tap = (int) (el / 0.33);
                    arm(v, r, tap % 2 == 0 ? 2 : 0);
                    if (tap != count && tap % 2 == 1 && el > 0.6) {
                        count = tap;
                        puff(v, "SPARK", hx + (rng.nextFloat() - 0.5f) * 8 * S, hy - 2 * S, 0, -20 * d, 0.6f, 0, S);
                    }
                } else if (phase == 1) {
                    phase = 2;
                    v.happyFor = 2;
                    v.kick(2);
                    String[] names = {"SHRIMP", "DOVE", "TINY_SQUIB", "FLOWERS"};
                    String name = names[variant];
                    if (variant == 1) {
                        v.spawn(bmp(name)[r ? 0 : 1], hx, hy - 2 * S, side * 50 * d, -90 * d, 2.5f, -10 * d, 0, 1, S, true);
                    } else {
                        v.spawn(bmp(name)[r ? 0 : 1], hx, hy - 2 * S, 0, -150 * d, 1.4f, 240 * d, 0, 1, S, true);
                    }
                    for (int i = 0; i < 5; i++) {
                        puff(v, "SPARK", hx + (rng.nextFloat() - 0.5f) * 12 * S, hy - (rng.nextFloat() * 8) * S, 0, -15 * d, 0.8f, 0, S);
                    }
                    maybeSay(v, "MAGIC_TADA", 0.9);
                } else {
                    v.armLiftL = 2;
                    v.armLiftR = 2;
                    v.face = SquibView.Face.HAPPY;
                    if (phase == 2 && el > 4.3) {
                        phase = 3;
                        poof(v, hx, hy + 2 * S);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case STARGAZE: {
                v.lookX = v.facing;
                v.lookY = 0;
                arm(v, right, 1);
                if (t >= nextEvent) {
                    nextEvent = t + rand(1.8, 3.5);
                    int dir = rng.nextBoolean() ? 1 : -1;
                    float x = center(v) + (rng.nextFloat() - 0.5f) * 200 * d;
                    float y = Math.max(v.statusBar + 30 * d, v.posY - (170 + rng.nextFloat() * 120) * d);
                    v.spawn(bmp("STREAK")[dir > 0 ? 0 : 1], x, y, dir * 170 * d, 70 * d, 0.9f, 0, 0, 1, S, true);
                    if (count == 0) {
                        count = 1;
                        v.happyFor = 1.2;
                        maybeSay(v, "STAR_WISH", 0.8);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case SNEEZE: {
                if (el < 1.4) {
                    v.face = SquibView.Face.SLEEPY;
                    v.lookY = -1;
                    int beat = (int) (el / 0.5);
                    if (beat != count) {
                        count = beat;
                        v.kick(-1.2);
                    }
                    v.armLiftL = el > 0.9 ? 1 : 0;
                    v.armLiftR = v.armLiftL;
                } else {
                    maybeSay(v, "SNEEZE", 0.9);
                    v.jetDir = -v.facing;
                    v.spawnInk(7, 0.8f);
                    v.surpriseFor = 0.7;
                    v.vx = v.jetDir * 230 * d;
                    v.inkJetLeft = 0;
                    v.kick(-2);
                    end(v);
                    v.setMotion(SquibView.Motion.JET);
                }
                break;
            }
            case JUMPROPE: {
                double period = 0.9;
                int cycle = (int) ((el + 0.25) / period);
                if (cycle != count && el > 0.3) {
                    count = cycle;
                    v.hop();
                }
                double ph = (el / period) % 1.0;
                int lift = Math.cos(ph * Math.PI * 2) > 0 ? 1 : 0;
                v.armLiftL = lift;
                v.armLiftR = lift;
                v.lookX = 0;
                v.lookY = 1;
                if (t > actEnd) {
                    v.happyFor = 0.8;
                    end(v);
                }
                break;
            }
            case PAINT: {
                boolean r = side > 0;
                v.facing = side;
                if (t < mark) {
                    v.lookX = side;
                    v.lookY = 0;
                    arm(v, r, ((int) (el * 5)) % 2 + 1);
                    arm(v, !r, 0);
                } else {
                    if (phase == 0) {
                        phase = 1;
                        v.happyFor = 2.5;
                        v.kick(2);
                        maybeSay(v, "PAINT_DONE", 0.85);
                    }
                    v.lookX = 0;
                    v.armLiftL = 2;
                    v.armLiftR = 2;
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case SELFIE: {
                int shot = (int) ((el - 1.2) / 1.4); // 0, 1, 2
                boolean r = right;
                arm(v, r, 3);
                v.lookX = 0;
                v.lookY = 0;
                v.face = SquibView.Face.HAPPY;
                if (shot >= 1) {
                    arm(v, !r, ((int) (el * 5)) % 2 + 1);
                }
                if (el > 1.2 && shot != count - 1 && shot <= 2) {
                    count = shot + 1;
                    float x = sx(v, handPx(v, r) + 0.5f), y = sy(v, handPy(v, r) - 3, v.lift());
                    v.spawn(bmp("FLASH")[0], x, y, 0, 0, 0.35f, 0, 2, 1, S, true);
                    if (shot == 2) {
                        v.hop();
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case CONFETTI: {
                v.face = SquibView.Face.HAPPY;
                v.armLiftL = 2;
                v.armLiftR = 2;
                if (t >= nextEvent && count < 2) {
                    count++;
                    nextEvent = t + 1.5;
                    v.hop();
                    float x = center(v), y = sy(v, top(v) - 2, v.lift());
                    for (int i = 0; i < 26; i++) {
                        String[] c = Art.CONFETTI[rng.nextInt(Art.CONFETTI.length)];
                        v.spawn(bmp("CONFETTI" + c[0], c)[0], x, y, (rng.nextFloat() - 0.5f) * 260 * d,
                                -(120 + rng.nextFloat() * 160) * d, 1.6f + rng.nextFloat(), 260 * d, 0, 0.25f, S, true);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case STRETCH: {
                boolean up = el > 0.15 && el < 1.25;
                v.armLiftL = up ? 3 : 0;
                v.armLiftR = up ? 3 : 0;
                if (up) {
                    v.face = SquibView.Face.SLEEPY;
                }
                if (t > actEnd) {
                    v.kick(1.5);
                    end(v);
                }
                break;
            }
            case SPIN: {
                if (t >= nextEvent) {
                    nextEvent = t + 0.15;
                    v.facing = -v.facing;
                }
                v.lookX = v.facing;
                v.face = SquibView.Face.HAPPY;
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case TRIP: {
                if (el < 0.5) {
                    int flail = ((int) (el * 12)) % 2;
                    v.armLiftL = flail * 2;
                    v.armLiftR = 2 - flail * 2;
                } else if (el < 1.2) {
                    v.seated = true;
                    v.face = el < 0.8 ? SquibView.Face.ANNOYED : SquibView.Face.BLINK;
                } else {
                    v.seated = false;
                    v.face = SquibView.Face.HAPPY;
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case SCRATCH: {
                arm(v, right, ((int) (el * 8)) % 2 + 2);
                v.lookY = -1;
                v.lookX = -v.facing;
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case YAWN: {
                v.face = SquibView.Face.SLEEPY;
                v.armLiftL = el < 1.5 ? 2 : 0;
                v.armLiftR = v.armLiftL;
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            default:
                end(v);
        }
    }

    static void poof(SquibView v, float x, float y) {
        float d = v.d;
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            puff(v, "STEAM", x, y, (float) Math.cos(a) * 40 * d, (float) Math.sin(a) * 40 * d - 10 * d, 0.6f, 0, v.S * 1.5f);
        }
    }

    // ---------- positions used by both update and draw ----------

    /** Juggled balls go round in a loop above his head, hand to hand. */
    static float[] juggle(SquibView v, int i, double el, float lift) {
        double a = el * Math.PI * 2 * 0.9 + i * Math.PI * 2 / 3;
        float gx = 13.5f + (float) Math.cos(a) * 12;
        float gy = (top(v) - 7) + (float) Math.sin(a) * 5.5f;
        return new float[]{sx(v, gx - 1.5f), sy(v, gy - 1.5f, lift)};
    }

    static float[] rodTip(SquibView v, float lift) {
        boolean r = right(v);
        return new float[]{sx(v, handPx(v, r) + (r ? 6.5f : -5.5f)), sy(v, handPy(v, r) - 6 + 0.5f, lift)};
    }

    /** Colour for kite, balloon and fishing strings: mid grey shows on light and dark apps. */
    static final int STRING = 0xFF9A9A9A;

    static final String[] BALLOON_COLORS = {"r", "m", "g", "u", "y"};

    static String balloonKey() {
        return "BALLOON" + variant;
    }

    static String[] balloonRows() {
        String c = BALLOON_COLORS[variant % BALLOON_COLORS.length];
        String dark = c.toUpperCase();
        if (c.equals("y")) {
            dark = "Y";
        }
        String[] r = Art.BALLOON.clone();
        for (int i = 0; i < r.length; i++) {
            r[i] = r[i].replace("R", "#").replace("r", c).replace("#", dark);
        }
        return r;
    }

    static float[] balloon(SquibView v, double el, float lift) {
        boolean r = right(v);
        float gx = handPx(v, r) - 2 + (float) Math.sin(el * 1.8) * 1.5f;
        float gy = handPy(v, r) - 16 + (float) Math.sin(el * 2.6) * 1.0f;
        return new float[]{sx(v, gx), sy(v, gy, lift)};
    }

    static float[] kite(SquibView v, double el, float lift) {
        boolean r = right(v);
        float d = v.d;
        double e = Math.min(1, el / 2.5) * Math.min(1, Math.max(0, (actEnd - v.t) / 1.5));
        float hx = sx(v, handPx(v, r) + 0.5f), hy = sy(v, handPy(v, r), lift);
        float kx = hx + v.facing * (float) (25 + 45 * e) * d + (float) Math.sin(v.t * 1.7) * 8 * d;
        float ky = hy - (float) (15 + 150 * e) * d + (float) Math.sin(v.t * 2.3) * 6 * d;
        ky = Math.max(ky, v.statusBar + 10 * d);
        return new float[]{hx, hy, kx, ky};
    }

    // ================= minecraft =================

    static void mcTick(SquibView v, double t, boolean busy) {
        itemMode = 0;
        for (int i = torches.size() - 1; i >= 0; i--) {
            float[] tr = torches.get(i);
            if (t > tr[2]) {
                torches.remove(i);
                puff(v, "STEAM", tr[0], tr[1] - 6 * v.S, 0, -15 * v.d, 0.8f, 0, v.S);
            } else if (rng.nextDouble() < 0.06) {
                puff(v, "FLAME", tr[0] + (rng.nextFloat() - 0.5f) * v.S, tr[1] - 6.5f * v.S, 0, -25 * v.d, 0.5f, 0, v.S);
            }
        }
        if (busy) {
            mcAct = 0;
            oreOn = false;
            itemOverride = null;
            return;
        }
        if (t >= nextItemAt && mcAct == 0) {
            item = (item + 1 + rng.nextInt(ITEMS.length - 1)) % ITEMS.length;
            nextItemAt = t + rand(20, 50);
            v.kick(1.2);
            maybeSay(v, Lines.MINECRAFT, 0.35);
        }
        if (mcAct == 0 && t >= nextMcAt && v.motion == SquibView.Motion.IDLE) {
            double r = rng.nextDouble();
            int a;
            if (r < 0.14) {
                a = MC_ORE;
            } else if (r < 0.22) {
                a = MC_TORCH;
            } else {
                String name = ITEMS[item];
                if (name.equals("GEM")) {
                    a = MC_SHOW;
                } else if (name.equals("SWORD") || rng.nextDouble() >= 0.65) {
                    a = rng.nextDouble() < 0.7 ? MC_SWING : MC_SHOW;
                } else {
                    a = MC_MINE;
                }
            }
            startMc(v, t, a);
        }
        if (mcAct != 0) {
            mcStep(v, t);
        }
    }

    static void startMc(SquibView v, double t, int a) {
        mcAct = a;
        mcStart = t;
        mcBeat = -1;
        crack = 0;
        switch (a) {
            case MC_SWING:
                mcEnd = t + 0.5;
                break;
            case MC_MINE:
                mcEnd = t + 2.4;
                break;
            case MC_SHOW:
                mcEnd = t + 1.6;
                break;
            case MC_ORE:
                savedItem = item;
                item = 1; // pickaxe
                oreOn = true;
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                mcEnd = t + 4.4;
                maybeSay(v, "ORE", 0.5);
                break;
            case MC_TORCH:
                itemOverride = "TORCH";
                mcEnd = t + 1.4;
                maybeSay(v, "TORCH", 0.4);
                break;
        }
        if (a != MC_ORE && a != MC_TORCH) {
            maybeSay(v, Lines.MINECRAFT, 0.12);
        }
    }

    static void mcStep(SquibView v, double t) {
        boolean right = right(v);
        float d = v.d, S = v.S;
        double el = t - mcStart;
        v.nextDecision = Math.max(v.nextDecision, 2);
        v.lookX = v.facing;
        float hx = sx(v, (right ? 5 : -5) + handPx(v, right));
        float hy = sy(v, handPy(v, right) + 3, v.lift());
        switch (mcAct) {
            case MC_SWING: {
                if (el / 0.5 < 0.4) {
                    arm(v, right, 2);
                    itemMode = 1;
                } else {
                    arm(v, right, 0);
                    itemMode = 2;
                    if (mcBeat < 0) {
                        mcBeat = 1;
                        for (int i = 0; i < 3; i++) {
                            puff(v, "SWOOSH", hx, hy - i * S * 2, v.facing * 60 * d, 0, 0.25f, 0, S);
                        }
                    }
                }
                break;
            }
            case MC_MINE:
            case MC_ORE: {
                double mineFor = mcAct == MC_ORE ? 2.6 : 9;
                if (el < mineFor) {
                    int beat = (int) (el / 0.35);
                    boolean up = el % 0.35 < 0.17;
                    arm(v, right, up ? 2 : 0);
                    itemMode = up ? 1 : 2;
                    v.lookY = 1;
                    if (!up && beat != mcBeat) {
                        mcBeat = beat;
                        crack = Math.min(3, beat / 2);
                        float cx = mcAct == MC_ORE ? sx(v, mx(right, 27, 9) + 4.5f) : hx;
                        float cy = mcAct == MC_ORE ? sy(v, 14, 0) : hy;
                        for (int i = 0; i < 2; i++) {
                            String bit = mcAct == MC_ORE ? (rng.nextBoolean() ? "STONE1" : "STONE2") : (rng.nextBoolean() ? "CRUMB1" : "CRUMB2");
                            puff(v, bit, cx, cy, (rng.nextFloat() - 0.5f) * 80 * d, -(40 + rng.nextFloat() * 60) * d, 0.9f, 400 * d, S);
                        }
                    }
                } else if (oreOn) { // the block breaks and a diamond pops out
                    oreOn = false;
                    float cx = sx(v, mx(right, 27, 9) + 4.5f), cy = sy(v, 15.5f, 0);
                    for (int i = 0; i < 12; i++) {
                        puff(v, rng.nextBoolean() ? "STONE1" : "STONE2", cx + (rng.nextFloat() - 0.5f) * 8 * S, cy + (rng.nextFloat() - 0.5f) * 8 * S,
                                (rng.nextFloat() - 0.5f) * 160 * d, -(60 + rng.nextFloat() * 120) * d, 1.0f, 500 * d, S);
                    }
                    v.spawn(bmp("GEM")[0], cx, cy, -v.facing * 60 * d, -170 * d, 0.6f, 500 * d, 0, 1, S, true);
                    v.kick(2);
                } else { // show off the diamond
                    item = 4;
                    arm(v, right, 3);
                    v.face = SquibView.Face.HAPPY;
                    v.lookY = -1;
                    int beat = (int) (el / 0.3);
                    if (beat != mcBeat) {
                        mcBeat = beat;
                        puff(v, "SPARK", sx(v, (right ? 2 : -2) + handPx(v, right)) + (rng.nextFloat() - 0.5f) * 14 * d,
                                sy(v, handPy(v, right) - 6, v.lift()) + (rng.nextFloat() - 0.5f) * 10 * d, 0, -10 * d, 0.6f, 0, S);
                    }
                }
                break;
            }
            case MC_TORCH: {
                if (el < 0.7) {
                    arm(v, right, 1);
                } else {
                    arm(v, right, 0);
                    if (itemOverride != null) {
                        itemOverride = null;
                        float x = sx(v, handPx(v, right) + (right ? 3 : -3) + 0.5f);
                        if (torches.size() >= 3) {
                            torches.remove(0);
                        }
                        torches.add(new float[]{x, v.posY, (float) (t + rand(25, 40))});
                        v.kick(1);
                    }
                }
                break;
            }
            default: { // MC_SHOW
                arm(v, right, 3);
                v.face = SquibView.Face.HAPPY;
                int beat = (int) (el / 0.3);
                if (beat != mcBeat) {
                    mcBeat = beat;
                    puff(v, "SPARK", sx(v, (right ? 2 : -2) + handPx(v, right)) + (rng.nextFloat() - 0.5f) * 14 * d,
                            sy(v, handPy(v, right) - 6, v.lift()) + (rng.nextFloat() - 0.5f) * 10 * d, 0, -10 * d, 0.6f, 0, S);
                }
            }
        }
        if (t > mcEnd) {
            if (mcAct == MC_ORE) {
                item = savedItem;
                oreOn = false;
            }
            itemOverride = null;
            mcAct = 0;
            nextMcAt = t + rand(3, 9);
        }
    }

    // ================= seasons =================

    static void season(SquibView v, double t, double dt) {
        if (t >= nextSeasonCheck) {
            nextSeasonCheck = t + 60;
            Calendar c = Calendar.getInstance();
            int month = c.get(Calendar.MONTH), day = c.get(Calendar.DAY_OF_MONTH), hour = c.get(Calendar.HOUR_OF_DAY);
            int age = v.day();
            partyDay = (month == Calendar.JANUARY && day == 1) || age == 7 || age == 30 || age == 50
                    || (age >= 100 && age % 50 == 0) || (age > 1 && age % 365 == 0);
            december = month == Calendar.DECEMBER;
            boolean spooky = month == Calendar.OCTOBER && day >= 20;
            night = hour >= 21 || hour < 5;
            hat = partyDay ? "PARTY_HAT" : december ? "WINTER_HAT" : spooky ? "WITCH_HAT" : null;
            seasonLines = partyDay ? Lines.PARTY : december ? Lines.WINTER : spooky ? Lines.SPOOKY : null;
        }
        if (december && snowEvery > 0 && v.visible() && v.kindNow() != Apps.Kind.WATCH && !v.perched) {
            nextSnow -= dt;
            if (nextSnow <= 0) {
                nextSnow = snowEvery * rand(0.5, 1.5);
                float d = v.d;
                v.spawn(bmp("SNOW")[0], center(v) + (rng.nextFloat() - 0.5f) * 320 * d, v.posY - (200 + rng.nextFloat() * 120) * d,
                        (rng.nextFloat() - 0.5f) * 14 * d, 28 * d, 7, 0, 0, 1, v.T, false);
            }
        }
    }

    // ================= drawing =================

    static void draw(SquibView v, Canvas c, float lift) {
        if (!v.visible()) {
            return;
        }
        float S = v.S;
        boolean right = right(v);
        double el = v.t - actStart;
        if (inMc) {
            for (float[] tr : torches) {
                drawBmp(c, bmp("TORCH")[0], Math.round(tr[0] - 1.5f * S), tr[1] - 7 * S, S, 255);
            }
            if (oreOn) {
                float x = mx(right, 27, 9);
                put(v, c, "ORE", 0, x, 11, 0, 255);
                if (crack > 0) {
                    put(v, c, "CRACK" + crack, 0, x, 11, 0, 255);
                }
            }
            if (helmet) {
                put(v, c, "HELMET", 0, (6 - v.squashLevel) - 1, top(v) - 2, lift, 255);
            }
            if (itemOverride != null) {
                held(v, c, lift, itemOverride, right, false, false);
            } else {
                String name = ITEMS[item];
                held(v, c, lift, name, right, !name.equals("GEM") && itemMode == 2, false);
            }
            return;
        }
        if (hat != null && act != RAIN) {
            if (hat.equals("PARTY_HAT")) {
                put(v, c, hat, 0, 10, top(v) - 6, lift, 255);
            } else if (hat.equals("WINTER_HAT")) {
                put(v, c, hat, 0, 7, top(v) - 4, lift, 255);
            } else {
                put(v, c, hat, right ? 0 : 1, 6, top(v) - 6, lift, 255);
            }
        }
        switch (act) {
            case BALL:
                drawBmp(c, bmp("BALL")[0], bx - 2.5f * S, (v.floorY - bh) - 5 * S, S, (int) alpha);
                break;
            case PLANE:
                if (!objOn) {
                    drawBmp(c, bmp("PLANE")[right ? 0 : 1], sx(v, handPx(v, right) - (right ? 1 : 6)), sy(v, handPy(v, right) - 3, lift), S, 255);
                } else {
                    drawBmp(c, bmp("PLANE")[ovx < 0 ? 1 : 0], ox - 3.5f * S, oy - 2 * S, S, 255);
                }
                break;
            case BUBBLES:
                held(v, c, lift, "WAND", right, false, false);
                break;
            case BOOK: {
                String name = v.t < flipUntil ? "BOOK2" : "BOOK";
                put(v, c, name, 0, 9, top(v) + 4, lift, 255);
                break;
            }
            case YOYO: {
                float x = sx(v, handPx(v, right) + 0.5f);
                float y = sy(v, handPy(v, right) + 1, lift);
                float len = (float) (Math.abs(Math.sin(el * 2.2)) * Math.min(Math.max(0, v.floorY - y - 3 * S), 30 * v.d));
                line(c, x, y, x, y + len, thin(v), 0xFF1D1D1D);
                drawBmp(c, bmp("YOYO")[0], x - 1.5f * S, y + len - 1.5f * S, S, 255);
                break;
            }
            case BUTTERFLY:
                if (objOn) {
                    String name = ((int) (v.t * 8)) % 2 == 0 ? "FLY1" : "FLY2";
                    drawBmp(c, bmp(name)[0], ox - 2.5f * S, oy - 1.5f * S, S, 255);
                }
                break;
            case TEA:
                held(v, c, lift, "MUG", right, false, true);
                break;
            case GUITAR:
                put(v, c, "GUITAR", right ? 0 : 1, mx(right, 9, 12), armRow(v) + 1, lift, 255);
                break;
            case JUGGLE:
                for (int i = 0; i < 3; i++) {
                    float[] p = juggle(v, i, el, lift);
                    drawBmp(c, bmp("JUGGLE" + (i + 1))[0], p[0], p[1], S, 255);
                }
                break;
            case KITE: {
                float[] k = kite(v, el, lift);
                // string with a little sag
                float th = thin(v);
                float mxp = (k[0] + k[2]) / 2, myp = (k[1] + k[3]) / 2 + 18 * v.d;
                float px = k[0], py = k[1];
                for (int i = 1; i <= 12; i++) {
                    float u = i / 12f;
                    float qx = (1 - u) * (1 - u) * k[0] + 2 * (1 - u) * u * mxp + u * u * k[2];
                    float qy = (1 - u) * (1 - u) * k[1] + 2 * (1 - u) * u * myp + u * u * (k[3] + 2 * S);
                    line(c, px, py, qx, qy, th, STRING);
                    px = qx;
                    py = qy;
                }
                for (int i = 1; i <= 3; i++) {
                    float tx = k[2] - v.facing * i * 1.5f * S + (float) Math.sin(v.t * 6 + i) * S;
                    float ty = k[3] + (3 + i * 2.5f) * S;
                    drawBmp(c, bmp("BOW")[0], tx - 1.5f * S, ty, S, 255);
                }
                drawBmp(c, bmp("KITE")[0], k[2] - 3.5f * S, k[3] - 3.5f * S, S, 255);
                break;
            }
            case FISHING: {
                held(v, c, lift, "ROD", right, false, false);
                if (phase >= 1) {
                    float[] tip = rodTip(v, lift);
                    double sinkEnd = actStart + 0.8 + 1.5;
                    float len = (float) Math.min(1, (v.t - (actStart + 0.8)) / 1.5) * bh;
                    if (phase == 2) {
                        len += (float) Math.sin(v.t * 3) * 2 * v.d;
                    } else if (phase == 3) {
                        len += (float) Math.abs(Math.sin((v.t - mark) * 14)) * 6 * v.d;
                    } else if (phase >= 4) {
                        len = Math.max(0, bh - ovy);
                    }
                    if (v.t < sinkEnd && phase < 3) {
                        len = Math.max(0, len);
                    }
                    float th = thin(v);
                    if (phase < 5) {
                        line(c, tip[0], tip[1], tip[0], tip[1] + len, th, STRING);
                    }
                    if (phase <= 3) {
                        drawBmp(c, bmp("BOBBER")[0], tip[0] - 0.5f * S, tip[1] + len, S, 255);
                    } else {
                        String name = variant == 0 ? "SHRIMP" : variant == 1 ? "FISH" : "BOOT";
                        Bitmap b = bmp(name)[right ? 0 : 1];
                        float cy = phase == 4 ? tip[1] + len : tip[1] + S;
                        if (!(phase == 5 && variant == 0 && v.t - mark > 0.4)) {
                            drawBmp(c, b, tip[0] - b.getWidth() * S / 2, cy, S, 255);
                        }
                    }
                }
                break;
            }
            case RAIN: {
                double stopAt = actEnd - 2.6;
                float a = (float) Math.min(1, el / 0.6);
                float rise = 0;
                if (v.t > stopAt) {
                    a = (float) Math.max(0, 1 - (v.t - stopAt) / 1.2);
                    rise = (float) (v.t - stopAt) * 6;
                }
                put(v, c, "RAINCLOUD", 0, 7.5f, top(v) - 21 - rise, lift, (int) (a * 255));
                if (v.t >= actStart + 1.6 && v.t < stopAt + 0.4) {
                    put(v, c, "UMBRELLA", 0, 6.5f, top(v) - 8, lift, 255);
                }
                if (v.t > stopAt + 0.3) {
                    float ra = (float) Math.min(1, (v.t - stopAt - 0.3) / 0.5) * (float) Math.min(1, (actEnd - v.t) / 0.6);
                    put(v, c, "RAINBOW", 0, 6.5f, top(v) - 13, lift, (int) (ra * 255));
                }
                break;
            }
            case SKATE: {
                // kickflip: deck, edge, underside, edge, deck
                String name = "BOARD";
                if (phase == 1 && v.t - mark < 0.5) {
                    int step = ((int) ((v.t - mark) / 0.1)) % 4;
                    name = step == 1 || step == 3 ? "BOARD_EDGE" : step == 2 ? "BOARD_FLIP" : "BOARD";
                }
                put(v, c, name, 0, 6, name.equals("BOARD_EDGE") ? 21 : 20, lift, 255);
                break;
            }
            case ICECREAM: {
                if (count > 0) {
                    held(v, c, lift, count == 3 ? "CONE3" : count == 2 ? "CONE2" : "CONE1", right, false, false);
                }
                break;
            }
            case BALLOON: {
                float[] p = balloon(v, el, lift);
                float hx = sx(v, handPx(v, right) + 0.5f), hy = sy(v, handPy(v, right), lift);
                line(c, hx, hy, p[0] + 2.5f * S, p[1] + 6 * S, thin(v), STRING);
                drawBmp(c, bmp(balloonKey(), balloonRows())[0], p[0], p[1], S, 255);
                break;
            }
            case MAGIC: {
                boolean r = side > 0;
                if (phase >= 1 && phase < 3) {
                    put(v, c, "TOPHAT", 0, mx(r, 28, 9), 15, 0, 255);
                }
                if (el < 4.3) {
                    held(v, c, lift, "MAGIC_WAND", r, false, false);
                }
                break;
            }
            case STARGAZE: {
                boolean r = right;
                float ex = r ? 22 : 5, ey = top(v) + 2;
                float tx = r ? 30 : -3, ty = ey - 9;
                gline(v, c, ex + (r ? 1 : -1), ey, tx, ty, 'M', lift);
                gline(v, c, ex + (r ? 2 : -2), ey, tx + (r ? 1 : -1), ty, 'M', lift);
                gline(v, c, ex, ey, ex, ey, 'e', lift);
                float jx = ex + (r ? 4 : -4), jy = ey - 2;
                gline(v, c, jx, jy + 1, jx - 5, 19, 'K', 0);
                gline(v, c, jx, jy + 1, jx + 5, 19, 'K', 0);
                gline(v, c, jx, jy + 1, jx + (r ? 1 : -1), 19, 'k', 0);
                break;
            }
            case JUMPROPE: {
                double ph = (el / 0.9) % 1.0;
                double a = Math.cos(ph * Math.PI * 2);
                float hy = sy(v, armRow(v) + 2, lift);
                float topY = sy(v, top(v) - 4, lift);
                float bottomY = v.posY;
                float lx = sx(v, 2.5f), rx = sx(v, 25.5f);
                float px = lx, py = hy;
                for (int i = 1; i <= 16; i++) {
                    float u = i / 16f;
                    float s = (float) Math.sin(Math.PI * u);
                    float off = a > 0 ? (float) (-a * (hy - topY) * s) : (float) (-a * (bottomY - hy) * s);
                    float qx = lx + (rx - lx) * u, qy = hy + off;
                    line(c, px, py, qx, qy, Math.max(1, v.S / 2), Art.color('r'));
                    px = qx;
                    py = qy;
                }
                break;
            }
            case PAINT: {
                boolean r = side > 0;
                float x0 = mx(r, 28, 10);
                put(v, c, "EASEL", 0, x0, 8, 0, 255);
                String[] pic = Art.PAINTINGS[variant];
                double p = v.t >= mark ? 1 : (v.t - actStart) / (mark - actStart);
                int shown = (int) (p * 48);
                for (int i = 0; i < shown && i < 48; i++) {
                    int px = order[i] % 8, py = order[i] / 8;
                    char ch = pic[py].charAt(px);
                    if (ch != '.') {
                        gline(v, c, x0 + 1 + px, 9 + py, x0 + 1 + px, 9 + py, ch, 0);
                    }
                }
                if (v.t < mark) {
                    held(v, c, lift, "BRUSH", r, false, false);
                }
                break;
            }
            case SELFIE:
                held(v, c, lift, "PHONE", right, false, false);
                break;
        }
    }

    // ================= scroll check-ins =================

    static void scroll(SquibView v) {
        if (v.kindNow() != Apps.Kind.SCROLL) {
            return;
        }
        double secs = v.scrollSecs;
        if (secs < lastScroll) {
            scrollN = 0;
        }
        lastScroll = secs;
        if (!v.reminders) {
            return;
        }
        double every = Math.max(1, scrollCheckMinutes) * 60;
        String[] lines = Lines.SCROLL_LONG;
        if (secs >= (scrollN + 1) * every && lines != null && lines.length > 0) {
            String line = lines[scrollN % lines.length].replace("{mins}", String.valueOf(Math.round(every * (scrollN + 1) / 60)));
            scrollN++;
            v.say(line, 4);
        }
    }
}
