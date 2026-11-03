// Port: turns squib's engine (the Android app's Java) into C# for the desktop app, so both apps
// run the same code. Run by desktop/build.cmd; writes desktop/gen/*.cs. Usage: port.exe <repo root>
//
// It's a rule-based translator for the plain Java squib is written in (no lambdas, Java 8), not a
// general one. What the rules can't handle goes in desktop/port/patches.txt: small find/replace
// fixes applied to the generated C#. If the Java changes so a patch no longer matches, the port
// says which one and fails, so the desktop app never silently drifts from the phone.

using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Web.Script.Serialization;

static class Port
{
    // Java files that make up the engine. The rest (Android services, settings screen, downloads)
    // have desktop versions written by hand in desktop/src.
    static readonly string[] Files = { "SquibView", "Fun", "Art", "Props", "Pick", "PixelFont", "Prefs" };

    // Enums whose constants appear as bare case labels in Java (C# wants them qualified).
    static readonly Dictionary<string, string> EnumOf = new Dictionary<string, string>();

    static readonly List<string> problems = new List<string>();

    static int Main(string[] args)
    {
        string root = Path.GetFullPath(args.Length > 0 ? args[0] : ".");
        string src = Path.Combine(root, @"app\src\main\java\com\squib\buddy");
        string gen = Path.Combine(root, @"desktop\gen");
        Directory.CreateDirectory(gen);

        foreach (string k in "OPEN BLINK HAPPY SLEEPY SURPRISED ANNOYED".Split(' ')) EnumOf[k] = "SquibView.Face";
        foreach (string k in "IDLE WANDER SLEEP DRAG FALL JET CHASE".Split(' ')) EnumOf[k] = "SquibView.Motion";
        foreach (string k in "HOME WATCH MUSIC SCROLL CHAT GAME NAV READ HIDE".Split(' ')) EnumOf[k] = "Apps.Kind";

        var patches = Patches.Load(Path.Combine(root, @"desktop\port\patches.txt"));
        foreach (string name in Files)
        {
            string java = File.ReadAllText(Path.Combine(src, name + ".java"));
            string cs = Translate(java, name, patches.Renames);
            cs = patches.Apply(name, cs, problems);
            Write(Path.Combine(gen, name + ".cs"), cs);
        }
        Write(Path.Combine(gen, "Lines.cs"), LinesFromJson(Path.Combine(root, "squib.json")));
        Write(Path.Combine(gen, "Version.cs"), VersionFromGradle(Path.Combine(root, @"app\build.gradle")));

        if (problems.Count > 0)
        {
            Console.Error.WriteLine("port: " + problems.Count + " problem(s):");
            foreach (string p in problems) Console.Error.WriteLine("  " + p);
            return 1;
        }
        Console.WriteLine("port: wrote " + (Files.Length + 2) + " files to desktop\\gen");
        return 0;
    }

    static void Write(string path, string text)
    {
        // only touch files that changed, so timestamps mean something
        string old = File.Exists(path) ? File.ReadAllText(path) : null;
        if (old != text) File.WriteAllText(path, text, new UTF8Encoding(false));
    }

    // ------------------------------------------------------------------ the translator

