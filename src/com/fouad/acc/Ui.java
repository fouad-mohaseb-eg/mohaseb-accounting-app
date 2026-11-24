package com.fouad.acc;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Ui {
    public static final int BG = Color.rgb(238, 242, 250);
    public static final int PRIMARY = Color.rgb(10, 132, 255);   // iOS system blue
    public static final int ACCENT = Color.rgb(0, 168, 120);
    public static final int CORAL = Color.rgb(255, 99, 89);
    public static final int VIOLET = Color.rgb(120, 105, 240);
    public static final int INK = Color.rgb(28, 28, 40);
    public static final int GRAY = Color.rgb(130, 134, 150);
    public static final int GREEN = Color.rgb(0, 150, 90);
    public static final int RED = Color.rgb(230, 55, 60);

    public static GradientDrawable bgGradient() {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(228, 236, 250), Color.rgb(242, 238, 250), Color.rgb(232, 244, 248)});
    }

    public static GradientDrawable glass(int alpha, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(alpha, 255, 255, 255));
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), Color.argb(150, 255, 255, 255));
        return d;
    }

    public static GradientDrawable cardBg() { return glass(185, 20); }

    public static GradientDrawable btnBg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.argb(215, Color.red(color), Color.green(color), Color.blue(color)));
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), Color.argb(90, 255, 255, 255));
        return d;
    }

    public static GradientDrawable headerBg() { return glass(170, 24); }

    public static int dp(int v) { return (int) (v * android.content.res.Resources.getSystem().getDisplayMetrics().density); }

    public static android.widget.Button btn(Activity a, String text, View.OnClickListener l) {
        android.widget.Button b = new android.widget.Button(a);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackground(btnBg(PRIMARY));
        b.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        pressAnim(b);
        return b;
    }

    public static void style(android.widget.Button b) {
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackground(btnBg(PRIMARY));
        b.setElevation(dp(1));
        pressAnim(b);
    }

    public static void pressAnim(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN)
                    view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
                else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL)
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                return false;
            }
        });
    }

    public static void row(TextView t) {
        t.setBackground(cardBg());
        t.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(4));
        t.setLayoutParams(lp);
    }

    public static void enter(Activity a) {
        a.overridePendingTransition(R.anim.slide_in, R.anim.fade_still);
    }

    public static void stagger(ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View v = g.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(24));
            v.animate().alpha(1f).translationY(0)
                .setDuration(320).setStartDelay(40L * i)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    public static void countUp(final TextView t, final String label, double value) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, (float) value);
        a.setDuration(550);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator anim) {
                t.setText(label + "  " + Util.fmt((Float) anim.getAnimatedValue()));
            }
        });
        a.start();
    }

    public static void popIn(View v) {
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(250).start();
    }
}
