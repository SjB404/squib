// Stand-ins for the bits of the Java library squib's engine uses, with Java's behaviour
// (Math.round rounds half up, HashMap.get gives null for a missing key, Random is Java's
// generator, and so on), so the code generated from the Java runs the same as on the phone.

using System;
using System.Collections.Generic;
using System.Globalization;
using System.Reflection;
using System.Text.RegularExpressions;
using SC = System.Collections.Generic;

namespace Squib.Engine
{
    public static class Math
    {
        public const double PI = System.Math.PI, E = System.Math.E;
        public static int abs(int a) { return a < 0 ? -a : a; }
        public static long abs(long a) { return a < 0 ? -a : a; }
        public static float abs(float a) { return a < 0 ? -a : a; }
        public static double abs(double a) { return a < 0 ? -a : a; }
        public static int max(int a, int b) { return a > b ? a : b; }
        public static long max(long a, long b) { return a > b ? a : b; }
        public static float max(float a, float b) { return a > b ? a : b; }
        public static double max(double a, double b) { return a > b ? a : b; }
        public static int min(int a, int b) { return a < b ? a : b; }
        public static long min(long a, long b) { return a < b ? a : b; }
        public static float min(float a, float b) { return a < b ? a : b; }
        public static double min(double a, double b) { return a < b ? a : b; }
        public static int round(float a) { return float.IsNaN(a) ? 0 : (int)System.Math.Floor(a + 0.5f); }
        public static long round(double a) { return double.IsNaN(a) ? 0 : (long)System.Math.Floor(a + 0.5); }
        public static double floor(double a) { return System.Math.Floor(a); }
        public static double ceil(double a) { return System.Math.Ceiling(a); }
        public static double rint(double a) { return System.Math.Round(a, MidpointRounding.ToEven); }
        public static double sqrt(double a) { return System.Math.Sqrt(a); }
        public static double cbrt(double a) { return a < 0 ? -System.Math.Pow(-a, 1.0 / 3) : System.Math.Pow(a, 1.0 / 3); }
        public static double pow(double a, double b) { return System.Math.Pow(a, b); }
        public static double sin(double a) { return System.Math.Sin(a); }
        public static double cos(double a) { return System.Math.Cos(a); }
        public static double tan(double a) { return System.Math.Tan(a); }
        public static double asin(double a) { return System.Math.Asin(a); }
        public static double acos(double a) { return System.Math.Acos(a); }
        public static double atan(double a) { return System.Math.Atan(a); }
        public static double atan2(double y, double x) { return System.Math.Atan2(y, x); }
        public static double exp(double a) { return System.Math.Exp(a); }
        public static double log(double a) { return System.Math.Log(a); }
        public static double log10(double a) { return System.Math.Log10(a); }
        public static double hypot(double a, double b) { return System.Math.Sqrt(a * a + b * b); }
        public static float signum(float a) { return a > 0 ? 1 : a < 0 ? -1 : a; }
        public static double signum(double a) { return a > 0 ? 1 : a < 0 ? -1 : a; }
        public static double toRadians(double a) { return a / 180.0 * PI; }
        public static double toDegrees(double a) { return a * 180.0 / PI; }
        public static int floorMod(int a, int b) { int m = a % b; return m != 0 && ((m < 0) != (b < 0)) ? m + b : m; }
        public static long floorMod(long a, long b) { long m = a % b; return m != 0 && ((m < 0) != (b < 0)) ? m + b : m; }
        public static int floorDiv(int a, int b) { int q = a / b; return (a % b != 0 && ((a < 0) != (b < 0))) ? q - 1 : q; }
        static readonly Random shared = new Random();
        public static double random() { return shared.nextDouble(); }
    }

    // java.util.Random, the same generator as Java's.
    public class Random
    {
        long seed;
        static long uniquifier = 8682522807148012L;
        double nextNextGaussian; bool haveNextNextGaussian;