    static string Translate(string java, string file, Dictionary<string, string> renames)
    {
        var held = new List<string>();
        string code = Mask(java, held);

        code = Regex.Replace(code, @"^\s*(package|import)\s[^;]*;[ \t]*\r?\n", "", RegexOptions.Multiline);
        // Java names that are C# keywords
        code = Regex.Replace(code, @"(?<![\w$@])(out|in|ref|base|params|lock|event|checked|unchecked|fixed|internal|is|as|operator|override|readonly|sizeof|typeof|delegate|implicit|explicit|unsafe|virtual|decimal|ulong|uint|ushort|sbyte|namespace|using|foreach|extern|stackalloc|struct|sealed|object|string|bool)\b", "@$1");
        code = Regex.Replace(code, @"@(Override|SuppressWarnings(\([^)]*\))?|SuppressLint(\([^)]*\))?)\s*", "");
        code = Regex.Replace(code, @"\)\s*throws\s+[\w.]+(\s*,\s*[\w.]+)*\s*\{", ") {");

        code = Members(code);

        // Java library names -> desktop stand-ins (desktop/src/Java.cs) or C# types
        code = Regex.Replace(code, @"\bString\.(\w+)", "JString.$1");
        code = Regex.Replace(code, @"\bInteger\.(MAX_VALUE|MIN_VALUE)\b", m => m.Groups[1].Value == "MAX_VALUE" ? "int.MaxValue" : "int.MinValue");
        code = Regex.Replace(code, @"\bInteger\.(\w+)\(", "JInt.$1(");
        code = Regex.Replace(code, @"\bDouble\.(NaN|MAX_VALUE|POSITIVE_INFINITY|NEGATIVE_INFINITY)\b", m => "double." + new Dictionary<string, string> { { "NaN", "NaN" }, { "MAX_VALUE", "MaxValue" }, { "POSITIVE_INFINITY", "PositiveInfinity" }, { "NEGATIVE_INFINITY", "NegativeInfinity" } }[m.Groups[1].Value]);
        code = Regex.Replace(code, @"\bDouble\.isNaN\(", "double.IsNaN(");
        code = Regex.Replace(code, @"\b(Double|Float|Long|Boolean)\.valueOf\(", m => "JBox." + m.Groups[1].Value[0] + "(");
        code = Regex.Replace(code, @"\bNumberFormatException\b", "FormatException");
        code = Regex.Replace(code, @"\.(doubleValue|intValue|floatValue|longValue|booleanValue)\(\)", ".Value");
        code = Regex.Replace(code, @"\bString\b", "string");
        code = Regex.Replace(code, @"\bboolean\b", "bool");
        code = Regex.Replace(code, @"\bbyte\b", "sbyte");
        code = Regex.Replace(code, @"\bObject\b", "object");
        code = Regex.Replace(code, @"\bInteger\b", "int?");
        code = Regex.Replace(code, @"\bDouble\b", "double?");
        code = Regex.Replace(code, @"\bFloat\b", "float?");
        code = Regex.Replace(code, @"\bLong\b", "long?");
        code = Regex.Replace(code, @"new\s+EnumMap<>\([\w.]+\.class\)", "new HashMap<>()");
        code = Regex.Replace(code, @"\bList<", "ArrayList<");
        code = Regex.Replace(code, @"\bSet<", "HashSet<");
        code = Regex.Replace(code, @"\b(Map|EnumMap)<", "HashMap<");
        code = Regex.Replace(code, @"\bSystem\.(currentTimeMillis|arraycopy|nanoTime)\b", "JSystem.$1");
        code = Regex.Replace(code, @"\b([\w.]+)\.class\.getDeclaredField\(", "JReflect.field(typeof($1), ");
        code = Regex.Replace(code, @"\.get\(null\)", ".get()");
        code = Regex.Replace(code, @"\b([\w.]+(?:\([^()]*\))?)\.clone\(\)", "JArr.copy($1)");
        code = Regex.Replace(code, @"\bnew\s+(char|int|float|double|bool)\[([^\[\]]+)\]\[([^\[\]]+)\]", "JArr.grid<$1>($2, $3)");
        code = Regex.Replace(code, @"(\w+(?:\.\w+)*|\([^()]*\))\s*>>>\s*(\w+)", "JBits.urs($1, $2)");
        code = Regex.Replace(code, @"\b0x([0-9A-Fa-f]{8})\b(?![Ll])", m => Convert.ToUInt32(m.Groups[1].Value, 16) >= 0x80000000u ? "unchecked((int)0x" + m.Groups[1].Value + ")" : m.Value);
        code = Regex.Replace(code, @"\.length\b(?!\s*\()", ".Length");
        code = Regex.Replace(code, @"(\w+)\s*\.\.\.\s*(\w+)", "params $1[] $2");
        code = Regex.Replace(code, @"\bfor\s*\(\s*([\w.<>\[\], ?]+?)\s+(\w+)\s*:\s*", "foreach ($1 $2 in ");
        code = Regex.Replace(code, @"\bfinal\s+", "");
        code = Regex.Replace(code, @"\bsuper\(", "super_(");
        code = Diamonds(code);
        code = CaseLabels(code);
        code = JaggedArrays(code);
        code = QualifyHidden(code, file);
        code = SwitchEnds(code);
        // Java turns int constants into chars where needed; C# needs a cast
        code = Regex.Replace(code, @"\bcase\s+(\d+)\s*:", m => int.Parse(m.Groups[1].Value) >= 128 ? "case (char) " + m.Groups[1].Value + ":" : m.Value);
        code = CharReturns(code);
        code = Regex.Replace(code, @"\bjava\.(util|lang)\.", "");

        foreach (var r in renames) code = Regex.Replace(code, @"(?<![\w$])" + Regex.Escape(r.Key) + @"(?=\s*\()", r.Value);

        code = Unmask(code, held);
        return "// GENERATED from app/src/main/java/com/squib/buddy/" + file + ".java by desktop/port/Port.cs.\r\n"
             + "// Don't edit: change the Java (or desktop/port/patches.txt) and run desktop/build.cmd.\r\n"
             + "#pragma warning disable\r\nusing System;\r\n\r\nnamespace Squib.Engine\r\n{\r\n" + code.TrimEnd() + "\r\n}\r\n";
    }

