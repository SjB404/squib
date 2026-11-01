package android.graphics;
public class Paint {
    int color = 0xFF000000; float textSize = 12;
    public void setColor(int c) { color = c; }
    public int getColor() { return color; }
    public void setAlpha(int a) { color = (color & 0xFFFFFF) | ((a & 0xFF) << 24); }
    public int getAlpha() { return color >>> 24; }
    public void setTextSize(float s) { textSize = s; }
    public float getTextSize() { return textSize; }
    public float ascent() { return -0.928f * textSize; }
    public void setAntiAlias(boolean b) {} public void setFilterBitmap(boolean b) {} public void setSubpixelText(boolean b) {}
    public Typeface setTypeface(Typeface t) { return t; }
}
