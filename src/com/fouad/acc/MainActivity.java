package com.fouad.acc;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private Db db;
    private TextView tvCash, tvClients, tvDebts, tvStock;
    private LinearLayout root;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(20), Ui.dp(20), Ui.dp(20), Ui.dp(20));
        root.setBackground(Ui.bgGradient());

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setBackground(Ui.headerBg());
        header.setPadding(Ui.dp(20), Ui.dp(28), Ui.dp(20), Ui.dp(28));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
        hlp.setMargins(0, 0, 0, Ui.dp(14));
        header.setLayoutParams(hlp);
        header.setElevation(Ui.dp(1));

        TextView title = new TextView(this);
        title.setText("محاسبة محمد عبدالله");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Ui.INK);
        header.addView(title);

        TextView dev = new TextView(this);
        dev.setText("تطوير: فؤاد");
        dev.setTextSize(14);
        dev.setGravity(Gravity.CENTER);
        dev.setTextColor(Ui.GRAY);
        dev.setPadding(0, Ui.dp(4), 0, 0);
        header.addView(dev);
        root.addView(header);

        tvCash = stat("رصيد الخزنة", Ui.PRIMARY);
        tvClients = stat("مستحقات لك عند العملاء", Ui.ACCENT);
        tvDebts = stat("الديون المتبقية", Ui.CORAL);
        tvStock = stat("قيمة المخزون", Ui.VIOLET);

        nav("المبيعات", SalesActivity.class);
        nav("الخزنة", CashActivity.class);
        nav("العملاء وحساباتهم", ClientsActivity.class);
        nav("الديون", DebtsActivity.class);
        nav("المخزن", StockActivity.class);
        nav("الأرباح والتقارير", ProfitsActivity.class);
        nav("تصدير Excel والنسخ الاحتياطي", ReportsActivity.class);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
        Ui.stagger(root);
    }

    private TextView stat(String label, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.cardBg());
        card.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(18), Ui.dp(14));
        card.setElevation(Ui.dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, Ui.dp(5), 0, Ui.dp(5));
        card.setLayoutParams(lp);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(13);
        l.setTextColor(Ui.GRAY);
        card.addView(l);

        TextView v = new TextView(this);
        v.setTextSize(21);
        v.setTextColor(color);
        v.setPadding(0, Ui.dp(4), 0, 0);
        card.addView(v);

        root.addView(card);
        v.setTag(label);
        return v;
    }

    private void nav(String text, final Class<?> act) {
        root.addView(Ui.btn(this, text, new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, act));
                Ui.enter(MainActivity.this);
            }
        }));
    }

    protected void onResume() {
        super.onResume();
        Ui.countUp(tvCash, "", db.cashBalance());
        Ui.countUp(tvClients, "", db.clientsReceivable());
        Ui.countUp(tvDebts, "", db.debtsRemaining());
        Ui.countUp(tvStock, "", db.stockValue());
    }
}
