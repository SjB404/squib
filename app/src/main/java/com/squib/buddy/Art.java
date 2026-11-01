package com.squib.buddy;

/**
 * Pixel art for props, as rows of characters. Each character is one pixel in the colour
 * from {@link #color(char)}; '.' (or any unknown character) is see-through.
 * A "_GRIP" array is the pixel {x, y} that sits in squib's hand when he holds it.
 * Everything here is original art drawn for squib.
 */
final class Art {
    private Art() {
    }

    static int color(char c) {
        switch (c) {
            case 'c': return 0xFF8CF4EC; // diamond light
            case 'b': return 0xFF33CFC4; // diamond
            case 'D': return 0xFF17868B; // diamond dark
            case 'w': return 0xFFFFFFFF; // white
            case 'k': return 0xFF8A5C34; // wood
            case 'K': return 0xFF54351C; // dark wood
            case 'o': return 0xFF1D1D1D; // near black
            case 'r': return 0xFFE0483D; // red
            case 'R': return 0xFFA02C25; // dark red
            case 'p': return 0xFFFAFAF5; // paper
            case 'q': return 0xFFB9B9B0; // light grey
            case 'y': return 0xFFFFD35C; // yellow
            case 'Y': return 0xFFF29A38; // orange
            case 'u': return 0xFF9A66D6; // purple
            case 'U': return 0xFF5E3A8C; // dark purple
            case 'l': return 0xCCBFE8FF; // bubble (see-through)
            case 'g': return 0xFF5BBF5A; // green
            case 'G': return 0xFF3C8A3E; // dark green
            case 'm': return 0xFF6FB7E8; // blue
            case 'M': return 0xFF3F7FB0; // dark blue
            case 's': return 0x99FFFFFF; // steam (see-through)
            case 'n': return 0xFF8B5A2B; // dirt
            case 'N': return 0xFF7D7D7D; // stone
            case 'e': return 0xFF2A2A2A; // dark grey
            case 'i': return 0xFFF7A8C8; // pink
            case 'I': return 0xFFE0709C; // dark pink
            case 'S': return 0xFF9A9A9A; // stone light
            case 'h': return 0xFF686868; // stone dark
            case 'f': return 0xFFFF9F1C; // flame
            case 'F': return 0xFFFFE680; // flame core
            case 'v': return 0xFFD87656; // squib coral
            case 'V': return 0xFFB85A3E; // squib coral dark
            case 'a': return 0xFFF08A6E; // shrimp
            case 'A': return 0xFFD86A55; // shrimp dark
            case 'z': return 0xFF2C3E70; // night blue
            case 'Z': return 0xFFC9D3FF; // star blue-white
            case 'x': return 0xFF111111; // eye black
            case 'j': return 0xFFFFF3B0; // light yellow
            case 'J': return 0xFFE6C6FF; // light purple
            default: return 0;
        }
    }

    // ---------- Minecraft (diamond only, never netherite) ----------
    static final String[] SWORD = {
            "........DD",
            ".......DcD",
            "......DcbD",
            ".....DcbD.",
            "....DcbD..",
            ".D.DcbD...",
            "..DcbD....",
            "..KDD.....",
            ".k.D......",
            "K........."};
    static final int[] SWORD_GRIP = {1, 8};
    static final String[] PICK = {
            "..DDDDD..",
            ".DccccbD.",
            "DcD..Kbb.",
            "D...K.DbD",
            "...K...Db",
            "..K....D.",
            ".k.......",
            "k........",
            "........."};
    static final int[] PICK_GRIP = {1, 6};
    static final String[] AXE = {
            "....DDD..",
            "...DcccD.",
            "..KDcbbD.",
            ".K.DbbbD.",
            "K...DbD..",
            "k.....D..",
            "........."};
    static final int[] AXE_GRIP = {0, 5};
    static final String[] SHOVEL = {
            ".....DDD",
            ".....DcD",
            "....DcbD",
            "....KDD.",
            "...K....",
            "..K.....",
            ".k......",
            "k......."};
    static final int[] SHOVEL_GRIP = {1, 6};
    static final String[] GEM = {
            ".DDD.",
            "DcwcD",
            "DbcbD",
            ".DbD.",
            "..D.."};
    static final int[] GEM_GRIP = {2, 3};
    static final String[] HELMET = {
            "..DDDDDDDDDDDDDD..",
            ".DccccwcccccccccD.",
            "DcbbbbbbbbbbbbbbbD",
            "DbD............DbD",
            "DbD............DbD",
            "DD..............DD"};
    /** A block of stone with diamonds in it. */
    static final String[] ORE = {
            "hSSSShSSh",
            "SSbcSSSSS",
            "SSDbSShSS",
            "hSSSSSSbS",
            "SSShSScDS",
            "SbcSSSSSh",
            "SSDSShSSS",
            "SSSSSSSSh",
            "hSShSSSSS"};
    static final String[] CRACK1 = {
            ".........",
            ".........",
            "....o....",
            "...o.o...",
            "....o....",
            ".........",
            ".........",
            ".........",
            "........."};
    static final String[] CRACK2 = {
            ".........",
            "..o...o..",
            "...o.o...",
            "..oo.oo..",
            "....o....",
            "...o.o...",
            "..o...o..",
            ".........",
            "........."};
    static final String[] CRACK3 = {
            "o.......o",
            ".o.o.o.o.",
            "..o...o..",
            ".ooo.ooo.",
            "....o....",
            ".ooo.ooo.",
            "..o...o..",
            ".o.o.o.o.",
            "o.......o"};
    static final String[] TORCH = {
            ".F.",
            "fFf",
            ".f.",
            ".k.",
            ".k.",
            ".K.",
            ".K."};
    static final int[] TORCH_GRIP = {1, 5};

