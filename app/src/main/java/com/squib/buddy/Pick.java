package com.squib.buddy;

import android.content.SharedPreferences;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Random;

/**
 * Picks lines so they don't repeat. Each group is a shuffled deck: every line in it gets said
 * once before any line comes around again. It also remembers the last few hundred lines it
 * said (saved on the phone, so a restart doesn't reset it); when a deck is reshuffled, lines
 * said most recently go to the bottom.
 */
final class Pick {
    private Pick() {
    }

    static final int REMEMBER = 400;
    static final Random rng = new Random();
    static final IdentityHashMap<String[], ArrayDeque<String>> decks = new IdentityHashMap<String[], ArrayDeque<String>>();
    static final ArrayList<Integer> recent = new ArrayList<Integer>(); // hashes, oldest first
    static SharedPreferences prefs;

    static void init(SharedPreferences p) {
        if (prefs != null) {
            return;
        }
        prefs = p;
        String saved = p.getString("recentLines", "");
        for (String h : saved.split(",")) {
            try {
                if (!h.isEmpty()) {
                    recent.add(Integer.parseInt(h));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /** Forget decks, after the line groups change. */
    static void reset() {
        decks.clear();
    }

    static String one(String[] lines) {
        if (lines == null || lines.length == 0) {
            return "";
        }
        ArrayDeque<String> deck = decks.get(lines);
        if (deck == null || deck.isEmpty()) {
            deck = shuffle(lines);
            decks.put(lines, deck);
        }
        String s = deck.poll();
        remember(s);
        return s;
    }

    static ArrayDeque<String> shuffle(String[] lines) {
        ArrayList<String> fresh = new ArrayList<String>();
        final ArrayList<String> stale = new ArrayList<String>();
        for (String s : lines) {
            if (recent.contains(s.hashCode())) {
                stale.add(s);
            } else {
                fresh.add(s);
            }
        }
        Collections.shuffle(fresh, rng);
        Collections.sort(stale, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return recent.lastIndexOf(a.hashCode()) - recent.lastIndexOf(b.hashCode());
            }
        });
        ArrayDeque<String> deck = new ArrayDeque<String>(fresh);
        deck.addAll(stale);
        return deck;
    }

    static void remember(String s) {
        Integer h = s.hashCode();
        recent.remove(h);
        recent.add(h);
        while (recent.size() > REMEMBER) {
            recent.remove(0);
        }
        if (prefs != null) {
            StringBuilder b = new StringBuilder();
            for (Integer x : recent) {
                if (b.length() > 0) {
                    b.append(',');
                }
                b.append(x);
            }
            prefs.edit().putString("recentLines", b.toString()).apply();
        }
    }

    /** A chatter line, sometimes with an opener stuck in front ("hm. ", "ok so, "). */
    static String muse(String[] lines) {
        String s = one(lines);
        if (rng.nextDouble() < 0.25 && !s.startsWith("*") && !s.startsWith("{") && s.length() > 12
                && Lines.OPENERS != null && Lines.OPENERS.length > 0) {
            String o = Lines.OPENERS[rng.nextInt(Lines.OPENERS.length)];
            return o.endsWith(" ") ? o + s : o + " " + s;
        }
        return s;
    }
}