    // Swaps comments and string/char literals for placeholders so the rules only see code.
    static string Mask(string s, List<string> held)
    {
        var sb = new StringBuilder();
        int i = 0;
        while (i < s.Length)
        {
            char c = s[i];
            int start = i;
            if (c == '/' && i + 1 < s.Length && s[i + 1] == '/') { while (i < s.Length && s[i] != '\n') i++; }
            else if (c == '/' && i + 1 < s.Length && s[i + 1] == '*') { i = s.IndexOf("*/", i + 2) + 2; }
            else if (c == '"' || c == '\'')
            {
                i++;
                while (s[i] != c) i += s[i] == '\\' ? 2 : 1;
                i++;
            }
            else { sb.Append(c); i++; continue; }
            held.Add(s.Substring(start, i - start));
            sb.Append('\u0001').Append(held.Count - 1).Append('\u0002');
        }
        return sb.ToString();
    }

    static string Unmask(string s, List<string> held)
    {
        return Regex.Replace(s, "\u0001(\\d+)\u0002", m =>
        {
            string t = held[int.Parse(m.Groups[1].Value)];
            if (t.StartsWith("/**")) t = "/*" + t.Substring(3); // not a C# doc comment
            return t;
        });
    }

    // Rewrites declarations at class level: everything public (Java's package access is C#'s
    // private), static final primitives become const, other finals readonly, static {} a static
    // constructor, nested static classes plain nested classes.
    static string Members(string code)
    {
        var sb = new StringBuilder();
        var stack = new Stack<string>(); // "class:Name", "enum", "block"
        int i = 0;
        bool atMember = true;
        while (i < code.Length)
        {
            bool inClass = stack.Count > 0 && stack.Peek().StartsWith("class:");
            bool top = stack.Count == 0;
            if ((inClass || top) && atMember)
            {
                // skip whitespace and placeholders (comments)
                int j = i;
                while (j < code.Length && (char.IsWhiteSpace(code[j]) || code[j] == '\u0001' || code[j] == '\u0002' || (char.IsDigit(code[j]) && Inside(code, j))))
                    j++;
                sb.Append(code, i, j - i);
                i = j;
                if (i >= code.Length) break;
                if (code[i] == '}') { stack.Pop(); sb.Append('}'); i++; continue; }
                if (code[i] == ';') { sb.Append(';'); i++; continue; }
                // read the header up to the first ; or { outside parens
                int depth = 0, k = i;
                while (k < code.Length)
                {
                    char ch = code[k];
                    if (ch == '(') depth++;
                    else if (ch == ')') depth--;
                    else if (depth == 0 && (ch == ';' || ch == '{')) break;
                    k++;
                }
                string header = code.Substring(i, k - i);
                char end = code[k];
                string cls = top ? null : stack.Peek().Substring(6);
                sb.Append(Rewrite(header, cls, end));
                i = k;
                if (end == ';') { sb.Append(';'); i++; continue; }
                // '{'
                sb.Append('{'); i++;
                Match m = Regex.Match(header, @"\b(class|enum|interface)\s+(\w+)");
                if (m.Success && !header.Contains("=")) stack.Push(m.Groups[1].Value == "enum" ? "enum" : "class:" + m.Groups[2].Value);
                else if (header.Contains("="))
                {
                    // array initializer: copy to the matching brace, then on to the ;
                    int d = 1;
                    while (d > 0) { char ch = code[i++]; sb.Append(ch); if (ch == '{') d++; else if (ch == '}') d--; }
                    while (code[i] != ';') sb.Append(code[i++]);
                    sb.Append(';'); i++;
                }
                else stack.Push("block");
                continue;
            }
            // inside a method or enum body: copy, tracking braces
            char c = code[i++];
            sb.Append(c);
            if (c == '{') stack.Push("block");
            else if (c == '}') { stack.Pop(); }
            atMember = true;
            if (!(stack.Count == 0 || stack.Peek().StartsWith("class:"))) atMember = false;
        }
        return sb.ToString();
    }