    // ---------- things he plays with ----------
    static final String[] BOOK = {
            "RRRRRRRRR",
            "RrrrrrrrR",
            "RryyyyyrR",
            "RrrrrrrrR",
            "RrrryrrrR",
            "RrrrrrrrR",
            "ppppppppp"};
    static final String[] BOOK2 = {
            "RRRRpRRRR",
            "RrrrpprrR",
            "Rryyppy.r",
            "RrrrpprrR",
            "RrrrprrrR",
            "RrrrrrrrR",
            "ppppppppp"};
    static final String[] BALL = {
            ".rrw.",
            "rrwyy",
            "rwwyy",
            "wwyyg",
            ".ygg."};
    static final String[] PLANE = {
            "ppq....",
            ".pppp..",
            "..qpppp",
            ".qq...."};
    static final String[] BUBBLE = {
            ".lll.",
            "lw..l",
            "l...l",
            "l...l",
            ".lll."};
    static final String[] WAND = {
            ".uu.",
            "u..u",
            ".uu.",
            "..k.",
            "..k.",
            "..k."};
    static final int[] WAND_GRIP = {2, 5};
    static final String[] MAGIC_WAND = {
            "...w",
            "..o.",
            ".o..",
            "o..."};
    static final int[] MAGIC_WAND_GRIP = {0, 3};
    static final String[] YOYO = {
            ".u.",
            "uUu",
            ".u."};
    static final String[] FLY1 = {
            "YY.YY",
            "yyeyy",
            ".yey."};
    static final String[] FLY2 = {
            ".y.y.",
            ".yey.",
            "..e.."};
    static final String[] MUG = {
            "ppppp..",
            "prrrpqq",
            "ppppp.q",
            "prrrpqq",
            "ppppp..",
            ".ppp..."};
    static final int[] MUG_GRIP = {0, 2};
    static final String[] GUITAR = {
            "......YYYY..",
            "K....YYYYYY.",
            "KkkkkkYoooYY",
            "K....YYYYYY.",
            "......YYYY.."};
    static final String[] JUGGLE1 = {".r.", "rwr", ".R."};
    static final String[] JUGGLE2 = {".y.", "ywy", ".Y."};
    static final String[] JUGGLE3 = {".m.", "mwm", ".M."};
    static final String[] KITE = {
            "...r...",
            "..rry..",
            ".rrkyy.",
            "rkkkkky",
            ".rrkyy.",
            "..rky..",
            "...y..."};
    static final String[] BOW = {"u.u", ".u."};
    static final String[] ROD = {
            "......q",
            ".....k.",
            "....k..",
            "...k...",
            "..k....",
            ".K.....",
            "K......"};
    static final int[] ROD_GRIP = {0, 6};
    static final String[] BOBBER = {"r", "w"};
    static final String[] FISH = {
            "..mm..m",
            ".mmmmmm",
            "mxmmmmm",
            ".mmmmmm",
            "..mm..m"};
    static final String[] BOOT = {
            ".kkk..",
            ".kkk..",
            ".kkk..",
            ".kkkkk",
            "KKKKKK"};
    static final String[] SHRIMP = {
            "...aaaa..",
            ".aaAAAaax",
            "aa.....A.",
            "a.....A..",
            "YY......."};
    static final String[] RAINCLOUD = {
            "...qq.qqq....",
            "..qqqqqqqqq..",
            ".qqqqqqqqqqqq",
            "NNNNNNNNNNNNN"};
    static final String[] DROP = {"m", "m", "M"};
    static final String[] UMBRELLA = {
            ".....rrrrr.....",
            "...rrrrrrrrr...",
            "..rrrrrrrrrrr..",
            ".rrrrrrrrrrrrr.",
            "rRrrRrrRrrRrrRr",
            ".......K.......",
            ".......K.......",
            ".......K......."};
    static final String[] RAINBOW = {
            "....rrrrrrr....",
            "..rrYYYYYYYrr..",
            ".rYYyyyyyyyYYr.",
            "rYyygggggggyyYr",
            "Yygg.......ggyY",
            "ygm.........mgy",
            "gm...........mg"};
    static final String[] BOARD = {
            "KkkkkkkkkkkkkkkK",
            ".KKKKKKKKKKKKKK.",
            "..oo........oo.."};
    static final String[] BOARD_FLIP = {
            "..oo........oo..",
            ".KKKKKKKKKKKKKK.",
            "KkkkkkkkkkkkkkkK"};
    static final String[] BOARD_EDGE = {"kKkKkKkKkKkKkKkK"};
    static final String[] CONE3 = {
            ".iii.",
            "iiwii",
            "iiiii",
            "YKYKY",
            ".YKY.",
            ".KYK.",
            "..Y.."};
    static final String[] CONE2 = {
            ".....",
            ".iii.",
            "iiiii",
            "YKYKY",
            ".YKY.",
            ".KYK.",
            "..Y.."};
    static final String[] CONE1 = {
            ".....",
            ".....",
            ".iIi.",
            "YKYKY",
            ".YKY.",
            ".KYK.",
            "..Y.."};
    static final int[] CONE_GRIP = {2, 5};
    static final String[] BALLOON = {
            ".rrr.",
            "rrwrr",
            "rwrrr",
            "rrrrr",
            ".rrr.",
            "..R.."};
    static final String[] TOPHAT = {
            "..ooooo..",
            "..ooooo..",
            "..ooooo..",
            "..rrrrr..",
            "ooooooooo"};
    static final String[] DOVE = {
            "..w..",
            ".www.",
            "wwwqY",
            "..q.."};
    static final String[] TINY_SQUIB = {
            "vvvvv",
            "vxvxv",
            "vvvvv",
            "v.v.v"};
    static final String[] FLOWERS = {
            "r.y.u",
            ".rgu.",
            "..g..",
            "..g.."};
    static final String[] PHONE = {
            "eee",
            "eme",
            "eme",
            "eee"};
    static final int[] PHONE_GRIP = {1, 3};
    static final String[] BRUSH = {
            "r",
            "k",
            "k",
            "k"};
    static final int[] BRUSH_GRIP = {0, 3};
    static final String[] EASEL = {
            "kkkkkkkkkk",
            "kppppppppk",
            "kppppppppk",
            "kppppppppk",
            "kppppppppk",
            "kppppppppk",
            "kppppppppk",
            "kkkkkkkkkk",
            ".K......K.",
            ".K......K.",
            "K........K",
            "K........K"};
    /** Pictures he paints: 8x6 each, revealed row by row. */
    static final String[][] PAINTINGS = {
            {"........", "...aaaa.", ".aaAAAax", "aa....A.", "a....A..", "YY......"},
            {"..yyyy..", ".yyyyyy.", "yyyyyyyy", "yyyyyyyy", ".yyyyyy.", "gggggggg"},
            {".rr..rr.", "rrrrrrrr", "rrrrrrrr", ".rrrrrr.", "..rrrr..", "...rr..."},
            {"vvvvvvvv", "vxvvvvxv", "vvvvvvvv", "vvvvvvvv", "v.v..v.v", "v.v..v.v"},
            {"mmmmmmmm", "mmmyymmm", "mmyyyymm", "mmmmmmmm", "MMMMMMMM", "gggggggg"}};
    static final String[] STREAK = {
            "w....",
            ".Zs..",
            "...s.",
            "....s"};
    static final String[] FLASH = {
            "wwwww",
            "wwwww",
            "wwwww",
            "wwwww",
            "wwwww"};
    static final String[] POP = {"w.w", ".w.", "w.w"};

