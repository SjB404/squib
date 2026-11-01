package android.view;
public class MotionEvent {
    public int action; public float x, y; public long time;
    public static MotionEvent obtain(long time, int action, float x, float y) { MotionEvent e = new MotionEvent(); e.time = time; e.action = action; e.x = x; e.y = y; return e; }
    public float getRawX() { return x; } public float getRawY() { return y; }
    public int getActionMasked() { return action; } public long getEventTime() { return time; }
}