    static bool Inside(string s, int j)
    {
        // a digit that belongs to a placeholder like \u0001 12 \u0002
        int a = s.LastIndexOf('\u0001', j), b = s.LastIndexOf('\u0002', j);
        return a > b;
    }

    static readonly HashSet<string> Primitive = new HashSet<string> { "int", "long", "float", "double", "char", "boolean", "short", "byte", "String" };

    static string Rewrite(string header, string cls, char end)
    {
        Match m = Regex.Match(header, @"^((?:(?:public|private|protected|static|final|abstract|synchronized|volatile|transient|native)\s+)*)(.*)$", RegexOptions.Singleline);
        string mods = m.Groups[1].Value, rest = m.Groups[2].Value;
        bool isStatic = Regex.IsMatch(mods, @"\bstatic\b"), isFinal = Regex.IsMatch(mods, @"\bfinal\b");
        if (rest.Trim().Length == 0 && isStatic && end == '{') return "static " + cls + "() "; // static initializer
        if (Regex.IsMatch(rest, @"^class\b")) return "public partial " + Regex.Replace(rest, @"\s+extends\s+View\b", "");
        if (Regex.IsMatch(rest, @"^(enum|interface)\b")) return "public " + rest;
        bool method = (end == '{' && !rest.Contains("=")) || (end == ';' && Regex.IsMatch(rest, @"^[^=]*\("));
        if (method) return "public " + (isStatic ? "static " : "") + rest;
        // a field
        string type = Regex.Match(rest, @"^[\w.<>\[\], ?]+?(?=\s+\w+\s*(=|,|$))").Value.Trim();
        if (isStatic && isFinal && Primitive.Contains(type) && rest.Contains("=") && !Regex.IsMatch(rest, @"\bnew\b|\(")) return "public const " + rest;
        return "public " + (isStatic ? "static " : "") + (isFinal ? "readonly " : "") + rest;
    }

    // new Foo<>() -> new Foo<A, B>(), from the declaration on the left or the field's declared type.
    static string Diamonds(string code)
    {
        var declared = new Dictionary<string, string>();
        foreach (Match d in Regex.Matches(code, @"\b(?:ArrayList|HashMap|HashSet|ArrayDeque|IdentityHashMap)<"))
        {
            int close = Angle(code, d.Index + d.Length - 1);
            if (close < 0) continue;
            Match name = Regex.Match(code.Substring(close + 1), @"^\s*(?:\[\])*\s+(\w+)\s*[;=,)]");
            if (name.Success) declared[name.Groups[1].Value] = code.Substring(d.Index + d.Length, close - d.Index - d.Length);
        }
        return Regex.Replace(code, @"(\b(\w+)\s*=\s*)new\s+(\w+)<>", m =>
        {
            string args;
            if (!declared.TryGetValue(m.Groups[2].Value, out args)) { problems.Add("can't tell the type for new " + m.Groups[3].Value + "<>() assigned to " + m.Groups[2].Value); return m.Value; }
            return m.Groups[1].Value + "new " + m.Groups[3].Value + "<" + args + ">";
        });
    }

    static int Angle(string s, int open)
    {
        int d = 0;
        for (int i = open; i < s.Length; i++)
        {
            if (s[i] == '<') d++;
            else if (s[i] == '>' && --d == 0) return i;
            else if (s[i] == ';' || s[i] == '{') return -1;
        }
        return -1;
    }

    static string CaseLabels(string code)
    {
        return Regex.Replace(code, @"\bcase\s+([A-Z][A-Z_0-9]*)\s*:", m =>
        {
            string e;
            if (!EnumOf.TryGetValue(m.Groups[1].Value, out e)) return m.Value;
            return "case " + e + "." + m.Groups[1].Value + ":";
        });
    }

    // T[][] x = {{..}, {..}}: C# wants the inner ones as new T[] {..}.
    static string JaggedArrays(string code)
    {
        var sb = new StringBuilder(code);
        foreach (Match m0 in Regex.Matches(code, @"\b([\w.@]+)\[\]\[\]\s+@?\w+\s*=\s*\{|\bnew\s+([\w.@]+)\[\]\[\]\s*\{"))
        {
            Match m = m0;
            string type = m.Groups[1].Success ? m.Groups[1].Value : m.Groups[2].Value;
            // walk the initializer; braces one level in become new T[] {
            int depth = 0;
            var inserts = new List<int>();
            for (int i = m.Index + m.Length - 1; i < code.Length; i++)
            {
                if (code[i] == '{') { depth++; if (depth == 2 && !Regex.IsMatch(code.Substring(0, i), @"\]\s*$")) inserts.Add(i); }
                else if (code[i] == '}' && --depth == 0) break;
            }
            int shift = sb.Length - code.Length; // from earlier initializers
            for (int k = inserts.Count - 1; k >= 0; k--) sb.Insert(inserts[k] + shift, "new " + type + "[] ");
        }
        return sb.ToString();
    }