        public Random() : this(System.Threading.Interlocked.Increment(ref uniquifier) ^ DateTime.UtcNow.Ticks * 100) { }
        public Random(long s) { setSeed(s); }
        public void setSeed(long s) { seed = (s ^ 0x5DEECE66DL) & ((1L << 48) - 1); haveNextNextGaussian = false; }
        protected int next(int bits) { seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1); return (int)(seed >> (48 - bits)); }
        public int nextInt() { return next(32); }
        public int nextInt(int bound)
        {
            if (bound <= 0) throw new ArgumentException("bound must be positive");
            if ((bound & -bound) == bound) return (int)((bound * (long)next(31)) >> 31);
            int bits, val;
            do { bits = next(31); val = bits % bound; } while (bits - val + (bound - 1) < 0);
            return val;
        }
        public long nextLong() { return ((long)next(32) << 32) + next(32); }
        public bool nextBoolean() { return next(1) != 0; }
        public float nextFloat() { return next(24) / (float)(1 << 24); }
        public double nextDouble() { return (((long)next(26) << 27) + next(27)) * (1.0 / (1L << 53)); }
        public double nextGaussian()
        {
            if (haveNextNextGaussian) { haveNextNextGaussian = false; return nextNextGaussian; }
            double v1, v2, s;
            do { v1 = 2 * nextDouble() - 1; v2 = 2 * nextDouble() - 1; s = v1 * v1 + v2 * v2; } while (s >= 1 || s == 0);
            double mul = System.Math.Sqrt(-2 * System.Math.Log(s) / s);
            nextNextGaussian = v2 * mul; haveNextNextGaussian = true;
            return v1 * mul;
        }
    }

    public class Iterator
    {
        protected readonly System.Collections.IEnumerator e; bool peeked, more;
        public Iterator(System.Collections.IEnumerable src) { e = src.GetEnumerator(); }
        public bool hasNext() { if (!peeked) { more = e.MoveNext(); peeked = true; } return more; }
        public object next() { if (!hasNext()) throw new InvalidOperationException("no more"); peeked = false; return e.Current; }
    }

    public class Iterator<T> : Iterator
    {
        public Iterator(IEnumerable<T> src) : base(src) { }
        public new T next() { return (T)base.next(); }
    }

    public interface Comparator<T> { int compare(T a, T b); }

    public interface Runnable { void run(); }

    public class ArrayList<T> : SC.List<T>
    {
        public ArrayList() { }
        public ArrayList(int capacity) : base(capacity) { }
        public ArrayList(IEnumerable<T> items) : base(items) { }
        public bool add(T x) { Add(x); return true; }
        public void add(int i, T x) { Insert(i, x); }
        public bool addAll(IEnumerable<T> xs) { AddRange(xs); return true; }
        public T get(int i) { return this[i]; }
        public T set(int i, T x) { T old = this[i]; this[i] = x; return old; }
        public int size() { return Count; }
        public bool isEmpty() { return Count == 0; }
        public void clear() { Clear(); }
        public bool contains(T x) { return Contains(x); }
        public int indexOf(T x) { return IndexOf(x); }
        public int lastIndexOf(T x) { return LastIndexOf(x); }
        public T remove(int i) { T old = this[i]; RemoveAt(i); return old; }
        public bool remove(T x) { return Remove(x); }
        public T[] toArray(T[] unused) { return ToArray(); }
        public Iterator<T> iterator() { return new Iterator<T>(this); }
    }

    public class HashMap<K, V> : Dictionary<K, V>
    {
        public HashMap() { }
        public HashMap(IEqualityComparer<K> c) : base(c) { }
        public V get(K k) { V v; return k != null && TryGetValue(k, out v) ? v : default(V); }
        public V put(K k, V v) { V old = get(k); this[k] = v; return old; }
        public bool containsKey(K k) { return ContainsKey(k); }
        public V remove(K k) { V old = get(k); Remove(k); return old; }
        public int size() { return Count; }
        public bool isEmpty() { return Count == 0; }
        public void clear() { Clear(); }
        public IEnumerable<K> keySet() { return new SC.List<K>(Keys); }
        public IEnumerable<V> values() { return new SC.List<V>(Values); }

        // like Java's: {a=1, b=2}
        public override string ToString()
        {
            var sb = new System.Text.StringBuilder("{");
            foreach (var kv in this) sb.Append(sb.Length > 1 ? ", " : "").Append(JString.valueOf(kv.Key)).Append('=').Append(JString.valueOf(kv.Value));
            return sb.Append('}').ToString();
        }
    }

    sealed class SameObject<K> : IEqualityComparer<K>
    {
        public bool Equals(K a, K b) { return ReferenceEquals(a, b); }
        public int GetHashCode(K a) { return System.Runtime.CompilerServices.RuntimeHelpers.GetHashCode(a); }
    }

    public class IdentityHashMap<K, V> : HashMap<K, V>
    {
        public IdentityHashMap() : base(new SameObject<K>()) { }
    }

    public class HashSet<T> : SC.HashSet<T>
    {
        public HashSet() { }
        public HashSet(IEnumerable<T> items) : base(items) { }
        public bool add(T x) { return Add(x); }
        public bool contains(T x) { return x != null && Contains(x); }
        public bool remove(T x) { return Remove(x); }
        public int size() { return Count; }
        public bool isEmpty() { return Count == 0; }
        public void clear() { Clear(); }
    }

    public class ArrayDeque<T> : SC.LinkedList<T>
    {
        public ArrayDeque() { }
        public ArrayDeque(IEnumerable<T> items) : base(items) { }
        public bool add(T x) { AddLast(x); return true; }
        public bool addAll(IEnumerable<T> xs) { foreach (T x in xs) AddLast(x); return true; }
        public T poll() { if (Count == 0) return default(T); T x = First.Value; RemoveFirst(); return x; }
        public T peek() { return Count == 0 ? default(T) : First.Value; }
        public bool isEmpty() { return Count == 0; }
        public int size() { return Count; }
        public void clear() { Clear(); }
    }

    public static class Arrays
    {
        public static void fill<T>(T[] a, T v) { for (int i = 0; i < a.Length; i++) a[i] = v; }
        public static ArrayList<T> asList<T>(params T[] a) { return new ArrayList<T>(a); }
        public static T[] copyOf<T>(T[] a, int n) { var r = new T[n]; Array.Copy(a, r, System.Math.Min(n, a.Length)); return r; }
        public static T[] copyOfRange<T>(T[] a, int from, int to) { var r = new T[to - from]; Array.Copy(a, from, r, 0, System.Math.Min(to, a.Length) - from); return r; }
        public static void sort(int[] a) { Array.Sort(a); }
        public static void sort(double[] a) { Array.Sort(a); }
    }

    public static class Collections
    {
        public static void shuffle<T>(ArrayList<T> list, Random rng)
        {
            for (int i = list.Count; i > 1; i--) { int j = rng.nextInt(i); T x = list[i - 1]; list[i - 1] = list[j]; list[j] = x; }
        }
        public static void sort<T>(ArrayList<T> list, Comparator<T> c)
        {
            // stable, like Java's
            var tagged = new SC.List<KeyValuePair<int, T>>();
            for (int i = 0; i < list.Count; i++) tagged.Add(new KeyValuePair<int, T>(i, list[i]));
            tagged.Sort((a, b) => { int r = c.compare(a.Value, b.Value); return r != 0 ? r : a.Key.CompareTo(b.Key); });
            for (int i = 0; i < list.Count; i++) list[i] = tagged[i].Value;
        }
    }

    public class StringBuilder
    {
        readonly System.Text.StringBuilder b;
        public StringBuilder() { b = new System.Text.StringBuilder(); }
        public StringBuilder(string s) { b = new System.Text.StringBuilder(s); }
        public StringBuilder append(object x) { b.Append(JString.valueOf(x)); return this; }
        public StringBuilder append(char x) { b.Append(x); return this; }
        public StringBuilder append(string x) { b.Append(x ?? "null"); return this; }
        public int length() { return b.Length; }
        public char charAt(int i) { return b[i]; }
        public void setCharAt(int i, char c) { b[i] = c; }
        public void setLength(int n) { b.Length = n; }
        public StringBuilder insert(int i, string s) { b.Insert(i, s); return this; }
        public StringBuilder reverse() { var a = b.ToString().ToCharArray(); Array.Reverse(a); b.Length = 0; b.Append(a); return this; }
        public string toString() { return b.ToString(); }
        public override string ToString() { return b.ToString(); }
    }

    public static class JString
    {
        public static string valueOf(object x)
        {
            if (x == null) return "null";
            if (x is bool) return (bool)x ? "true" : "false";
            if (x is float) return JavaNumber((float)x);
            if (x is double) return JavaNumber((double)x);
            return Convert.ToString(x, CultureInfo.InvariantCulture);
        }
        public static string valueOf(char[] a) { return new string(a); }
        static string JavaNumber(double d)
        {
            string s = d.ToString("R", CultureInfo.InvariantCulture);
            return s.Contains(".") || s.Contains("E") || s.Contains("N") || s.Contains("I") ? s : s + ".0";
        }
        public static string join(string sep, IEnumerable<string> parts) { return string.Join(sep, parts); }

        // Java's String.format, for the conversions squib uses: %d, %s, %f with a precision, %%.
        public static string format(Locale l, string fmt, params object[] args) { return format(fmt, args); }
        public static string format(string fmt, params object[] args)
        {
            int n = 0;
            return Regex.Replace(fmt, @"%(\.(\d+))?([dsf%])", m =>
            {
                if (m.Groups[3].Value == "%") return "%";
                object a = n < args.Length ? args[n++] : null;
                if (m.Groups[3].Value == "f")
                    return Convert.ToDouble(a, CultureInfo.InvariantCulture).ToString("F" + (m.Groups[2].Success ? m.Groups[2].Value : "6"), CultureInfo.InvariantCulture);
                return valueOf(a);
            });
        }
    }

    public static class JInt
    {
        public static int parseInt(string s) { return int.Parse(s.Trim(), NumberStyles.AllowLeadingSign, CultureInfo.InvariantCulture); }
        public static string toString(int x) { return x.ToString(CultureInfo.InvariantCulture); }
    }

    public static class JBox
    {
        // Double.valueOf, Float.valueOf, Long.valueOf, Boolean.valueOf
        public static double? D(double x) { return x; }
        public static float? F(float x) { return x; }
        public static long? L(long x) { return x; }
        public static bool? B(bool x) { return x; }
    }

    public static class JSystem
    {
        public static long currentTimeMillis() { return (DateTime.UtcNow.Ticks - 621355968000000000L) / 10000; }
        public static long nanoTime() { return System.Diagnostics.Stopwatch.GetTimestamp() * (1000000000L / System.Diagnostics.Stopwatch.Frequency); }
        public static void arraycopy(Array src, int si, Array dst, int di, int n) { Array.Copy(src, si, dst, di, n); }
    }

    public static class JBits
    {
        public static int urs(int x, int n) { return (int)((uint)x >> n); }
        public static long urs(long x, int n) { return (long)((ulong)x >> n); }
    }

    public static class JArr
    {
        public static T[] copy<T>(T[] a) { return (T[])a.Clone(); }
        public static T[][] grid<T>(int rows, int cols) { var g = new T[rows][]; for (int i = 0; i < rows; i++) g[i] = new T[cols]; return g; }
    }

    // Java reflection on static fields (Fun looks up Art's pictures by name).
    public static class JReflect
    {
        public sealed class Field
        {
            readonly FieldInfo f;
            public Field(FieldInfo f) { this.f = f; }
            public object get() { return f.GetValue(null); }
            public Type getType() { return f.FieldType; }
        }

        public static Field field(Type t, string name)
        {
            FieldInfo f = t.GetField(name, BindingFlags.Static | BindingFlags.Public | BindingFlags.NonPublic);
            if (f == null) throw new MissingFieldException(t.Name, name);
            return new Field(f);
        }
    }

    // What String methods look like in Java.
    public static class JavaStrings
    {
        public static int length(this string s) { return s.Length; }
        public static char charAt(this string s, int i) { return s[i]; }
        public static bool isEmpty(this string s) { return s.Length == 0; }
        public static bool equals(this string s, object o) { return o is string && (string)o == s; }
        public static bool equalsIgnoreCase(this string s, string o) { return o != null && string.Equals(s, o, StringComparison.OrdinalIgnoreCase); }
        public static bool startsWith(this string s, string p) { return s.StartsWith(p, StringComparison.Ordinal); }
        public static bool endsWith(this string s, string p) { return s.EndsWith(p, StringComparison.Ordinal); }
        public static bool contains(this string s, string p) { return s.IndexOf(p, StringComparison.Ordinal) >= 0; }
        public static int indexOf(this string s, char c) { return s.IndexOf(c); }
        public static int indexOf(this string s, string p) { return s.IndexOf(p, StringComparison.Ordinal); }
        public static int indexOf(this string s, char c, int from) { return from >= s.Length ? -1 : s.IndexOf(c, System.Math.Max(0, from)); }
        public static int lastIndexOf(this string s, char c) { return s.LastIndexOf(c); }
        public static string substring(this string s, int b) { return s.Substring(b); }
        public static string substring(this string s, int b, int e) { return s.Substring(b, e - b); }
        public static string toLowerCase(this string s) { return s.ToLowerInvariant(); }
        public static string toLowerCase(this string s, Locale l) { return s.ToLowerInvariant(); }
        public static string toUpperCase(this string s) { return s.ToUpperInvariant(); }
        public static string toUpperCase(this string s, Locale l) { return s.ToUpperInvariant(); }
        public static string trim(this string s) { return s.Trim(' ', '\t', '\n', '\r', '\f', '\v', '\0'); }
        public static string replace(this string s, string a, string b) { return s.Replace(a, b); }
        public static string replace(this string s, char a, char b) { return s.Replace(a, b); }
        public static char[] toCharArray(this string s) { return s.ToCharArray(); }
        public static bool matches(this string s, string re) { return Regex.IsMatch(s, "^(?:" + re + ")$"); }
        public static int compareTo(this string s, string o) { return string.CompareOrdinal(s, o); }
        public static string[] split(this string s, string re)
        {
            // Java drops trailing empty strings
            string[] parts = re.Length == 1 && !"\\.[]{}()*+-?^$|".Contains(re) ? s.Split(re[0]) : Regex.Split(s, re);
            int n = parts.Length;
            while (n > 0 && parts[n - 1].Length == 0) n--;
            if (n == parts.Length) return parts;
            var r = new string[n]; Array.Copy(parts, r, n); return r;
        }
        public static int hashCode(this string s) { int h = 0; foreach (char c in s) h = unchecked(31 * h + c); return h; }
    }

    public sealed class Locale
    {
        public static readonly Locale US = new Locale(), ROOT = new Locale(), ENGLISH = new Locale();
    }

    public sealed class Date
    {
        public readonly DateTime When = DateTime.Now;
    }

    public sealed class SimpleDateFormat
    {
        readonly string pattern;
        public SimpleDateFormat(string javaPattern, Locale l)
        {
            // enough of Java's pattern letters for squib: h, H, mm, a, E, d, M, y
            pattern = javaPattern.Replace("a", "tt").Replace("EEEE", "dddd").Replace("EEE", "ddd");
        }
        public string format(Date d) { return d.When.ToString(pattern, CultureInfo.InvariantCulture); }
    }

    public sealed class Calendar
    {
        public const int YEAR = 1, MONTH = 2, DATE = 5, DAY_OF_MONTH = 5, DAY_OF_YEAR = 6, DAY_OF_WEEK = 7,
            HOUR = 10, HOUR_OF_DAY = 11, MINUTE = 12, SECOND = 13,
            JANUARY = 0, FEBRUARY = 1, MARCH = 2, APRIL = 3, MAY = 4, JUNE = 5, JULY = 6, AUGUST = 7,
            SEPTEMBER = 8, OCTOBER = 9, NOVEMBER = 10, DECEMBER = 11,
            SUNDAY = 1, MONDAY = 2, TUESDAY = 3, WEDNESDAY = 4, THURSDAY = 5, FRIDAY = 6, SATURDAY = 7;

        // For trying things out: a fixed clock instead of the real one.
        public static DateTime? Fake;

        readonly DateTime now;
        Calendar(DateTime now) { this.now = now; }
        public static Calendar getInstance() { return new Calendar(Fake ?? DateTime.Now); }

        public int get(int field)
        {
            switch (field)
            {
                case YEAR: return now.Year;
                case MONTH: return now.Month - 1;
                case DAY_OF_MONTH: return now.Day;
                case DAY_OF_YEAR: return now.DayOfYear;
                case DAY_OF_WEEK: return (int)now.DayOfWeek + 1;
                case HOUR: return now.Hour % 12;
                case HOUR_OF_DAY: return now.Hour;
                case MINUTE: return now.Minute;
                case SECOND: return now.Second;
                default: throw new ArgumentException("calendar field " + field);
            }
        }
    }
}
