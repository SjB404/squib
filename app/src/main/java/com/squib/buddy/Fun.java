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
            PAINT = 23, SELFIE = 24, CONFETTI = 25, GUM = 26, CAMPFIRE = 27, DRUMS = 28, HULA = 29,
            BOOMERANG = 30, GARDEN = 31, BOWLING = 32, KARATE = 33, WORKOUT = 34, FRIEND = 35, BLOCKS = 36,
            MAIL = 37, MEDITATE = 38, CUBE = 39, PHONE = 40,
            LAST_ACTIVITY = 40,
            // quick fidgets (their own timer)
            STRETCH = 41, SPIN = 42, TRIP = 43, SCRATCH = 44, YAWN = 45, SPINNER = 46, HICCUP = 47, SHAKE = 48,
            WHISTLE = 49, COIN = 50, STRESSBALL = 51, DICE = 52, PEN = 53, CARDS = 54, BUBBLEWRAP = 55, TEXT = 56, TOSS = 57, SPRING = 58,
            FIRST_FIDGET = 41;

    /** Name of each activity. Also the name of its line group in squib.json. */
    static final String[] NAMES = {"NONE", "BALL", "PLANE", "BUBBLES", "BOOK", "YOYO", "DANCE", "WAVE",
            "BUTTERFLY", "NAP", "TEA", "GUITAR", "JUGGLE", "KITE", "FISHING", "RAIN", "SKATE", "ICECREAM",
            "BALLOON", "MAGIC", "STARGAZE", "SNEEZE", "JUMPROPE", "PAINT", "SELFIE", "PARTY",
            "GUM", "CAMPFIRE", "DRUMS", "HULA", "BOOMERANG", "GARDEN", "BOWLING", "KARATE", "WORKOUT", "FRIEND",
            "BLOCKS", "MAIL", "MEDITATE", "CUBE", "PHONE",
            "STRETCH", "SPIN", "TRIP", "SCRATCH", "YAWN", "SPINNER", "HICCUP", "SHAKE", "WHISTLE", "COIN", "STRESSBALL", "DICE", "PEN", "CARDS", "BUBBLEWRAP", "TEXT", "TOSS", "SPRING"};

    static final int ANY = 0, FLOOR = 1, PERCH = 2;
    /** Where each activity can happen: anywhere, only on the floor, or only perched on a cloud. */
    static final int[] WHERE = {ANY, FLOOR, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, FLOOR, PERCH, ANY, FLOOR, ANY, ANY, ANY,
            FLOOR, ANY, FLOOR, ANY, ANY, ANY, ANY, FLOOR, ANY, ANY, ANY, FLOOR, FLOOR, FLOOR, ANY, FLOOR, ANY, ANY, ANY, ANY,
            ANY, ANY, ANY, FLOOR, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, ANY, FLOOR, ANY};
    /** How likely each activity is, before app-mode and time-of-day adjustments. */
    static final int[] WEIGHT = {0, 3, 2, 3, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 6, 1, 2, 2, 2, 2,
            5, 1, 2, 2, 2, 25, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3,
            2, 3, 3, 2, 3, 2, 7, 2, 2, 2, 4, 6, 4, 5, 4, 3, 0, 4, 4};

    // ---------- tunable from squib.json "settings" ----------
    static double scrollCheckMinutes = 15;
    /** Seconds between activities. 1 to 2 hours: at most 1 an hour. */
    static double activityGapMin = 3600;
    static double activityGapMax = 7200;
    /** Seconds between fidgets. 1.5 to 5 minutes, so he fidgets a lot (some fidgets last minutes). */
    static double fidgetGapMin = 90;
    static double fidgetGapMax = 300;
    /**
     * Hours when he sleeps (24-hour clock, can wrap past midnight: 23 to 6 works). Set on the phone in his
     * settings (SquibView.reloadPrefs), not from squib.json. Equal start and end: no bedtime.
     */
    static double bedtimeStart = 0;
    static double bedtimeEnd = 8;

    /**
     * Where we are in the late-night nagging, by the clock: -1 none, 0 the hour before bedtime, 1 the first
     * hour of bedtime, 2 later. He nags from an hour before bedtime until 5 hours into it (or bedtime's end,
     * if that's sooner), so a long bedtime ends with him just sleeping instead of nagging while you get up.
     */
    static int nagPhase() {
        double len = ((bedtimeEnd - bedtimeStart) % 24 + 24) % 24;
        if (len == 0) {
            return -1;
        }
        Calendar c = Calendar.getInstance();
        double h = c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60.0;
        double since = ((h - bedtimeStart) % 24 + 24) % 24; // hours since bedtime started, 0 to 24
        if (since >= 23) {
            return 0;
        }
        if (since < Math.min(len, 5)) {
            return since < 1 ? 1 : 2;
        }
        return -1;
    }
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
    static final boolean[] pins = new boolean[3];
    static final ArrayList<float[]> flowers = new ArrayList<float[]>(); // {x, goneAt}
    static double gap;

    /** The variant asked for in the picker in his settings, or -1 for a random one. Only used inside start(). */
    static int want = -1;

    // ---------- looks for props drawn by Props (one animation, many versions) ----------
    static int[] ballLook = {5, 0, 0, 1};
    static int[][] juggleLooks = new int[0][];
    static int balls = 3;
    static float roll;
    static int spinType, spinBody, spinWeight;
    static double spinAngle, spinVel;
    static Props.Cube cube;
    static final ArrayList<int[]> cubeMoves = new ArrayList<int[]>();
    static int[] lastMove;
    static double lastMoveAt;
    static String cubeKey;
    static Bitmap cubeBmp;
    static int metal, flipsLeft;
    static boolean heads, spinHand, scrambling;
    static double spinToss = -1, nextFlick, sessionEnd;
    static final ArrayList<int[]> pendingSolve = new ArrayList<int[]>();
    static int squeeze;
    static int dieColor, dieFace = 6, rollsLeft;
    /** Pen: body colour pair, clip colour, tip pushed out, when a twirl started (-1: not twirling). */
    static int penColor, penClip;
    static boolean penOut;
    static double penTwirl = -1;
    /** Cards: back colour (a PAIRS index), mode (0 riffle, 1 spring, 2 drops them), which hand a spring starts from, when this round starts. */
    static int cardColor, cardMode;
    static boolean cardFromRight;
    static double cardRound;
    static final int CARD_N = 12;
    static final double RIFFLE_GAP = 0.08, RIFFLE_FLY = 0.2, SPRING_GAP = 0.07, SPRING_FLY = 0.45;

    /** Bubble wrap: bubbles across and down, tint, mode (0 one at a time, 1 row by row, 2 twists it), which are popped. */
    static final String[] WRAP_MODES = {"one at a time", "row by row", "twists it"};
    /** Each tint: sheet, bubble, popped bubble. Solid colours, like the rest of him. */
    static final int[][] WRAP_TINTS = {{0xFFC9DCE6, 0xFFF4FAFF, 0xFFA9BBC6}, {0xFF8FC2EA, 0xFFD6EEFF, 0xFF6C9EC4},
            {0xFFF0AFCB, 0xFFFFE3EF, 0xFFC98AA6}, {0xFFA3D9AC, 0xFFE2FBE6, 0xFF79AE83}, {0xFFEBD994, 0xFFFFF7D6, 0xFFC4B26A}};
    static final String[] WRAP_POP = {"w.w", ".w.", "w.w"};
    static int wrapCols, wrapRows, wrapTint, wrapMode, wrapLeft, wrapHits;
    static final boolean[] wrapPopped = new boolean[40];
    static boolean wrapHand;
    static double wrapPressAt = -1, wrapTwist = -1;

    // ---------- texting: thumbs on his phone, message bubbles going back and forth over his head ----------
    /** Each message: {born, side (1 his, -1 theirs), width (his pixels), kind (0 text, 1 picture, 2 heart, 3 typing dots), seed, stack offset (his pixels, eased), gone at (-1: still up)}. */
    static final ArrayList<double[]> texts = new ArrayList<double[]>();
    /** Bubble colours for his messages: yellow, blue, green, purple, pink, teal. */
    static final int[] TEXT_COLORS = {1, 2, 3, 4, 5, 8};
    static final int[] HEART_ROWS = {0x36, 0x7F, 0x7F, 0x3E, 0x1C, 0x08}; // 7 wide, one bit per pixel
    static boolean autoText;
    static double nextTextAt, typeFrom;
    static int textColor, textPhase, burst;
    /** His phone case: a PAIRS index, picked each time he gets it out (not red, coral or orange: they vanish against him). */
    static int phoneCase = 2;
    static final int[] CASE_COLORS = {1, 2, 3, 4, 5, 8, 9, 10}; // textPhase 0 he types, 1 waits for a reply, 2 they're typing

    // ---------- paper toss: crumples paper and throws it at a bin on the floor ----------
    /** Bin colour (a PAIRS index), bin x on screen, throws left, whether this throw goes in, the paper ball's state (0 none, 1 crumpling, 2 flying, 3 on the floor). */
    static int binColor, tossesLeft, paperState;
    static boolean tossIn;
    static float binX;
    static int tossWant = -1;

    // ---------- spring toy: a coil of rings poured from hand to hand, over his head ----------
    static final int RING_N = 8;
    /** Each ring takes RING_FLY seconds to go over, RING_GAP after the one before, so they stay close like one coil. */
    static final double RING_GAP = 0.08, RING_FLY = 0.9;
    /** Ring colours (PAIRS indexes): bright ones that show against him (no red, coral, orange or pink). */
    static final int[] RING_COLORS = {1, 2, 3, 4, 8};
    static final String[] SPRING_MODES = {"one colour", "striped", "rainbow", "metal", "drops it"};
    /** Mode (an index of SPRING_MODES), pours left before a drop, each ring's colour bottom to top, which hand it pours from, when this pour starts. */
    static int springMode, springDrops;
    static final char[] ringCol = new char[RING_N];
    static boolean springFromRight;
    static double springRound;

    // ---------- headphones: on while music is actually playing in a music app ----------
    /** On while music plays. listenOn / listenOff: when they went on or came off (-1: never), for the animation. */
    static boolean listening, hpSide;
    static int vibe; // vibe 0 nods along, 1 eyes shut, 2 a hand on the headphones
    /** Seconds music has been playing without a break, so a blip can't put them on. */
    static double musicFor, pkgSince;
    /** Last reason the headphones didn't go on, so it's logged once per change. */
    static String blockedWhy;
    /** The app on screen when the current sound started, and whether that app makes its own sound. */
    static String soundFrom;
    static boolean soundFromApp;
    /** The app on screen, and whether it's been quiet in it for a bit (games make sound as soon as they open). */
    static String listenPkg;
    static boolean quietInApp;
    static double listenOn = -1, listenOff = -1, quietFor, nextVibe, vibeEnd, beatGap = 0.5;

    // ---------- phone: scrolls along with you, shows you funny things ----------
    static final String[] FUNNY = {"CAT", "PIGEON", "TOAST", "CAR", "DOG", "RACCOON", "PLANT"};
    static final String[] FUNNY_NAMES = {"cat in a box", "judging pigeon", "toast with a face", "bad parking",
            "dog in sunglasses", "raccoon in a bin", "dead plant"};
    /** Background colour of each picture, and which colours can be swapped (and to what). */
    static final String FUNNY_BG = "qmqhmzp";
    static final String[] FUNNY_FROM = {"Y", "", "", "rR", "kK", "", ""};
    static final String[][] FUNNY_TO = {{"Y", "q", "e", "w", "k"}, null, null,
            {"rR", "mM", "gG", "yY", "uU", "wq", "eo"}, {"kK", "yY", "qh", "wq"}, null, null};
    static boolean autoPhone, showing;
    static double nextPhoneAt, showAt, showStart, feedOff, scrollVel;
    static int feedSeed, funny = -1, lastFunny = -1, cardSide = 1;
    static String funnyTo = "";
    static int[] hulaColors = {4, 1};
    static int[] blockColors = {0, 1, 2, 3};
    static int yoyoColor = 4;

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
    static boolean partyDay, december, night, bedtime;

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
        if (cache.size() > 800) { // made-on-the-fly props add up over days; they're cheap to make again
            cache.clear();
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

    /** A playing card's back, 3 x 4: a dark border round a light middle, in colour pair i. */
    static String[] cardRows(int i) {
        char l = Props.PAIRS[i].charAt(0), k = Props.PAIRS[i].charAt(1);
        String edge = "" + k + k + k, mid = "" + k + l + k;
        return new String[]{edge, mid, mid, edge};
    }

    /** One ring of the spring toy: 4 x 1 grid pixels in one colour. */
    static Bitmap ringBmp(char col) {
        return bmp("RING" + col, new String[]{"" + col + col + col + col})[0];
    }

    /** How many cards to draw for a stack: thicker as it grows. */
    static int stackLayers(int n) {
        return n <= 0 ? 0 : n <= 3 ? 1 : n <= 7 ? 2 : 3;
    }

    /** Draws a card (or a stack of layers, each one pixel higher) centred on grid point x, y. */
    static void cardAt(SquibView v, Canvas c, Bitmap card, float x, float y, int layers, float lift) {
        int gx = Math.round(x - 1.5f), gy = Math.round(y - 2);
        for (int i = 0; i < layers; i++) {
            drawBmp(c, card, sx(v, gx), sy(v, gy - i, lift), v.S, 255);
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
        held(v, c, lift, rows(name), bmp(name), grip(name), right, rotated, artFacesIn);
    }

    static void held(SquibView v, Canvas c, float lift, String[] r, Bitmap[] b, int[] g, boolean right, boolean rotated, boolean artFacesIn) {
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

    /**
     * A random length of time between lo and hi seconds, spread so short ones are common and
     * long ones (minutes) still happen: each doubling of length is about as likely as the last.
     */
    static double span(double lo, double hi) {
        return lo * Math.pow(hi / lo, rng.nextDouble());
    }

    /** A weighted random index that ignores the picker. */
    static int chooseRandom(double... w) {
        int keep = want;
        want = -1;
        int i = chooseW(w);
        want = keep;
        return i;
    }

    /** One of n variants: the one picked in his settings, or a random one. */
    static int choose(int n) {
        return want >= 0 ? want % n : rng.nextInt(n);
    }

    /** Like choose, but random picks follow the weights. */
    static int chooseW(double... w) {
        if (want >= 0) {
            return want % w.length;
        }
        double total = 0;
        for (double x : w) {
            total += x;
        }
        double r = rng.nextDouble() * total;
        for (int i = 0; i < w.length; i++) {
            r -= w[i];
            if (r < 0) {
                return i;
            }
        }
        return w.length - 1;
    }

    /** Name shown in the picker. */
    static String label(int a) {
        switch (a) {
            case CONFETTI: return "party confetti";
            case ICECREAM: return "ice cream";
            case JUMPROPE: return "jump rope";
            case CUBE: return "puzzle cube";
            case SPINNER: return "fidget spinner";
            case COIN: return "coin flip";
            case STRESSBALL: return "stress ball";
            case PHONE: return "phone (shows you something)";
            case DICE: return "dice";
            case PEN: return "clicky pen";
            case CARDS: return "card shuffle";
            case BUBBLEWRAP: return "bubble wrap";
            case TEXT: return "texting";
            case TOSS: return "paper toss";
            case SPRING: return "spring toy";
            case HULA: return "hula hoop";
            case YOYO: return "yo-yo";
            default: return NAMES[a].toLowerCase();
        }
    }

    /** Versions the picker can ask for, or null if there's only one. */
    static String[] variantNames(int a) {
        switch (a) {
            case BALL: return Props.PATTERNS;
            case JUGGLE: return new String[]{"3 balls", "4 balls", "5 balls", "drops them"};
            case FISHING: return new String[]{"shrimp", "fish", "old boot"};
            case BALLOON: return Arrays.copyOf(Props.PAIR_NAMES, Props.BRIGHT);
            case MAGIC: return new String[]{"shrimp", "dove", "tiny squid", "flowers"};
            case PAINT: return new String[]{"shrimp", "sun", "heart", "himself", "landscape"};
            case CAMPFIRE: return new String[]{"toasted", "catches fire"};
            case BOOMERANG: return new String[]{"catches it", "bonks him"};
            case BOWLING: return new String[]{"misses", "strike"};
            case BLOCKS: return new String[]{"stays up", "falls over"};
            case SPINNER: return Props.SPINNERS;
            case CUBE: return new String[]{"solves it", "speed solve", "2x2 cube", "gives up", "messes it up"};
            case COIN: return new String[]{"gold", "silver", "copper", "drops it"};
            case STRESSBALL: return Props.PATTERNS;
            case PHONE: return FUNNY_NAMES;
            case DICE: return Props.DIE_NAMES;
            case PEN: return Props.PAIR_NAMES;
            case CARDS: return new String[]{"riffle", "card spring", "drops them"};
            case BUBBLEWRAP: return WRAP_MODES;
            case TOSS: return new String[]{"gets them in", "misses"};
            case SPRING: return SPRING_MODES;
            default: return null;
        }
    }

    /** Art with some colours swapped: from.charAt(i) becomes to.charAt(i). */
    static Bitmap[] tint(String name, String from, String to) {
        String key = name + "|" + from + ">" + to;
        Bitmap[] b = cache.get(key);
        if (b != null) {
            return b;
        }
        String[] r = rows(name).clone();
        for (int i = 0; i < r.length; i++) {
            StringBuilder sb = new StringBuilder(r[i]);
            for (int k = 0; k < sb.length(); k++) {
                int j = from.indexOf(sb.charAt(k));
                if (j >= 0) {
                    sb.setCharAt(k, to.charAt(j));
                }
            }
            r[i] = sb.toString();
        }
        return bmp(key, r);
    }

    /** Colours of a toy in colour pair i, for tint(): its light and dark. */
    static String pair(int i) {
        return Props.PAIRS[i % Props.PAIRS.length];
    }

    static Bitmap[] ball(int[] look, int rot) {
        return bmp(Props.ballKey(look, rot), Props.ballRows(look, rot));
    }

    /** One grid pixel in any colour. */
    static void dot(SquibView v, Canvas c, float gx, float gy, int color, int a, float lift) {
        ink.setColor(color);
        ink.setAlpha(Math.max(0, Math.min(255, a * (color >>> 24) / 255)));
        float x = sx(v, gx), y = sy(v, gy, lift);
        c.drawRect(x, y, x + v.S, y + v.S, ink);
        ink.setColor(0xFF1D1D1D);
    }

    // ================= main tick =================

    static void pose(SquibView v) {
        double t = v.t;
        double dt = Math.min(0.05, Math.max(0, t - lastT));
        lastT = t;
        if (nextAt < 0) {
            // Pick up where the last run left off, so restarting him can't squeeze in extra activities.
            long now = System.currentTimeMillis();
            double sinceAct = (now - v.prefs.getLong("lastActivityAt", 0)) / 1000.0;
            double sinceFidget = (now - v.prefs.getLong("lastFidgetAt", 0)) / 1000.0;
            nextAt = t + Math.max(60, rand(activityGapMin, Math.max(activityGapMin, activityGapMax)) - sinceAct);
            nextFidgetAt = t + Math.max(30, rand(fidgetGapMin, Math.max(fidgetGapMin, fidgetGapMax)) - sinceFidget);
        }
        for (int i = flowers.size() - 1; i >= 0; i--) {
            if (t > flowers.get(i)[1]) {
                flowers.remove(i);
            }
        }
        season(v, t, dt);
        scroll(v);
        listen(v, t, dt);
        textTick(v, t, dt);
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
            mcTick(v, t, busy || motion == SquibView.Motion.SLEEP);
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
        if (busy || motion != SquibView.Motion.IDLE || v.seated || kind == Apps.Kind.WATCH || kind == Apps.Kind.HIDE || bedtime) {
            return;
        }
        if (t >= nextAt) {
            start(v, pick(v, kind));
        } else if (t >= nextFidgetAt) {
            start(v, pickFidget(v));
        } else if (kind == Apps.Kind.SCROLL && t >= nextPhoneAt && !v.perched) {
            // scrolling apps: he gets his own phone out and scrolls along with you
            autoPhone = true;
            start(v, PHONE);
        } else if (kind == Apps.Kind.CHAT && t >= nextTextAt) {
            // messaging apps: he texts on his own phone, bubbles going back and forth over his head
            autoText = true;
            start(v, TEXT);
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
        int[] w = new int[LAST_ACTIVITY + 1];
        int total = 0;
        for (int a = 1; a <= LAST_ACTIVITY; a++) {
            if (!allowed(v, a)) {
                continue;
            }
            int weight = WEIGHT[a];
            if (kind == Apps.Kind.MUSIC && (a == DANCE || a == GUITAR || a == DRUMS)) {
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
        for (int a = 1; a <= LAST_ACTIVITY; a++) {
            n -= w[a];
            if (n < 0) {
                return a;
            }
        }
        return WAVE;
    }

    /**
     * The random fidget timer. TEXT isn't in it (its weight is 0): he only texts in messaging apps, or when
     * it's picked in his settings.
     */
    static int pickFidget(SquibView v) {
        int[] options = new int[NAMES.length - FIRST_FIDGET];
        for (int i = 0; i < options.length; i++) {
            options[i] = FIRST_FIDGET + i;
        }
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

    /** The picker in his settings: do this one now, as this version (-1: random). */
    static void trick(SquibView v, String name, int var) {
        int a = -1;
        for (int i = 1; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                a = i;
            }
        }
        if (a < 0 || inMc) {
            trick(v);
            return;
        }
        if (!v.visible()) {
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
        int where = WHERE[a];
        if ((where == FLOOR && v.perched) || (where == PERCH && !v.perched)) {
            // Hop to wherever this one needs (a cloud for fishing, the floor for the rest) until it's done.
            poof(v, center(v), v.posY - 8 * v.S);
            v.forcePlace = where == PERCH ? 1 : 2;
            v.place();
            poof(v, center(v), v.posY - 8 * v.S);
        }
        want = var;
        start(v, a);
        want = -1;
    }

    static void start(SquibView v, int a) {
        Trace.i("act", "start " + NAMES[a] + (autoPhone ? " (scroll-along)" : "") + (autoText ? " (chat app)" : "")
                + (want >= 0 ? " version " + want : "") + ", app " + v.pkg + " (" + v.kindNow() + ")");
        double t = v.t;
        float d = v.d;
        act = a;
        actStart = t;
        objOn = false;
        phase = 0;
        count = 0;
        variant = 0;
        boolean quick = a >= FIRST_FIDGET;
        if (!quick && !autoPhone) {
            System.arraycopy(recentActs, 0, recentActs, 1, recentActs.length - 1);
            recentActs[0] = a;
        }
        if (a != SNEEZE && a != TEXT) { // texting: the message bubbles are his talking
            maybeSay(v, NAMES[a], a == WAVE || a == CONFETTI ? 0.8 : quick || autoPhone ? 0.12 : 0.45);
        }
        switch (a) {
            case BALL:
                ballLook = Props.ballLook(rng, 4, 7, want >= 0 ? want % Props.PATTERNS.length : -1);
                roll = 0;
                bx = clamp(center(v) + (rng.nextBoolean() ? -1 : 1) * (40 + rng.nextFloat() * 60) * d,
                        v.minX + v.S * 14, v.maxX + v.S * 14);
                bh = 170 * d;
                bvh = 0;
                bvx = 0;
                kicks = 0;
                kicksWanted = 2 + (int) span(1, 16);
                alpha = 255;
                actEnd = t + 20 + kicksWanted * 6;
                break;
            case PLANE:
                actEnd = t + 10;
                break;
            case BUBBLES:
                actEnd = t + span(7, 70);
                nextEvent = t + 0.6;
                break;
            case BOOK:
                actEnd = t + span(12, 240);
                nextEvent = t + 3;
                flipUntil = 0;
                break;
            case YOYO:
                yoyoColor = rng.nextInt(Props.BRIGHT);
                actEnd = t + span(7, 90);
                break;
            case DANCE:
                actEnd = t + span(6, 75);
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
                actEnd = t + span(20, 300);
                break;
            case TEA:
                actEnd = t + span(9, 120);
                nextEvent = t + 0.5;
                sipAt = t + 2;
                break;
            case GUITAR:
                actEnd = t + span(7, 150);
                nextEvent = t + 0.4;
                break;
            case JUGGLE: {
                actEnd = t + span(6, 100);
                int how = chooseW(0.45, 0.17, 0.08, 0.3); // 3, 4 or 5 balls, or drops them
                variant = how == 3 ? 1 : 0; // 1: drops them at the end
                balls = how < 3 ? 3 + how : 3 + chooseRandom(0.6, 0.3, 0.1);
                int size = balls >= 5 ? 3 : 3 + rng.nextInt(2);
                int pat = rng.nextBoolean() ? rng.nextInt(Props.PATTERNS.length) : -1; // a matching set, or all different
                int first = rng.nextInt(Props.BRIGHT);
                juggleLooks = new int[balls][];
                for (int i = 0; i < balls; i++) {
                    int[] look = Props.ballLook(rng, size, size, pat);
                    look[2] = (first + i * 2) % Props.BRIGHT;
                    if (look[3] == look[2]) {
                        look[3] = (look[2] + 3) % Props.PAIRS.length;
                    }
                    juggleLooks[i] = look;
                }
                break;
            }
            case KITE:
                v.facing = center(v) < v.screenW / 2f ? 1 : -1;
                actEnd = t + span(10, 200);
                break;
            case FISHING:
                v.facing = center(v) < v.screenW / 2f ? 1 : -1; // cast toward the middle of the screen
                variant = chooseW(0.55, 0.3, 0.15); // shrimp, fish, boot
                bh = (float) rand(90, 150) * d; // line length
                mark = t + 0.8 + 1.5 + span(3, 90); // when he gets a bite
                actEnd = mark + 30;
                break;
            case RAIN:
                actEnd = t + rand(9, 12);
                nextEvent = t + 0.6;
                break;
            case SKATE:
                count = 1 + (int) span(1, 9); // passes
                phase = 0;
                pickSkateTarget(v);
                actEnd = t + 15 + count * 10;
                break;
            case ICECREAM:
                count = 3; // bites left
                nextEvent = t + 1.4;
                sipAt = t + 1.0;
                actEnd = t + 30;
                break;
            case BALLOON:
                variant = choose(Props.BRIGHT); // colour
                actEnd = t + rand(7, 11);
                nextEvent = t + 1.5;
                break;
            case MAGIC:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                variant = choose(4);
                actEnd = t + 5;
                break;
            case STARGAZE:
                actEnd = t + span(10, 240);
                nextEvent = t + rand(1.5, 2.5);
                break;
            case SNEEZE:
                actEnd = t + 1.5;
                break;
            case JUMPROPE:
                actEnd = t + span(6, 45);
                count = -1;
                break;
            case PAINT:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                variant = choose(Art.PAINTINGS.length);
                order = new int[48];
                for (int i = 0; i < 48; i++) {
                    order[i] = i;
                }
                for (int i = 47; i > 0; i--) {
                    int j = rng.nextInt(i + 1), tmp = order[i];
                    order[i] = order[j];
                    order[j] = tmp;
                }
                mark = t + span(6, 90); // done painting
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
            case GUM:
                actEnd = t + 7;
                mark = t + rand(3, 4.2); // pop
                break;
            case CAMPFIRE:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                variant = chooseW(0.55, 0.45); // 1: the marshmallow catches fire
                mark = t + 4.5; // toasted
                actEnd = mark + (variant == 1 ? 1.4 : 0) + 2.8;
                nextEvent = t;
                break;
            case DRUMS:
                actEnd = t + span(6, 100);
                count = -1;
                break;
            case HULA:
                hulaColors = Props.twoColors(rng, Props.BRIGHT);
                actEnd = t + span(6, 60);
                nextEvent = t;
                break;
            case BOOMERANG:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                variant = chooseW(0.7, 0.3); // 1: it bonks him on the way back
                actEnd = t + 30;
                break;
            case GARDEN:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                actEnd = t + 11;
                nextEvent = t + 1;
                break;
            case BOWLING:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                variant = chooseW(0.35, 0.65); // 1: strike
                ballLook = Props.ballLook(rng, 4, 4, new int[]{0, 2, 3, 6}[rng.nextInt(4)]);
                roll = 0;
                pins[0] = pins[1] = pins[2] = true;
                ox = clamp(center(v) + side * 150 * d, 20 * d, v.screenW - 20 * d); // pins
                actEnd = t + 30;
                break;
            case KARATE:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                actEnd = t + 3.2;
                break;
            case WORKOUT:
                actEnd = t + 30;
                count = 0;
                break;
            case FRIEND:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                ox = side > 0 ? v.screenW + 40 * d : -40 * d;
                actEnd = t + 30;
                break;
            case BLOCKS:
                side = center(v) < v.screenW / 2f ? 1 : -1;
                v.facing = side;
                variant = chooseW(0.55, 0.45); // 1: the tower falls
                for (int i = 0; i < 4; i++) {
                    blockColors[i] = i == 0 ? rng.nextInt(Props.BRIGHT) : (blockColors[i - 1] + 1 + rng.nextInt(Props.BRIGHT - 1)) % Props.BRIGHT;
                }
                nextEvent = t + 0.6;
                actEnd = t + 30;
                break;
            case MAIL:
                ox = clamp(center(v) + (rng.nextFloat() - 0.5f) * 120 * d, 20 * d, v.screenW - 20 * d);
                oy = Math.max(v.statusBar + 10 * d, v.posY - 320 * d);
                actEnd = t + 30;
                break;
            case MEDITATE:
                v.seated = true;
                actEnd = t + span(9, 200);
                nextEvent = t + 1;
                break;
            case CUBE: {
                side = center(v) < v.screenW / 2f ? 1 : -1;
                variant = chooseW(0.35, 0.15, 0.2, 0.15, 0.15); // solves it, speed solve, 2x2, gives up, messes it up
                cube = new Props.Cube(variant == 2 ? 2 : 3);
                cubeMoves.clear();
                lastMove = null;
                cubeKey = null;
                ArrayList<int[]> scramble = new ArrayList<int[]>();
                int[] prev = null;
                int k = variant == 4 ? 0 : variant == 1 ? 14 + rng.nextInt(6) : variant == 2 ? 6 + rng.nextInt(4) : 12 + rng.nextInt(9);
                for (int i = 0; i < k; i++) {
                    prev = cube.randomMove(rng, prev);
                    cube.turn(prev);
                    scramble.add(prev);
                }
                if (variant <= 2) { // undo the scramble, last move first: that solves it for real
                    for (int i = scramble.size() - 1; i >= 0; i--) {
                        int[] m = scramble.get(i);
                        cubeMoves.add(new int[]{m[0], m[1], -m[2]});
                    }
                } else { // just twists it about
                    int moves = 6 + rng.nextInt(4);
                    for (int i = 0; i < moves; i++) {
                        prev = cube.randomMove(rng, prev);
                        cubeMoves.add(prev);
                    }
                }
                gap = variant == 1 ? 0.16 : rand(0.5, 1.0);
                mark = t + rand(1, 3); // looks at it first
                scrambling = false;
                pendingSolve.clear();
                // How long he keeps at it: solves it again and again, or works at it before giving up.
                sessionEnd = t + (variant == 4 ? 0 : variant == 1 ? span(20, 200) : span(40, 360));
                actEnd = t + 3600; // ends itself
                break;
            }
            case SPINNER:
                spinType = choose(Props.SPINNERS.length);
                int[] sc = Props.twoColors(rng, Props.BRIGHT);
                spinBody = sc[0];
                spinWeight = rng.nextDouble() < 0.3 ? 9 : sc[1];
                spinAngle = rng.nextDouble() * Math.PI * 2;
                spinVel = 0;
                spinHand = right(v);
                spinToss = -1;
                nextFlick = t + 0.3;
                actEnd = t + span(4, 300); // a few seconds, or minutes of it
                break;
            case PHONE:
                phoneCase = CASE_COLORS[rng.nextInt(CASE_COLORS.length)];
                feedSeed = rng.nextInt();
                feedOff = rng.nextInt(1000);
                scrollVel = 0;
                nextEvent = t + rand(0.5, 2);
                showing = false;
                funny = want >= 0 ? want % FUNNY.length : -1;
                if (autoPhone) {
                    actEnd = t + span(40, 400);
                    showAt = t + span(20, 240);
                } else {
                    actEnd = t + span(15, 150);
                    showAt = t + rand(3, 8);
                }
                break;
            case DICE:
                dieColor = choose(Props.DIE_COLORS.length);
                dieFace = 1 + rng.nextInt(6);
                rollsLeft = (int) span(1, 10) - 1; // rolls after this one
                mark = t + rand(0.5, 1.2); // shakes it first
                nextEvent = t;
                actEnd = t + 900;
                break;
            case PEN:
                penColor = choose(Props.PAIRS.length);
                penClip = rng.nextDouble() < 0.5 ? 'q' : Props.PAIRS[penColor].charAt(1);
                penOut = false;
                penTwirl = -1;
                spinHand = right(v);
                nextEvent = t + rand(0.4, 1.0);
                actEnd = t + span(4, 200); // a few clicks, or a few minutes of it
                break;
            case CARDS:
                cardColor = rng.nextInt(Props.BRIGHT);
                cardMode = chooseW(3, 3, 1);
                cardFromRight = right(v);
                cardRound = t + rand(0.4, 0.9);
                actEnd = t + span(5, 240); // a couple of shuffles, or minutes of it
                break;
            case TEXT:
                phoneCase = CASE_COLORS[rng.nextInt(CASE_COLORS.length)];
                textColor = TEXT_COLORS[rng.nextInt(TEXT_COLORS.length)];
                burst = 0;
                if (rng.nextBoolean()) { // he starts it
                    textPhase = 0;
                    typeFrom = t + rand(0.3, 1);
                    nextEvent = typeFrom + rand(0.8, 3);
                } else { // waits on them
                    textPhase = 1;
                    nextEvent = t + rand(0.8, 4);
                }
                actEnd = t + (autoText ? span(20, 300) : span(8, 150));
                break;
            case TOSS:
                side = center(v) < v.screenW / 2f ? 1 : -1; // throws toward the middle of the screen
                v.facing = side;
                binColor = CASE_COLORS[rng.nextInt(CASE_COLORS.length)];
                binX = clamp(center(v) + side * (float) rand(70, 120) * d, 10 * d, v.screenW - 10 * d);
                tossesLeft = (int) span(1, 8);
                tossWant = want;
                paperState = 1;
                mark = t + rand(0.6, 1.2); // crumpling
                actEnd = t + 600;
                break;
            case SPRING: {
                // Colours are data: one pair in light and dark, two pairs striped, a run of colours, or a metal.
                springMode = chooseW(3, 3, 2, 2, 1);
                int ai = rng.nextInt(RING_COLORS.length);
                int ca = RING_COLORS[ai], cb = RING_COLORS[(ai + 1 + rng.nextInt(RING_COLORS.length - 1)) % RING_COLORS.length];
                String metal = Props.METAL_PAIRS[rng.nextInt(2)]; // gold or silver
                int from = rng.nextInt(RING_COLORS.length), step = rng.nextBoolean() ? 1 : RING_COLORS.length - 1;
                for (int i = 0; i < RING_N; i++) {
                    switch (springMode) {
                        case 1: ringCol[i] = Props.light(i % 2 == 0 ? ca : cb); break;
                        case 2: ringCol[i] = Props.light(RING_COLORS[(from + i * step) % RING_COLORS.length]); break;
                        case 3: ringCol[i] = metal.charAt(i % 2); break;
                        default: ringCol[i] = Props.PAIRS[ca].charAt(i % 2); break;
                    }
                }
                springFromRight = right(v);
                springDrops = 1 + rng.nextInt(3);
                springRound = t + rand(0.4, 0.9);
                actEnd = t + span(6, 240); // a few pours, or minutes of it
                break;
            }
            case BUBBLEWRAP:
                wrapMode = chooseW(5, 3, 2);
                newWrap();
                nextEvent = t + rand(0.5, 1.2); // looks it over first
                actEnd = t + span(6, 240); // one sheet, or a few minutes of sheets
                break;
            case STRESSBALL:
                ballLook = Props.ballLook(rng, 5, 6, want >= 0 ? want % Props.PATTERNS.length : -1);
                spinHand = right(v);
                spinToss = -1;
                squeeze = 0;
                nextEvent = t + rand(0.4, 1.0);
                actEnd = t + span(5, 240);
                break;
            case HICCUP:
                count = 0;
                gap = rand(0.55, 0.9);
                actEnd = t + 0.3 + (2 + rng.nextInt(4)) * gap + 0.3;
                break;
            case SHAKE:
                actEnd = t + rand(0.9, 1.7);
                break;
            case WHISTLE:
                actEnd = t + span(1.8, 25);
                nextEvent = t + 0.3;
                gap = rand(0.45, 0.8);
                break;
            case STRETCH:
                v.kick(-2.5);
                actEnd = t + rand(1.3, 2.3);
                break;
            case SPIN:
                v.hop();
                actEnd = t + rand(0.9, 1.7);
                nextEvent = t;
                break;
            case COIN:
                metal = want >= 0 && want < 3 ? want : rng.nextInt(Props.METALS.length);
                variant = want == 3 ? 1 : want >= 0 ? 0 : rng.nextDouble() < 0.2 ? 1 : 0; // 1: drops it
                heads = rng.nextBoolean();
                mark = t + rand(0.35, 0.6); // flick
                gap = rand(0.85, 1.15); // time in the air
                flipsLeft = (int) span(1, 15) - 1; // flips after this one
                actEnd = t + 900;
                break;
            case TRIP:
                v.kick(4);
                v.surpriseFor = 0.6;
                actEnd = t + 1.6;
                break;
            case SCRATCH:
                actEnd = t + rand(1.3, 2.6);
                break;
            case YAWN:
                v.kick(-1.5);
                actEnd = t + rand(1.8, 2.9);
                break;
        }
    }

    /**
     * His phone held up in front of him, seen from the back: a case in this session's colour, the camera in the
     * corner.
     */
    static void phoneBack(SquibView v, Canvas c, float lift) {
        int px = 12, py = armRow(v) + 1;
        char light = Props.PAIRS[phoneCase].charAt(0), dark = Props.PAIRS[phoneCase].charAt(1);
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 5; x++) {
                boolean edge = x == 0 || x == 4 || y == 0 || y == 6;
                char ch = edge ? dark : light;
                if ((x == 1 || x == 2) && (y == 1 || y == 2) && !(x == 2 && y == 2)) {
                    ch = 'o'; // camera lenses
                }
                dot(v, c, px + x, py + y, Art.color(ch), 255, lift);
            }
        }
    }

    /** A message bubble over his head: side 1 his, -1 theirs. kind 0 text, 1 picture, 2 heart, 3 "..." typing. */
    static void addText(SquibView v, int side, int kind) {
        int seed = rng.nextInt(1 << 24);
        int w = kind == 1 || kind == 2 || kind == 3 ? 7 : 5 + rng.nextInt(7); // in his own pixels
        texts.add(new double[]{v.t, side, w, kind, seed, -textH(kind, seed), -1});
    }

    static void dropTyping() {
        for (int i = texts.size() - 1; i >= 0; i--) {
            if (texts.get(i)[3] == 3) {
                texts.remove(i);
            }
        }
    }

    /** Height of a bubble in his pixels (S). */
    static int textH(int kind, int seed) {
        return kind == 1 || kind == 2 ? 6 : kind == 3 ? 3 : seed % 3 == 0 ? 5 : 3;
    }

    /** Moves the bubbles: newest at the bottom, older ones pushed up, then fading. */
    static void textTick(SquibView v, double t, double dt) {
        if (!v.visible() || inMc) {
            texts.clear();
            return;
        }
        double stack = 0;
        int n = 0;
        for (int i = texts.size() - 1; i >= 0; i--) {
            double[] m = texts.get(i);
            m[5] += (stack - m[5]) * Math.min(1, dt * 9);
            stack += textH((int) m[3], (int) m[4]) + 1;
            if (m[6] < 0 && (n >= 3 || t - m[0] > 14)) {
                m[6] = t;
            }
            if (m[6] >= 0 && t - m[6] > 0.5) {
                texts.remove(i);
                continue;
            }
            n++;
        }
    }

    static void rectT(Canvas c, float x0, float y0, float x1, float y1, int color, int a) {
        ink.setColor(color);
        ink.setAlpha(Math.max(0, Math.min(255, a * (color >>> 24) / 255)));
        c.drawRect(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), ink);
        ink.setColor(0xFF1D1D1D);
    }

    /**
     * Draws the bubbles like a chat in a film, his on the right, theirs on the left. Built like he is: his pixel
     * size, square blocks, flat colours, words as solid bars.
     */
    static void drawTexts(SquibView v, Canvas c, float lift) {
        if (texts.isEmpty()) {
            return;
        }
        float S = v.S, cx = center(v);
        float base = sy(v, top(v), lift) - 2 * S;
        double t = v.t;
        for (double[] m : texts) {
            int side = (int) m[1], kind = (int) m[3], seed = (int) m[4];
            int w = (int) m[2], h = textH(kind, seed);
            int a = m[6] >= 0 ? (int) (255 * (1 - (t - m[6]) / 0.5)) : (int) Math.min(255, 255 * (t - m[0]) / 0.15);
            float bottom = base - Math.round(m[5]) * S, top = bottom - h * S;
            float left = side > 0 ? cx + 10 * S - w * S : cx - 10 * S;
            left = Math.round(clamp(left, 4 * v.d, v.screenW - 4 * v.d - w * S));
            float right = left + w * S;
            boolean mine = side > 0;
            int fill = mine ? Art.color(Props.light(textColor)) : Art.color('w');
            int mark = mine ? Art.color(Props.PAIRS[textColor].charAt(1)) : Art.color('q');
            rectT(c, left, top, right, bottom, fill, a);
            float tail = mine ? right - S : left; // one block hanging off the sender's corner
            rectT(c, tail, bottom, tail + S, bottom + S, fill, a);
            switch (kind) {
                case 1: { // a photo: sky, ground, sun
                    int sky = Props.light(new int[]{2, 8, 4, 1}[seed % 4]), ground = Props.light(new int[]{3, 3, 1, 5}[(seed >> 3) % 4]);
                    rectT(c, left + S, top + S, right - S, top + 3 * S, Art.color((char) sky), a);
                    rectT(c, left + S, top + 3 * S, right - S, bottom - S, Art.color((char) ground), a);
                    float sx0 = left + (1 + (seed >> 6) % 4) * S;
                    rectT(c, sx0, top + S, sx0 + S, top + 2 * S, Art.color('w'), a);
                    break;
                }
                case 2: { // a heart
                    String[] heart = {".r.r.", "rrrrr", ".rrr.", "..r.."};
                    for (int y = 0; y < 4; y++) {
                        for (int x = 0; x < 5; x++) {
                            if (heart[y].charAt(x) == 'r') {
                                rectT(c, left + (1 + x) * S, top + (1 + y) * S, left + (2 + x) * S, top + (2 + y) * S, Art.color('r'), a);
                            }
                        }
                    }
                    break;
                }
                case 3: { // "..." while they type: the dots light up in turn
                    int on = (int) (t * 3) % 3;
                    for (int k = 0; k < 3; k++) {
                        float x = left + (1 + k * 2) * S;
                        rectT(c, x, top + S, x + S, top + 2 * S, k == on ? Art.color('h') : mark, a);
                    }
                    break;
                }
                default: { // a line or two of words: solid bars, a gap between words
                    int lines = h == 5 ? 2 : 1;
                    for (int li = 0; li < lines; li++) {
                        float y = top + (1 + li * 2) * S;
                        int len = li == lines - 1 && lines > 1 ? 2 + (seed >> 4) % Math.max(1, w - 4) : w - 2;
                        int gap = len >= 5 ? 1 + (seed >> (8 + li)) % (len - 3) : -1;
                        for (int x = 0; x < len; x++) {
                            if (x != gap) {
                                rectT(c, left + (1 + x) * S, y, left + (2 + x) * S, y + S, mark, a);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    /** A new sheet of bubble wrap: 5 to 9 bubbles across, 2 or 3 down, in a random tint. */
    static void newWrap() {
        wrapCols = 5 + rng.nextInt(5);
        wrapRows = 2 + rng.nextInt(2);
        wrapTint = rng.nextInt(WRAP_TINTS.length);
        wrapLeft = wrapCols * wrapRows;
        wrapHits = 0;
        wrapTwist = -1;
        wrapPressAt = -1;
        Arrays.fill(wrapPopped, false);
    }

    static int randomBubble() {
        int n = rng.nextInt(wrapLeft);
        for (int i = 0; i < wrapCols * wrapRows; i++) {
            if (!wrapPopped[i] && n-- == 0) {
                return i;
            }
        }
        return firstBubble();
    }

    static int firstBubble() {
        for (int i = 0; i < wrapCols * wrapRows; i++) {
            if (!wrapPopped[i]) {
                return i;
            }
        }
        return 0;
    }

    static void popWrap(SquibView v, int i) {
        if (wrapPopped[i]) {
            return;
        }
        wrapPopped[i] = true;
        wrapLeft--;
        wrapHits++;
        int col = i % wrapCols, row = i / wrapCols;
        wrapHand = col * 2 >= wrapCols;
        wrapPressAt = v.t;
        v.kick(wrapTwist >= 0 ? 0.15 : 0.35);
        float gx = (28 - (wrapCols * 2 + 1)) / 2 + 1 + col * 2 + 0.5f, gy = armRow(v) + 1 + 1 + row * 2 + 0.5f;
        float d = v.d;
        v.spawn(bmp("WRAP_POP", WRAP_POP)[0], sx(v, gx), sy(v, gy, v.lift()), (rng.nextFloat() - 0.5f) * 30 * d,
                (-20 - rng.nextFloat() * 25) * d, 0.3f, 0, 0, 1, v.T, true);
    }

    static void end(SquibView v) {
        if (act != NONE) {
            Trace.i("act", "end " + NAMES[act] + " after " + Math.round(v.t - actStart) + "s");
        }
        if (v.forcePlace != 0) { // back to where this app puts him
            v.forcePlace = 0;
            poof(v, center(v), v.posY - 8 * v.S);
            v.place();
            poof(v, center(v), v.posY - 8 * v.S);
        }
        if (act == NAP && v.motion == SquibView.Motion.SLEEP) {
            v.setMotion(SquibView.Motion.IDLE);
        }
        if (act == TRIP || act == MEDITATE) {
            v.seated = false;
        }
        v.raise = 0;
        if (act != NONE) {
            double t = v.t;
            if (act == PHONE && autoPhone) { // a scroll-along session: doesn't count as an activity
                nextPhoneAt = t + rand(15, 60);
            } else if (act == TEXT && autoText) { // same for texting in chat apps
                nextTextAt = t + rand(8, 40);
            } else if (act >= FIRST_FIDGET) {
                nextFidgetAt = t + rand(fidgetGapMin, Math.max(fidgetGapMin, fidgetGapMax));
                nextAt = Math.max(nextAt, t + 4);
                v.prefs.edit().putLong("lastFidgetAt", System.currentTimeMillis()).apply();
            } else {
                nextAt = t + rand(activityGapMin, Math.max(activityGapMin, activityGapMax));
                nextFidgetAt = Math.max(nextFidgetAt, t + rand(6, 12));
                v.prefs.edit().putLong("lastActivityAt", System.currentTimeMillis()).apply();
            }
            v.nextDecision = rand(2, 5);
        }
        if (act == TEXT) {
            dropTyping();
        }
        act = NONE;
        objOn = false;
        autoPhone = false;
        autoText = false;
        showing = false;
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
                roll += bvx * f / (ballLook[0] * S / 2);
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
                int beat = (int) (el * 0.9 * 2 * balls);
                v.armLiftL = beat % 2 == 0 ? 3 : 1;
                v.armLiftR = 4 - v.armLiftL;
                v.lookX = 0;
                v.lookY = -1;
                if (t > actEnd) {
                    if (variant == 1) {
                        for (int i = 0; i < balls; i++) {
                            float[] p = juggle(v, i, el, v.lift());
                            v.spawn(ball(juggleLooks[i], i)[0], p[0], p[1], (rng.nextFloat() - 0.5f) * 60 * d,
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
                boolean up = el > 0.15 && t < actEnd - 0.35;
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
                v.armLiftL = t < actEnd - 0.7 ? 2 : 0;
                v.armLiftR = v.armLiftL;
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case GUM: {
                v.lookX = 0;
                v.lookY = 0;
                if (phase == 0 && t >= mark) {
                    phase = 1;
                    float x = center(v), y = sy(v, top(v) + 5, v.lift());
                    for (int i = 0; i < 10; i++) {
                        double a = i * Math.PI / 5;
                        puff(v, "DRIP", x, y, (float) Math.cos(a) * 110 * d, (float) Math.sin(a) * 110 * d, 0.4f, 0, S);
                    }
                    v.surpriseFor = 0.7;
                    v.kick(2.5);
                    maybeSay(v, "GUM_POP", 0.8);
                }
                if (phase == 1) {
                    v.face = t < mark + 0.7 ? SquibView.Face.SURPRISED : t < mark + 1.8 ? SquibView.Face.ANNOYED : SquibView.Face.HAPPY;
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case CAMPFIRE: {
                boolean r = side > 0;
                v.lookX = side;
                v.lookY = 1;
                if (t >= nextEvent) {
                    nextEvent = t + rand(0.3, 0.7);
                    puff(v, "FLAME", sx(v, mx(r, 28, 7) + 3.5f), sy(v, 15, 0), (rng.nextFloat() - 0.5f) * 15 * d, -35 * d, 0.7f, 0, S);
                }
                double eatAt = mark + (variant == 1 ? 1.4 : 0);
                if (phase == 0 && t >= mark) {
                    phase = 1;
                    if (variant == 1) {
                        v.surpriseFor = 1.0;
                        v.kick(2);
                        maybeSay(v, "MARSHMALLOW_FIRE", 0.8);
                    }
                }
                if (phase == 1 && variant == 1 && t >= mark + 1.0) {
                    float[] tip = stickTip(v, v.lift());
                    puff(v, "STEAM", tip[0], tip[1] - 2 * S, 0, -25 * d, 0.8f, 0, S * 1.5f);
                    variant = 2; // blown out, now burnt
                }
                arm(v, r, t > eatAt + 0.4 && t < eatAt + 1.2 ? 3 : 0);
                if (phase == 1 && t >= eatAt + 1.2) {
                    phase = 2;
                    v.happyFor = 1.5;
                    v.heart();
                    maybeSay(v, "MARSHMALLOW_YUM", 0.8);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case DRUMS: {
                int beat = (int) (el * 5);
                v.armLiftL = beat % 2 == 0 ? 2 : 0;
                v.armLiftR = beat % 2 == 0 ? 0 : 2;
                v.lookY = 1;
                if (((int) el) % 2 == 1) {
                    v.face = SquibView.Face.HAPPY;
                }
                if (beat != count) {
                    count = beat;
                    v.kick(0.8);
                    if (rng.nextDouble() < 0.35) {
                        v.note();
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case HULA: {
                v.face = SquibView.Face.HAPPY;
                v.armLiftL = 2;
                v.armLiftR = 2;
                v.lookX = Math.sin(el * 9) > 0 ? 1 : -1;
                if (t >= nextEvent) {
                    nextEvent = t + 0.35;
                    v.kick(0.7);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case BOOMERANG: {
                boolean r = side > 0;
                v.facing = side;
                if (phase == 0) {
                    arm(v, r, 2);
                    v.lookX = side;
                    if (el > 0.5) {
                        phase = 1;
                        mark = t;
                        objOn = true;
                        v.kick(1.5);
                    }
                } else if (phase == 1) {
                    arm(v, r, 0);
                    float[] p = boomerang(v, t, v.lift());
                    ox = p[0];
                    oy = p[1];
                    float gap = ox - center(v);
                    v.lookX = Math.abs(gap) < 8 * d ? 0 : gap > 0 ? 1 : -1;
                    v.lookY = oy < v.posY - 22 * S ? -1 : 0;
                    if (t - mark >= 2.6) {
                        phase = 2;
                        mark = t;
                        objOn = false;
                        if (variant == 1) {
                            v.surpriseFor = 0.9;
                            v.annoyedFor = 1.2;
                            v.kick(3.5);
                            for (int i = 0; i < 4; i++) {
                                double a = -Math.PI / 2 + (i - 1.5) * 0.6;
                                puff(v, "STAR", center(v), sy(v, top(v), v.lift()), (float) Math.cos(a) * 60 * d, (float) Math.sin(a) * 60 * d, 0.7f, 0, S);
                            }
                            v.spawn(bmp("BOOMERANG")[0], center(v), sy(v, top(v) - 2, v.lift()), side * 50 * d, -60 * d, 1.0f, 500 * d, 0, 1, S, true);
                            maybeSay(v, "BOOMERANG_BONK", 0.8);
                        } else {
                            v.happyFor = 1.0;
                            v.kick(1.5);
                            maybeSay(v, "BOOMERANG_CATCH", 0.5);
                        }
                    }
                } else {
                    if (variant == 0) {
                        arm(v, r, 2);
                    }
                    if (t - mark > 1.0) {
                        end(v);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case GARDEN: {
                boolean r = side > 0;
                v.lookX = side;
                v.lookY = 1;
                boolean watering = el > 1 && el < 6.5;
                arm(v, r, watering ? 1 : 0);
                if (watering && t >= nextEvent) {
                    nextEvent = t + 0.12;
                    float x = sx(v, handPx(v, r) + (r ? 6.5f : -5.5f)), y = sy(v, handPy(v, r) - 1, v.lift());
                    float speed = 120 * d;
                    puff(v, "DROP", x, y, 0, speed, Math.max(0.05f, (v.posY - 2 * S - y) / speed), 0, S * 0.6f);
                }
                if (phase == 0 && el > 7) {
                    phase = 1;
                    v.happyFor = 2;
                    v.heart();
                    v.kick(2);
                    maybeSay(v, "GARDEN_DONE", 0.8);
                }
                if (t > actEnd) {
                    if (flowers.size() >= 3) {
                        flowers.remove(0);
                    }
                    flowers.add(new float[]{sx(v, mx(r, 28, 5)), (float) (t + 600)});
                    end(v);
                }
                break;
            }
            case BOWLING: {
                boolean r = side > 0;
                if (phase == 0) {
                    arm(v, r, el < 0.5 ? 2 : 0);
                    v.lookX = side;
                    if (el > 0.8) {
                        phase = 1;
                        bx = center(v) + side * 12 * S;
                        v.kick(1.5);
                    }
                } else if (phase == 1) {
                    bx += side * 190 * d * f;
                    roll += side * 190 * d * f / (2 * S);
                    v.lookX = side;
                    if ((side > 0 && bx >= ox - 5 * S) || (side < 0 && bx <= ox + 5 * S)) {
                        phase = 2;
                        mark = t;
                        int down = variant == 1 ? 3 : 1 + rng.nextInt(2);
                        for (int i = 0; i < 3 && down > 0; i++) {
                            int k = variant == 1 ? i : rng.nextInt(3);
                            if (pins[k]) {
                                pins[k] = false;
                                down--;
                                v.spawn(bmp("PIN")[0], ox + (k - 1) * 4 * S, v.posY - 3 * S, side * (60 + rng.nextFloat() * 80) * d,
                                        -(120 + rng.nextFloat() * 100) * d, 1.0f, 500 * d, 0, 1, S, true);
                            }
                        }
                        if (variant == 1) {
                            v.happyFor = 2;
                            v.kick(2.5);
                            maybeSay(v, "BOWLING_STRIKE", 0.9);
                        } else {
                            v.annoyedFor = 1.0;
                            maybeSay(v, "BOWLING_MISS", 0.8);
                        }
                    }
                } else {
                    bx += side * 190 * d * f;
                    roll += side * 190 * d * f / (2 * S);
                    if (variant == 1) {
                        v.armLiftL = 2;
                        v.armLiftR = 2;
                    }
                    if (t - mark > 2.2) {
                        end(v);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case KARATE: {
                boolean r = side > 0;
                v.lookX = side;
                v.lookY = 1;
                if (el < 1.2) {
                    v.face = SquibView.Face.ANNOYED;
                    arm(v, r, 3);
                } else {
                    if (phase == 0) {
                        phase = 1;
                        float x = sx(v, mx(r, 27, 9) + 4.5f), y = sy(v, 15.5f, 0);
                        v.spawn(bmp("HALF_PLANK")[0], x - 2 * S, y, -60 * d, -160 * d, 1.0f, 500 * d, 0, 1, S, true);
                        v.spawn(bmp("HALF_PLANK")[1], x + 2 * S, y, 60 * d, -160 * d, 1.0f, 500 * d, 0, 1, S, true);
                        for (int i = 0; i < 6; i++) {
                            puff(v, "CRUMB1", x, y, (rng.nextFloat() - 0.5f) * 140 * d, -(40 + rng.nextFloat() * 90) * d, 0.8f, 400 * d, S);
                        }
                        v.kick(2.5);
                        maybeSay(v, "KARATE", 0.85);
                    }
                    arm(v, r, 0);
                    if (el > 1.6) {
                        v.face = SquibView.Face.HAPPY;
                        v.armLiftL = 2;
                        v.armLiftR = 2;
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case WORKOUT: {
                int rep = (int) (el / 0.75);
                boolean r = rep < 4 ? right : !right;
                boolean up = (el % 0.75) > 0.35;
                arm(v, r, up ? 3 : 0);
                arm(v, !r, 0);
                v.face = up ? SquibView.Face.ANNOYED : SquibView.Face.OPEN;
                if (rep != count) {
                    count = rep;
                    puff(v, "DROP", sx(v, r ? 20 : 7), sy(v, top(v), v.lift()), (r ? 1 : -1) * 50 * d, -60 * d, 0.6f, 300 * d, S * 0.6f);
                }
                if (rep >= 8) {
                    v.happyFor = 1.2;
                    v.kick(2);
                    end(v);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case FRIEND: {
                float stop = center(v) + side * 26 * S;
                if (phase == 0) { // walking in
                    float dir = stop > ox ? 1 : -1;
                    ox += dir * 70 * d * f;
                    v.lookX = side;
                    if ((dir > 0 && ox >= stop) || (dir < 0 && ox <= stop)) {
                        ox = stop;
                        phase = 1;
                        mark = t;
                        v.happyFor = 2.5;
                        maybeSay(v, "FRIEND", 0.9);
                    }
                } else if (phase == 1) { // saying hi
                    arm(v, side > 0, ((int) ((t - mark) * 5)) % 2 == 0 ? 2 : 1);
                    v.lookX = side;
                    if (t >= nextEvent) {
                        nextEvent = t + 0.8;
                        v.hop();
                        v.heart();
                        v.spawn(bmp("HEART_SMALL", new String[]{".r.r.", "rrrrr", ".rrr.", "..r.."})[0], ox, v.posY - 12 * S,
                                0, -40 * d, 1.2f, -8 * d, 0, 0.5f, v.T, true);
                    }
                    if (t - mark > 3) {
                        phase = 2;
                        maybeSay(v, "FRIEND_BYE", 0.6);
                    }
                } else { // walking off
                    float dir = side;
                    ox += dir * 70 * d * f;
                    arm(v, side > 0, ((int) (t * 5)) % 2 == 0 ? 2 : 1);
                    v.lookX = side;
                    if (ox < -40 * d || ox > v.screenW + 40 * d) {
                        end(v);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case BLOCKS: {
                boolean r = side > 0;
                v.lookX = side;
                if (count < 4) {
                    arm(v, r, Math.min(3, count));
                    v.lookY = 1;
                    if (t >= nextEvent) {
                        count++;
                        nextEvent = t + 0.9;
                        v.kick(0.8);
                        if (count == 4) {
                            mark = t;
                        }
                    }
                } else if (phase == 0) {
                    arm(v, r, 0);
                    v.lookY = -1;
                    if (t - mark > 1.6) {
                        phase = 1;
                        mark = t;
                        if (variant == 1) {
                            float x0 = mx(r, 28, 3);
                            for (int i = 0; i < 4; i++) {
                                String ch = String.valueOf(Props.light(blockColors[i]));
                                v.spawn(bmp("BLOCK" + ch, block(ch))[0], sx(v, x0 + 1.5f), sy(v, 18.5f - i * 3, 0),
                                        side * (30 + i * 35) * d, -(30 + i * 20) * d, 1.2f, 500 * d, 0, 1, S, true);
                            }
                            v.surpriseFor = 0.8;
                            maybeSay(v, "BLOCKS_FALL", 0.8);
                        } else {
                            v.happyFor = 1.5;
                            maybeSay(v, "BLOCKS_DONE", 0.7);
                        }
                    }
                } else {
                    if (variant == 0) {
                        v.armLiftL = 2;
                        v.armLiftR = 2;
                    }
                    if (t - mark > 1.5) {
                        end(v);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case MAIL: {
                if (phase == 0) {
                    oy += 50 * d * f;
                    ox += (float) Math.sin(t * 3) * 35 * d * f;
                    float gap = ox - center(v);
                    v.lookX = Math.abs(gap) < 8 * d ? 0 : gap > 0 ? 1 : -1;
                    v.lookY = -1;
                    float catchY = sy(v, top(v) - 6, v.lift());
                    float targetX = center(v);
                    ox += (targetX - ox) * Math.min(1, 1.5f * f);
                    if (oy >= catchY) {
                        phase = 1;
                        mark = t;
                        v.happyFor = 1;
                        v.kick(1.5);
                        maybeSay(v, "MAIL", 0.6);
                    }
                } else {
                    v.armLiftL = 1;
                    v.armLiftR = 1;
                    v.lookX = 0;
                    v.lookY = 1;
                    if (phase == 1 && t - mark > 1.4) {
                        phase = 2;
                        v.speech = null;
                        maybeSay(v, "MAIL_READ", 0.95);
                    }
                    if (t - mark > 5.5) {
                        v.happyFor = 1.2;
                        v.heart();
                        end(v);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case MEDITATE: {
                v.seated = true;
                v.face = SquibView.Face.SLEEPY;
                v.armLiftL = 1;
                v.armLiftR = 1;
                double left = actEnd - t;
                int up = el < 0.8 ? 1 : el < 1.6 ? 2 : 3;
                if (left < 1.6) {
                    up = left < 0.8 ? 1 : 2;
                }
                if (up == 3 && ((int) (el / 0.9)) % 2 == 1) {
                    up = 4;
                }
                v.raise = up;
                if (t >= nextEvent) {
                    nextEvent = t + rand(0.5, 1.0);
                    puff(v, "SPARK", center(v) + (rng.nextFloat() - 0.5f) * 30 * S, v.posY - rng.nextFloat() * 14 * S, 0, -12 * d, 0.9f, 0, S);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case SPINNER: {
                boolean h = spinHand;
                arm(v, h, 1);
                arm(v, !h, 0);
                v.lookX = h ? 1 : -1;
                v.lookY = -1;
                boolean wrapping = t > actEnd;
                if (spinToss >= 0) { // tossed up, spinning, comes back down
                    double p = (t - spinToss) / 0.9;
                    arm(v, h, p < 0.2 || p > 0.8 ? 2 : 1);
                    if (p >= 1) {
                        spinToss = -1;
                        v.kick(0.8);
                    }
                } else if (spinVel < 3 && t >= nextFlick && !wrapping) {
                    double r = rng.nextDouble();
                    if (r < 0.15) { // over to the other hand
                        spinHand = !spinHand;
                        v.kick(0.6);
                    } else if (r < 0.3 && spinVel > 0) {
                        spinToss = t;
                        v.kick(1.2);
                    }
                    spinVel = rand(18, 32);
                    arm(v, h, 2);
                    v.kick(0.8);
                    nextFlick = 1e18;
                } else if (spinVel < 3 && nextFlick > 1e17 && !wrapping) {
                    nextFlick = t + span(0.3, 6); // stops to look at it, or gets straight back to it
                }
                if (spinVel < 3 && nextFlick > t + 2) {
                    v.lookY = 0; // staring into space between flicks
                    v.lookX = 0;
                }
                spinVel *= Math.exp(-0.45 * dt);
                if (spinVel < 0.4) {
                    spinVel = 0;
                }
                spinAngle += spinVel * dt * (h ? 1 : -1);
                if (spinVel > 8 && spinVel < 22) {
                    v.face = SquibView.Face.HAPPY;
                }
                if ((wrapping && spinVel < 5 && spinToss < 0) || t > actEnd + 12) {
                    end(v);
                }
                break;
            }
            case PHONE: {
                if (autoPhone && v.kindNow() != Apps.Kind.SCROLL) {
                    end(v);
                    break;
                }
                if (showing) { // holding the picture out to you
                    arm(v, cardSide > 0, 2);
                    arm(v, cardSide < 0, 0);
                    v.lookX = cardSide;
                    v.lookY = 0;
                    if (t - showStart > 1.2) {
                        v.lookX = 0; // checks you're looking
                    }
                    if (t - showStart > 7) {
                        showing = false;
                        funny = -1;
                        showAt = autoPhone ? t + span(40, 400) : rng.nextDouble() < 0.35 ? t + rand(6, 20) : 1e18;
                        nextEvent = t + rand(0.5, 1.5);
                    }
                    break;
                }
                v.armLiftL = 1;
                v.armLiftR = 1;
                v.lookX = 0;
                v.lookY = 1;
                if (t >= nextEvent) { // thumb flick
                    scrollVel = rand(6, 16);
                    arm(v, right, 2);
                    nextEvent = t + (rng.nextDouble() < 0.25 ? span(2, 12) : rand(0.6, 2.2)); // sometimes stops on a post
                    double r = rng.nextDouble();
                    if (r < 0.04) {
                        v.happyFor = 1;
                    } else if (r < 0.07) {
                        v.annoyedFor = 1;
                    } else if (r < 0.09) {
                        v.surpriseFor = 0.6;
                    }
                }
                feedOff += scrollVel * dt;
                scrollVel *= Math.exp(-3 * dt);
                if (t >= showAt && v.speech == null) {
                    showing = true;
                    showStart = t;
                    if (funny < 0) {
                        funny = rng.nextInt(FUNNY.length);
                        if (funny == lastFunny) {
                            funny = (funny + 1 + rng.nextInt(FUNNY.length - 1)) % FUNNY.length;
                        }
                    }
                    lastFunny = funny;
                    String[] to = FUNNY_TO[funny];
                    funnyTo = to == null ? "" : to[rng.nextInt(to.length)];
                    cardSide = center(v) < v.screenW / 2f ? 1 : -1;
                    v.facing = cardSide;
                    v.happyFor = 1.5;
                    v.kick(1.2);
                    maybeSay(v, "FUNNY_" + FUNNY[funny], 0.95);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case DICE: {
                if (phase == 0) { // shaking it in his hand
                    arm(v, right, 1 + ((int) (el * 10)) % 2);
                    v.lookX = v.facing;
                    if (t >= nextEvent) {
                        nextEvent = t + 0.1;
                        dieFace = 1 + rng.nextInt(6);
                    }
                    if (t >= mark) {
                        phase = 1;
                        ox = sx(v, handPx(v, right) + 0.5f);
                        oy = sy(v, handPy(v, right) - 1, v.lift());
                        ovx = v.facing * (float) rand(25, 55) * d;
                        ovy = -(float) rand(110, 170) * d;
                        arm(v, right, 2);
                        v.kick(1);
                    }
                } else if (phase == 1) { // tumbling
                    arm(v, right, 0);
                    ovy += 900 * d * f;
                    ox += ovx * f;
                    oy += ovy * f;
                    ox = clamp(ox, 4 * S, v.screenW - 4 * S);
                    if (t >= nextEvent) {
                        nextEvent = t + 0.07;
                        dieFace = 1 + rng.nextInt(6);
                    }
                    float gx = ox - center(v);
                    v.lookX = Math.abs(gx) < 6 * d ? 0 : gx > 0 ? 1 : -1;
                    v.lookY = 1;
                    if (oy >= v.floorY - 2.5f * S) {
                        oy = v.floorY - 2.5f * S;
                        ovy = -ovy * 0.45f;
                        ovx *= 0.55f;
                        if (Math.abs(ovy) < 60 * d) {
                            phase = 2;
                            mark = t;
                            dieFace = 1 + rng.nextInt(6);
                            if (dieFace == 6) {
                                v.happyFor = 1.5;
                                v.hop();
                                maybeSay(v, "DICE_SIX", 0.5);
                            } else if (dieFace == 1) {
                                v.annoyedFor = 1.5;
                                maybeSay(v, "DICE_ONE", 0.5);
                            }
                        }
                    }
                } else { // looks at what he rolled
                    float gx = ox - center(v);
                    v.lookX = Math.abs(gx) < 6 * d ? 0 : gx > 0 ? 1 : -1;
                    v.lookY = 1;
                    if (t - mark > 1.6) {
                        if (rollsLeft > 0) { // picks it up and goes again
                            rollsLeft--;
                            phase = 0;
                            mark = t + span(0.5, 5);
                        } else {
                            end(v);
                        }
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case PEN: {
                boolean h = spinHand;
                v.lookX = h ? 1 : -1;
                arm(v, !h, 0);
                if (penTwirl >= 0) { // spins it around his fingers
                    arm(v, h, 2);
                    v.lookY = -1;
                    if (t - penTwirl >= 1.0) {
                        penTwirl = -1;
                        v.kick(0.5);
                        if (rng.nextDouble() < 0.3) {
                            spinHand = !spinHand; // passes it to the other hand
                        }
                    }
                } else {
                    arm(v, h, 1);
                    v.lookY = 0;
                    if (t >= nextEvent) {
                        if (!penOut && rng.nextDouble() < 0.1) {
                            penTwirl = t;
                            nextEvent = t + 1.0 + rand(0.3, 1.2);
                        } else {
                            penOut = !penOut;
                            v.kick(penOut ? 0.25f : -0.25f);
                            // bursts of fast clicking, then a pause
                            nextEvent = t + (rng.nextDouble() < 0.2 ? span(0.6, 6) : rand(0.1, 0.3));
                        }
                    }
                }
                if (t > actEnd && penTwirl < 0) {
                    end(v);
                }
                break;
            }
            case TEXT: {
                if (autoText && v.kindNow() != Apps.Kind.CHAT) {
                    end(v);
                    break;
                }
                v.lookX = 0;
                v.lookY = 1;
                arm(v, false, 1);
                arm(v, true, 1);
                if (textPhase == 0) { // reads, then thumbs going
                    if (t >= typeFrom) {
                        int k = ((int) (t * 9)) % 2;
                        arm(v, false, 1 + k);
                        arm(v, true, 2 - k);
                    }
                    if (t >= nextEvent) { // sends it
                        double r = rng.nextDouble();
                        addText(v, 1, r < 0.07 ? 2 : r < 0.13 ? 1 : 0);
                        v.kick(0.6);
                        if (burst < 3 && rng.nextDouble() < 0.3) { // and another one
                            burst++;
                            typeFrom = t + 0.2;
                            nextEvent = t + rand(0.6, 2);
                        } else {
                            burst = 0;
                            textPhase = 1;
                            nextEvent = t + (rng.nextDouble() < 0.2 ? span(4, 25) : rand(1, 5));
                        }
                    }
                } else if (textPhase == 1) { // waiting on a reply
                    if (t - typeFrom > 6 && ((int) (t / 2.5)) % 3 == 0) {
                        v.lookY = 0; // glances up while it takes a while
                    }
                    if (t >= nextEvent) {
                        if (t > actEnd) {
                            end(v);
                            break;
                        }
                        textPhase = 2;
                        addText(v, -1, 3);
                        nextEvent = t + rand(1, 4);
                    }
                } else if (t >= nextEvent) { // their "..." turns into a message
                    dropTyping();
                    double r = rng.nextDouble();
                    addText(v, -1, r < 0.08 ? 2 : r < 0.16 ? 1 : 0);
                    r = rng.nextDouble();
                    if (r < 0.25) {
                        v.happyFor = 1;
                    } else if (r < 0.32) {
                        v.surpriseFor = 0.6;
                    } else if (r < 0.36) {
                        v.annoyedFor = 0.8;
                    }
                    if (burst < 2 && rng.nextDouble() < 0.3) { // they're sending more
                        burst++;
                        addText(v, -1, 3);
                        nextEvent = t + rand(0.8, 2.5);
                    } else if (t > actEnd && rng.nextDouble() < 0.6) {
                        end(v); // leaves it there
                    } else {
                        burst = 0;
                        textPhase = 0;
                        typeFrom = t + rand(0.6, 2.5);
                        nextEvent = typeFrom + rand(0.8, 3.5);
                    }
                }
                break;
            }
            case TOSS: {
                boolean r = side > 0;
                v.lookX = side;
                float g = 900 * d;
                if (paperState == 1) { // scrunching it up in both hands
                    int k = ((int) (t * 8)) % 2;
                    arm(v, false, 1 + k);
                    arm(v, true, 2 - k);
                    v.lookY = 1;
                    if (t >= mark) { // throws: aimed to land in the bin, or not
                        tossIn = tossWant == 0 || (tossWant < 0 && rng.nextDouble() < 0.6);
                        ox = sx(v, handPx(v, r) + 0.5f);
                        oy = sy(v, handPy(v, r) - 1, v.lift());
                        float tx = binX + (tossIn ? 0 : (rng.nextBoolean() ? 1 : -1) * (float) rand(6, 9) * S);
                        float ty = v.posY - 6 * S; // top of the bin
                        float time = (float) rand(0.6, 0.85);
                        ovx = (tx - ox) / time;
                        ovy = (ty - oy - 0.5f * g * time * time) / time;
                        paperState = 2;
                        arm(v, r, 2);
                        v.kick(1);
                    }
                } else if (paperState == 2) { // in the air
                    arm(v, r, 2);
                    ovy += g * f;
                    ox += ovx * f;
                    oy += ovy * f;
                    float binTop = v.posY - 6 * S;
                    if (ovy > 0 && oy >= binTop && Math.abs(ox - binX) <= 2.5f * S) { // in
                        paperState = 0;
                        v.happyFor = 1.2;
                        v.kick(1.5);
                        maybeSay(v, "TOSS_IN", 0.2);
                        mark = t + rand(0.8, 1.6);
                    } else if (oy >= v.posY - S) { // on the floor
                        oy = v.posY - S;
                        paperState = 3;
                        v.annoyedFor = 0.8;
                        maybeSay(v, "TOSS_MISS", 0.2);
                        mark = t + rand(1.2, 2.0);
                    }
                } else if (t >= mark) { // the next one, or done
                    tossesLeft--;
                    if (tossesLeft <= 0) {
                        end(v);
                        break;
                    }
                    paperState = 1;
                    mark = t + rand(0.5, 1.1);
                }
                break;
            }
            case BUBBLEWRAP: {
                v.lookX = 0;
                v.lookY = 1;
                boolean twisting = wrapTwist >= 0;
                if (twisting) { // wrings it out: hands going opposite ways, bubbles going off fast
                    int k = ((int) ((t - wrapTwist) * 10)) % 2;
                    arm(v, false, 1 + k);
                    arm(v, true, 2 - k);
                } else {
                    arm(v, false, 1);
                    arm(v, true, 1);
                    if (wrapPressAt >= 0 && t - wrapPressAt < 0.16) {
                        arm(v, wrapHand, 2); // the hand that just pressed one
                    }
                }
                if (t < nextEvent) {
                    break;
                }
                if (wrapLeft == 0) {
                    wrapTwist = -1;
                    if (t > actEnd) {
                        maybeSay(v, "BUBBLEWRAP_DONE", 0.25);
                        end(v);
                        break;
                    }
                    newWrap(); // gets another sheet out
                    v.kick(0.6);
                    nextEvent = t + rand(0.8, 1.8);
                    break;
                }
                if (twisting) {
                    popWrap(v, randomBubble());
                    nextEvent = t + rand(0.03, 0.09);
                    break;
                }
                if (wrapMode == 2 && wrapHits >= 3) { // had enough of one at a time
                    wrapTwist = t;
                    v.kick(-1);
                    nextEvent = t + 0.25;
                    break;
                }
                popWrap(v, wrapMode == 1 ? firstBubble() : randomBubble());
                if (wrapMode == 1) {
                    nextEvent = t + rand(0.15, 0.32);
                } else {
                    nextEvent = t + (rng.nextDouble() < 0.1 ? span(1, 8) : rand(0.25, 0.9)); // sometimes stops to look
                }
                break;
            }
            case CARDS: {
                double rt = t - cardRound;
                if (cardMode == 0) { // riffle: a half in each hand, falling together in the middle
                    arm(v, false, 1);
                    arm(v, true, 1);
                    v.lookX = 0;
                    v.lookY = 1;
                    if (rt > (CARD_N - 1) * RIFFLE_GAP + RIFFLE_FLY + 0.4) {
                        v.kick(0.4f); // squares the deck up and splits it again
                        if (t > actEnd) {
                            end(v);
                            break;
                        }
                        cardRound = t + rand(0.3, 1.5);
                    }
                    break;
                }
                boolean from = cardFromRight;
                arm(v, from, 2);
                arm(v, !from, 1);
                double total = (CARD_N - 1) * SPRING_GAP + SPRING_FLY;
                boolean early = rt < total / 2;
                v.lookX = (early == from) ? 1 : -1;
                v.lookY = 0;
                if (cardMode == 2) { // drops them: the cards spray out and fall
                    while (rt >= 0 && count < CARD_N && rt >= count * SPRING_GAP) {
                        float hx = handPx(v, from) + 0.5f, hy = handPy(v, from) - 1;
                        float dir = from ? -1 : 1;
                        v.spawn(bmp("CARD" + cardColor, cardRows(cardColor))[0], sx(v, hx), sy(v, hy, v.lift()),
                                dir * (float) rand(50, 170) * d, -(float) rand(40, 130) * d, (float) rand(0.7, 1.0), 600 * d, 0, 0.6f, S, true);
                        count++;
                    }
                    if (count >= CARD_N && phase == 0 && rt > total + 0.3) {
                        phase = 1;
                        v.annoyedFor = 1.4;
                        mark = t + 1.6;
                    }
                    if (phase == 1 && t >= mark) {
                        end(v);
                    }
                    break;
                }
                if (rt > total + 0.3) { // all across: back the other way
                    v.kick(0.3f);
                    if (t > actEnd) {
                        end(v);
                        break;
                    }
                    cardFromRight = !cardFromRight;
                    cardRound = t + rand(0.4, 1.6);
                }
                break;
            }
            case SPRING: {
                boolean from = springFromRight;
                arm(v, from, 2);
                arm(v, !from, 1);
                double rt = t - springRound;
                double total = (RING_N - 1) * RING_GAP + RING_FLY;
                v.lookX = ((rt < total / 2) == from) ? 1 : -1; // follows the rings over
                v.lookY = 0;
                if (phase == 1) { // dropped it
                    if (t >= mark) {
                        end(v);
                    }
                    break;
                }
                if (springMode == 4 && springDrops <= 0 && rt >= total / 2) {
                    // halfway over, it slips: every ring falls to the floor
                    float hx = (handPx(v, from) + handPx(v, !from)) / 2f, hy = handPy(v, from) - 5;
                    for (int i = 0; i < RING_N; i++) {
                        v.spawn(ringBmp(ringCol[i]), sx(v, hx - 2), sy(v, hy - i * 0.5f, v.lift()), (float) rand(-90, 90) * d,
                                -(float) rand(10, 70) * d, (float) rand(0.8, 1.1), 600 * d, 0, 0.6f, S, true);
                    }
                    Trace.d("act", "spring toy slips and falls");
                    phase = 1;
                    v.annoyedFor = 1.4;
                    mark = t + 1.6;
                    break;
                }
                if (rt > total + 0.2) { // all poured over: it's upside down now, so back the other way
                    v.kick(0.3f);
                    for (int i = 0, j = RING_N - 1; i < j; i++, j--) {
                        char x = ringCol[i];
                        ringCol[i] = ringCol[j];
                        ringCol[j] = x;
                    }
                    springDrops--;
                    if (t > actEnd && springMode != 4) {
                        end(v);
                        break;
                    }
                    springFromRight = !springFromRight;
                    springRound = t + rand(0.2, 1.2);
                }
                break;
            }
            case STRESSBALL: {
                boolean h = spinHand;
                v.lookX = h ? 1 : -1;
                arm(v, !h, 0);
                if (spinToss >= 0) { // tossed over his head to the other hand
                    double p = (t - spinToss) / 0.8;
                    v.armLiftL = 2;
                    v.armLiftR = 2;
                    v.lookY = -1;
                    v.lookX = 0;
                    if (p >= 1) {
                        spinToss = -1;
                        spinHand = !spinHand;
                        v.kick(0.6);
                    }
                } else {
                    arm(v, h, 1);
                    if (t >= nextEvent) {
                        if (squeeze == 0 && rng.nextDouble() < 0.12) {
                            spinToss = t;
                            nextEvent = t + 0.8 + rand(0.4, 1.5);
                        } else {
                            // squeeze harder, hold, let go
                            squeeze = squeeze >= 2 ? 0 : squeeze + 1;
                            nextEvent = t + (squeeze == 2 ? rand(0.4, 1.8) : squeeze == 0 ? span(0.3, 4) : rand(0.12, 0.3));
                            if (squeeze == 2) {
                                v.kick(-0.6);
                            }
                        }
                    }
                    if (squeeze == 2) {
                        v.face = SquibView.Face.ANNOYED;
                    }
                }
                if (t > actEnd && spinToss < 0) {
                    end(v);
                }
                break;
            }
            case CUBE: {
                boolean r = side > 0;
                v.lookX = 0;
                v.lookY = 1;
                v.armLiftL = 1;
                v.armLiftR = 1;
                if (phase == 0) {
                    if (t >= mark && cubeMoves.isEmpty() && scrambling) { // scrambled: now solve it
                        scrambling = false;
                        cubeMoves.addAll(pendingSolve);
                        pendingSolve.clear();
                        mark = t + rand(1, 4); // studies it first
                    }
                    if (t >= mark && cubeMoves.isEmpty() && variant == 3 && t < sessionEnd) { // keeps trying
                        for (int i = 0; i < 6; i++) {
                            lastMove = cube.randomMove(rng, lastMove);
                            cubeMoves.add(lastMove);
                        }
                    }
                    if (t >= mark) {
                        if (cubeMoves.isEmpty()) {
                            phase = 1;
                            mark = t;
                            if (variant <= 2) {
                                v.happyFor = 2.4;
                                v.kick(2);
                                v.heart();
                                maybeSay(v, "CUBE_DONE", 0.8);
                            } else if (variant == 3) {
                                v.annoyedFor = 2.2;
                                maybeSay(v, "CUBE_GIVEUP", 0.8);
                            } else {
                                v.surpriseFor = 1.4;
                                maybeSay(v, "CUBE_OOPS", 0.8);
                            }
                        } else {
                            lastMove = cubeMoves.remove(0);
                            cube.turn(lastMove);
                            lastMoveAt = t;
                            count++;
                            double g = scrambling ? 0.1 : gap * rand(0.8, 1.25);
                            if (!scrambling && variant != 1 && rng.nextDouble() < 0.2) {
                                g += span(1, 8); // stops to think
                            }
                            mark = t + g;
                        }
                    }
                    if (t - lastMoveAt < 0.12) {
                        arm(v, count % 2 == 0, 2);
                    } else if (mark - t > 0.6 && count > 0) { // thinking
                        v.lookX = r ? 1 : -1;
                        v.lookY = -1;
                    }
                } else {
                    double e = t - mark;
                    if (variant <= 2) { // holds it up, solved
                        v.armLiftL = 3;
                        v.armLiftR = 3;
                        v.face = SquibView.Face.HAPPY;
                        v.lookY = -1;
                        if (((int) (e / 0.3)) != count) {
                            count = (int) (e / 0.3);
                            puff(v, "SPARK", center(v) + (rng.nextFloat() - 0.5f) * 16 * S, sy(v, armRow(v) - 8, v.lift()), 0, -15 * d, 0.6f, 0, S);
                        }
                    } else if (variant == 3) { // shakes it
                        int beat = (int) (e / 0.09);
                        v.armLiftL = beat % 2 == 0 ? 2 : 1;
                        v.armLiftR = 3 - v.armLiftL;
                        v.face = SquibView.Face.ANNOYED;
                    } else {
                        v.face = SquibView.Face.SURPRISED;
                        v.lookX = 0;
                    }
                    if (e > 2.3) {
                        if (variant <= 2 && t < sessionEnd) { // scramble it and go again
                            phase = 0;
                            scrambling = true;
                            cubeMoves.clear();
                            pendingSolve.clear();
                            int[] prev = null;
                            int k = cube.n == 2 ? 6 + rng.nextInt(4) : variant == 1 ? 14 + rng.nextInt(6) : 12 + rng.nextInt(9);
                            for (int i = 0; i < k; i++) {
                                prev = cube.randomMove(rng, prev);
                                cubeMoves.add(prev);
                                pendingSolve.add(0, new int[]{prev[0], prev[1], -prev[2]});
                            }
                            mark = t + rand(0.5, 1.5);
                            maybeSay(v, "CUBE_AGAIN", 0.3);
                        } else {
                            end(v);
                        }
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case COIN: {
                if (phase == 0) {
                    arm(v, right, 1);
                    v.lookX = v.facing;
                    if (t >= mark) {
                        phase = 1;
                        mark = t;
                        v.kick(1.2);
                    }
                } else if (phase == 1) {
                    double p = (t - mark) / gap;
                    arm(v, right, p < 0.15 ? 2 : 1);
                    v.lookY = p > 0.2 && p < 0.8 ? -1 : 0;
                    if (p >= 1) {
                        if (variant == 0) {
                            phase = 2;
                            mark = t;
                            v.kick(0.8);
                            v.happyFor = 1;
                            maybeSay(v, heads ? "COIN_HEADS" : "COIN_TAILS", 0.6);
                        } else {
                            phase = 3;
                            mark = t;
                            ox = sx(v, handPx(v, right) + 0.5f);
                            oy = sy(v, handPy(v, right) - 3, v.lift());
                            ovy = 250 * d;
                            ovx = v.facing * 40 * d;
                        }
                    }
                } else if (phase == 2) {
                    arm(v, right, 2);
                    v.lookX = v.facing;
                    if (t - mark > 1.4) {
                        if (flipsLeft > 0) { // again
                            flipsLeft--;
                            phase = 0;
                            mark = t + span(0.4, 6);
                            gap = rand(0.85, 1.15);
                            heads = rng.nextBoolean();
                            variant = rng.nextDouble() < 0.08 ? 1 : 0;
                        } else {
                            end(v);
                        }
                    }
                } else {
                    arm(v, right, 0);
                    v.lookY = 1;
                    float gx = ox - center(v);
                    v.lookX = Math.abs(gx) < 6 * d ? 0 : gx > 0 ? 1 : -1;
                    if (phase == 3) {
                        ovy += 900 * d * f;
                        oy += ovy * f;
                        ox += ovx * f;
                        if (oy >= v.floorY - 2.5f * S) {
                            oy = v.floorY - 2.5f * S;
                            ovy = -ovy * 0.4f;
                            ovx *= 0.6f;
                            if (Math.abs(ovy) < 60 * d) {
                                phase = 4;
                                mark = t;
                                v.surpriseFor = 0.8;
                                maybeSay(v, "COIN_DROP", 0.7);
                            }
                        }
                    } else if (t - mark > 1.4) {
                        end(v);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case HICCUP: {
                int hic = el < 0.3 ? 0 : 1 + (int) ((el - 0.3) / gap);
                if (hic != count) {
                    count = hic;
                    v.hop();
                    v.surpriseFor = 0.3;
                    v.kick(-1.5);
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case SHAKE: {
                int beat = (int) (el / 0.07);
                v.lookX = beat % 2 == 0 ? -1 : 1;
                v.armLiftL = beat % 2 == 0 ? 2 : 0;
                v.armLiftR = 2 - v.armLiftL;
                v.face = SquibView.Face.BLINK;
                if (beat != count) {
                    count = beat;
                    if (beat % 3 == 0) {
                        puff(v, "DROP", center(v) + (beat % 2 == 0 ? -1 : 1) * 10 * S, sy(v, top(v) + 3, v.lift()),
                                (beat % 2 == 0 ? -1 : 1) * 90 * d, -50 * d, 0.5f, 300 * d, S * 0.6f);
                    }
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            case WHISTLE: {
                v.lookY = -1;
                v.lookX = 0;
                if (t >= nextEvent) {
                    nextEvent = t + gap;
                    v.note();
                }
                if (t > actEnd) {
                    end(v);
                }
                break;
            }
            default:
                end(v);
        }
    }

    static String[] block(String ch) {
        String dark = String.valueOf(Props.dark(ch.charAt(0)));
        return new String[]{ch + ch + ch, ch + dark + ch, ch + ch + dark};
    }

    static float[] stickTip(SquibView v, float lift) {
        boolean r = side > 0;
        return new float[]{sx(v, handPx(v, r) + (r ? 6.5f : -5.5f)), sy(v, handPy(v, r) + 3.5f, lift)};
    }

    static float[] boomerang(SquibView v, double t, float lift) {
        boolean r = side > 0;
        float d = v.d;
        double p = Math.min(1, (t - mark) / 2.6);
        float hx = sx(v, handPx(v, r) + 0.5f), hy = sy(v, handPy(v, r), lift);
        float x = hx + side * 150 * d * (float) Math.sin(Math.PI * p);
        float y = hy - 70 * d * (float) Math.sin(Math.PI * p) + 30 * d * (float) Math.sin(2 * Math.PI * p);
        if (variant == 1 && p > 0.75) { // drifts onto his head instead of his hand
            float k = (float) ((p - 0.75) / 0.25);
            x += (center(v) - hx) * k;
            y += (sy(v, top(v) - 1, lift) - hy) * k;
        }
        return new float[]{x, y};
    }

    static void poof(SquibView v, float x, float y) {
        float d = v.d;
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            puff(v, "STEAM", x, y, (float) Math.cos(a) * 40 * d, (float) Math.sin(a) * 40 * d - 10 * d, 0.6f, 0, v.S * 1.5f);
        }
    }

    // ---------- positions used by both update and draw ----------

    /** Juggled balls go round in a loop above his head, hand to hand. Returns the middle of ball i. */
    static float[] juggle(SquibView v, int i, double el, float lift) {
        double a = el * Math.PI * 2 * 0.9 + i * Math.PI * 2 / balls;
        float gx = 13.5f + (float) Math.cos(a) * 12;
        float gy = (top(v) - 7) + (float) Math.sin(a) * (5.5f + (balls - 3) * 0.8f);
        return new float[]{sx(v, gx), sy(v, gy, lift)};
    }

    static float[] rodTip(SquibView v, float lift) {
        boolean r = right(v);
        return new float[]{sx(v, handPx(v, r) + (r ? 6.5f : -5.5f)), sy(v, handPy(v, r) - 6 + 0.5f, lift)};
    }

    /** Colour for kite, balloon and fishing strings: mid grey shows on light and dark apps. */
    static final int STRING = 0xFF9A9A9A;

    static String balloonKey() {
        return "BALLOON" + variant;
    }

    static String[] balloonRows() {
        String c = String.valueOf(Props.light(variant % Props.BRIGHT));
        String dark = String.valueOf(Props.dark(c.charAt(0)));
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

    // ================= headphones =================

    /** The only apps whose music puts his headphones on: Spotify, SoundCloud and Apple Music. */
    static final Set<String> HEADPHONE_APPS = new HashSet<String>(Arrays.asList(
            "com.spotify.music", "com.spotify.lite", "com.soundcloud.android", "com.apple.android.music"));

    /**
     * Headphones go on only when music is actually playing (SquibView.musicOn) while Spotify, SoundCloud or
     * Apple Music is on screen. Android won't say which app is making the sound, so the app being on screen is
     * the proof. They stay on if you leave it and the music keeps going (texting, home screen, games), and come
     * off when it stops for a few seconds or another app with its own sound comes up: video, scrolling feeds,
     * other music and podcast apps, calls and the camera.
     */
    static void listen(SquibView v, double t, double dt) {
        Apps.Kind kind = v.kindNow();
        boolean musicApp = v.pkg != null && HEADPHONE_APPS.contains(v.pkg);
        if (v.musicOn && musicFor == 0) {
            // The sound just started: note which app was on screen. If that app makes its own sound (video,
            // scrolling feed, another music or podcast app, a game), the sound is probably its, even after you
            // leave it, until it stops.
            soundFrom = v.pkg;
            soundFromApp = !musicApp && (kind == Apps.Kind.WATCH || kind == Apps.Kind.SCROLL || kind == Apps.Kind.HIDE
                    || kind == Apps.Kind.MUSIC || (kind == Apps.Kind.GAME && !quietInApp));
            Trace.d("headphones", "sound started in " + v.pkg + " (" + kind + ")" + (soundFromApp ? ": counted as that app's own sound" : ""));
        }
        musicFor = v.musicOn ? musicFor + dt : 0;
        if (v.pkg != null && !v.pkg.equals(listenPkg)) {
            listenPkg = v.pkg;
            pkgSince = t;
            quietInApp = false;
        }
        if (!v.musicOn && t - pkgSince > 3) {
            quietInApp = true;
        }
        if (!listening) {
            boolean longEnough = musicFor >= 1.5;
            String why = !longEnough ? null : v.ownAppOpen ? "his own settings are open" : !v.visible() ? "he's hidden"
                    : !musicApp ? "app on screen is " + v.pkg + ", not spotify, soundcloud or apple music"
                    : soundFromApp ? "the sound started in " + soundFrom + ", it's that app's sound" : null;
            if (why != null && !why.equals(blockedWhy)) {
                Trace.d("headphones", "music playing " + String.format(java.util.Locale.US, "%.1f", musicFor) + "s but not putting them on: " + why);
            }
            blockedWhy = why;
            if (longEnough && why == null) {
                Trace.i("headphones", "ON: music playing " + String.format(java.util.Locale.US, "%.1f", musicFor) + "s, app " + v.pkg
                        + " (" + kind + ")");
                listening = true;
                beatGap = rand(0.42, 0.62); // each song gets its own speed of nodding
                listenOn = t;
                listenOff = -1;
                quietFor = 0;
                vibe = 0;
                nextVibe = t + rand(4, 10);
                if (!bedtime) {
                    maybeSay(v, "LISTEN_ON", 0.4);
                }
            }
            return;
        }
        // video, calls, camera, scrolling feeds, and music or podcast apps other than the three
        boolean otherSound = kind == Apps.Kind.WATCH || kind == Apps.Kind.HIDE || kind == Apps.Kind.SCROLL
                || (kind == Apps.Kind.MUSIC && !musicApp);
        quietFor = v.musicOn ? 0 : quietFor + dt;
        if (otherSound || quietFor > 6) { // a few seconds' grace for the gap between songs
            Trace.i("headphones", "OFF: " + (otherSound ? "app " + v.pkg + " (" + kind + ") has its own sound" : "quiet for 6s") + ", on for "
                    + Math.round(t - listenOn) + "s");
            listening = false;
            listenOff = t;
            vibe = 0;
            if (!otherSound && !bedtime) {
                maybeSay(v, "LISTEN_OFF", 0.3);
            }
            return;
        }
        if (t >= nextVibe) {
            vibe = chooseRandom(4, 3, 2);
            vibeEnd = t + span(3, 25);
            nextVibe = vibeEnd + rand(4, 15);
            hpSide = rng.nextBoolean();
        }
        if (t > vibeEnd) {
            vibe = 0;
        }
        boolean free = act == NONE && v.motion == SquibView.Motion.IDLE && !v.talking && !v.petting && !v.seated
                && v.face != SquibView.Face.SURPRISED && v.face != SquibView.Face.ANNOYED;
        if (!free) {
            return;
        }
        if (t - listenOn < 0.7) { // putting them on
            arm(v, false, 2);
            arm(v, true, 2);
            return;
        }
        if (vibe == 1 && (v.face == SquibView.Face.OPEN || v.face == SquibView.Face.BLINK)) {
            v.face = SquibView.Face.HAPPY; // eyes shut, listening
        } else if (vibe == 2) {
            arm(v, hpSide, 2); // holds one side of the headphones
            v.lookY = 0;
        }
    }

    /** His headphones (white, so they show on dark apps too), coming down on and going up off. */
    static void drawHeadphones(SquibView v, Canvas c, float lift) {
        double t = v.t;
        int dy = 0, a = 255;
        if (listening) {
            double p = (t - listenOn) / 0.7;
            if (p < 1) {
                dy = -(int) Math.round((1 - p) * 5);
                a = (int) (120 + 135 * p);
            }
        } else if (listenOff >= 0 && t - listenOff < 0.6) {
            double p = (t - listenOff) / 0.6;
            dy = -(int) Math.round(p * 5);
            a = (int) (255 * (1 - p));
        } else {
            return;
        }
        int L = 6 - v.squashLevel, R = 21 + v.squashLevel, top = top(v) + dy;
        // Same black as his eyes, solid blocks, sitting on his head: drawn like the rest of him, so it shows on any app.
        int ink = SquibView.EYE;
        for (int x = L - 1; x <= R + 1; x++) { // band along the top of his head
            dot(v, c, x, top, ink, a, lift);
        }
        for (int y = top + 1; y <= top + 3; y++) { // cups, sticking out from the sides of his head
            dot(v, c, L - 2, y, ink, a, lift);
            dot(v, c, L - 1, y, ink, a, lift);
            dot(v, c, R + 1, y, ink, a, lift);
            dot(v, c, R + 2, y, ink, a, lift);
        }
    }

    static void season(SquibView v, double t, double dt) {
        if (t >= nextSeasonCheck) {
            nextSeasonCheck = t + 20;
            Calendar c = Calendar.getInstance();
            int month = c.get(Calendar.MONTH), day = c.get(Calendar.DAY_OF_MONTH), hour = c.get(Calendar.HOUR_OF_DAY);
            int age = v.day();
            partyDay = (month == Calendar.JANUARY && day == 1) || age == 7 || age == 30 || age == 50
                    || (age >= 100 && age % 50 == 0) || (age > 1 && age % 365 == 0);
            december = month == Calendar.DECEMBER;
            boolean spooky = month == Calendar.OCTOBER && day >= 20;
            night = hour >= 21 || hour < 5;
            double h = hour + c.get(Calendar.MINUTE) / 60.0;
            bedtime = bedtimeStart <= bedtimeEnd ? (h >= bedtimeStart && h < bedtimeEnd) : (h >= bedtimeStart || h < bedtimeEnd);
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
            } else {
                drawHeadphones(v, c, lift);
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
        drawHeadphones(v, c, lift);
        drawTexts(v, c, lift);
        switch (act) {
            case BALL:
                drawBmp(c, ball(ballLook, Math.round(roll / (float) (Math.PI / 4)))[0], bx - ballLook[0] / 2f * S,
                        (v.floorY - bh) - ballLook[0] * S, S, (int) alpha);
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
                line(c, x, y, x, y + len, thin(v), STRING);
                drawBmp(c, tint("YOYO", "uU", pair(yoyoColor))[0], x - 1.5f * S, y + len - 1.5f * S, S, 255);
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
                for (int i = 0; i < balls && i < juggleLooks.length; i++) {
                    float[] p = juggle(v, i, el, lift);
                    int[] look = juggleLooks[i];
                    drawBmp(c, ball(look, (int) (el * 8) + i)[0], p[0] - look[0] / 2f * S, p[1] - look[0] / 2f * S, S, 255);
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
            case GUM: {
                float cy = top(v) + 5;
                if (phase == 0) {
                    float r = (float) Math.min(1, el / (mark - actStart)) * 6.5f;
                    for (int gy = (int) -Math.ceil(r); gy <= Math.ceil(r); gy++) {
                        for (int gx = (int) -Math.ceil(r); gx <= Math.ceil(r); gx++) {
                            float dist = (float) Math.hypot(gx, gy);
                            if (dist <= r) {
                                char ch = dist > r - 1 ? 'I' : 'i';
                                if (r >= 2.5f && gx == (int) -(r * 0.4f) && gy == (int) -(r * 0.4f)) {
                                    ch = 'w';
                                }
                                gline(v, c, 13 + gx, cy + gy, 13 + gx, cy + gy, ch, lift);
                            }
                        }
                    }
                } else if (v.t < mark + 2.2) {
                    int[][] splat = {{8, 0}, {10, 1}, {12, 0}, {15, 1}, {17, 0}, {19, 1}, {9, 3}, {14, 4}, {18, 3}, {11, 5}, {16, 6}, {13, 2}};
                    for (int[] sp : splat) {
                        gline(v, c, sp[0], top(v) + sp[1], sp[0], top(v) + sp[1], 'i', lift);
                    }
                }
                break;
            }
            case CAMPFIRE: {
                boolean r = side > 0;
                put(v, c, ((int) (v.t * 6)) % 2 == 0 ? "FIRE1" : "FIRE2", 0, mx(r, 28, 7), 14, 0, 255);
                double eatAt = mark + (variant >= 1 ? 1.4 : 0);
                if (v.t < eatAt + 1.2) {
                    float hx = handPx(v, r), hy = handPy(v, r);
                    float tx = hx + (r ? 6 : -6), ty = hy + 3;
                    gline(v, c, hx, hy, tx, ty, 'k', lift);
                    char m = v.t < mark - 2 ? 'p' : v.t < mark ? 'j' : variant >= 1 ? 'o' : 'Y';
                    gline(v, c, tx, ty, tx + 1, ty, m, lift);
                    gline(v, c, tx, ty + 1, tx + 1, ty + 1, m, lift);
                    if (variant == 1 && phase == 1) {
                        gline(v, c, tx, ty - 1, tx + 1, ty - 1, 'f', lift);
                        gline(v, c, tx + (((int) (v.t * 8)) % 2), ty - 2, tx + (((int) (v.t * 8)) % 2), ty - 2, 'F', lift);
                    }
                }
                break;
            }
            case DRUMS: {
                put(v, c, "DRUM", 0, 9, 13, lift, 255);
                for (int k = 0; k < 2; k++) {
                    boolean r = k == 1;
                    float hx = handPx(v, r), hy = handPy(v, r);
                    boolean up = (r ? v.armLiftR : v.armLiftL) > 0;
                    if (up) { // raised for the next hit: stick points up and out, away from his face
                        gline(v, c, hx, hy, hx + (r ? 2 : -2), hy - 4, 'K', lift);
                    } else {
                        gline(v, c, hx, hy, hx + (r ? -4 : 4), hy + 4, 'K', lift);
                    }
                }
                break;
            }
            case HULA: {
                float cx = 13.5f + (float) Math.sin(el * 9) * 2.5f, cy = 13;
                int bl = 6 - v.squashLevel, br = 21 + v.squashLevel;
                for (int i = 0; i < 48; i++) {
                    double a = i * Math.PI * 2 / 48;
                    float gx = cx + (float) Math.cos(a) * 11, gy = cy + (float) Math.sin(a) * 1.6f;
                    if (Math.sin(a) >= 0 || gx < bl - 0.5f || gx > br + 0.5f) {
                        gline(v, c, gx, gy, gx, gy, Props.light((i / 4) % 2 == 0 ? hulaColors[0] : hulaColors[1]), lift);
                    }
                }
                break;
            }
            case BOOMERANG: {
                boolean r = side > 0;
                if (phase == 0) {
                    held(v, c, lift, "BOOMERANG", r, false, false);
                } else if (phase == 1) {
                    int[] spin = {0, 2, 1, 3};
                    Bitmap b = bmp("BOOMERANG")[spin[((int) (v.t / 0.07)) % 4]];
                    drawBmp(c, b, ox - b.getWidth() * S / 2, oy - b.getHeight() * S / 2, S, 255);
                } else if (variant == 0) {
                    held(v, c, lift, "BOOMERANG", r, false, false);
                }
                break;
            }
            case GARDEN: {
                boolean r = side > 0;
                float x0 = mx(r, 28, 5);
                if (el > 0.4) {
                    put(v, c, "SOIL", 0, x0, 18, 0, 255);
                }
                int stage = el < 1.5 ? -1 : el < 3 ? 0 : el < 4.5 ? 1 : el < 6 ? 2 : 3;
                if (stage >= 0) {
                    drawBmp(c, bmp("SPROUT" + stage, Art.SPROUT[stage])[0], sx(v, x0), sy(v, 12, 0), S, 255);
                }
                if (el > 1 && el < 6.5) {
                    held(v, c, lift, "CAN", r, false, false);
                }
                break;
            }
            case BOWLING: {
                for (int k = 0; k < 3; k++) {
                    if (pins[k]) {
                        drawBmp(c, bmp("PIN")[0], ox + (k - 1) * 4 * S - 1.5f * S, v.posY - 5 * S, S, 255);
                    }
                }
                if (phase == 0) {
                    held(v, c, lift, Props.ballRows(ballLook, 0), ball(ballLook, 0), new int[]{0, 0}, side > 0, false, false);
                } else if (bx > -10 * S && bx < v.screenW + 10 * S) {
                    drawBmp(c, ball(ballLook, Math.round(roll / (float) (Math.PI / 4)))[0], bx - 2 * S, v.posY - 4 * S, S, 255);
                }
                break;
            }
            case KARATE: {
                boolean r = side > 0;
                float x0 = mx(r, 27, 9);
                put(v, c, "BRICK", 0, x0, 17, 0, 255);
                put(v, c, "BRICK", 0, x0 + 6, 17, 0, 255);
                if (phase == 0) {
                    put(v, c, "PLANK", 0, x0, 15, 0, 255);
                }
                break;
            }
            case WORKOUT: {
                int rep = (int) (el / 0.75);
                held(v, c, lift, "DUMBBELL", rep < 4 ? right : !right, false, false);
                break;
            }
            case FRIEND: {
                boolean walking = phase != 1;
                String name = walking && ((int) (v.t * 6)) % 2 == 1 ? "FRIEND2" : "FRIEND1";
                boolean facingRight = phase == 1 ? side < 0 : (phase == 0 ? (center(v) + side * 26 * S) > ox : side > 0);
                float hop = phase == 1 ? -(float) Math.abs(Math.sin((v.t - mark) * 4)) * 3 * S : 0;
                drawBmp(c, bmp(name)[facingRight ? 0 : 1], ox - 6 * S, v.posY - 8 * S + Math.round(hop / S) * S, S, 255);
                break;
            }
            case BLOCKS: {
                boolean r = side > 0;
                if (phase == 0 || variant == 0) {
                    float x0 = mx(r, 28, 3);
                    float wob = phase == 0 && count >= 4 ? (float) Math.sin(v.t * 12) * (float) Math.min(1, (v.t - mark) / 1.6) : 0;
                    for (int i = 0; i < count; i++) {
                        String ch = String.valueOf(Props.light(blockColors[i]));
                        drawBmp(c, bmp("BLOCK" + ch, block(ch))[0], sx(v, x0 + Math.round(wob * i * 0.5f)), sy(v, 17 - i * 3, 0), S, 255);
                    }
                }
                break;
            }
            case MAIL: {
                if (phase == 0) {
                    drawBmp(c, bmp("ENVELOPE")[0], ox - 3.5f * S, oy - 2.5f * S, S, 255);
                } else {
                    put(v, c, "LETTER", 0, 10, top(v) + 3, lift, 255);
                }
                break;
            }
            case SPINNER: {
                int hx = handPx(v, spinHand), hy = handPy(v, spinHand) - 3;
                if (spinToss >= 0) {
                    double p = Math.min(1, (v.t - spinToss) / 0.9);
                    hy -= (int) Math.round(4 * 9 * p * (1 - p));
                }
                char body = Props.light(spinBody), weight = Props.light(spinWeight);
                double reach = Props.spinnerReach(spinType);
                int blur = (int) clamp((float) (spinVel - 10) * 8, 0, 150); // fast: a see-through disc
                int R = (int) Math.ceil(reach);
                for (int dy = -R; dy <= R; dy++) {
                    for (int dx = -R; dx <= R; dx++) {
                        double dist = Math.hypot(dx, dy);
                        char ch = Props.spinnerPixel(spinType, body, weight, spinAngle, dx, dy);
                        if (ch != 0) {
                            dot(v, c, hx + dx, hy + dy, Art.color(ch), 255 - blur, lift);
                        }
                        if (blur > 0 && dist > 1.2 && dist < reach) {
                            dot(v, c, hx + dx, hy + dy, Art.color(dist > reach - 1.3 ? weight : body), blur * 2 / 3, lift);
                        }
                    }
                }
                break;
            }
            case CUBE: {
                if (cube == null) {
                    break;
                }
                int[] shift = null;
                if (lastMove != null && v.t - lastMoveAt < 0.09) {
                    shift = lastMove;
                }
                String[] rows = cube.rows(shift);
                StringBuilder key = new StringBuilder();
                for (String row : rows) {
                    key.append(row);
                }
                if (!key.toString().equals(cubeKey)) {
                    cubeKey = key.toString();
                    cubeBmp = bmp("CUBE_TMP", rows)[0];
                    cache.remove("CUBE_TMP");
                }
                int w = rows[0].length();
                boolean up = phase == 1 && variant <= 2;
                float gy = up ? top(v) - 10 : armRow(v) + 1;
                float wob = phase == 1 && variant == 3 ? (((int) ((v.t - mark) / 0.09)) % 2 == 0 ? -1 : 1) : 0;
                drawBmp(c, cubeBmp, sx(v, 14 - w / 2 + wob), sy(v, gy, lift), S, 255);
                break;
            }
            case PHONE: {
                if (showing && funny >= 0) {
                    // the picture, in a phone-shaped frame, held out beside him
                    String name = "FUNNY_" + FUNNY[funny];
                    Bitmap pic = tint(name, "." + FUNNY_FROM[funny], FUNNY_BG.charAt(funny) + funnyTo)[0];
                    float gx = cardSide > 0 ? 27 : -19, gy = 3;
                    int a = (int) clamp((float) ((v.t - showStart) / 0.15) * 255, 0, 255);
                    float x0 = sx(v, gx), y0 = sy(v, gy, lift);
                    ink.setColor(Art.color('e'));
                    ink.setAlpha(a);
                    c.drawRect(x0, y0, x0 + 18 * S, y0 + 16 * S, ink);
                    ink.setColor(0xFF1D1D1D);
                    dot(v, c, gx + 8, gy + 0.5f, Art.color('o'), a, lift); // camera
                    dot(v, c, gx + 9, gy + 0.5f, Art.color('o'), a, lift);
                    drawBmp(c, pic, x0 + S, y0 + 2 * S, S, a);
                    for (int k = 0; k < 3; k++) { // fake like and comment buttons under it
                        dot(v, c, gx + 2 + k * 3, gy + 14.5f, Art.color(k == 0 ? 'r' : 'q'), a, lift);
                    }
                    break;
                }
                phoneBack(v, c, lift); // the screen faces him, so you see the back
                break;
            }
            case DICE: {
                Bitmap b = bmp("DIE" + dieColor + "_" + dieFace, Props.dieRows(dieColor, dieFace))[0];
                if (phase == 0) {
                    drawBmp(c, b, sx(v, handPx(v, right) - 2), sy(v, handPy(v, right) - 4, lift), S, 255);
                } else {
                    drawBmp(c, b, ox - 2.5f * S, oy - 2.5f * S, S, 255);
                }
                break;
            }
            case PEN: {
                // 7 pixels long: click button, clip end, body, grip, tip
                char body = Props.PAIRS[penColor].charAt(0), dark = Props.PAIRS[penColor].charAt(1);
                char[] parts = {(char) penClip, dark, body, body, body, 'h', penOut ? 'e' : 0};
                float hx = handPx(v, spinHand) + 0.5f, hy = handPy(v, spinHand) - 0.5f;
                double ang = 0;
                if (penTwirl >= 0) {
                    double p = Math.min(1, (v.t - penTwirl) / 1.0);
                    ang = (spinHand ? 1 : -1) * p * p * (3 - 2 * p) * Math.PI * 4; // two full turns, easing in and out
                }
                float dx = (float) Math.sin(ang), dy = (float) Math.cos(ang);
                int top = penOut ? 1 : 0; // the button sinks in when the tip comes out
                for (int i = top - 1; i < parts.length; i++) {
                    char ch = i < 0 ? (char) penClip : parts[i];
                    if (ch == 0) {
                        continue;
                    }
                    float k = i - 3.5f; // the hand holds it in the middle
                    float gx = hx + dx * k, gy = hy + dy * k;
                    dot(v, c, Math.round(gx - 0.5f), Math.round(gy - 0.5f), Art.color(ch), 255, lift);
                }
                if (penTwirl < 0) { // the clip, on the side away from him
                    float cxp = hx + (spinHand ? 1 : -1);
                    dot(v, c, Math.round(cxp - 0.5f), Math.round(hy - 2.5f - 0.5f), Art.color((char) penClip), 255, lift);
                }
                break;
            }
            case TEXT:
                phoneBack(v, c, lift); // the screen faces him, so you see the back
                break;
            case TOSS: {
                // the bin: solid blocks, a darker rim, sitting on the floor
                char light = Props.PAIRS[binColor].charAt(0), dark = Props.PAIRS[binColor].charAt(1);
                float bx0 = Math.round(binX - 2.5f * S), floor = Math.round(v.posY);
                rectT(c, bx0, floor - 6 * S, bx0 + 5 * S, floor - 5 * S, Art.color(dark), 255);
                rectT(c, bx0 + S * 0.0f, floor - 5 * S, bx0 + 5 * S, floor, Art.color(light), 255);
                rectT(c, bx0 + 2 * S, floor - 4 * S, bx0 + 3 * S, floor - S, Art.color(dark), 255); // a stripe
                // the paper ball: 2 x 2, white with a grey corner
                float px0, py0;
                if (paperState == 1) {
                    px0 = sx(v, 13);
                    py0 = sy(v, armRow(v) + 1, lift);
                } else if (paperState == 2 || paperState == 3) {
                    px0 = ox - S;
                    py0 = oy - S;
                } else {
                    break;
                }
                px0 = Math.round(px0 / S) * S;
                py0 = Math.round(py0 / S) * S;
                rectT(c, px0, py0, px0 + 2 * S, py0 + 2 * S, Art.color('w'), 255);
                rectT(c, px0 + S, py0 + S, px0 + 2 * S, py0 + 2 * S, Art.color('q'), 255);
                break;
            }
            case BUBBLEWRAP: {
                int[] tint = WRAP_TINTS[wrapTint];
                int w = wrapCols * 2 + 1, h = wrapRows * 2 + 1;
                int x0 = (28 - w) / 2, y0 = armRow(v) + 1;
                if (wrapTwist >= 0) {
                    x0 += ((int) ((v.t - wrapTwist) * 10)) % 2; // shakes while he wrings it
                }
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int col = (x - 1) / 2, row = (y - 1) / 2;
                        boolean bubble = x % 2 == 1 && y % 2 == 1;
                        int color = !bubble ? tint[0] : wrapPopped[row * wrapCols + col] ? tint[2] : tint[1];
                        dot(v, c, x0 + x, y0 + y, color, 255, lift);
                    }
                }
                break;
            }
            case CARDS: {
                Bitmap card = bmp("CARD" + cardColor, cardRows(cardColor))[0];
                double rt = v.t - cardRound;
                if (cardMode == 0) {
                    float lx = handPx(v, false) + 0.5f, rx = handPx(v, true) + 0.5f;
                    float hy = (handPy(v, false) + handPy(v, true)) / 2f - 1, mid = 13.5f, midY = hy + 4; // the pile is in front of his middle
                    int half = CARD_N / 2, leftOut = 0, rightOut = 0, landed = 0;
                    if (rt >= 0) {
                        for (int i = 0; i < CARD_N; i++) {
                            double p = (rt - i * RIFFLE_GAP) / RIFFLE_FLY;
                            if (p < 0) {
                                continue;
                            }
                            boolean fromRight = (i % 2 == 0) == cardFromRight;
                            if (fromRight) {
                                rightOut++;
                            } else {
                                leftOut++;
                            }
                            if (p >= 1) {
                                landed++;
                            } else {
                                float x0 = fromRight ? rx : lx;
                                float x = (float) (x0 + (mid - x0) * p), y = (float) (hy + (midY - hy) * p - 3 * 4 * p * (1 - p));
                                cardAt(v, c, card, x, y, 1, lift);
                            }
                        }
                    }
                    cardAt(v, c, card, lx, hy, stackLayers(half - leftOut), lift);
                    cardAt(v, c, card, rx, hy, stackLayers(half - rightOut), lift);
                    cardAt(v, c, card, mid, midY, stackLayers(landed), lift);
                    break;
                }
                boolean from = cardFromRight;
                float ax = handPx(v, from) + 0.5f, ay = handPy(v, from) - 1;
                float bxx = handPx(v, !from) + 0.5f, byy = handPy(v, !from) - 1;
                int launched = 0, landed = 0;
                if (cardMode == 2) {
                    launched = count;
                } else if (rt >= 0) {
                    for (int i = 0; i < CARD_N; i++) {
                        double p = (rt - i * SPRING_GAP) / SPRING_FLY;
                        if (p < 0) {
                            continue;
                        }
                        launched++;
                        if (p >= 1) {
                            landed++;
                        } else { // an arc from hand to hand, over the front of him
                            float x = (float) (ax + (bxx - ax) * p), y = (float) (ay + (byy - ay) * p - 4 * 6 * p * (1 - p));
                            cardAt(v, c, card, x, y, 1, lift);
                        }
                    }
                }
                cardAt(v, c, card, ax, ay, stackLayers(CARD_N - launched), lift);
                cardAt(v, c, card, bxx, byy, stackLayers(landed), lift);
                break;
            }
            case SPRING: {
                if (phase == 1) {
                    break; // on the floor, as falling rings
                }
                boolean from = springFromRight;
                float ax = handPx(v, from) + 0.5f, ay = handPy(v, from) - 1;
                float bx2 = handPx(v, !from) + 0.5f, by2 = handPy(v, !from) - 1;
                double rt = v.t - springRound;
                for (int j = 0; j < RING_N; j++) {
                    int k = RING_N - 1 - j; // the top ring goes first and lands at the bottom
                    double p = Math.max(0, (rt - j * RING_GAP) / RING_FLY);
                    float x, y;
                    if (p <= 0) {
                        x = ax;
                        y = ay - k;
                    } else if (p >= 1) {
                        x = bx2;
                        y = by2 - j;
                    } else { // an arch over his head
                        x = (float) (ax + (bx2 - ax) * p);
                        y = (float) ((ay - k) + ((by2 - j) - (ay - k)) * p - 7 * 4 * p * (1 - p));
                    }
                    drawBmp(c, ringBmp(ringCol[k]), sx(v, Math.round(x - 2)), sy(v, Math.round(y), lift), v.S, 255);
                }
                break;
            }
            case STRESSBALL: {
                int size = ballLook[0];
                int sq = spinToss >= 0 ? 0 : squeeze;
                String[] rows = Props.ballRows(ballLook, 0, sq);
                Bitmap b = bmp(Props.ballKey(ballLook, 0) + "_sq" + sq, rows)[0];
                float w = rows[0].length(), hgt = rows.length;
                float cx, by;
                if (spinToss >= 0) { // arc from one hand to the other, over his head
                    double p = Math.min(1, (v.t - spinToss) / 0.8);
                    float x0 = handPx(v, spinHand) + 0.5f, x1 = handPx(v, !spinHand) + 0.5f;
                    cx = (float) (x0 + (x1 - x0) * p);
                    by = handPy(v, spinHand) + 1 - (float) (4 * 14 * p * (1 - p));
                } else {
                    cx = handPx(v, spinHand) + 0.5f;
                    by = handPy(v, spinHand) + 1;
                }
                drawBmp(c, b, sx(v, Math.round(cx - w / 2)), sy(v, Math.round(by - hgt), lift), S, 255);
                break;
            }
            case COIN: {
                float hx = sx(v, handPx(v, right) + 0.5f);
                float hy = sy(v, handPy(v, right) - 3, lift);
                int w = 5;
                boolean face = heads;
                float x = hx, y = hy;
                if (phase == 0) {
                    w = 3;
                } else if (phase == 1 || phase == 3) {
                    double spin = (v.t - mark) * 24;
                    double cs = Math.cos(spin);
                    w = Math.max(1, (int) Math.round(Math.abs(cs) * 5));
                    face = cs > 0;
                    if (phase == 1) {
                        double p = Math.min(1, (v.t - mark) / gap);
                        y = hy - (float) (4 * 18 * p * (1 - p)) * S;
                    } else {
                        x = ox;
                        y = oy;
                    }
                } else if (phase == 4) {
                    x = ox;
                    y = oy;
                }
                String key = "COIN" + metal + "_" + w + (face ? "h" : "t");
                drawBmp(c, bmp(key, Props.coinRows(metal, w, face))[0], x - w / 2f * S, y - 2.5f * S, S, 255);
                break;
            }
        }
        if (!v.perched && !flowers.isEmpty()) {
            for (float[] fl : flowers) {
                drawBmp(c, bmp("SOIL")[0], fl[0], v.floorY - 2 * S, S, 255);
                drawBmp(c, bmp("SPROUT3", Art.SPROUT[3])[0], fl[0], v.floorY - 8 * S, S, 255);
            }
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
