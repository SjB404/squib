package com.squib.buddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import com.squib.buddy.Apps;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class SquibView extends View {
    static final int BLUSH = -875894;
    static final int CORAL = -2591146;
    static final int EYE = -15658735;
    static final int GH = 20;
    static final int GW = 28;
    static final int INK = -14737891;
    static final int PAPER = -329227;
    static final int SHADOW = 1426063360;
    float S;
    float T;
    double activeSecs;
    double annoyedFor;
    boolean appModes;
    int armLiftL;
    int armLiftR;
    double awayFromScroll;
    double beatIn;
    double blinkIn;
    double blinkLeft;
    int boxH;
    int boxW;
    Bitmap bubble;
    boolean chatty;
    final Bitmap cloudArt;
    final float d;
    double downAt;
    float downX;
    float downY;
    float dragStartX;
    float dragStartY;
    boolean dragging;
    boolean drawnHidden;
    final Rect dst;
    Face face;
    int facing;
    final Paint fill;
    float floorY;
    int fontPx;
    boolean greeted;
    double happyFor;
    double heartIn;
    double hopV;
    double hopY;
    double hungerSecs;
    double inkJetLeft;
    int jetDir;
    double kernelIn;
    Apps.Kind kind;
    final EnumMap<Apps.Kind, Double> lastEnterLine;
    double lastHungry;
    long lastMoveTime;
    float lastMoveX;
    double lastT;
    double lastTapAt;
    double lookAroundFor;
    int lookX;
    int lookY;
    float maxX;
    float minX;
    double modeTime;
    Motion motion;
    int navBar;
    double nextChatAt;
    double nextDecision;
    double nextLateCheckAt;
    double noteIn;
    final List<Particle> particles;
    boolean perched;
    boolean petting;
    final Paint pixels;
    String pkg;
    final Bitmap popcornArt;
    float posX;
    float posY;
    final SharedPreferences prefs;
    final int[] px;
    boolean reminders;
    final Random rng;
    int screenH;
    int screenW;
    double scrollSecs;
    boolean seated;
    final Bitmap shrimpArt;
    boolean shrimpLanded;
    boolean shrimpOn;
    float shrimpX;
    float shrimpY;
    String speech;
    double speechClock;
    double speechHold;
    int speechLength;
    List<String> speechLines;
    int speechShown;
    final Bitmap sprite;
    double sq;
    double sqV;
    int squashLevel;
    /** Extra grid pixels he's lifted off the floor (for the skateboard). */
    int raise;
    int statusBar;
    boolean stepA;
    boolean stepB;
    double surpriseFor;
    double t;
    final Bitmap tailDown;
    final Bitmap tailUp;
    boolean talking;
    final List<Double> taps;
    double targetX;
    final Paint text;
    boolean touching;
    boolean userHidden;
    float vx;
    float vy;
    double walkPhase;
    boolean watchNudged;
    double watchSecs;
    double zzzIn;
    static final int[] FONT_SIZES = {9, 11, 14};
    static final String[] SHRIMP = {"...pppp..", ".pplllppk", "pp.....a.", "p.....a..", "tt......."};
    static final String[] HEART = {".hh.hh.", "hwhhhhh", "hhhhhhh", ".hhhhh.", "..hhh..", "...h..."};
    static final String[] ZED = {"zzzz", "..z.", ".z..", "zzzz"};
    static final String[] NOTE = {"...nn", "...n.", "...n.", "...n.", ".nnn.", "nnnn.", ".nn.."};
    static final String[] KERNEL = {".y.", "yyy", ".y."};
    static final String[] POPCORN = {".y.yy.", "yyyyyy", "rwrwrw", "rwrwrw", ".wrwr.", ".rwrw."};
    static final String[] CLOUD = {"..ww....www...", ".wwwwwwwwwwww.", "wwwwwwwwwwwwww", ".gggggggggggg."};
    static final String[][] INK_ART = {new String[]{"#+", "++"}, new String[]{".+.", "+#+", ".+."}, new String[]{".++.", "+##+", "+##+", ".++."}};
    static final String[] TAIL_DOWN = {"ddffffdd", "ddffffdd", ".ddffdd.", ".ddffdd.", "..dddd..", "...dd..."};
    static final String[] TAIL_UP = {"...dd...", "..dddd..", ".ddffdd.", ".ddffdd.", "ddffffdd", "ddffffdd"};

    enum Face {
        OPEN,
        BLINK,
        HAPPY,
        SLEEPY,
        SURPRISED,
        ANNOYED
    }

    enum Motion {
        IDLE,
        WANDER,
        SLEEP,
        DRAG,
        FALL,
        JET,
        CHASE
    }

    static final class Particle {
        float age;
        Bitmap art;
        float drag;
        boolean front;
        float gravity;
        float grow;
        float life;
        float scale;
        float vx;
        float vy;
        float x;
        float y;

        Particle() {
        }
    }

    SquibView(Context context) {
        super(context);
        this.rng = new Random();
        this.px = new int[560];
        this.sprite = Bitmap.createBitmap(GW, GH, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        this.pixels = paint;
        Paint paint2 = new Paint();
        this.fill = paint2;
        Paint paint3 = new Paint();
        this.text = paint3;
        this.dst = new Rect();
        this.particles = new ArrayList<>();
        this.screenW = 1080;
        this.screenH = 2400;
        this.S = 6.0f;
        this.T = 4.0f;
        this.fontPx = 11;
        this.chatty = true;
        this.reminders = true;
        this.appModes = true;
        this.motion = Motion.FALL;
        this.face = Face.OPEN;
        this.nextDecision = 3.0d;
        this.facing = 1;
        this.jetDir = 1;
        this.blinkIn = 3.0d;
        this.kind = Apps.Kind.HOME;
        this.pkg = "";
        this.lastEnterLine = new EnumMap<>(Apps.Kind.class);
        this.nextChatAt = 25.0d;
        this.lastTapAt = -1.0d;
        this.taps = new ArrayList<>();
        this.nextLateCheckAt = 90.0d;
        this.lastHungry = -1.0E9d;
        float f = context.getResources().getDisplayMetrics().density;
        this.d = f;
        this.prefs = Prefs.get(context);
        Pick.init(this.prefs);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        paint2.setAntiAlias(false);
        paint3.setAntiAlias(false);
        paint3.setSubpixelText(false);
        paint3.setTypeface(Typeface.DEFAULT);
        this.shrimpArt = art(SHRIMP);
        this.popcornArt = art(POPCORN);
        this.cloudArt = art(CLOUD);
        this.tailDown = art(TAIL_DOWN);
        this.tailUp = art(TAIL_UP);
        this.lastT = SystemClock.elapsedRealtime() / 1000.0d;
        reloadPrefs();
        this.posX = this.screenW * 0.6f;
        this.posY = this.floorY - (f * 300.0f);
    }

    void reloadPrefs() {
        this.S = Math.max(2, Math.round(this.prefs.getInt("size", 2) * this.d));
        this.T = Math.max(2, Math.round(this.d * 1.2f));
        this.fontPx = FONT_SIZES[Math.max(0, Math.min(2, this.prefs.getInt("textSize", 1)))];
        this.chatty = this.prefs.getBoolean("chatty", true);
        this.reminders = this.prefs.getBoolean("reminders", true);
        this.appModes = this.prefs.getBoolean("appModes", true);
        place();
        String str = this.speech;
        if (str != null) {
            say(str, 0.0d);
        }
    }

    void onScreen(int i, int i2, int i3, int i4) {
        this.screenW = i;
        this.screenH = i2;
        this.statusBar = i3;
        this.navBar = i4;
        place();
        double d = this.posX;
        float f = this.S;
        this.posX = clamp(d, (-2.0f) * f, this.screenW - (f * 26.0f));
        if (this.motion == Motion.FALL || this.motion == Motion.DRAG) {
            return;
        }
        this.posY = this.floorY;
    }

    boolean visible() {
        return (this.userHidden || (this.appModes && this.kind == Apps.Kind.HIDE)) ? false : true;
    }

    Apps.Kind kindNow() {
        return this.appModes ? this.kind : Apps.Kind.HOME;
    }

    void place() {
        float f = this.d;
        float f2 = 8.0f * f;
        int i = this.screenW;
        float f3 = (this.screenH - (i > this.screenH ? 0 : this.navBar)) - (f * 2.0f);
        float f4 = this.statusBar + (f * 14.0f);
        float f5 = this.S;
        float f6 = f4 + (20.0f * f5);
        float floor = f3;
        float f7 = f2 - (2.0f * f5);
        float f8 = (i - f2) - (f5 * 26.0f);
        this.perched = false;
        switch (kindNow()) {
            case WATCH:
                setFloor(floor, f8 - (this.d * 12.0f), f8);
                break;
            case CHAT:
            case NAV:
            case READ:
                this.perched = true;
                setFloor(f6, f8 - (this.d * 24.0f), f8);
                break;
            case GAME:
                this.perched = true;
                setFloor(f6, f7, (this.d * 24.0f) + f7);
                break;
            default:
                setFloor(floor, f7, f8);
                break;
        }
    }

    void setFloor(float f, float f2, float f3) {
        boolean z = (f == this.floorY && f2 == this.minX && f3 == this.maxX) ? false : true;
        this.floorY = f;
        this.minX = f2;
        this.maxX = Math.max(f2, f3);
        if (!z || this.motion == Motion.DRAG || this.motion == Motion.FALL) {
            return;
        }
        float clamp = clamp(this.posX, this.minX, this.maxX);
        this.posX = clamp;
        this.posY = this.floorY;
        this.targetX = clamp;
    }

    void setApp(String str) {
        String[] strArr;
        if (str.equals(this.pkg)) {
            return;
        }
        this.pkg = str;
        Apps.Kind kindOf = Apps.kindOf(getContext(), str);
        if (kindOf == this.kind) {
            return;
        }
        this.kind = kindOf;
        this.seated = false;
        if (this.motion == Motion.SLEEP || this.motion == Motion.WANDER) {
            setMotion(Motion.IDLE);
        }
        place();
        if (!this.appModes || !this.chatty) {
            return;
        }
        Double d = this.lastEnterLine.get(this.kind);
        if ((d == null || this.t - d.doubleValue() >= 600.0d) && this.rng.nextDouble() <= 0.3d) {
            switch (this.kind) {
                case WATCH:
                    strArr = Lines.WATCH_ENTER;
                    break;
                case CHAT:
                    strArr = Lines.CHAT_ENTER;
                    break;
                case NAV:
                    strArr = Lines.NAV_ENTER;
                    break;
                case READ:
                    strArr = Lines.READ_ENTER;
                    break;
                case GAME:
                    if (!Fun.MC.contains(this.pkg)) {
                        strArr = Lines.GAME_ENTER;
                        break;
                    } else {
                        strArr = Lines.MINECRAFT_ENTER;
                        break;
                    }
                case MUSIC:
                    strArr = Lines.MUSIC_ENTER;
                    break;
                default:
                    strArr = null;
                    break;
            }
            if (strArr != null) {
                this.lastEnterLine.put(this.kind, Double.valueOf(this.t));
                say(Pick.one(strArr), 0.0d);
            }
        }
    }

    void step() {
        double elapsedRealtime = SystemClock.elapsedRealtime() / 1000.0d;
        double min = Math.min(0.05d, elapsedRealtime - this.lastT);
        this.lastT = elapsedRealtime;
        this.t = elapsedRealtime;
        if (!visible()) {
            if (!this.drawnHidden) {
                this.drawnHidden = true;
                invalidate();
                return;
            }
            return;
        }
        this.drawnHidden = false;
        wellbeing(min);
        move(min);
        face(min);
        body(min);
        food(min);
        updateParticles(min);
        updateSpeech(min);
        Fun.pose(this);
        drawSprite();
        invalidate();
    }

    void setMotion(Motion motion) {
        this.motion = motion;
        this.modeTime = 0.0d;
    }

    void move(double d) {
        double d2;
        this.modeTime += d;
        if (this.touching && !this.dragging && !this.petting && this.t - this.downAt > 0.45d) {
            startPetting();
        }
        switch (this.motion) {
            case FALL:
                double d3 = this.vy;
                float f = this.d;
                float min = (float) Math.min(d3 + (520.0f * f * d), f * 260.0f);
                this.vy = min;
                this.posY = (float) (this.posY + (min * d));
                this.posX = (float) (this.posX + (this.vx * d) + (Math.sin(this.t * 3.0d) * 20.0d * this.d * d));
                this.vx = (float) (this.vx * Math.pow(0.15d, d));
                bounceScreenEdges();
                float f2 = this.posY;
                float f3 = this.floorY;
                if (f2 >= f3) {
                    this.posY = f3;
                    this.vy = 0.0f;
                    this.vx = 0.0f;
                    kick(3.0d);
                    this.happyFor = 0.8d;
                    walkBackOrIdle();
                    if (!this.greeted) {
                        this.greeted = true;
                        greet();
                        break;
                    }
                }
                break;
            case IDLE:
                if (!this.shrimpOn) {
                    if (kindNow() == Apps.Kind.WATCH) {
                        this.seated = true;
                    }
                    double d4 = this.nextDecision - d;
                    this.nextDecision = d4;
                    if (d4 <= 0.0d) {
                        decide();
                        break;
                    }
                } else {
                    setMotion(Motion.CHASE);
                    break;
                }
                break;
            case WANDER:
            case CHASE:
                if (this.motion == Motion.CHASE) {
                    float f4 = this.shrimpX;
                    float f5 = this.S;
                    d2 = clamp(f4 - (14.0f * f5), (-2.0f) * f5, this.screenW - (f5 * 26.0f));
                } else {
                    d2 = this.targetX;
                }
                double d5 = d2 - this.posX;
                if (Math.abs(d5) > 1.0d) {
                    this.facing = d5 <= 0.0d ? -1 : 1;
                }
                this.posX = (float) (this.posX + (this.facing * Math.min(Math.abs(d5), (this.motion == Motion.CHASE ? 120 : 45) * this.d * d)));
                if (Math.abs(d5) < 2.0d && this.motion == Motion.WANDER) {
                    setMotion(Motion.IDLE);
                    this.nextDecision = (this.rng.nextDouble() * 6.0d) + 2.0d;
                }
                if (!this.shrimpOn && this.motion == Motion.CHASE) {
                    walkBackOrIdle();
                    break;
                }
                break;
            case JET:
                double d6 = this.inkJetLeft - d;
                this.inkJetLeft = d6;
                if (d6 > 0.0d) {
                    spawnInk(2, 0.6f);
                }
                double d7 = this.posX;
                float f6 = this.vx;
                this.posX = (float) (d7 + (f6 * d));
                this.vx = (float) (f6 * Math.pow(0.03d, d));
                bounceScreenEdges();
                if (Math.abs(this.vx) < this.d * 15.0f) {
                    walkBackOrIdle();
                    break;
                }
                break;
            case SLEEP:
                double d8 = this.zzzIn - d;
                this.zzzIn = d8;
                if (d8 <= 0.0d) {
                    this.zzzIn = 1.5d;
                    zzz();
                    break;
                }
                break;
        }
    }

    void walkBackOrIdle() {
        float f = this.posX;
        if (f < this.minX - 1.0f || f > this.maxX + 1.0f) {
            this.targetX = clamp(f, this.minX, this.maxX);
            setMotion(Motion.WANDER);
        } else {
            setMotion(Motion.IDLE);
            this.nextDecision = 3.0d;
        }
    }

    void decide() {
        if (this.chatty && this.t >= this.nextChatAt && this.speech == null) {
            chatter();
            this.nextChatAt = this.t + chatGap();
            this.nextDecision = (this.rng.nextDouble() * 6.0d) + 4.0d;
            return;
        }
        double nextDouble = this.rng.nextDouble();
        if (this.seated) {
            this.lookAroundFor = nextDouble >= 0.2d ? 0.0d : 2.0d;
            this.nextDecision = (this.rng.nextDouble() * 8.0d) + 5.0d;
        } else if (nextDouble < 0.68d) {
            this.targetX = clamp(this.posX + (((this.rng.nextDouble() * 2.0d) - 1.0d) * 120.0d * this.d), this.minX, this.maxX);
            setMotion(Motion.WANDER);
        } else if (nextDouble < 0.78d) {
            hop();
            this.nextDecision = (this.rng.nextDouble() * 4.0d) + 3.0d;
        } else {
            this.lookAroundFor = 2.5d;
            this.nextDecision = (this.rng.nextDouble() * 8.0d) + 4.0d;
        }
    }

    double chatGap() {
        switch (kindNow()) {
            case WATCH:
            case GAME:
                return (this.rng.nextDouble() * 180.0d) + 180.0d;
            case CHAT:
                return 1.0E9d;
            case NAV:
                return (this.rng.nextDouble() * 300.0d) + 300.0d;
            case READ:
            case MUSIC:
                return (this.rng.nextDouble() * 240.0d) + 240.0d;
            default:
                return (this.rng.nextDouble() * 150.0d) + 35.0d;
        }
    }

    void chatter() {
        switch (kindNow()) {
            case WATCH:
                say(Pick.one(Lines.WATCH), 0.0d);
                return;
            case NAV:
                say(Pick.one(Lines.NAV), 0.0d);
                return;
            case READ:
                say(Pick.one(Lines.READ), 0.0d);
                return;
            case GAME:
                say(Pick.one(Lines.GAME), 0.0d);
                return;
            case MUSIC:
                say(Pick.one(Lines.MUSIC), 0.0d);
                return;
            case SCROLL:
                if (this.rng.nextDouble() < 0.3d) {
                    say(Pick.one(Lines.SCROLL), 0.0d);
                    return;
                }
                break;
        }
        int i = Calendar.getInstance().get(11);
        ArrayList<String[]> arrayList = new ArrayList<String[]>();
        ArrayList<Double> arrayList2 = new ArrayList<Double>();
        arrayList.add(Lines.THOUGHTS);
        arrayList2.add(Double.valueOf(40.0d));
        arrayList.add(Lines.MUTTERS);
        arrayList2.add(Double.valueOf(12.0d));
        arrayList.add(Lines.JOKES);
        arrayList2.add(Double.valueOf(9.0d));
        arrayList.add(Lines.FACTS);
        arrayList2.add(Double.valueOf(10.0d));
        arrayList.add(Lines.STATS);
        arrayList2.add(Double.valueOf(4.0d));
        arrayList.add((i < 5 || i >= 22) ? Lines.NIGHT : i < 12 ? Lines.MORNING : i < 18 ? Lines.AFTERNOON : Lines.EVENING);
        arrayList2.add(Double.valueOf(7.0d));
        if (Fun.seasonLines != null) {
            arrayList.add(Fun.seasonLines);
            arrayList2.add(Double.valueOf(8.0d));
        }
        if (this.hungerSecs > 7200.0d) {
            arrayList.add(Lines.HUNGRY);
            arrayList2.add(Double.valueOf(5.0d));
        }
        Iterator it = arrayList2.iterator();
        double d = 0.0d;
        while (it.hasNext()) {
            d += ((Double) it.next()).doubleValue();
        }
        double nextDouble = this.rng.nextDouble() * d;
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            nextDouble -= ((Double) arrayList2.get(i2)).doubleValue();
            if (nextDouble <= 0.0d || i2 == arrayList.size() - 1) {
                say(Pick.muse((String[]) arrayList.get(i2)), 0.0d);
                return;
            }
        }
    }

    void wellbeing(double d) {
        double d2 = this.activeSecs + d;
        this.activeSecs = d2;
        this.hungerSecs += d;
        if (this.reminders && d2 >= 3000.0d) {
            this.activeSecs = 0.0d;
            say(Pick.one(Lines.BREAK), 5.0d);
        }
        if (kindNow() == Apps.Kind.SCROLL) {
            double d3 = this.scrollSecs + d;
            this.scrollSecs = d3;
            this.awayFromScroll = 0.0d;
        } else {
            double d4 = this.awayFromScroll + d;
            this.awayFromScroll = d4;
            if (d4 > 300.0d) {
                this.scrollSecs = 0.0d;
            }
        }
        if (kindNow() == Apps.Kind.WATCH) {
            double d5 = this.watchSecs + d;
            this.watchSecs = d5;
            if (!this.watchNudged && d5 > 7200.0d) {
                this.watchNudged = true;
                say(Pick.one(Lines.WATCH_LONG), 3.0d);
            }
            if (this.seated && this.motion == Motion.IDLE && this.watchSecs > 5400.0d && this.rng.nextDouble() < d / 1800.0d) {
                setMotion(Motion.SLEEP);
            }
        } else {
            this.watchSecs = 0.0d;
            this.watchNudged = false;
        }
        int i3 = Calendar.getInstance().get(11);
        if (i3 >= 23 || i3 < 5) {
            double d6 = this.t;
            if (d6 >= this.nextLateCheckAt) {
                this.nextLateCheckAt = d6 + ((i3 == 23 ? 40.0d : i3 == 0 ? 25.0d : 15.0d) * 60.0d * ((this.rng.nextDouble() * 0.5d) + 0.75d));
                say(Pick.one((i3 >= 23 || i3 < 1) ? Lines.LATE : Lines.LATE_NIGHT), 2.0d);
            }
        }
        if (this.chatty && this.hungerSecs > 14400.0d && this.t - this.lastHungry > 1800.0d && this.motion == Motion.IDLE && this.speech == null) {
            this.lastHungry = this.t;
            say(Pick.one(Lines.HUNGRY), 0.0d);
        }
    }

    void onScreenOn(double d) {
        this.lastT = SystemClock.elapsedRealtime() / 1000.0d;
        if (d > 300.0d) {
            this.activeSecs = 0.0d;
        }
        if (this.motion == Motion.SLEEP) {
            setMotion(Motion.IDLE);
        }
        if (this.chatty && d > 600.0d && this.rng.nextDouble() < 0.5d && visible()) {
            this.surpriseFor = 0.6d;
            say(Pick.one(Lines.WAKE), 0.0d);
        }
    }

    void face(double d) {
        double d2 = this.blinkIn - d;
        this.blinkIn = d2;
        this.blinkLeft -= d;
        if (d2 <= 0.0d) {
            this.blinkLeft = 0.14d;
            this.blinkIn = this.rng.nextDouble() < 0.2d ? 0.3d : (this.rng.nextDouble() * 4.0d) + 2.0d;
        }
        this.happyFor -= d;
        this.surpriseFor -= d;
        this.annoyedFor -= d;
        this.lookAroundFor -= d;
        if (this.motion == Motion.WANDER || this.motion == Motion.CHASE) {
            this.lookX = this.facing;
            this.lookY = 0;
        } else {
            if (this.motion == Motion.JET) {
                this.lookX = this.vx > 0.0f ? 1 : -1;
                this.lookY = 0;
            } else if (this.lookAroundFor > 0.0d) {
                this.lookX = Math.max(-1, Math.min(1, (int) Math.round(Math.sin(this.t * 2.3d) * 1.3d)));
                this.lookY = Math.sin(this.t * 1.1d) > 0.6d ? 1 : 0;
            } else if (this.seated) {
                this.lookX = this.posX + (this.S * 14.0f) <= ((float) this.screenW) / 2.0f ? 1 : -1;
                this.lookY = 0;
            } else {
                this.lookX = 0;
                this.lookY = this.perched ? 1 : 0;
            }
        }
        if (this.petting) {
            double d3 = this.heartIn - d;
            this.heartIn = d3;
            if (d3 <= 0.0d) {
                this.heartIn = 0.35d;
                heart();
            }
        }
        if (this.motion == Motion.SLEEP) {
            this.face = Face.SLEEPY;
            return;
        }
        if (this.annoyedFor > 0.0d) {
            this.face = Face.ANNOYED;
            return;
        }
        if (this.surpriseFor > 0.0d || (this.motion == Motion.FALL && this.greeted && this.modeTime < 0.6d)) {
            this.face = Face.SURPRISED;
            return;
        }
        if (this.happyFor > 0.0d || this.petting || this.motion == Motion.DRAG) {
            this.face = Face.HAPPY;
        } else if (this.blinkLeft <= 0.0d) {
            this.face = Face.OPEN;
        } else {
            this.face = Face.BLINK;
        }
    }

    void body(double d) {
        double d2 = this.sqV;
        double d3 = this.sq;
        double d4 = d2 + ((((-170.0d) * d3) - (d2 * 8.0d)) * d);
        this.sqV = d4;
        this.sq = d3 + (d4 * d);
        int i = (this.motion != Motion.SLEEP ? this.t % 2.8d >= 0.3d : Math.sin(this.t * 1.3d) <= 0.2d) ? 0 : 1;
        if (this.motion == Motion.FALL || this.motion == Motion.DRAG || this.motion == Motion.JET) {
            i = 0;
        }
        if (kindNow() == Apps.Kind.MUSIC && this.motion == Motion.IDLE) {
            double d5 = this.beatIn - d;
            this.beatIn = d5;
            if (d5 <= 0.0d) {
                this.beatIn = 0.5d;
                kick(1.2d);
            }
            double d6 = this.noteIn - d;
            this.noteIn = d6;
            if (d6 <= 0.0d) {
                this.noteIn = (this.rng.nextDouble() * 1.5d) + 1.2d;
                note();
            }
        }
        if (this.seated && this.motion == Motion.IDLE) {
            double d7 = this.kernelIn - d;
            this.kernelIn = d7;
            if (d7 <= 0.0d) {
                this.kernelIn = (this.rng.nextDouble() * 25.0d) + 15.0d;
                kernel();
            }
        }
        this.squashLevel = (int) Math.max(-2L, Math.min(2L, Math.round(this.sq * 8.0d) + i));
        double d8 = this.hopY;
        if (d8 < 0.0d || this.hopV != 0.0d) {
            double d9 = this.hopV + (this.d * 1400.0f * d);
            this.hopV = d9;
            double d10 = d8 + (d9 * d);
            this.hopY = d10;
            if (d10 >= 0.0d) {
                this.hopY = 0.0d;
                this.hopV = 0.0d;
                kick(2.0d);
            }
        }
        boolean walking = this.motion == Motion.WANDER || this.motion == Motion.CHASE;
        if (walking) {
            double d11 = this.walkPhase + ((this.motion == Motion.CHASE ? 12 : 7) * d);
            this.walkPhase = d11;
            boolean z = ((int) d11) % 2 == 0;
            this.stepA = z;
            this.stepB = !z;
        } else {
            boolean z2 = this.motion == Motion.JET;
            this.stepB = z2;
            this.stepA = z2;
        }
        int i2 = ((int) (this.t * 8.0d)) % 2;
        this.armLiftR = 0;
        this.armLiftL = 0;
        if (this.motion == Motion.DRAG || this.motion == Motion.FALL) {
            this.armLiftL = 2 - i2;
            this.armLiftR = i2 + 1;
            return;
        }
        if (this.motion == Motion.SLEEP || this.face == Face.ANNOYED) {
            this.armLiftR = -1;
            this.armLiftL = -1;
            return;
        }
        if (this.face == Face.HAPPY) {
            int i3 = (((int) (this.t * 5.0d)) % 2) + 1;
            this.armLiftR = i3;
            this.armLiftL = i3;
        } else if (this.talking) {
            int i4 = ((int) (this.t * 6.0d)) % 2;
            this.armLiftL = i4;
            this.armLiftR = 1 - i4;
        } else if (walking) {
            this.armLiftL = this.stepA ? 1 : 0;
            this.armLiftR = this.stepB ? 1 : 0;
        }
    }

    float lift() {
        return (Math.round(this.hopY / this.S) * this.S) - (this.raise * this.S);
    }

    void kick(double d) {
        this.sqV += d;
    }

    void hop() {
        if (this.hopY != 0.0d || this.hopV != 0.0d) {
            return;
        }
        this.hopV = (-((this.prefs.getInt("size", 2) * 40) + 160)) * this.d;
        kick(-1.5d);
    }

    void bounceScreenEdges() {
        float f = this.S;
        float f2 = (-2.0f) * f;
        float f3 = this.screenW - (f * 26.0f);
        if (this.posX < f2) {
            this.posX = f2;
            this.vx = Math.abs(this.vx) * 0.5f;
        }
        if (this.posX > f3) {
            this.posX = f3;
            this.vx = (-Math.abs(this.vx)) * 0.5f;
        }
    }

    void greet() {
        if (!this.prefs.getBoolean("greeted", false)) {
            this.prefs.edit().putBoolean("greeted", true).apply();
            say("hi! i'm squib. i live in your phone now. tap me, hold your finger on me to pet me, or double-tap for ink.", 6.0d);
        } else {
            int i = Calendar.getInstance().get(11);
            say((i < 5 ? "you're up late." : i < 12 ? "good morning!" : i < 18 ? "good afternoon!" : "good evening!") + " day " + day() + " of living in your phone.", 0.0d);
        }
    }

    int day() {
        long currentTimeMillis = System.currentTimeMillis() / 86400000;
        long j = this.prefs.getLong("born", -1L);
        if (j < 0) {
            this.prefs.edit().putLong("born", currentTimeMillis).apply();
            j = currentTimeMillis;
        }
        return ((int) (currentTimeMillis - j)) + 1;
    }

    boolean onHandleTouch(MotionEvent motionEvent) {
        float rawX = motionEvent.getRawX();
        float rawY = motionEvent.getRawY();
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.touching = true;
                this.petting = false;
                this.dragging = false;
                this.downX = rawX;
                this.downY = rawY;
                this.downAt = this.t;
                this.dragStartX = this.posX;
                this.dragStartY = this.posY;
                this.lastMoveX = rawX;
                this.lastMoveTime = motionEvent.getEventTime();
                return true;
            case 1:
            case 3:
                this.touching = false;
                if (this.dragging) {
                    this.dragging = false;
                    this.vy = 0.0f;
                    double d = this.vx;
                    float f = this.d;
                    this.vx = clamp(d, (-1500.0f) * f, f * 1500.0f);
                    setMotion(Motion.FALL);
                } else if (this.petting) {
                    this.petting = false;
                } else if (motionEvent.getActionMasked() == 1) {
                    tap();
                }
                return true;
            case 2:
                if (!this.dragging && !this.petting && Math.hypot(rawX - this.downX, rawY - this.downY) > this.d * 10.0f) {
                    this.dragging = true;
                    this.seated = false;
                    this.vx = 0.0f;
                    setMotion(Motion.DRAG);
                    if (this.rng.nextDouble() < 0.5d) {
                        say(Pick.one(Lines.GRABBED), 0.0d);
                    }
                }
                if (this.dragging) {
                    double d2 = (this.dragStartX + rawX) - this.downX;
                    float f2 = this.S;
                    float clamp = clamp(d2, (-2.0f) * f2, this.screenW - (f2 * 26.0f));
                    float clamp2 = clamp((this.dragStartY + rawY) - this.downY, this.statusBar + (this.S * 20.0f), this.screenH);
                    this.vx = (this.vx * 0.5f) + (((rawX - this.lastMoveX) / (Math.max(1L, motionEvent.getEventTime() - this.lastMoveTime) / 1000.0f)) * 0.5f);
                    this.lastMoveX = rawX;
                    this.lastMoveTime = motionEvent.getEventTime();
                    this.posX = clamp;
                    this.posY = clamp2;
                }
                return true;
            default:
                return false;
        }
    }

    void startPetting() {
        this.petting = true;
        this.heartIn = 0.0d;
        if (this.motion == Motion.SLEEP) {
            setMotion(Motion.IDLE);
        }
        this.prefs.edit().putInt("pets", this.prefs.getInt("pets", 0) + 1).apply();
        if (this.speech == null) {
            say(Pick.one(Lines.PET), 0.0d);
        }
    }

    void tap() {
        double d = this.t;
        boolean z = d - this.lastTapAt < 0.3d;
        this.lastTapAt = d;
        if (z) {
            squirt();
            return;
        }
        this.taps.add(Double.valueOf(d));
        while (!this.taps.isEmpty() && this.t - this.taps.get(0).doubleValue() > 2.5d) {
            this.taps.remove(0);
        }
        kick(3.0d);
        this.prefs.edit().putInt("boops", this.prefs.getInt("boops", 0) + 1).apply();
        if (this.motion == Motion.SLEEP) {
            setMotion(Motion.IDLE);
            this.surpriseFor = 0.6d;
            say("huh? i'm up! i'm up.", 0.0d);
        } else {
            if (this.taps.size() >= 5) {
                this.taps.clear();
                this.annoyedFor = 1.6d;
                say(Pick.one(Lines.SPAM), 0.0d);
                squirt();
                return;
            }
            this.happyFor = 0.7d;
            if (this.rng.nextDouble() >= 0.65d) {
                heart();
            } else {
                say(Pick.one(Lines.BOOP), 0.0d);
            }
        }
    }

    void squirt() {
        if (this.motion == Motion.DRAG) {
            return;
        }
        if (this.motion == Motion.SLEEP) {
            setMotion(Motion.IDLE);
        }
        this.seated = false;
        this.jetDir = this.posX + (this.S * 14.0f) > ((float) this.screenW) / 2.0f ? -1 : 1;
        spawnInk(10, 1.0f);
        this.vx = this.jetDir * 460 * this.d;
        this.inkJetLeft = 0.3d;
        this.surpriseFor = 0.5d;
        kick(-2.0d);
        setMotion(Motion.JET);
    }

    void feed() {
        if (this.shrimpOn || !visible()) {
            return;
        }
        if (this.motion == Motion.SLEEP) {
            setMotion(Motion.IDLE);
        }
        float nextFloat = (this.rng.nextBoolean() ? -1 : 1) * ((this.rng.nextFloat() * 60.0f) + 40.0f);
        float f = this.d;
        this.shrimpX = clamp(this.posX + (this.S * 14.0f) + (nextFloat * f), f * 16.0f, this.screenW - (f * 16.0f));
        this.shrimpY = (this.posY - (this.S * 20.0f)) - (this.d * 120.0f);
        this.shrimpOn = true;
        this.shrimpLanded = false;
        this.surpriseFor = 0.5d;
        this.seated = false;
        if (this.motion == Motion.IDLE || this.motion == Motion.WANDER) {
            setMotion(Motion.CHASE);
        }
    }

    void trick() {
        Fun.trick(this);
    }

    void talk() {
        double nextDouble = this.rng.nextDouble();
        say(Pick.one(nextDouble < 0.55d ? Lines.FACTS : nextDouble < 0.8d ? Lines.JOKES : Lines.THOUGHTS), 2.0d);
    }

    void food(double d) {
        if (this.shrimpOn) {
            if (!this.shrimpLanded) {
                this.shrimpY = (float) (this.shrimpY + (this.d * 70.0f * d));
                this.shrimpX = (float) (this.shrimpX + (Math.sin(this.t * 4.0d) * 25.0d * this.d * d));
                float height = this.floorY - ((this.shrimpArt.getHeight() * this.S) / 2.0f);
                if (this.shrimpY >= height) {
                    this.shrimpY = height;
                    this.shrimpLanded = true;
                }
            }
            boolean z = this.shrimpY > this.posY - (this.S * 8.0f);
            if (this.motion != Motion.DRAG && this.motion != Motion.FALL && Math.abs(this.shrimpX - (this.posX + (this.S * 14.0f))) < this.d * 10.0f && z) {
                this.shrimpOn = false;
                this.hungerSecs = 0.0d;
                this.prefs.edit().putInt("fed", this.prefs.getInt("fed", 0) + 1).apply();
                this.happyFor = 2.0d;
                kick(3.0d);
                for (int i = 0; i < 3; i++) {
                    heart();
                }
                say(Pick.one(Lines.FED), 0.0d);
                walkBackOrIdle();
            }
        }
    }

    boolean handleRect(Rect rect) {
        if (this.prefs.getBoolean("clickThrough", false) || !visible()) {
            return false;
        }
        float lift = lift();
        int round = Math.round(this.d * 6.0f);
        rect.set(Math.round(this.posX + (this.S * 2.0f)) - round, Math.round((this.posY - (this.S * 20.0f)) + lift) - round, Math.round(this.posX + (this.S * 26.0f)) + round, Math.round(this.posY + lift) + round);
        return true;
    }

    void say(String str, double d) {
        String replace = str.replace("{time}", new SimpleDateFormat("h:mm a", Locale.US).format(new Date()).toLowerCase(Locale.US)).replace("{day}", String.valueOf(day())).replace("{fed}", String.valueOf(this.prefs.getInt("fed", 0))).replace("{boops}", String.valueOf(this.prefs.getInt("boops", 0))).replace("{pets}", String.valueOf(this.prefs.getInt("pets", 0)));
        this.speech = replace;
        this.text.setTextSize(this.fontPx);
        List<String> wrap = wrap(replace, Math.min((this.fontPx * 140) / 11, ((int) (this.screenW / this.T)) - 30));
        this.speechLines = wrap;
        this.speechLength = 0;
        Iterator<String> it = wrap.iterator();
        while (it.hasNext()) {
            this.speechLength += it.next().length() + 1;
        }
        this.speechShown = -1;
        this.speechClock = 0.0d;
        this.talking = true;
        this.speechHold = (replace.length() * 0.055d) + 2.4d + d;
        Iterator<String> it2 = this.speechLines.iterator();
        int i = 0;
        while (it2.hasNext()) {
            i = Math.max(i, (int) Math.ceil(PixelFont.measure(this.text, it2.next())));
        }
        this.boxW = i + 14;
        int size = (this.speechLines.size() * (this.fontPx + 2)) + 9;
        this.boxH = size;
        this.bubble = Bitmap.createBitmap(this.boxW + 2, size + 2, Bitmap.Config.ARGB_8888);
        renderBubble(0);
    }

    List<String> wrap(String str, int i) {
        ArrayList<String> arrayList = new ArrayList<>();
        String str2 = "";
        for (String str3 : str.split(" ")) {
            String str4 = str2.isEmpty() ? str3 : str2 + " " + str3;
            if (str2.isEmpty() || PixelFont.measure(this.text, str4) <= i) {
                str2 = str4;
            } else {
                arrayList.add(str2);
                str2 = str3;
            }
        }
        arrayList.add(str2);
        return arrayList;
    }

    void renderBubble(int i) {
        this.bubble.eraseColor(0);
        Canvas canvas = new Canvas(this.bubble);
        box(canvas, SHADOW, 2, 2, this.boxW, this.boxH, 2);
        box(canvas, INK, 0, 0, this.boxW, this.boxH, 2);
        box(canvas, PAPER, 2, 2, this.boxW - 4, this.boxH - 4, 2);
        this.text.setColor(INK);
        int round = Math.round(-this.text.ascent());
        for (int i2 = 0; i2 < this.speechLines.size() && i > 0; i2++) {
            String str = this.speechLines.get(i2);
            PixelFont.draw(canvas, str, 0, Math.min(str.length(), i), 7.0f, ((((this.fontPx + 2) * i2) + 4) + round) - 1, this.text);
            i -= str.length() + 1;
        }
    }

    void box(Canvas canvas, int i, int i2, int i3, int i4, int i5, int i6) {
        this.fill.setColor(i);
        for (int i7 = 0; i7 <= i6; i7++) {
            canvas.drawRect((i2 + i6) - i7, i3 + i7, (i2 + i4) - (i6 - i7), (i3 + i5) - i7, this.fill);
        }
    }

    void updateSpeech(double d) {
        if (this.speech == null) {
            return;
        }
        double d2 = this.speechClock + d;
        this.speechClock = d2;
        if (!this.talking) {
            if (d2 > this.speechHold + 0.3d) {
                this.speech = null;
                return;
            }
            return;
        }
        int min = Math.min(this.speechLength, (int) (d2 / 0.028d));
        if (min != this.speechShown) {
            this.speechShown = min;
            renderBubble(min);
        }
        if (min >= this.speechLength) {
            this.talking = false;
            this.speechClock = 0.0d;
        }
    }

    void spawn(Bitmap bitmap, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, boolean z) {
        Particle particle = new Particle();
        particle.art = bitmap;
        particle.x = f;
        particle.y = f2;
        particle.vx = f3;
        particle.vy = f4;
        particle.life = f5;
        particle.gravity = f6;
        particle.grow = f7;
        particle.drag = f8;
        particle.scale = f9;
        particle.front = z;
        this.particles.add(particle);
    }

    void spawnInk(int i, float f) {
        char c;
        float f2 = this.posX;
        float f3 = this.S;
        float f4 = (f2 + (f3 * 14.0f)) - ((this.jetDir * 10) * f3);
        float lift = (this.posY - (f3 * 10.0f)) + lift();
        for (int i2 = 0; i2 < i; i2++) {
            double nextDouble = this.rng.nextDouble();
            String[][] strArr = INK_ART;
            if (nextDouble < 0.35d) {
                c = 0;
            } else {
                c = nextDouble < 0.7d ? (char) 1 : (char) 2;
            }
            spawn(art(strArr[c]), f4 + (((this.rng.nextFloat() * 12.0f) - 6.0f) * this.d), lift + (((this.rng.nextFloat() * 28.0f) - 14.0f) * f * this.d), this.d * (-this.jetDir) * ((this.rng.nextFloat() * 60.0f) + 10.0f), this.d * ((this.rng.nextFloat() * 50.0f) - 30.0f) * f, 1.4f + (this.rng.nextFloat() * 0.9f), this.d * (-8.0f), 0.5f, 0.2f, this.S, false);
        }
    }

    void heart() {
        Bitmap art = art(HEART);
        float f = this.posX + (this.S * 14.0f);
        float nextFloat = (this.rng.nextFloat() - 0.5f) * 10.0f;
        float f2 = this.S;
        float f3 = f + (nextFloat * f2);
        float lift = (this.posY - (f2 * 18.0f)) + lift();
        float nextFloat2 = ((this.rng.nextFloat() * 30.0f) - 15.0f) * this.d;
        float nextFloat3 = (-45.0f) - (this.rng.nextFloat() * 20.0f);
        float f4 = this.d;
        spawn(art, f3, lift, nextFloat2, nextFloat3 * f4, 1.3f, f4 * (-10.0f), 0.0f, 0.5f, this.T, true);
    }

    void zzz() {
        Bitmap art = art(ZED);
        float f = this.posX;
        float f2 = this.S;
        float f3 = (23.0f * f2) + f;
        float f4 = this.posY - (f2 * 16.0f);
        float f5 = this.d;
        spawn(art, f3, f4, f5 * 12.0f, f5 * (-22.0f), 2.6f, 0.0f, 1.0f, 1.0f, this.T, true);
    }

    void note() {
        Bitmap art = art(NOTE);
        float f = this.posX + (this.S * 14.0f);
        int i = this.rng.nextBoolean() ? -1 : 1;
        float f2 = this.S;
        float f3 = f + (i * 10 * f2);
        float f4 = this.posY - (f2 * 16.0f);
        float nextFloat = (this.rng.nextFloat() * 20.0f) - 10.0f;
        float f5 = this.d;
        spawn(art, f3, f4, nextFloat * f5, f5 * (-35.0f), 2.0f, 0.0f, 0.0f, 1.0f, this.T, true);
    }

    void kernel() {
        Bitmap art = art(KERNEL);
        float f = this.posX;
        float f2 = this.S;
        float f3 = f - (2.0f * f2);
        float f4 = this.posY - (6.0f * f2);
        float f5 = this.d;
        spawn(art, f3, f4, 40.0f * f5, (-90.0f) * f5, 0.7f, 260.0f * f5, 0.0f, 1.0f, f2 * 0.75f, true);
    }

    void updateParticles(double d) {
        for (int size = this.particles.size() - 1; size >= 0; size--) {
            Particle particle = this.particles.get(size);
            particle.age = (float) (particle.age + d);
            if (particle.age >= particle.life) {
                this.particles.remove(size);
            } else {
                particle.vy = (float) (particle.vy + (particle.gravity * d));
                particle.vx = (float) (particle.vx * Math.pow(particle.drag, d));
                particle.x = (float) (particle.x + (particle.vx * d));
                particle.y = (float) (particle.y + (particle.vy * d));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (visible()) {
            float lift = lift();
            if (this.shrimpOn) {
                Bitmap bitmap = this.shrimpArt;
                float width = this.shrimpX - ((bitmap.getWidth() * this.S) / 2.0f);
                float f = this.shrimpY;
                float height = this.shrimpArt.getHeight();
                float f2 = this.S;
                drawArt(canvas, bitmap, width, f - ((height * f2) / 2.0f), f2, 255);
            }
            for (Particle particle : this.particles) {
                if (!particle.front) {
                    drawParticle(canvas, particle);
                }
            }
            if (this.perched) {
                Bitmap bitmap2 = this.cloudArt;
                float f3 = this.posX + (this.S * 14.0f);
                float width2 = bitmap2.getWidth();
                float f4 = this.S;
                drawArt(canvas, bitmap2, f3 - ((width2 * f4) / 2.0f), this.posY - f4, f4, 255);
            }
            this.dst.set(Math.round(this.posX), Math.round((this.posY - (this.S * 20.0f)) + lift), Math.round(this.posX + (this.S * 28.0f)), Math.round(this.posY + lift));
            canvas.drawBitmap(this.sprite, (Rect) null, this.dst, this.pixels);
            if (this.seated && kindNow() == Apps.Kind.WATCH) {
                Bitmap bitmap3 = this.popcornArt;
                float f5 = this.posX - (this.S * 4.0f);
                float f6 = this.posY;
                float height2 = bitmap3.getHeight();
                float f7 = this.S;
                drawArt(canvas, bitmap3, f5, f6 - (height2 * f7), f7, 255);
            }
            for (Particle particle2 : this.particles) {
                if (particle2.front) {
                    drawParticle(canvas, particle2);
                }
            }
            Fun.draw(this, canvas, lift);
            if (this.speech != null) {
                drawBubble(canvas, lift);
            }
        }
    }

    void drawParticle(Canvas canvas, Particle particle) {
        float f = particle.age / particle.life;
        int i = f < 0.5f ? 255 : f < 0.75f ? 170 : 85;
        float floor = particle.scale * ((((float) Math.floor((particle.grow * f) * 2.0f)) / 2.0f) + 1.0f);
        drawArt(canvas, particle.art, particle.x - ((particle.art.getWidth() * floor) / 2.0f), particle.y - ((particle.art.getHeight() * floor) / 2.0f), floor, i);
    }

    void drawArt(Canvas canvas, Bitmap bitmap, float f, float f2, float f3, int i) {
        float round = Math.round(f / f3) * f3;
        float round2 = Math.round(f2 / f3) * f3;
        this.dst.set(Math.round(round), Math.round(round2), Math.round(round + (bitmap.getWidth() * f3)), Math.round(round2 + (bitmap.getHeight() * f3)));
        this.pixels.setAlpha(i);
        canvas.drawBitmap(bitmap, (Rect) null, this.dst, this.pixels);
        this.pixels.setAlpha(255);
    }

    void drawBubble(Canvas canvas, float lift) {
        float w = this.bubble.getWidth() * this.T;
        float cx = this.posX + (this.S * 14.0f);
        float bx = clamp(cx - (w / 2.0f), this.d * 4.0f, (this.screenW - (this.d * 4.0f)) - w);
        float by = ((((this.posY - (this.S * 20.0f)) + (this.S * 4.0f)) + lift) - ((this.boxH + 4) * this.T)) - (this.d * 2.0f);
        boolean below = by < this.statusBar + (this.d * 2.0f);
        if (below) {
            by = this.posY + lift + (this.T * 4.0f) + (this.d * 2.0f);
        }
        int alpha = 255;
        if (!this.talking && this.speechClock > this.speechHold) {
            alpha = this.speechClock - this.speechHold < 0.15d ? 170 : 85;
        }
        drawArt(canvas, this.bubble, bx, by, this.T, alpha);
        float tx = clamp(cx - (this.T * 4.0f), bx + (this.T * 6.0f), bx + ((this.boxW - 14) * this.T));
        if (below) {
            drawArt(canvas, this.tailUp, tx, by - (this.T * 4.0f), this.T, alpha);
        } else {
            drawArt(canvas, this.tailDown, tx, by + ((this.boxH - 2) * this.T), this.T, alpha);
        }
    }

    void drawSprite() {
        int i;
        int i2;
        Arrays.fill(this.px, 0);
        int i3 = this.squashLevel;
        int i4 = 6 - i3;
        int i5 = i3 + 21;
        int i6 = (i3 * 2) + 4;
        if (this.seated) {
            i6 += 3;
            i = 18;
        } else {
            i = 15;
        }
        int i7 = i6;
        int i8 = i;
        fillPx(i4, i7, i5, i8, CORAL);
        int i9 = i7 + (((i8 - i7) + 1) / 3);
        int i10 = this.armLiftL;
        fillPx(i4 - 4, i9 - i10, i4 - 1, (i9 - i10) + 3, CORAL);
        int i11 = this.armLiftR;
        fillPx(i5 + 1, i9 - i11, i5 + 4, (i9 - i11) + 3, CORAL);
        int[] iArr = {i4, i4 + 4, i5 - 5, i5 - 1};
        int i12 = 0;
        while (i12 < 4) {
            boolean z = (i12 == 0 || i12 == 2) ? this.stepA : this.stepB;
            if (this.seated) {
                i2 = i8 + 1;
            } else {
                i2 = (z ? 3 : 4) + i8;
            }
            int i13 = i2;
            int i14 = iArr[i12];
            fillPx(i14, i8 + 1, i14 + 1, i13, CORAL);
            i12++;
        }
        int i15 = i7 + 2;
        int[] iArr2 = {i4 + 2, i5 - 3};
        int i16 = 0;
        while (i16 < 2) {
            int i17 = iArr2[i16];
            switch (this.face) {
                case OPEN:
                    int i18 = this.lookX;
                    int i19 = this.lookY;
                    fillPx(i17 + i18, i15 + i19, i17 + i18 + 1, i19 + i15 + 1, EYE);
                    break;
                case BLINK:
                    int i20 = i15 + 1;
                    fillPx(i17, i20, i17 + 1, i20, EYE);
                    break;
                case SLEEPY:
                    int i21 = i15 + 1;
                    fillPx(i17 - 1, i21, i17 + 2, i21, EYE);
                    break;
                case SURPRISED:
                    fillPx(i17, i15 - 1, i17 + 1, i15 + 1, EYE);
                    break;
                case HAPPY:
                    int i22 = i15 + 1;
                    setPx(i17 - 1, i22, EYE);
                    setPx(i17, i15, EYE);
                    setPx(i17 + 1, i15, EYE);
                    setPx(i17 + 2, i22, EYE);
                    break;
                case ANNOYED:
                    int i23 = i16 == 0 ? i17 + 1 : i17;
                    if (i16 != 0) {
                        i17++;
                    }
                    setPx(i17, i15 - 1, EYE);
                    setPx(i23, i15, EYE);
                    setPx(i17, i15 + 1, EYE);
                    break;
            }
            i16++;
        }
        if (this.petting) {
            int i24 = iArr2[0];
            int i25 = i15 + 3;
            fillPx(i24 - 1, i25, i24, i25, BLUSH);
            int i26 = iArr2[1];
            fillPx(i26 + 1, i25, i26 + 2, i25, BLUSH);
        }
        this.sprite.setPixels(this.px, 0, GW, 0, 0, GW, GH);
    }


    void setPx(int i, int i2, int i3) {
        if (i < 0 || i2 < 0 || i >= GW || i2 >= GH) {
            return;
        }
        this.px[(i2 * GW) + i] = i3;
    }

    void fillPx(int i, int i2, int i3, int i4, int i5) {
        while (i2 <= i4) {
            for (int i6 = i; i6 <= i3; i6++) {
                setPx(i6, i2, i5);
            }
            i2++;
        }
    }

    static Bitmap art(String[] strArr) {
        int length = strArr[0].length();
        int length2 = strArr.length;
        int[] iArr = new int[length * length2];
        for (int i = 0; i < length2; i++) {
            for (int i2 = 0; i2 < length; i2++) {
                int i3 = -9544557;
                switch (strArr[i].charAt(i2)) {
                    case '#':
                        i3 = -14016192;
                        break;
                    case '+':
                    case 'n':
                        break;
                    case 'a':
                        i3 = -2594219;
                        break;
                    case 'd':
                        i3 = INK;
                        break;
                    case 'f':
                        i3 = PAPER;
                        break;
                    case 'g':
                        i3 = -2497554;
                        break;
                    case 'h':
                        i3 = -1944200;
                        break;
                    case 'k':
                        i3 = EYE;
                        break;
                    case 'l':
                        i3 = -412518;
                        break;
                    case 'p':
                        i3 = -1013138;
                        break;
                    case 'r':
                        i3 = -2077877;
                        break;
                    case 't':
                        i3 = -2070961;
                        break;
                    case 'w':
                        i3 = -1;
                        break;
                    case 'y':
                        i3 = -8054;
                        break;
                    case 'z':
                        i3 = -7636043;
                        break;
                    default:
                        i3 = 0;
                        break;
                }
                iArr[(i * length) + i2] = i3;
            }
        }
        return Bitmap.createBitmap(iArr, length, length2, Bitmap.Config.ARGB_8888);
    }

    static float clamp(double d, double d2, double d3) {
        if (d < d2) {
            d = d2;
        } else if (d > d3) {
            d = d3;
        }
        return (float) d;
    }
}
