package com.squib.buddy;

import java.util.ArrayList;
import java.util.Random;

/**
 * Props that are drawn by code instead of stored as fixed pictures, so one animation can show
 * many versions: balls in any size, pattern and colours, fidget spinners of different shapes,
 * coins, and a twistable puzzle cube that really keeps track of its stickers.
 * Everything returns rows of Art colour characters, like the art in {@link Art}.
 */
final class Props {
    private Props() {
    }

    /** Colour pairs, light then dark, as Art.color characters. */
    static final String[] PAIRS = {"rR", "yY", "mM", "gG", "uU", "iI", "vV", "YR", "cb", "wq", "eo"};
    static final String[] PAIR_NAMES = {"red", "yellow", "blue", "green", "purple", "pink", "coral", "orange", "teal",
            "white", "black"};
    /** How many of PAIRS are bright enough for balloons and toys (the rest are white and black). */
    static final int BRIGHT = 9;

    static char light(int pair) {
        return PAIRS[pair].charAt(0);
    }

    static char dark(char ch) {
        for (String p : PAIRS) {
            if (p.charAt(0) == ch) {
                return p.charAt(1);
            }
        }
        switch (ch) {
            case 'p': return 'q';
            case 'q': return 'h';
            case 'R': return 'K';
            default: return ch;
        }
    }

    /** Two different colour pairs. */
    static int[] twoColors(Random rng, int count) {
        int a = rng.nextInt(count), b = rng.nextInt(count - 1);
        if (b >= a) {
            b++;
        }
        return new int[]{a, b};
    }

    // ================= spinning top =================

    /**
     * A spinning top in colour pair `pair`: 9 wide (room to lean) and 5 tall. A grey handle, a body of
     * light and dark stripes, a black tip. stripe (0 or 1) swaps the stripes, so they turn as it spins;
     * tilt (-2 to 2) leans it over from the tip, for the wobble before it falls.
     */
    static String[] topRows(int pair, int stripe, int tilt) {
        char light = light(pair), dark = PAIRS[pair].charAt(1);
        int[] half = {0, 1, 2, 1, 0};
        String[] rows = new String[5];
        for (int r = 0; r < 5; r++) {
            int shift = tilt * (4 - r) / 4;
            char[] row = new char[9];
            for (int x = 0; x < 9; x++) {
                row[x] = '.';
            }
            for (int x = -half[r]; x <= half[r]; x++) {
                char ch;
                if (r == 0) {
                    ch = 'h';
                } else if (r == 4) {
                    ch = 'o';
                } else {
                    ch = ((x + stripe) & 1) == 0 ? light : dark;
                }
                row[4 + x + shift] = ch;
            }
            rows[r] = new String(row);
        }
        return rows;
    }

    // ================= balls =================

    static final String[] PATTERNS = {"plain", "striped", "two-tone", "spotty", "beach", "checkered", "banded"};

    /** A ball look: {size, pattern, colour pair 1, colour pair 2}. pattern < 0 picks one. */
    static int[] ballLook(Random rng, int sizeLo, int sizeHi, int pattern) {
        int[] cs = twoColors(rng, PAIRS.length);
        if (rng.nextDouble() < 0.6) { // mostly bright, sometimes white or black as the second colour
            cs = twoColors(rng, BRIGHT);
            if (rng.nextDouble() < 0.3) {
                cs[1] = BRIGHT + rng.nextInt(PAIRS.length - BRIGHT);
            }
        }
        return new int[]{sizeLo + rng.nextInt(sizeHi - sizeLo + 1), pattern >= 0 ? pattern : rng.nextInt(PATTERNS.length), cs[0], cs[1]};
    }

    static String ballKey(int[] look, int rot) {
        return "BALL_" + look[0] + "_" + look[1] + "_" + look[2] + "_" + look[3] + "_" + (rot & 7);
    }

    /**
     * Draws a round ball. rot (0..7) turns the pattern in 45 degree steps so it can roll;
     * the shading and shine stay put, like light from the top left.
     */
    static String[] ballRows(int[] look, int rot) {
        return ballRows(look, rot, 0);
    }

