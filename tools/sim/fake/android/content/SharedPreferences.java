package android.content;
import java.util.HashMap;
public class SharedPreferences {
    final HashMap<String, Object> m = new HashMap<>();
    public int getInt(String k, int d) { Object o = m.get(k); return o == null ? d : (Integer) o; }
    public long getLong(String k, long d) { Object o = m.get(k); return o == null ? d : (Long) o; }
    public boolean getBoolean(String k, boolean d) { Object o = m.get(k); return o == null ? d : (Boolean) o; }
    public String getString(String k, String d) { Object o = m.get(k); return o == null ? d : (String) o; }
    public java.util.Map<String, ?> getAll() { return new HashMap<>(m); }
    public Editor edit() { return new Editor(); }
    public class Editor {
        public Editor putInt(String k, int v) { m.put(k, v); return this; }
        public Editor putLong(String k, long v) { m.put(k, v); return this; }
        public Editor putBoolean(String k, boolean v) { m.put(k, v); return this; }
        public Editor putString(String k, String v) { m.put(k, v); return this; }
        public Editor remove(String k) { m.remove(k); return this; }
        public void apply() {}
        public boolean commit() { return true; }
    }
}
