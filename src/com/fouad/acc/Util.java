package com.fouad.acc;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Util {
    public static String fmt(double v) {
        return String.format(Locale.US, "%,.2f", v) + " ج.م";
    }
    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
    }
    public static String signed(double v) {
        if (v > 0.004) return "له " + fmt(v);
        if (v < -0.004) return "عليه " + fmt(-v);
        return "متعادل";
    }
    public static double parse(String s) {
        try { return Double.parseDouble(s.trim().replace("٫", ".").replace(",", "")); }
        catch (Exception e) { return -1; }
    }
}