    /** squash 0..2: squeezed flatter and wider by that many pixels (a stress ball). */
    static String[] ballRows(int[] look, int rot, int squash) {
        int size = look[0], pattern = look[1];
        char c1 = light(look[2]), c2 = light(look[3]);
        if (size <= 3 && squash == 0) { // too small for patterns: a dot of the second colour in the middle
            char mid = pattern == 0 ? 'w' : c2 == c1 ? 'w' : c2;
            return new String[]{"." + c1 + ".", "" + c1 + mid + c1, "." + dark(c1) + "."};
        }
        double r = size / 2.0;
        double ang = (rot & 7) * Math.PI / 4, cos = Math.cos(ang), sin = Math.sin(ang);
        int bw = size + squash, h = size - squash;
        double rx = bw / 2.0, ry = h / 2.0;
        int shineX = (int) Math.floor(rx - rx * 0.45), shineY = (int) Math.floor(ry - ry * 0.45);
        String[] rows = new String[h];
        for (int y = 0; y < h; y++) {
            StringBuilder b = new StringBuilder();
            for (int x = 0; x < bw; x++) {
                // squashed into a circle of radius r, so patterns and shading still fit
                double px = (x + 0.5 - rx) / rx * r, py = (y + 0.5 - ry) / ry * r;
                double dist = Math.hypot(px, py);
                if (dist > r - (size <= 3 ? 0.15 : 0.05)) {
                    b.append('.');
                    continue;
                }
                double qx = px * cos - py * sin, qy = px * sin + py * cos;
                char ch;
                switch (pattern) {
                    case 1: { // striped
                        double w = size <= 4 ? 1 : 1.5;
                        ch = ((int) Math.floor(qy / w + 100)) % 2 == 0 ? c1 : c2;
                        break;
                    }
                    case 2: // two-tone
                        ch = qx < 0 ? c1 : c2;
                        break;
                    case 3: // spotty
                        ch = (((int) Math.floor(qx + 100)) + 2 * ((int) Math.floor(qy + 100))) % 4 == 0 ? c2 : c1;
                        break;
                    case 4: { // beach ball: six slices
                        int slice = (int) Math.floor((Math.atan2(qy, qx) + Math.PI) / (2 * Math.PI) * 6) % 6;
                        ch = slice % 2 == 1 ? 'w' : slice % 4 == 0 ? c1 : c2;
                        if (dist < 0.6) {
                            ch = 'w';
                        }
                        break;
                    }
                    case 5: { // checkered
                        double w = size >= 6 ? 2 : 1;
                        ch = (((int) Math.floor(qx / w + 100)) + ((int) Math.floor(qy / w + 100))) % 2 == 0 ? c1 : c2;
                        break;
                    }
                    case 6: // banded: one stripe round the middle
                        ch = Math.abs(qy) < (size <= 4 ? 0.55 : 0.9) ? c2 : c1;
                        break;
                    default: // plain
                        ch = c1;
                }
                if (dist > r - 1.1 && (px + py) / (dist * 1.414 + 1e-9) > 0.3) {
                    ch = dark(ch);
                }
                if (size >= 4 && x == shineX && y == shineY && ch != 'w') {
                    ch = 'w';
                }
                b.append(ch);
            }
            rows[y] = b.toString();
        }
        return rows;
    }

    // ================= fidget spinners =================

    static final String[] SPINNERS = {"three arms", "two arms", "four arms", "ring", "star"};

    /**
     * The colour of one grid pixel of a spinner, or 0 for see-through. Each pixel is sampled
     * at 9 points so the shape stays clean at any angle.
     * dx, dy: offset from the middle. angle: how far it has turned.
     */
    static char spinnerPixel(int type, char body, char weight, double angle, int dx, int dy) {
        char[] seen = new char[9];
        int[] n = new int[9];
        int kinds = 0;
        for (int sy = -1; sy <= 1; sy++) {
            for (int sx = -1; sx <= 1; sx++) {
                char ch = spinnerAt(type, body, weight, angle, dx + sx / 3.0, dy + sy / 3.0);
                if (ch == 0) {
                    continue;
                }
                int k = 0;
                while (k < kinds && seen[k] != ch) {
                    k++;
                }
                if (k == kinds) {
                    seen[kinds++] = ch;
                }
                n[k]++;
            }
        }
        int best = -1, total = 0;
        for (int k = 0; k < kinds; k++) {
            total += n[k];
            if (best < 0 || n[k] > n[best]) {
                best = k;
            }
        }
        return total >= 4 ? seen[best] : 0;
    }

    static char spinnerAt(int type, char body, char weight, double angle, double x, double y) {
        double d = Math.hypot(x, y);
        if (d < 0.55) {
            return 'w'; // the bearing in the middle
        }
        if (d < 1.5) {
            return body;
        }
        if (type == 3) { // ring with three weights
            for (int k = 0; k < 3; k++) {
                double a = angle + k * Math.PI * 2 / 3;
                if (Math.hypot(x - Math.cos(a) * 3, y - Math.sin(a) * 3) < 1.15) {
                    return weight;
                }
            }
            return Math.abs(d - 3) < 0.6 ? body : 0;
        }
        int arms = type == 1 ? 2 : type == 2 ? 4 : type == 4 ? 5 : 3;
        double len = type == 2 ? 2.7 : type == 4 ? 3.2 : 2.9;
        double lobe = type == 4 ? 0.75 : type == 2 ? 1.1 : 1.4;
        for (int k = 0; k < arms; k++) {
            double a = angle + k * Math.PI * 2 / arms;
            double ex = Math.cos(a) * len, ey = Math.sin(a) * len;
            if (Math.hypot(x - ex, y - ey) < lobe) {
                return weight;
            }
            double t = Math.max(0, Math.min(1, (x * ex + y * ey) / (len * len)));
            if (Math.hypot(x - ex * t, y - ey * t) < (type == 4 ? 0.6 : 0.8)) {
                return body;
            }
        }
        return 0;
    }

