package com.fouad.acc;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class StockActivity extends Activity {
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

        android.widget.Button buy = new android.widget.Button(this); Ui.style(buy);
        buy.setText("شراء بضاعة (توريد للمخزن)");
        buy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { purchaseDialog(); }
        });
        root.addView(buy);

        android.widget.Button sell = new android.widget.Button(this); Ui.style(sell);
        sell.setText("تسجيل بيع");
        sell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(StockActivity.this, SalesActivity.class)); Ui.enter(StockActivity.this); }
        });
        root.addView(sell);

        TextView hint = new TextView(this);
        hint.setText("اضغط على الصنف لتعديل أسعاره، واضغط مطولًا لحذفه");
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

    private void purchaseDialog() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(40, 20, 40, 0);

        final EditText name = new EditText(this);
        name.setHint("اسم الصنف (اسم موجود = زيادة كميته)");
        f.addView(name);

        final EditText qty = new EditText(this);
        qty.setHint("الكمية المشتراة");
        qty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(qty);

        final EditText buy = new EditText(this);
        buy.setHint("سعر شراء الوحدة");
        buy.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(buy);

        final EditText sell = new EditText(this);
        sell.setHint("سعر بيع الوحدة (للأصناف الجديدة)");
        sell.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(sell);

        final CheckBox viaCash = new CheckBox(this);
        viaCash.setText("دفعت ثمنها من الخزنة");
        viaCash.setChecked(true);
        f.addView(viaCash);

        new AlertDialog.Builder(this)
            .setTitle("شراء بضاعة")
            .setView(f)
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String n = name.getText().toString().trim();
                    double q = Util.parse(qty.getText().toString());
                    double bp = Util.parse(buy.getText().toString());
                    if (n.length() == 0 || q <= 0 || bp < 0) return;
                    String date = Util.now();

                    Cursor c = db.getReadableDatabase().rawQuery("SELECT id FROM items WHERE name = ? COLLATE NOCASE", new String[]{n});
                    if (c.moveToFirst()) {
                        db.getWritableDatabase().execSQL("UPDATE items SET qty = qty + " + q + ", buy = " + bp + " WHERE id=" + c.getLong(0));
                    } else {
                        double sp = Util.parse(sell.getText().toString());
                        if (sp < 0) sp = bp;
                        ContentValues cv = new ContentValues();
                        cv.put("name", n);
                        cv.put("qty", q);
                        cv.put("buy", bp);
                        cv.put("sell", sp);
                        db.getWritableDatabase().insert("items", null, cv);
                    }
                    c.close();

                    if (viaCash.isChecked()) {
                        ContentValues cc = new ContentValues();
                        cc.put("type", "OUT");
                        cc.put("cat", "PURCHASE");
                        cc.put("amount", q * bp);
                        cc.put("note", "شراء: " + n);
                        cc.put("date", date);
                        db.getWritableDatabase().insert("cash", null, cc);
                    }
                    refresh();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void editDialog(final long id, final String n, String bp, String sp) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(40, 20, 40, 0);

        final EditText buy = new EditText(this);
        buy.setHint("سعر الشراء");
        buy.setText(bp);
        buy.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(buy);

        final EditText sell = new EditText(this);
        sell.setHint("سعر البيع");
        sell.setText(sp);
        sell.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(sell);

        new AlertDialog.Builder(this)
            .setTitle("تعديل أسعار: " + n)
            .setView(f)
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    double b2 = Util.parse(buy.getText().toString());
                    double s2 = Util.parse(sell.getText().toString());
                    if (b2 < 0 || s2 < 0) return;
                    ContentValues cv = new ContentValues();
                    cv.put("buy", b2);
                    cv.put("sell", s2);
                    db.getWritableDatabase().update("items", cv, "id=" + id, null);
                    refresh();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void refresh() {
        head.setText("المخزن — القيمة بسعر الشراء: " + Util.fmt(db.stockValue()) + "\nربح متوقع غير محقق: " + Util.fmt(db.stockProfit()));
        list.removeAllViews();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT id,name,qty,buy,sell FROM items ORDER BY name", null);
        while (c.moveToNext()) {
            final long id = c.getLong(0);
            final String name = c.getString(1);
            final String bs = String.valueOf(c.getDouble(3));
            final String ss = String.valueOf(c.getDouble(4));
            TextView t = new TextView(this);
            t.setText(name + "\nالكمية: " + c.getDouble(2) + " — شراء: " + Util.fmt(c.getDouble(3)) + " — بيع: " + Util.fmt(c.getDouble(4)));
            t.setTextSize(16);
            Ui.row(t);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { editDialog(id, name, bs, ss); }
            });
            t.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(StockActivity.this)
                        .setMessage("حذف الصنف «" + name + "»؟ (سجل المبيعات السابق لا يتأثر)")
                        .setPositiveButton("حذف", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                db.getWritableDatabase().delete("items", "id=" + id, null);
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
            empty.setText("لا توجد أصناف بعد — ابدأ بشراء بضاعة");
            empty.setTextColor(Ui.GRAY);
            list.addView(empty);
        }
    }

    protected void onResume() { super.onResume(); refresh(); }
}