    // C# won't let the last case of a switch run off the end the way Java does: end every switch
    // with a break (a redundant one is only "unreachable code", which is fine).
    static string SwitchEnds(string code)
    {
        var sb = new StringBuilder(code);
        var ends = new List<int>();
        foreach (Match m in Regex.Matches(code, @"\bswitch\s*\("))
        {
            int i = m.Index + m.Length, d = 1;
            while (d > 0) { if (code[i] == '(') d++; else if (code[i] == ')') d--; i++; }
            while (code[i] != '{') i++;
            d = 0;
            for (; i < code.Length; i++)
            {
                if (code[i] == '{') d++;
                else if (code[i] == '}' && --d == 0) { ends.Add(i); break; }
            }
        }
        ends.Sort();
        for (int k = ends.Count - 1; k >= 0; k--)
        {
            int at = ends[k];
            int line = code.LastIndexOf('\n', at);
            string indent = code.Substring(line + 1, at - line - 1);
            if (indent.Trim().Length == 0) sb.Insert(line + 1, indent + "    break;\r\n");
            else sb.Insert(at, " break; ");
        }
        return sb.ToString();
    }

    // In methods that return a char, "return 0;" and "cond ? x : 0;" need (char) 0.
    static string CharReturns(string code)
    {
        var sb = new StringBuilder();
        int last = 0;
        foreach (Match m in Regex.Matches(code, @"public\s+(?:static\s+)?char\s+\w+\s*\([^)]*\)\s*\{"))
        {
            if (m.Index < last) continue;
            int i = m.Index + m.Length, d = 1;
            while (d > 0) { if (code[i] == '{') d++; else if (code[i] == '}') d--; i++; }
            string body = code.Substring(m.Index, i - m.Index);
            body = Regex.Replace(body, @"\breturn\s+0\s*;", "return (char) 0;");
            body = Regex.Replace(body, @"(\breturn\b[^;]*\?[^;]*:)\s*0\s*;", "$1 (char) 0;");
            sb.Append(code, last, m.Index - last).Append(body);
            last = i;
        }
        sb.Append(code, last, code.Length - last);
        return sb.ToString();
    }

    // Java lets a local share a method's name (int top = top(v)); C# doesn't, unless the call is
    // qualified. So calls to methods whose names are also used for locals get Class. or this.
    static string QualifyHidden(string code, string cls)
    {
        var methods = new Dictionary<string, bool>(); // name -> static
        foreach (Match m in Regex.Matches(code, @"^\s*public\s+(static\s+)?[\w.<>\[\]?@]+\s+(\w+)\s*\(", RegexOptions.Multiline))
            methods[m.Groups[2].Value] = m.Groups[1].Success;
        foreach (var kv in methods)
        {
            string name = kv.Key;
            if (!Regex.IsMatch(code, @"\b(?:int|long|float|double|char|bool|string|[A-Z]\w*)(?:<[^;=()]*>)?(?:\[\])*\??\s+" + name + @"\s*[=;,)]")) continue;
            string q = (kv.Value ? cls : "this") + ".";
            code = Regex.Replace(code, @"(?<![\w.@])" + name + @"(?=\s*\()", m =>
            {
                string before = code.Substring(System.Math.Max(0, m.Index - 40), System.Math.Min(40, m.Index));
                Match prev = Regex.Match(before, @"([\w\]>?]+)\s*$");
                bool decl = prev.Success && Regex.IsMatch(prev.Groups[1].Value, @"[\w\]>?]$") && !Regex.IsMatch(prev.Groups[1].Value, @"^(return|else|case|throw|new)$");
                return decl ? m.Value : q + m.Value;
            });
        }
        return code;
    }

    // ------------------------------------------------------------------ lines and version

