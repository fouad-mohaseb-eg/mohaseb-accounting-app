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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

public class CashActivity extends Activity {
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
        add.setText("إضافة حركة يدوية");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addDialog(); }
        });
        root.addView(add);

        TextView hint = new TextView(this);
        hint.setText("حركات البيع والتحصيل والشراء تسجَّل تلقائيًا من شاشاتها. اضغط مطولًا على حركة يدوية لحذفها");
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

    static String catLabel(String cat) {
        if ("SALE".equals(cat)) return "بيع نقدي";
        if ("COLLECT".equals(cat)) return "تحصيل من عميل";
        if ("REPAY".equals(cat)) return "تسوية دين";
        if ("INCOME".equals(cat)) return "إيراد آخر";
        if ("DEPOSIT".equals(cat)) return "إيداع رأس مال";
        if ("EXPENSE".equals(cat)) return "مصروف";
        if ("PURCHASE".equals(cat)) return "شراء بضاعة";
        if ("DRAW".equals(cat)) return "سحب شخصي";
        return cat;
    }
    static boolean isAuto(String cat) {
        return "SALE".equals(cat) || "COLLECT".equals(cat) || "PURCHASE".equals(cat);
    }

    private void addDialog() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(40, 20, 40, 0);

        final RadioGroup rg = new RadioGroup(this);
        final RadioButton in = new RadioButton(this); in.setText("قبض (وارد)");
        final RadioButton out = new RadioButton(this); out.setText("صرف (منصرف)");
        rg.addView(in); rg.addView(out);
        in.setChecked(true);
        f.addView(rg);

        final Spinner cat = new Spinner(this);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) {
                String[] cats = (id == in.getId())
                    ? new String[]{"إيراد آخر (يدخل في الأرباح)", "إيداع رأس مال (لا يدخل في الأرباح)"}
                    : new String[]{"مصروف (يدخل في الأرباح)", "سحب شخصي (لا يدخل في الأرباح)"};
                cat.setAdapter(new ArrayAdapter<String>(CashActivity.this, android.R.layout.simple_spinner_dropdown_item, cats));
            }
        });
        rg.check(in.getId());
        f.addView(cat);

        final EditText amount = new EditText(this);
        amount.setHint("المبلغ بالجنيه");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(amount);

        final EditText note = new EditText(this);
        note.setHint("البيان (اختياري)");
        f.addView(note);

        new AlertDialog.Builder(this)
            .setTitle("حركة خزنة يدوية")
            .setView(f)
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    double v = Util.parse(amount.getText().toString());
                    if (v <= 0) return;
                    boolean isIn = in.isChecked();
                    String c = isIn ? (cat.getSelectedItemPosition() == 0 ? "INCOME" : "DEPOSIT")
                                    : (cat.getSelectedItemPosition() == 0 ? "EXPENSE" : "DRAW");
                    ContentValues cv = new ContentValues();
                    cv.put("type", isIn ? "IN" : "OUT");
                    cv.put("cat", c);
                    cv.put("amount", v);
                    cv.put("note", note.getText().toString());
                    cv.put("date", Util.now());
                    db.getWritableDatabase().insert("cash", null, cv);
                    refresh();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void refresh() {
        head.setText("الخزنة — الرصيد: " + Util.fmt(db.cashBalance()));
        list.removeAllViews();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT id,type,cat,amount,note,date FROM cash ORDER BY id DESC", null);
        while (c.moveToNext()) {
            final long id = c.getLong(0);
            boolean isIn = "IN".equals(c.getString(1));
            final String cat = c.getString(2);
            TextView t = new TextView(this);
            t.setText(c.getString(5) + "\n" + (isIn ? "+ " : "- ") + Util.fmt(c.getDouble(3)) + " — " + catLabel(cat)
                + (c.getString(4).length() > 0 ? " — " + c.getString(4) : ""));
            t.setTextSize(16);
            t.setTextColor(isIn ? Ui.GREEN : Ui.RED);
            Ui.row(t);
            if (!isAuto(cat)) t.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(CashActivity.this)
                        .setMessage("حذف هذه الحركة؟")
                        .setPositiveButton("حذف", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                db.getWritableDatabase().delete("cash", "id=" + id, null);
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
            empty.setText("لا توجد حركات بعد");
            empty.setTextColor(Ui.GRAY);
            list.addView(empty);
        }
    }

    protected void onResume() { super.onResume(); refresh(); }
}
