#!/usr/bin/env bash
# Runs squib on a computer, no phone needed. Needs Java 17+.
#   tools/sim/run.sh sheets            picture of every activity -> build/sim/*.png
#   tools/sim/run.sh sheets KITE MC    only some
#   tools/sim/run.sh stress 8          8 simulated hours of random use, fails on any crash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
python3 tools/gen_lines.py > /dev/null
S=app/src/main/java/com/squib/buddy
OUT=build/sim
rm -rf "$OUT/classes" && mkdir -p "$OUT/classes"
javac -nowarn -encoding UTF-8 -d "$OUT/classes" $(find tools/sim/fake -name "*.java") \
  $S/SquibView.java $S/Fun.java $S/Art.java $S/Lines.java $S/Pick.java $S/Apps.java $S/PixelFont.java $S/Prefs.java $S/Props.java $S/Trace.java \
  tools/sim/Sim.java 2>&1 | grep -v "^Note" || true
java -Dout="$OUT" ${SIM_OPTS:-} -cp "$OUT/classes" com.squib.buddy.Sim "$@"