    // ---------- batch two ----------
    static final String[] FIRE1 = {
            "...F...",
            "..fF...",
            "..fFf..",
            ".ffFff.",
            "kKkKkKk",
            "KkKkKkK"};
    static final String[] FIRE2 = {
            "....F..",
            "...Ff..",
            "..fFf..",
            ".fffFf.",
            "kKkKkKk",
            "KkKkKkK"};
    static final String[] DRUM = {
            "..qqqqqq..",
            ".qwwwwwwq.",
            "rqqqqqqqqr",
            "rRryrRryrR",
            "rRrRrRrRrR",
            ".rrrrrrrr."};
    static final String[] BOOMERANG = {
            "kkkkk",
            "kKKKk",
            "kK...",
            "kK...",
            "kk..."};
    static final int[] BOOMERANG_GRIP = {0, 4};
    static final String[] SOIL = {
            ".nnn.",
            "nnnnn"};
    static final String[][] SPROUT = {
            {".....", ".....", ".....", ".....", "..g..", "..g.."},
            {".....", ".....", "..g..", ".gg..", "..gg.", "..g.."},
            {".....", "..r..", ".rgr.", "..g..", ".ggg.", "..g.."},
            {".r.r.", "rrYrr", ".rrr.", "..g..", "gggg.", "..g.."}};
    static final String[] CAN = {
            "..mm...",
            ".mmmm.m",
            "mmmmmm.",
            ".mmmm.."};
    static final int[] CAN_GRIP = {0, 2};
    static final String[] PIN = {
            ".w.",
            "www",
            "wrw",
            "www",
            "www"};
    static final String[] BOWL = {
            ".MM.",
            "MzMM",
            "MMMM",
            ".MM."};
    static final String[] BRICK = {
            "SSS",
            "ShS",
            "SSS"};
    static final String[] PLANK = {
            "kkkkkkkkk",
            "KkKkKkKkK"};
    static final String[] HALF_PLANK = {
            "kkkk",
            "KkKk"};
    static final String[] DUMBBELL = {
            "o.....o",
            "oqqqqqo",
            "o.....o"};
    static final int[] DUMBBELL_GRIP = {3, 1};
    static final String[] FRIEND1 = {
            "..bbbbbbbb..",
            "..bbbbbbbb..",
            "..bbbbxbbx..",
            "bbbbbbbbbbbb",
            "..bbbbbbbb..",
            "..bbbbbbbb..",
            "..b.b..b.b..",
            "..b.b..b.b.."};
    static final String[] FRIEND2 = {
            "..bbbbbbbb..",
            "..bbbbbbbb..",
            "..bbbbxbbx..",
            "bbbbbbbbbbbb",
            "..bbbbbbbb..",
            "..bbbbbbbb..",
            "..b.b..b.b..",
            "...b.b..b.b."};
    static final String[] ENVELOPE = {
            "ppppppp",
            "pqpppqp",
            "ppqrqpp",
            "ppppppp",
            "ppppppp"};
    static final String[] LETTER = {
            "ppppppp",
            "pqqqqqp",
            "ppppppp",
            "pqqqqpp",
            "ppppppp",
            "pqqqppp"};
    static final String[] SPINNER1 = {
            "..u..",
            "..u..",
            "..w..",
            ".u.u.",
            "u...u"};
    static final String[] SPINNER2 = {
            "u...u",
            ".u.u.",
            "..w..",
            "..u..",
            "..u.."};
    static final String[] STAR = {".y.", "yyy", ".y."};

