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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class SalesActivity extends Activity {
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
        add.setText("تسجيل بيع");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saleDialog(); }
        });
        root.addView(add);

        TextView hint = new TextView(this);
        hint.setText("البيع النقدي يضيف للخزنة، والآجل يضيف لحساب العميل، وكلاهما يخصم من المخزون");
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

    private void saleDialog() {
        final List<Long> itemIds = new ArrayList<Long>();
        final List<String> itemNames = new ArrayList<String>();
        final List<Double> itemQty = new ArrayList<Double>();
        final List<Double> itemBuy = new ArrayList<Double>();
        final List<Double> itemSell = new ArrayList<Double>();
        Cursor ic = db.getReadableDatabase().rawQuery("SELECT id,name,qty,buy,sell FROM items WHERE qty > 0 ORDER BY name", null);
        while (ic.moveToNext()) {
            itemIds.add(ic.getLong(0)); itemNames.add(ic.getString(1));
            itemQty.add(ic.getDouble(2)); itemBuy.add(ic.getDouble(3)); itemSell.add(ic.getDouble(4));
        }
        ic.close();
        if (itemIds.isEmpty()) {
            Toast.makeText(this, "أضف أصنافًا للمخزن أولًا", Toast.LENGTH_LONG).show();
            return;
        }

        final List<Long> clientIds = new ArrayList<Long>();
        final List<String> clientNames = new ArrayList<String>();
        Cursor cc = db.getReadableDatabase().rawQuery("SELECT id,name FROM clients ORDER BY name", null);
        while (cc.moveToNext()) { clientIds.add(cc.getLong(0)); clientNames.add(cc.getString(1)); }
        cc.close();

        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(40, 20, 40, 0);

        TextView li = new TextView(this); li.setText("الصنف:"); f.addView(li);
        final Spinner item = new Spinner(this);
        item.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, itemNames));
        f.addView(item);

        final EditText qty = new EditText(this);
        qty.setHint("الكمية");
        qty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(qty);

        final EditText price = new EditText(this);
        price.setHint("سعر البيع للوحدة (يُملأ تلقائيًا)");
        price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        f.addView(price);
        price.setText(String.valueOf(itemSell.get(0)));
        item.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                price.setText(String.valueOf(itemSell.get(pos)));
            }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        final RadioGroup rg = new RadioGroup(this);
        final RadioButton cash = new RadioButton(this); cash.setText("نقدي (للخزنة)");
        final RadioButton credit = new RadioButton(this); credit.setText("آجل (على حساب عميل)");
        rg.addView(cash); rg.addView(credit);
        cash.setChecked(true);
        f.addView(rg);

        TextView lc = new TextView(this); lc.setText("العميل (للبيع الآجل):"); f.addView(lc);
        final Spinner client = new Spinner(this);
        client.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, clientNames));
        f.addView(client);
        lc.setVisibility(View.GONE); client.setVisibility(View.GONE);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) {
                int vis = (id == credit.getId()) ? View.VISIBLE : View.GONE;
                lc.setVisibility(vis); client.setVisibility(vis);
            }
        });

        new AlertDialog.Builder(this)
            .setTitle("تسجيل بيع")
            .setView(f)
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    int ip = item.getSelectedItemPosition();
                    double q = Util.parse(qty.getText().toString());
                    double p = Util.parse(price.getText().toString());
                    if (q <= 0 || p < 0) return;
                    if (q > itemQty.get(ip)) {
                        Toast.makeText(SalesActivity.this, "الكمية المتاحة: " + itemQty.get(ip), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (credit.isChecked() && clientIds.isEmpty()) {
                        Toast.makeText(SalesActivity.this, "أضف العميل أولًا من شاشة العملاء", Toast.LENGTH_LONG).show();
                        return;
                    }
                    double buy = itemBuy.get(ip);
                    double total = q * p, cost = q * buy;
                    String date = Util.now();
                    long itemId = itemIds.get(ip);
                    String itemName = itemNames.get(ip);

                    ContentValues sv = new ContentValues();
                    sv.put("item", itemName);
                    sv.put("qty", q);
                    sv.put("price", p);
                    sv.put("buy", buy);
                    sv.put("total", total);
                    sv.put("cost", cost);
                    sv.put("mode", credit.isChecked() ? "CREDIT" : "CASH");
                    sv.put("client", credit.isChecked() ? clientNames.get(client.getSelectedItemPosition()) : "");
                    sv.put("date", date);
                    db.getWritableDatabase().insert("sales", null, sv);

                    db.getWritableDatabase().execSQL("UPDATE items SET qty = qty - " + q + " WHERE id=" + itemId);

                    if (credit.isChecked()) {
                        ContentValues ct = new ContentValues();
                        ct.put("client_id", clientIds.get(client.getSelectedItemPosition()));
                        ct.put("dir", "له");
                        ct.put("amount", total);
                        ct.put("note", "بيع آجل: " + itemName);
                        ct.put("date", date);
                        db.getWritableDatabase().insert("client_tx", null, ct);
                    } else {
                        ContentValues cv = new ContentValues();
                        cv.put("type", "IN");
                        cv.put("cat", "SALE");
                        cv.put("amount", total);
                        cv.put("note", "بيع: " + itemName);
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
        head.setText("المبيعات — الإجمالي: " + Util.fmt(db.periodSalesTotal(null)));
        list.removeAllViews();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT item,qty,price,total,mode,client,date FROM sales ORDER BY id DESC", null);
        while (c.moveToNext()) {
            TextView t = new TextView(this);
            boolean isCash = "CASH".equals(c.getString(4));
            t.setText(c.getString(6) + "\n" + c.getString(0) + " × " + c.getDouble(1) + " — " + Util.fmt(c.getDouble(3))
                + (isCash ? " — نقدي" : " — آجل: " + c.getString(5)));
            t.setTextSize(16);
            Ui.row(t);
            list.addView(t);
        }
        c.close();
        if (list.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد مبيعات بعد");
            empty.setTextColor(Ui.GRAY);
            list.addView(empty);
        }
    }

    protected void onResume() { super.onResume(); refresh(); }
}