    static double spinnerReach(int type) {
        return type == 3 ? 4.2 : type == 4 ? 4.0 : 4.3;
    }

    // ================= coins =================

    static final String[] METALS = {"gold", "silver", "copper"};
    static final String[] METAL_PAIRS = {"yY", "wq", "YR"};

    /** A 5-pixel coin, squashed to width w (1 is edge-on). heads shows a little squid face. */
    static String[] coinRows(int metal, int w, boolean heads) {
        char c = METAL_PAIRS[metal].charAt(0), dk = METAL_PAIRS[metal].charAt(1);
        String[] full = {
                "." + dk + dk + dk + ".",
                dk + "" + c + c + c + dk,
                dk + "" + c + c + c + dk,
                dk + "" + c + c + c + dk,
                "." + dk + dk + dk + "."};
        char[][] g = new char[5][];
        for (int i = 0; i < 5; i++) {
            g[i] = full[i].toCharArray();
        }
        if (heads) { // two eyes
            g[2][1] = 'o';
            g[2][3] = 'o';
        } else { // a dot in the middle
            g[2][2] = dk;
        }
        g[1][1] = 'w';
        if (w >= 5) {
            String[] out = new String[5];
            for (int i = 0; i < 5; i++) {
                out[i] = new String(g[i]);
            }
            return out;
        }
        String[] out = new String[5];
        for (int y = 0; y < 5; y++) {
            StringBuilder b = new StringBuilder();
            for (int x = 0; x < w; x++) {
                if (w == 1) {
                    b.append(y == 0 || y == 4 ? '.' : dk);
                } else {
                    int col = (int) Math.floor((x + 0.5) * 5.0 / w);
                    b.append(g[y][Math.min(4, col)]);
                }
            }
            out[y] = b.toString();
        }
        return out;
    }

    // ================= dice =================

    static final String[] DIE_COLORS = {"wq", "rR", "eo", "mM", "gG", "yY", "uU"};
    static final String[] DIE_NAMES = {"white", "red", "black", "blue", "green", "yellow", "purple"};

