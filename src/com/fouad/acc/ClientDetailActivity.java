package com.fouad.acc;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class ClientDetailActivity extends Activity {
    private Db db;
    private long clientId;
    private String clientName;
    private LinearLayout list;
    private TextView head;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);
        clientId = getIntent().getLongExtra("id", -1);
        clientName = getIntent().getStringExtra("name");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16)); root.setBackground(Ui.bgGradient());

        head = new TextView(this);
        head.setTextSize(22);
        head.setTextColor(Ui.INK);
        head.setPadding(0, 0, 0, 20);
        root.addView(head);

        android.widget.Button add = new android.widget.Button(this); Ui.style(add);
        add.setText("إضافة حركة على الحساب");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addDialog(); }
        });
        root.addView(add);

        TextView hint = new TextView(this);
        hint.setText("«له» مبلغ مستحق لك على العميل، «عليه» يخصم من مستحقاته. الدفعات النقدية تنعكس على الخزنة تلقائيًا");
        hint.setTextSize(12);
        hint.setTextColor(Ui.GRAY);
        hint.setPadding(0, 10, 0, 10);
        root.addView(hint);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv); Ui.stagger(root);
    }

    private void addDialog() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(40, 20, 40, 0);

        final RadioGroup rg = new RadioGroup(this);
        final RadioButton leh = new RadioButton(this); leh.setText("له (زيادة مديونية)");
        final RadioButton aleh = new RadioButton(this); aleh.setText("عليه (دفعة / خصم)");
        rg.addView(leh); rg.addView(aleh);
        leh.setChecked(true);
        f.addView(rg);

        final EditText amount = new EditText(this);
        amount.setHint("المبلغ بالجنيه");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(amount);

        final EditText note = new EditText(this);
        note.setHint("البيان (اختياري)");
        f.addView(note);

        final CheckBox viaCash = new CheckBox(this);
        viaCash.setText("حركة نقدية عبر الخزنة");
        viaCash.setChecked(true);
        f.addView(viaCash);

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) {
                viaCash.setText(id == aleh.getId() ? "استلمت المبلغ نقدًا (يدخل الخزنة)" : "سلّمت المبلغ نقدًا (يخرج من الخزنة)");
            }
        });
        rg.check(leh.getId());

        new AlertDialog.Builder(this)
            .setTitle("حركة على حساب " + clientName)
            .setView(f)
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    double v = Util.parse(amount.getText().toString());
                    if (v <= 0) return;
                    boolean isLeh = leh.isChecked();
                    String date = Util.now();
                    String n = note.getText().toString();

                    ContentValues ct = new ContentValues();
                    ct.put("client_id", clientId);
                    ct.put("dir", isLeh ? "له" : "عليه");
                    ct.put("amount", v);
                    ct.put("note", n);
                    ct.put("date", date);
                    db.getWritableDatabase().insert("client_tx", null, ct);

                    if (viaCash.isChecked()) {
                        ContentValues cv = new ContentValues();
                        cv.put("type", isLeh ? "OUT" : "IN");
                        cv.put("cat", isLeh ? "REPAY" : "COLLECT");
                        cv.put("amount", v);
                        cv.put("note", (isLeh ? "دفع للعميل " : "تحصيل من العميل ") + clientName + (n.length() > 0 ? " — " + n : ""));
                        cv.put("date", date);
                        db.getWritableDatabase().insert("cash", null, cv);
                    }
                    refresh();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void refresh() {
        head.setText(clientName + " — الرصيد: " + Util.signed(db.clientBalance(clientId)));
        list.removeAllViews();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT dir,amount,note,date FROM client_tx WHERE client_id=" + clientId + " ORDER BY id DESC", null);
        while (c.moveToNext()) {
            boolean leh = "له".equals(c.getString(0));
            TextView t = new TextView(this);
            t.setText(c.getString(3) + "\n" + c.getString(0) + ": " + Util.fmt(c.getDouble(1)) + (c.getString(2).length() > 0 ? " — " + c.getString(2) : ""));
            t.setTextSize(16);
            t.setTextColor(leh ? Ui.GREEN : Ui.RED);
            Ui.row(t);
            list.addView(t);
        }
        c.close();
        if (list.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد حركات بعد");
            empty.setTextColor(Ui.GRAY);
            list.addView(empty);
        }
    }

    protected void onResume() { super.onResume(); refresh(); }
}
