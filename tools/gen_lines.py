#!/usr/bin/env python3
"""Writes app/src/main/java/com/squib/buddy/Lines.java from squib.json.

squib.json is the one place lines live. The app downloads it from GitHub, and this script
bakes the same lines into the app so he has them offline and on first start.
Run it after editing squib.json:  python3 tools/gen_lines.py
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "squib.json")
OUT = os.path.join(ROOT, "app", "src", "main", "java", "com", "squib", "buddy", "Lines.java")


def jstr(s):
    out = []
    for ch in s:
        if ch == "\\":
            out.append("\\\\")
        elif ch == '"':
            out.append('\\"')
        elif ch == "\n":
            out.append("\\n")
        elif 32 <= ord(ch) < 127:
            out.append(ch)
        else:
            out.append("\\u%04x" % ord(ch))
    return '"' + "".join(out) + '"'


def main():
    data = json.load(open(SRC, encoding="utf-8"))
    groups = data.get("lines", {})
    problems = []
    for name, lines in groups.items():
        if not re.fullmatch(r"[A-Z][A-Z0-9_]*", name):
            problems.append(f"group name {name!r} must be CAPITALS_AND_UNDERSCORES")
        for line in lines:
            bad = sorted({c for c in line if not (32 <= ord(c) < 127)})
            if bad:
                problems.append(f"{name}: {line!r} has characters the pixel font can't show: {bad}")
            if "{mins}" in line and name != "SCROLL_LONG":
                problems.append(f"{name}: {{mins}} only works in SCROLL_LONG: {line!r}")
    if problems:
        print("\n".join(problems))
        sys.exit(1)

    body = []
    for name, lines in groups.items():
        items = ",\n            ".join(jstr(x) for x in lines)
        body.append(f"    static String[] {name} = {{\n            {items}}};")
    java = f'''package com.squib.buddy;

import java.lang.reflect.Field;

/**
 * Built-in lines. GENERATED from squib.json by tools/gen_lines.py: edit squib.json, not this file.
 * The copy downloaded from GitHub replaces these group by group (see {{@link Remote}}).
 */
final class Lines {{
    private Lines() {{
    }}

{chr(10).join(body)}

    /** A line group by name, or null if there's no such group. */
    static String[] get(String name) {{
        try {{
            return (String[]) Lines.class.getDeclaredField(name).get(null);
        }} catch (Exception e) {{
            return null;
        }}
    }}

    /** Replaces a group. Returns false if there's no such group. */
    static boolean set(String name, String[] lines) {{
        try {{
            Field f = Lines.class.getDeclaredField(name);
            if (f.getType() != String[].class) {{
                return false;
            }}
            f.setAccessible(true);
            f.set(null, lines);
            Pick.reset();
            return true;
        }} catch (Exception e) {{
            return false;
        }}
    }}
}}
'''
    open(OUT, "w", encoding="utf-8").write(java)
    total = sum(len(v) for v in groups.values())
    print(f"wrote {OUT}: {len(groups)} groups, {total} lines")


if __name__ == "__main__":
    main()
