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

public class DebtsActivity extends Activity {
    private Db db;
    private LinearLayout list;
    private TextView head;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16)); root.setBackground(Ui.bgGradient());

        head = new TextView(this);
        head.setTextSize(22);
        head.setTextColor(Ui.INK);
        head.setPadding(0, 0, 0, 20);
        root.addView(head);

        android.widget.Button add = new android.widget.Button(this); Ui.style(add);
        add.setText("تسجيل دين");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addDialog(); }
        });
        root.addView(add);

        TextView hint = new TextView(this);
        hint.setText("اضغط على الدين لتسجيل سداد، واضغط مطولًا لحذفه. السداد النقدي ينعكس على الخزنة تلقائيًا");
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
        final RadioButton lea = new RadioButton(this); lea.setText("دين لي (مستحق لك)");
        final RadioButton alay = new RadioButton(this); alay.setText("دين عليّ (مستحق عليك)");
        rg.addView(lea); rg.addView(alay);
        lea.setChecked(true);
        f.addView(rg);

        final EditText person = new EditText(this);
        person.setHint("اسم الطرف الآخر");
        f.addView(person);

        final EditText amount = new EditText(this);
        amount.setHint("المبلغ بالجنيه");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(amount);

        final EditText note = new EditText(this);
        note.setHint("البيان (اختياري)");
        f.addView(note);

        new AlertDialog.Builder(this)
            .setTitle("دين جديد")
            .setView(f)
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    double v = Util.parse(amount.getText().toString());
                    String p = person.getText().toString().trim();
                    if (v <= 0 || p.length() == 0) return;
                    ContentValues cv = new ContentValues();
                    cv.put("person", p);
                    cv.put("dir", lea.isChecked() ? "لي" : "علي");
                    cv.put("amount", v);
                    cv.put("paid", 0);
                    cv.put("note", note.getText().toString());
                    cv.put("date", Util.now());
                    db.getWritableDatabase().insert("debts", null, cv);
                    refresh();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void payDialog(final long id, final String person, final boolean mine, final double paid, final double total) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(40, 20, 40, 0);

        final EditText amount = new EditText(this);
        amount.setHint("المبلغ المسدد (المتبقي: " + Util.fmt(total - paid) + ")");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(amount);

        final CheckBox viaCash = new CheckBox(this);
        viaCash.setText(mine ? "استلمت المبلغ نقدًا (يدخل الخزنة)" : "دفعت المبلغ نقدًا (يخرج من الخزنة)");
        viaCash.setChecked(true);
        f.addView(viaCash);

        new AlertDialog.Builder(this)
            .setTitle("تسجيل سداد")
            .setView(f)
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    double v = Util.parse(amount.getText().toString());
                    if (v <= 0) return;
                    double np = paid + v;
                    if (np > total) np = total;
                    String date = Util.now();
                    ContentValues cv = new ContentValues();
                    cv.put("paid", np);
                    db.getWritableDatabase().update("debts", cv, "id=" + id, null);
                    if (viaCash.isChecked()) {
                        ContentValues cc = new ContentValues();
                        cc.put("type", mine ? "IN" : "OUT");
                        cc.put("cat", "REPAY");
                        cc.put("amount", v);
                        cc.put("note", (mine ? "سداد من " : "سداد إلى ") + person);
                        cc.put("date", date);
                        db.getWritableDatabase().insert("cash", null, cc);
                    }
                    refresh();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void refresh() {
        head.setText("الديون — المتبقي: " + Util.fmt(db.debtsRemaining()));
        list.removeAllViews();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT id,person,dir,amount,paid,note,date FROM debts ORDER BY id DESC", null);
        while (c.moveToNext()) {
            final long id = c.getLong(0);
            final String person = c.getString(1);
            final boolean mine = "لي".equals(c.getString(2));
            final double paid = c.getDouble(4);
            final double total = c.getDouble(3);
            double rem = total - paid;
            boolean done = rem <= 0.004;
            TextView t = new TextView(this);
            t.setText(person + (mine ? " — دين لي" : " — دين عليّ") + (done ? "  (تم السداد)" : "") + "\n"
                + "المبلغ: " + Util.fmt(total) + " — المسدد: " + Util.fmt(paid) + " — المتبقي: " + Util.fmt(rem)
                + "\n" + c.getString(6) + (c.getString(5).length() > 0 ? " — " + c.getString(5) : ""));
            t.setTextSize(16);
            t.setTextColor(done ? Ui.GRAY : (mine ? Ui.GREEN : Ui.RED));
            Ui.row(t);
            if (!done) t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { payDialog(id, person, mine, paid, total); }
            });
            t.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(DebtsActivity.this)
                        .setMessage("حذف هذا الدين؟ (لا يحذف حركات الخزنة المرتبطة به)")
                        .setPositiveButton("حذف", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                db.getWritableDatabase().delete("debts", "id=" + id, null);
                                refresh();
                            }
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
                    return true;
                }
            });
            list.addView(t);
        }
        c.close();
        if (list.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد ديون مسجلة");
            empty.setTextColor(Ui.GRAY);
            list.addView(empty);
        }
    }

    protected void onResume() { super.onResume(); refresh(); }
}
