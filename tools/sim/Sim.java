package com.squib.buddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Runs squib off-phone, using the stand-in Android classes in tools/sim/fake.
 *   sheets [NAMES]  renders a contact sheet per activity into build/sim (e.g. "sheets KITE MC HATS")
 *   stress HOURS    simulates hours of random use; any exception fails the run, then reports line repeats
 */
public class Sim {
    static final String OUT = System.getProperty("out", "build/sim");
    static final int W = 1080, H = 2400;
    static Bitmap screen = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    static Canvas canvas = new Canvas(screen);
    static Context ctx = new Context();

    static SquibView fresh() {
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
        java.util.Arrays.fill(Fun.recentActs, 0);
        SquibView v = new SquibView(ctx);
        v.onScreen(W, H, 80, 130);
        v.greeted = true;
        run(v, 4.0);
        Fun.nextAt = 1e18;
        Fun.nextFidgetAt = 1e18;
        v.nextChatAt = 1e18;
        v.speech = null;
        return v;
    }

    static void run(SquibView v, double secs) {
        int frames = (int) Math.round(secs * 30);
        for (int i = 0; i < frames; i++) {
            SystemClock.now += 33;
            v.step();
        }
    }

    static BufferedImage shot(SquibView v, int size) {
        screen.eraseColor(0xFFE9E5DD);
        v.onDraw(canvas);
        int cx = Math.round(v.posX + v.S * 14);
        int cy = Math.round(v.posY - v.S * 14);
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int sx = cx - size / 2 + x, sy = cy - size * 2 / 3 + y;
                int c = (sx >= 0 && sy >= 0 && sx < W && sy < H) ? screen.getPixel(sx, sy) : 0xFF555555;
                img.setRGB(x, y, c);
            }
        }
        return img;
    }

    static void sheet(String name, List<BufferedImage> frames, List<String> labels) throws Exception {
        int cols = 5, size = frames.get(0).getWidth();
        int rows = (frames.size() + cols - 1) / cols;
        BufferedImage out = new BufferedImage(cols * size, rows * (size + 18) + 22, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(name, 6, 17);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        for (int i = 0; i < frames.size(); i++) {
            int x = (i % cols) * size, y = 22 + (i / cols) * (size + 18);
            g.drawImage(frames.get(i), x, y + 18, null);
            g.drawString(labels.get(i), x + 4, y + 14);
        }
        ImageIO.write(out, "png", new File(OUT, name + ".png"));
    }

    static void capture(String name, SquibView v, double total, int n, int size, Runnable each) throws Exception {
        List<BufferedImage> frames = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        double step = total / n;
        for (int i = 0; i < n; i++) {
            run(v, step);
            if (each != null) {
                each.run();
            }
            frames.add(shot(v, size));
            labels.add(String.format("%.1fs act=%d", (i + 1) * step, Fun.act));
        }
        sheet(name, frames, labels);
    }

    public static void main(String[] a) throws Exception {
        new File(OUT).mkdirs();
        String mode = a.length > 0 ? a[0] : "sheets";
        if (mode.equals("sheets")) {
            sheets(a.length > 1 ? a[1] : null);
        } else if (mode.equals("picks")) {
            picks();
        } else {
            stress(Double.parseDouble(a[1]));
        }
    }

    static void sheets(String only) throws Exception {
        for (int act = 1; act < Fun.NAMES.length; act++) {
            String name = Fun.NAMES[act];
            if (only != null && !only.contains(name)) {
                continue;
            }
            SquibView v = fresh();
            if (act == Fun.FISHING) {
                v.setApp("com.google.android.apps.maps");
                run(v, 1);
            }
            if (act == Fun.STARGAZE) {
                Fun.night = true;
            }
            if (act == Fun.CONFETTI) {
                Fun.partyDay = true;
                Fun.hat = "PARTY_HAT";
            }
            v.speech = null;
            Fun.want = Integer.getInteger("want", -1);
            Fun.start(v, act);
            Fun.want = -1;
            double dur = Math.max(1.5, Math.min(20, Fun.actEnd - v.t + 0.5));
            if (act == Fun.BALL || act == Fun.FISHING || act == Fun.SKATE || act == Fun.ICECREAM) {
                dur = 16;
            }
            if (act == Fun.BOOMERANG || act == Fun.BOWLING || act == Fun.BLOCKS || act == Fun.MAIL || act == Fun.WORKOUT) {
                dur = 9;
            }
            if (act == Fun.FRIEND) {
                dur = 16;
            }
            capture(String.format("%02d_%s", act, name), v, dur, 15, 300, null);
        }
        if (only == null || only.contains("MC")) {
            for (int m = 1; m <= 5; m++) {
                SquibView v = fresh();
                v.setApp("com.mojang.minecraftpe");
                run(v, 0.5);
                Fun.nextMcAt = 1e18;
                Fun.nextItemAt = 1e18;
                Fun.helmet = true;
                Fun.item = m == 3 ? 4 : 1;
                Fun.startMc(v, v.t, m);
                capture("MC_" + m, v, Math.max(1.5, Fun.mcEnd - v.t + 0.5), 10, 300, null);
            }
        }
        if (only == null || only.contains("HATS")) {
            List<BufferedImage> frames = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (String hat : new String[]{"PARTY_HAT", "WINTER_HAT", "WITCH_HAT"}) {
                for (int facing : new int[]{1, -1}) {
                    SquibView v = fresh();
                    Fun.hat = hat;
                    v.facing = facing;
                    run(v, 0.2);
                    frames.add(shot(v, 220));
                    labels.add(hat + " " + facing);
                }
            }
            SquibView v = fresh();
            Fun.december = true;
            Fun.hat = "WINTER_HAT";
            run(v, 12);
            frames.add(shot(v, 600));
            labels.add("snow");
            // pad to same size
            List<BufferedImage> small = new ArrayList<>();
            for (BufferedImage f : frames) {
                BufferedImage b = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
                b.createGraphics().drawImage(f, 0, 0, 300, 300, null);
                small.add(b);
            }
            sheet("HATS", small, labels);
        }
    }

    /** Every activity and fidget, every version, picked from the floor and from a cloud, the way the picker in settings does it. */
    static void picks() {
        int n = 0;
        for (String app : new String[]{"com.android.launcher", "com.whatsapp"}) {
            for (int act = 1; act < Fun.NAMES.length; act++) {
                String[] vs = Fun.variantNames(act);
                for (int var = -1; var < (vs == null ? 0 : vs.length); var++) {
                    SquibView v = fresh();
                    v.setApp(app);
                    run(v, 1);
                    boolean perched = v.perched;
                    Fun.trick(v, Fun.NAMES[act], var);
                    if (Fun.act != act) {
                        throw new IllegalStateException(Fun.NAMES[act] + " " + var + " didn't start in " + app);
                    }
                    double start = v.t;
                    while (Fun.act == act && v.t - start < 90) {
                        run(v, 0.5);
                        v.onDraw(canvas);
                    }
                    if (Fun.act == act) {
                        throw new IllegalStateException(Fun.NAMES[act] + " " + var + " never ended");
                    }
                    run(v, 0.5);
                    if (v.forcePlace != 0 || v.perched != perched) {
                        throw new IllegalStateException(Fun.NAMES[act] + " " + var + " didn't go back to its spot");
                    }
                    n++;
                }
            }
        }
        System.out.println("picked " + n + " tricks and versions, all started, ended and went back to their spot");
    }

    static int mostIn(List<Double> ts, double window) {
        int best = 0;
        for (int i = 0; i < ts.size(); i++) {
            int n = 0;
            for (int j = i; j < ts.size() && ts.get(j) - ts.get(i) < window; j++) {
                n++;
            }
            best = Math.max(best, n);
        }
        return best;
    }

    /** Hours of random use. Fails loudly on any exception; reports how lines repeat. */
    static void stress(double hours) throws Exception {
        Random r = new Random(7);
        String[] apps = {"com.android.launcher", "com.netflix.mediaclient", "com.spotify.music", "com.zhiliaoapp.musically",
                "com.whatsapp", "com.roblox.client", "com.google.android.apps.maps", "com.amazon.kindle",
                "com.google.android.GoogleCamera", "com.mojang.minecraftpe", "net.kdt.pojavlaunch"};
        boolean real = System.getProperty("real") != null;
        if (!real) { // compress the gaps so every activity gets exercised
            Fun.activityGapMin = 25;
            Fun.activityGapMax = 70;
            Fun.fidgetGapMin = 12;
            Fun.fidgetGapMax = 35;
        }
        SquibView v = fresh();
        Fun.nextAt = real ? -1 : v.t + 10;
        Fun.nextFidgetAt = v.t + 5;
        List<Double> actStarts = new ArrayList<>(), fidgetStarts = new ArrayList<>();
        v.nextChatAt = v.t + 5;
        Fun.nextSeasonCheck = 1e18; // seasons frozen; bedtime alternates below so both day and night get tested
        boolean night = false;
        long frames = (long) (hours * 3600 * 30);
        Map<String, List<Double>> said = new HashMap<>();
        String last = null;
        int[] actCount = new int[Fun.NAMES.length];
        int lastAct = 0;
        int mcActs = 0;
        double nextEvent = v.t + 30;
        for (long i = 0; i < frames; i++) {
            SystemClock.now += 33;
            v.step();
            if (i % 3 == 0) {
                v.onDraw(canvas);
            }
            if (v.speech != null && v.speech != last) {
                said.computeIfAbsent(v.speech, k -> new ArrayList<>()).add(v.t);
            }
            last = v.speech;
            if (Fun.act != lastAct && Fun.act != 0) {
                actCount[Fun.act]++;
                (Fun.act >= Fun.FIRST_FIDGET ? fidgetStarts : actStarts).add(v.t);
            }
            lastAct = Fun.act;
            if (Fun.mcAct != 0 && i % 30 == 0) {
                mcActs++;
            }
            if (i % (30 * 60 * 90) == 0) { // every 90 simulated minutes, switch between day and night
                Fun.bedtime = night;
                night = !night;
            }
            if (i % (30 * 60 * 7) == 0 && Fun.bedtime) { // the screen goes off and on now and then
                v.onScreenOn(r.nextBoolean() ? 30 : 900);
            }
            if (v.t >= nextEvent) {
                nextEvent = v.t + 20 + r.nextDouble() * 240;
                double e = r.nextDouble();
                float x = v.posX + v.S * 14, y = v.posY - v.S * 8;
                long now = SystemClock.now;
                if (e < 0.35) {
                    v.setApp(apps[r.nextInt(apps.length)]);
                } else if (e < 0.5) {
                    v.onHandleTouch(MotionEvent.obtain(now, 0, x, y));
                    v.onHandleTouch(MotionEvent.obtain(now + 50, 1, x, y));
                } else if (e < 0.58) {
                    v.onHandleTouch(MotionEvent.obtain(now, 0, x, y));
                    run(v, 1.5);
                    v.onHandleTouch(MotionEvent.obtain(SystemClock.now, 1, x, y));
                } else if (e < 0.66) {
                    v.onHandleTouch(MotionEvent.obtain(now, 0, x, y));
                    for (int k = 1; k <= 10; k++) {
                        SystemClock.now += 33;
                        v.onHandleTouch(MotionEvent.obtain(SystemClock.now, 2, x - k * 25, y - k * 40));
                        v.step();
                    }
                    v.onHandleTouch(MotionEvent.obtain(SystemClock.now, 1, x - 250, y - 400));
                } else if (e < 0.74) {
                    v.feed();
                } else if (e < 0.86) {
                    if (!real) {
                        v.trick();
                    }
                } else if (e < 0.92) {
                    v.talk();
                } else {
                    v.userHidden = !v.userHidden;
                }
            }
        }
        // repeat report
        int lines = 0, repeats = 0;
        double minGap = 1e18;
        String worst = null;
        for (Map.Entry<String, List<Double>> en : said.entrySet()) {
            List<Double> ts = en.getValue();
            lines += ts.size();
            for (int k = 1; k < ts.size(); k++) {
                repeats++;
                double gap = ts.get(k) - ts.get(k - 1);
                if (gap < minGap) {
                    minGap = gap;
                    worst = en.getKey();
                }
            }
        }
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new File(OUT, "said.tsv"))) {
            for (Map.Entry<String, List<Double>> en : said.entrySet()) for (double tt : en.getValue()) pw.println(tt + "\t" + en.getKey());
        }
        System.out.printf("simulated %.1f h: %d lines said, %d distinct, %d repeats, closest repeat %.0f min apart: %s%n",
                hours, lines, said.size(), repeats, minGap / 60, worst);
        StringBuilder b = new StringBuilder("activities:");
        for (int k = 1; k < Fun.NAMES.length; k++) {
            b.append(' ').append(Fun.NAMES[k]).append('=').append(actCount[k]);
        }
        System.out.println(b);
        System.out.println("minecraft action seconds: " + mcActs);
        System.out.println("most activities in any 60 min: " + mostIn(actStarts, 3600) + ", most fidgets in any 10 min: " + mostIn(fidgetStarts, 600)
                + ", activities total: " + actStarts.size() + ", fidgets total: " + fidgetStarts.size());
    }
}