    // ---------- hats ----------
    static final String[] PARTY_HAT = {
            "...w...",
            "...u...",
            "..uyu..",
            "..yuy..",
            ".uyuyu.",
            ".yuyuy.",
            "uyuyuyu"};
    static final String[] WINTER_HAT = {
            "......ww......",
            ".....wwww.....",
            "....rrrrrr....",
            "..rrrrrrrrrr..",
            ".rrrrrrrrrrrr.",
            "wwwwwwwwwwwwww"};
    static final String[] WITCH_HAT = {
            "......U.........",
            "......UU........",
            ".....UUU........",
            ".....UUUU.......",
            "....UUUUU.......",
            "....UyyyyU......",
            "..UUUUUUUUUUU...",
            "UUUUUUUUUUUUUUUU"};

    // ---------- little effect pixels ----------
    static final String[] STEAM = {"s"};
    static final String[] CRUMB1 = {"n"};
    static final String[] CRUMB2 = {"N"};
    static final String[] STONE1 = {"S"};
    static final String[] STONE2 = {"h"};
    static final String[] SPARK = {".w.", "wcw", ".w."};
    static final String[] SWOOSH = {"w"};
    static final String[] FLAME = {"f"};
    static final String[] SNOW = {".Z.", "ZwZ", ".Z."};
    static final String[] DRIP = {"i"};
    static final String[][] CONFETTI = {{"r"}, {"y"}, {"m"}, {"g"}, {"u"}, {"i"}};
}
