package com.fouad.acc;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfitsActivity extends Activity {
    private Db db;
    private LinearLayout body;
    private TextView rangeView;
    private int period = 6;

    static final String[] LABELS = {
        "يومي (اليوم)",
        "أسبوعي (الأسبوع يبدأ السبت)",
        "شهري (من أول الشهر)",
        "سنوي (من أول السنة)",
        "آخر ٧ أيام",
        "آخر ٣٠ يوم",
        "كل الفترات حتى الآن"
    };

    // Returns {start inclusive or null, end inclusive = now}
    static String[] bounds(int period) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        String end = f.format(c.getTime());
        String start = null;
        switch (period) {
            case 0:
                zeroTime(c);
                start = f.format(c.getTime());
                break;
            case 1:
                while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) c.add(Calendar.DAY_OF_YEAR, -1);
                zeroTime(c);
                start = f.format(c.getTime());
                break;
            case 2:
                c.set(Calendar.DAY_OF_MONTH, 1);
                zeroTime(c);
                start = f.format(c.getTime());
                break;
            case 3:
                c.set(Calendar.DAY_OF_YEAR, 1);
                zeroTime(c);
                start = f.format(c.getTime());
                break;
            case 4:
                c.add(Calendar.DAY_OF_YEAR, -7);
                start = f.format(c.getTime());
                break;
            case 5:
                c.add(Calendar.DAY_OF_YEAR, -30);
                start = f.format(c.getTime());
                break;
            default:
                start = null;
        }
        return new String[]{start, end};
    }

    private static void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
    }

    static String clause(int period) {
        String[] b = bounds(period);
        String s = "";
        if (b[0] != null) s += " AND date >= '" + b[0] + "'";
        return s + " AND date <= '" + b[1] + "'";
    }

    static String rangeLabel(int period) {
        String[] b = bounds(period);
        if (b[0] == null) return "حتى " + b[1];
        return "من " + b[0] + "  إلى " + b[1];
    }

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16)); root.setBackground(Ui.bgGradient());

        TextView head = new TextView(this);
        head.setText("الأرباح والتقارير");
        head.setTextSize(22);
        head.setTextColor(Ui.INK);
        head.setPadding(0, 0, 0, 16);
        root.addView(head);

        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, LABELS);
        sp.setAdapter(ad);
        sp.setSelection(6);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { period = pos; refresh(); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        root.addView(sp);

        rangeView = new TextView(this);
        rangeView.setTextSize(13);
        rangeView.setTextColor(Ui.GRAY);
        rangeView.setPadding(0, 8, 0, 0);
        root.addView(rangeView);

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, 20, 0, 0);
        root.addView(body);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv); Ui.stagger(root);
    }

    private void line(String label, String value, int color) {
        TextView t = new TextView(this);
        t.setText(label + ":  " + value);
        t.setTextSize(18);
        t.setTextColor(color);
        t.setPadding(0, 8, 0, 8);
        body.addView(t);
    }

    private void refresh() {
        body.removeAllViews();
        rangeView.setText("الفترة: " + rangeLabel(period));
        String sc = clause(period);

        double salesTotal = db.periodSalesTotal(sc);
        double salesCash = db.periodSalesCash(sc);
        double salesCredit = db.periodSalesCredit(sc);
        double salesCost = db.periodSalesCost(sc);
        double gross = salesTotal - salesCost;
        double income = db.periodIncome(sc);
        double expense = db.periodExpense(sc);
        double net = gross + income - expense;

        line("مبيعات نقدية", Util.fmt(salesCash), Ui.INK);
        line("مبيعات آجلة (على حساب عملاء)", Util.fmt(salesCredit), Ui.INK);
        line("إيراد المبيعات الإجمالي", Util.fmt(salesTotal), Ui.INK);
        line("تكلفة البضاعة المباعة", Util.fmt(salesCost), Ui.INK);
        line("ربح المبيعات", Util.fmt(gross), gross >= 0 ? Ui.GREEN : Ui.RED);
        line("إيرادات أخرى", Util.fmt(income), Ui.INK);
        line("المصروفات", Util.fmt(expense), Ui.INK);
        line("صافي الربح", Util.fmt(net), net >= 0 ? Ui.GREEN : Ui.RED);

        TextView div = new TextView(this);
        div.setText("———");
        div.setTextColor(Ui.GRAY);
        body.addView(div);

        line("ربح متوقع غير محقق من المخزون", Util.fmt(db.stockProfit()), Ui.GRAY);
        line("مستحقات لك عند العملاء", Util.fmt(db.clientsReceivable()), Ui.GRAY);
        line("رصيد الخزنة الحالي", Util.fmt(db.cashBalance()), Ui.GRAY);

        TextView note = new TextView(this);
        note.setText("صافي الربح = ربح المبيعات (سعر البيع − تكلفة الشراء) + الإيرادات الأخرى − المصروفات، خلال الفترة المعروضة أعلاه. الربح يشمل المبيعات الآجلة وقت البيع حتى قبل تحصيلها، فقارنه برصيد الخزنة ومستحقات العملاء. تحصيل ديون العملاء وشراء البضاعة ورأس المال لا تُحسب ربحًا أو خسارة.");
        note.setTextSize(12);
        note.setTextColor(Ui.GRAY);
        note.setPadding(0, 20, 0, 0);
        body.addView(note);
    }

    protected void onResume() { super.onResume(); refresh(); }
}
