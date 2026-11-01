package android.view;
import android.content.Context;
public class View {
    final Context c;
    public View(Context c) { this.c = c; }
    public Context getContext() { return c; }
    public void invalidate() {}
    protected void onDraw(android.graphics.Canvas canvas) {}
    public void draw(android.graphics.Canvas canvas) { onDraw(canvas); }
}