    /** A 5-pixel die showing face 1 to 6. */
    static String[] dieRows(int color, int face) {
        char body = DIE_COLORS[color].charAt(0), shade = DIE_COLORS[color].charAt(1);
        char pip = body == 'w' || body == 'y' ? 'o' : 'w';
        char[][] g = new char[5][5];
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                boolean corner = (x == 0 || x == 4) && (y == 0 || y == 4);
                g[y][x] = corner ? '.' : (y == 4 || x == 4) ? shade : body;
            }
        }
        int[][] pips;
        switch (face) {
            case 1: pips = new int[][]{{2, 2}}; break;
            case 2: pips = new int[][]{{1, 1}, {3, 3}}; break;
            case 3: pips = new int[][]{{1, 1}, {2, 2}, {3, 3}}; break;
            case 4: pips = new int[][]{{1, 1}, {3, 1}, {1, 3}, {3, 3}}; break;
            case 5: pips = new int[][]{{1, 1}, {3, 1}, {2, 2}, {1, 3}, {3, 3}}; break;
            default: pips = new int[][]{{1, 1}, {3, 1}, {1, 2}, {3, 2}, {1, 3}, {3, 3}};
        }
        for (int[] p : pips) {
            g[p[1]][p[0]] = pip;
        }
        String[] out = new String[5];
        for (int y = 0; y < 5; y++) {
            out[y] = new String(g[y]);
        }
        return out;
    }

    // ================= puzzle cube =================

    /**
     * A twistable puzzle cube (2x2 or 3x3). It keeps every sticker, so scrambling and
     * undoing the moves really does solve it. Coordinates are doubled so they stay whole
     * numbers: a 3x3 uses -2, 0, 2 and a 2x2 uses -1, 1.
     */
    static final class Cube {
        static final char[] FACE_COLORS = {'r', 'Y', 'w', 'y', 'g', 'm'}; // +x -x +y -y +z(front) -z
        final int n, edge;
        final ArrayList<int[]> st = new ArrayList<int[]>(); // {x, y, z, nx, ny, nz, colour}

        Cube(int n) {
            this.n = n;
            edge = n - 1;
            for (int f = 0; f < 6; f++) {
                int axis = f / 2, sgn = f % 2 == 0 ? 1 : -1;
                for (int a = -edge; a <= edge; a += 2) {
                    for (int b = -edge; b <= edge; b += 2) {
                        int[] s = new int[7];
                        s[axis] = sgn * edge;
                        s[(axis + 1) % 3] = a;
                        s[(axis + 2) % 3] = b;
                        s[3 + axis] = sgn;
                        s[6] = FACE_COLORS[f];
                        st.add(s);
                    }
                }
            }
        }

        /** Turns one layer a quarter turn. move = {axis, layer, dir}. */
        void turn(int[] move) {
            int axis = move[0], layer = move[1], dir = move[2];
            int i = (axis + 1) % 3, j = (axis + 2) % 3;
            for (int[] s : st) {
                if (s[axis] != layer) {
                    continue;
                }
                int a = s[i], b = s[j], na = s[3 + i], nb = s[3 + j];
                if (dir > 0) {
                    s[i] = -b;
                    s[j] = a;
                    s[3 + i] = -nb;
                    s[3 + j] = na;
                } else {
                    s[i] = b;
                    s[j] = -a;
                    s[3 + i] = nb;
                    s[3 + j] = -na;
                }
            }
        }

        int[] randomMove(Random rng, int[] last) {
            for (int tries = 0; tries < 20; tries++) {
                int axis = rng.nextInt(3);
                int layer = rng.nextBoolean() ? edge : -edge;
                if (n == 3 && rng.nextDouble() < 0.15) {
                    layer = 0;
                }
                int[] m = {axis, layer, rng.nextBoolean() ? 1 : -1};
                if (last == null || last[0] != m[0] || last[1] != m[1]) {
                    return m;
                }
            }
            return new int[]{0, edge, 1};
        }

        boolean solved() {
            for (int[] s : st) {
                for (int f = 0; f < 6; f++) {
                    int axis = f / 2, sgn = f % 2 == 0 ? 1 : -1;
                    if (s[3 + axis] == sgn && s[6] != FACE_COLORS[f]) {
                        return false;
                    }
                }
            }
            return true;
        }

        /** Colour of the sticker facing (nx, ny, nz) at a cubie position. */
        char at(int x, int y, int z, int nx, int ny, int nz) {
            for (int[] s : st) {
                if (s[0] == x && s[1] == y && s[2] == z && s[3] == nx && s[4] == ny && s[5] == nz) {
                    return (char) s[6];
                }
            }
            return 'o';
        }

        /**
         * The cube as seen from the front and a little from above and the right: a 1-pixel
         * strip of the top face, the front face, and a 1-pixel strip of the right face.
         * shift nudges one row (axis 1) or column (axis 0) by a pixel, to show it turning.
         */
        String[] rows(int[] shift) {
            int k = n == 3 ? 2 : 3; // sticker size in pixels
            int face = n * k + 2;
            char[][] g = new char[face + 1][face + 1];
            for (char[] row : g) {
                java.util.Arrays.fill(row, '.');
            }
            // top strip (row 0), offset one pixel right for depth
            for (int x = 1; x <= face; x++) {
                g[0][x] = 'o';
            }
            for (int c = 0; c < n; c++) {
                int cx = -edge + c * 2;
                char col = at(cx, edge, edge, 0, 1, 0);
                for (int p = 0; p < k; p++) {
                    g[0][2 + c * k + p] = col;
                }
            }
            // front face
            for (int y = 0; y < face; y++) {
                for (int x = 0; x < face; x++) {
                    g[1 + y][x] = 'o';
                }
            }
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    int cx = -edge + c * 2, cy = edge - r * 2;
                    char col = at(cx, cy, edge, 0, 0, 1);
                    int sx = 0, sy = 0;
                    if (shift != null && shift[0] == 1 && shift[1] == cy) {
                        sx = shift[2];
                    }
                    if (shift != null && shift[0] == 0 && shift[1] == cx) {
                        sy = shift[2];
                    }
                    for (int py = 0; py < k; py++) {
                        for (int px = 0; px < k; px++) {
                            int gx = 1 + c * k + px + sx, gy = 2 + r * k + py + sy;
                            if (gx >= 1 && gx < face - 1 && gy >= 2 && gy < face) {
                                g[gy][gx] = col;
                            }
                        }
                    }
                }
            }
            // right strip
            for (int y = 1; y <= face; y++) {
                g[y][face] = 'o';
            }
            for (int r = 0; r < n; r++) {
                int cy = edge - r * 2;
                char col = dark(at(edge, cy, edge, 1, 0, 0));
                for (int p = 0; p < k; p++) {
                    g[2 + r * k + p][face] = col;
                }
            }
            String[] out = new String[g.length];
            for (int i = 0; i < g.length; i++) {
                out[i] = new String(g[i]);
            }
            return out;
        }
    }
}
