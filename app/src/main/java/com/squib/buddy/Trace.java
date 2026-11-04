package com.squib.buddy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Squib's log, for working out bugs. Everything he notices and does is written here: what Android says about
 * the screen, the app on screen and music, every poll, the headphones going on and off and why, activities,
 * lines he says, touches, downloads and crashes.
 *
 * Kept in the app's own storage as squib-log.txt, about 2 MB, with the one before it in squib-log.1.txt, so
 * there's roughly the last several hours of screen-on time. "export log" in his settings saves both as one file.
 * Plain Java (no Android classes) so the sim can use it too: there, -Dtrace=1 prints it instead.
 */
final class Trace {
    private Trace() {
    }

    static final int TRACE = 0, DEBUG = 1, INFO = 2, WARN = 3, ERROR = 4;
    private static final String[] LEVEL = {"T", "D", "I", "W", "E"};
    private static final long MAX = 2000000;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);
    private static final boolean PRINT = System.getProperty("trace") != null;
    private static File file, old;
    private static Writer out;
    private static long size;

    static synchronized void init(File dir) {
        if (out != null) {
            return;
        }
        file = new File(dir, "squib-log.txt");
        old = new File(dir, "squib-log.1.txt");
        open();
    }

    private static void open() {
        try {
            size = file.exists() ? file.length() : 0;
            out = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
        } catch (IOException e) {
            out = null;
        }
    }

    static synchronized void log(int level, String tag, String msg) {
        if (out == null && !PRINT) {
            return;
        }
        String line = TIME.format(new Date()) + " " + LEVEL[level] + " " + tag + ": " + msg + "\n";
        if (PRINT) {
            System.out.print(line);
        }
        if (out == null) {
            return;
        }
        try {
            out.write(line);
            size += line.length();
            if (level >= INFO) {
                out.flush();
            }
            if (size > MAX) {
                out.close();
                old.delete();
                file.renameTo(old);
                open();
            }
        } catch (IOException e) {
            // logging must never break him
        }
    }

    static void t(String tag, String msg) {
        log(TRACE, tag, msg);
    }

    static void d(String tag, String msg) {
        log(DEBUG, tag, msg);
    }

    static void i(String tag, String msg) {
        log(INFO, tag, msg);
    }

    static void w(String tag, String msg) {
        log(WARN, tag, msg);
    }

    static void e(String tag, String msg, Throwable err) {
        StringWriter sw = new StringWriter();
        if (err != null) {
            err.printStackTrace(new PrintWriter(sw));
        }
        log(ERROR, tag, msg + (err != null ? "\n" + sw : ""));
        flush();
    }

    static synchronized void flush() {
        try {
            if (out != null) {
                out.flush();
            }
        } catch (IOException e) {
        }
    }

    static synchronized void clear() {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
        }
        if (old != null) {
            old.delete();
        }
        if (file != null) {
            file.delete();
            open();
        }
    }

    /** The older file, then the current one, after a header. */
    static synchronized void exportTo(String header, OutputStream os) throws IOException {
        flush();
        os.write(header.getBytes("UTF-8"));
        for (File f : new File[]{old, file}) {
            if (f == null || !f.exists()) {
                continue;
            }
            InputStream in = new FileInputStream(f);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
            }
            in.close();
        }
        os.flush();
    }

    /** How big the log is, in KB, for his settings. */
    static synchronized long kb() {
        long n = (file != null && file.exists() ? file.length() : 0) + (old != null && old.exists() ? old.length() : 0);
        return n / 1024;
    }
}