    // Same groups as tools/gen_lines.py writes into Lines.java, straight from squib.json.
    static string LinesFromJson(string path)
    {
        var root = (Dictionary<string, object>)new JavaScriptSerializer { MaxJsonLength = int.MaxValue }.DeserializeObject(File.ReadAllText(path));
        var lines = (Dictionary<string, object>)root["lines"];
        var sb = new StringBuilder();
        sb.Append("// GENERATED from squib.json by desktop/port/Port.cs. Edit squib.json, not this file.\r\n");
        sb.Append("#pragma warning disable\r\nnamespace Squib.Engine\r\n{\r\n    public static partial class Lines\r\n    {\r\n");
        foreach (var g in lines)
        {
            if (!Regex.IsMatch(g.Key, "^[A-Z][A-Z0-9_]*$")) { problems.Add("squib.json: bad group name " + g.Key); continue; }
            sb.Append("        public static string[] ").Append(g.Key).Append(" = {\r\n");
            foreach (object o in (object[])g.Value)
            {
                string s = (string)o;
                foreach (char ch in s) if (ch < 32 || ch > 126) problems.Add("squib.json " + g.Key + ": the pixel font can't show '" + ch + "' in: " + s);
                sb.Append("            \"").Append(s.Replace("\\", "\\\\").Replace("\"", "\\\"")).Append("\",\r\n");
            }
            sb.Append("        };\r\n");
        }
        sb.Append("    }\r\n}\r\n");
        return sb.ToString();
    }

    static string VersionFromGradle(string path)
    {
        string g = File.ReadAllText(path);
        string name = Regex.Match(g, "versionName\\s+\"([^\"]+)\"").Groups[1].Value;
        string code = Regex.Match(g, @"versionCode\s+(\d+)").Groups[1].Value;
        if (name.Length == 0) problems.Add("app/build.gradle: no versionName");
        return "// GENERATED from app/build.gradle by desktop/port/Port.cs: the desktop app has the same version as the phone app.\r\n"
             + "namespace Squib.Engine\r\n{\r\n    public static class Version\r\n    {\r\n"
             + "        public const string Name = \"" + name + "\";\r\n        public const int Code = " + (code.Length > 0 ? code : "0") + ";\r\n    }\r\n}\r\n";
    }
}

// desktop/port/patches.txt:
//   rename NAME NEWNAME          method calls NAME( become NEWNAME( in every file (C# can't have a field
//                                and a method with the same name, or a local hiding a method)
//   @@ File                      the patches after this apply to gen/File.cs
//   <<<                          start of the text to find (must match exactly once)
//   ===                          start of the replacement
//   >>>                          end
// Lines starting with # are comments.
class Patches
{
    public readonly Dictionary<string, string> Renames = new Dictionary<string, string>();
    readonly List<string[]> list = new List<string[]>(); // file, find, replace, line number

    public static Patches Load(string path)
    {
        var p = new Patches();
        if (!File.Exists(path)) return p;
        string[] lines = File.ReadAllLines(path);
        string file = null;
        for (int i = 0; i < lines.Length; i++)
        {
            string l = lines[i];
            if (l.StartsWith("#") || l.Trim().Length == 0 && file == null) continue;
            if (l.StartsWith("rename ")) { string[] w = l.Split(' '); p.Renames[w[1]] = w[2]; continue; }
            if (l.StartsWith("@@ ")) { file = l.Substring(3).Trim(); continue; }
            if (l == "<<<")
            {
                int at = i + 1;
                var find = new List<string>(); var repl = new List<string>();
                i++;
                while (lines[i] != "===") find.Add(lines[i++]);
                i++;
                while (lines[i] != ">>>") repl.Add(lines[i++]);
                p.list.Add(new[] { file, string.Join("\r\n", find), string.Join("\r\n", repl), at.ToString() });
            }
        }
        return p;
    }

    public string Apply(string file, string cs, List<string> problems)
    {
        cs = cs.Replace("\r\n", "\n").Replace("\n", "\r\n");
        foreach (string[] p in list)
        {
            if (p[0] != file) continue;
            int at = cs.IndexOf(p[1], StringComparison.Ordinal);
            if (at < 0 || cs.IndexOf(p[1], at + 1, StringComparison.Ordinal) >= 0)
            {
                problems.Add("patches.txt line " + p[3] + " (" + file + "): " + (at < 0 ? "text not found" : "text found more than once") + ": " + p[1].Split('\n')[0].Trim());
                continue;
            }
            cs = cs.Substring(0, at) + p[2] + cs.Substring(at + p[1].Length);
        }
        return cs;
    }
}
