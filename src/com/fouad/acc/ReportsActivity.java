package com.fouad.acc;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReportsActivity extends Activity {
    private static final int REQ_XLSX = 1, REQ_BACKUP = 2, REQ_RESTORE = 3;
    private Db db;
    private int period = 6;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16)); root.setBackground(Ui.bgGradient());

        TextView head = new TextView(this);
        head.setText("التقارير والنسخ الاحتياطي");
        head.setTextSize(22);
        head.setTextColor(Ui.INK);
        head.setPadding(0, 0, 0, 20);
        root.addView(head);

        TextView lp = new TextView(this);
        lp.setText("فترة تقرير الأرباح داخل ملف Excel:");
        lp.setPadding(0, 0, 0, 8);
        root.addView(lp);

        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, ProfitsActivity.LABELS);
        sp.setAdapter(ad);
        sp.setSelection(6);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { period = pos; }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        root.addView(sp);

        btn(root, "تصدير تقرير Excel", new View.OnClickListener() {
            public void onClick(View v) { createFile(REQ_XLSX, "محاسبة-تقرير.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); }
        });
        btn(root, "حفظ نسخة احتياطية (ملف)", new View.OnClickListener() {
            public void onClick(View v) { createFile(REQ_BACKUP, "محاسبة-نسخة-احتياطية.json", "application/json"); }
        });
        btn(root, "استعادة نسخة احتياطية (ملف)", new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, REQ_RESTORE);
            }
        });
        btn(root, "نسخة احتياطية سريعة داخل التطبيق", new View.OnClickListener() {
            public void onClick(View v) { quickBackup(); }
        });
        btn(root, "استعادة النسخة السريعة", new View.OnClickListener() {
            public void onClick(View v) { quickRestore(); }
        });

        TextView note = new TextView(this);
        note.setText("حفظ الملفات يتم عبر منتقي ملفات النظام لتختار مكان الحفظ. الاستعادة تستبدل كل البيانات الحالية. النسخة السريعة تُحفظ داخل التطبيق وتُحدَّث مع كل مرة.");
        note.setTextSize(12);
        note.setTextColor(Ui.GRAY);
        note.setPadding(0, 20, 0, 0);
        root.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv); Ui.stagger(root);
    }

    private void btn(LinearLayout root, String text, View.OnClickListener l) {
        android.widget.Button b = new android.widget.Button(this); Ui.style(b);
        b.setText(text);
        b.setTextSize(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 10, 0, 10);
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        root.addView(b);
    }

    private void createFile(int req, String name, String mime) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(i, req);
    }

    private File quickFile() { return new File(getFilesDir(), "backup-auto.json"); }

    private void quickBackup() {
        try {
            FileOutputStream f = new FileOutputStream(quickFile());
            f.write(buildBackup());
            f.close();
            Toast.makeText(this, "تم حفظ النسخة السريعة داخل التطبيق", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "حدث خطأ أثناء الحفظ", Toast.LENGTH_LONG).show();
        }
    }

    private void quickRestore() {
        if (!quickFile().exists()) {
            Toast.makeText(this, "لا توجد نسخة سريعة محفوظة", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setMessage("الاستعادة ستحذف كل البيانات الحالية وتستبدلها بالنسخة السريعة. متابعة؟")
            .setPositiveButton("استعادة", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    try {
                        FileInputStream in = new FileInputStream(quickFile());
                        restore(in);
                        in.close();
                        Toast.makeText(ReportsActivity.this, "تمت الاستعادة بنجاح", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ReportsActivity.this, "فشلت الاستعادة: ملف غير صالح", Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private byte[] buildXlsx() throws Exception {
        Xlsx x = new Xlsx();

        List<String[]> cash = new ArrayList<String[]>();
        cash.add(new String[]{"التاريخ", "قبض/صرف", "التصنيف", "المبلغ (ج.م)", "البيان"});
        Cursor c = db.getReadableDatabase().rawQuery("SELECT date,type,cat,amount,note FROM cash ORDER BY id", null);
        while (c.moveToNext())
            cash.add(new String[]{c.getString(0), "IN".equals(c.getString(1)) ? "قبض" : "صرف", CashActivity.catLabel(c.getString(2)), String.valueOf(c.getDouble(3)), c.getString(4)});
        c.close();
        cash.add(new String[]{"", "", "الرصيد", String.valueOf(db.cashBalance()), ""});
        x.sheet("الخزنة", cash);

        List<String[]> sa = new ArrayList<String[]>();
        sa.add(new String[]{"التاريخ", "الصنف", "الكمية", "سعر البيع", "الإجمالي (ج.م)", "التكلفة (ج.م)", "الربح (ج.م)", "الدفع", "العميل"});
        c = db.getReadableDatabase().rawQuery("SELECT date,item,qty,price,total,cost,mode,client FROM sales ORDER BY id", null);
        while (c.moveToNext())
            sa.add(new String[]{c.getString(0), c.getString(1), String.valueOf(c.getDouble(2)), String.valueOf(c.getDouble(3)),
                String.valueOf(c.getDouble(4)), String.valueOf(c.getDouble(5)), String.valueOf(c.getDouble(4) - c.getDouble(5)),
                "CASH".equals(c.getString(6)) ? "نقدي" : "آجل", c.getString(7)});
        c.close();
        x.sheet("المبيعات", sa);

        List<String[]> cl = new ArrayList<String[]>();
        cl.add(new String[]{"العميل", "الهاتف", "الرصيد (ج.م)", "الحالة"});
        c = db.getReadableDatabase().rawQuery("SELECT id,name,phone FROM clients ORDER BY name", null);
        while (c.moveToNext()) {
            double bal = db.clientBalance(c.getLong(0));
            cl.add(new String[]{c.getString(1), c.getString(2), String.valueOf(Math.abs(bal)), Util.signed(bal)});
        }
        c.close();
        x.sheet("العملاء", cl);

        List<String[]> ct = new ArrayList<String[]>();
        ct.add(new String[]{"التاريخ", "العميل", "الاتجاه", "المبلغ (ج.م)", "البيان"});
        c = db.getReadableDatabase().rawQuery("SELECT t.date,cl.name,t.dir,t.amount,t.note FROM client_tx t LEFT JOIN clients cl ON cl.id=t.client_id ORDER BY t.id", null);
        while (c.moveToNext())
            ct.add(new String[]{c.getString(0), c.getString(1) == null ? "(محذوف)" : c.getString(1), c.getString(2), String.valueOf(c.getDouble(3)), c.getString(4)});
        c.close();
        x.sheet("حركات العملاء", ct);

        List<String[]> de = new ArrayList<String[]>();
        de.add(new String[]{"التاريخ", "الطرف", "الاتجاه", "المبلغ (ج.م)", "المسدد (ج.م)", "المتبقي (ج.م)", "البيان"});
        c = db.getReadableDatabase().rawQuery("SELECT date,person,dir,amount,paid,note FROM debts ORDER BY id", null);
        while (c.moveToNext())
            de.add(new String[]{c.getString(0), c.getString(1), c.getString(2), String.valueOf(c.getDouble(3)), String.valueOf(c.getDouble(4)), String.valueOf(c.getDouble(3) - c.getDouble(4)), c.getString(5)});
        c.close();
        x.sheet("الديون", de);

        List<String[]> it = new ArrayList<String[]>();
        it.add(new String[]{"الصنف", "الكمية", "سعر الشراء (ج.م)", "سعر البيع (ج.م)", "قيمة المخزون (ج.م)"});
        c = db.getReadableDatabase().rawQuery("SELECT name,qty,buy,sell FROM items ORDER BY name", null);
        while (c.moveToNext())
            it.add(new String[]{c.getString(0), String.valueOf(c.getDouble(1)), String.valueOf(c.getDouble(2)), String.valueOf(c.getDouble(3)), String.valueOf(c.getDouble(1) * c.getDouble(2))});
        c.close();
        x.sheet("المخزن", it);

        String pname = ProfitsActivity.LABELS[period] + " — " + ProfitsActivity.rangeLabel(period);
        String sc = ProfitsActivity.clause(period);
        double st = db.periodSalesTotal(sc), sco = db.periodSalesCost(sc);
        double inc = db.periodIncome(sc), exp = db.periodExpense(sc);
        List<String[]> pr = new ArrayList<String[]>();
        pr.add(new String[]{"تقرير الأرباح — " + pname, ""});
        pr.add(new String[]{"مبيعات نقدية (ج.م)", String.valueOf(db.periodSalesCash(sc))});
        pr.add(new String[]{"مبيعات آجلة على حساب عملاء (ج.م)", String.valueOf(db.periodSalesCredit(sc))});
        pr.add(new String[]{"إيراد المبيعات (ج.م)", String.valueOf(st)});
        pr.add(new String[]{"تكلفة البضاعة المباعة (ج.م)", String.valueOf(sco)});
        pr.add(new String[]{"ربح المبيعات (ج.م)", String.valueOf(st - sco)});
        pr.add(new String[]{"إيرادات أخرى (ج.م)", String.valueOf(inc)});
        pr.add(new String[]{"المصروفات (ج.م)", String.valueOf(exp)});
        pr.add(new String[]{"صافي الربح (ج.م)", String.valueOf(st - sco + inc - exp)});
        pr.add(new String[]{"ربح متوقع غير محقق من المخزون (ج.م)", String.valueOf(db.stockProfit())});
        x.sheet("الأرباح", pr);

        return x.bytes();
    }

    private String dumpTable(SQLiteDatabase d, String table, String[] cols) throws Exception {
        JSONArray a = new JSONArray();
        Cursor c = d.rawQuery("SELECT * FROM " + table, null);
        while (c.moveToNext()) {
            JSONObject o = new JSONObject();
            for (int i = 0; i < cols.length; i++) o.put(cols[i], c.getString(i));
            a.put(o);
        }
        c.close();
        return a.toString();
    }

    private byte[] buildBackup() throws Exception {
        SQLiteDatabase d = db.getReadableDatabase();
        JSONObject o = new JSONObject();
        o.put("app", "محاسبة محمد عبدالله");
        o.put("dev", "فؤاد");
        o.put("version", 2);
        o.put("date", Util.now());
        o.put("cash", new JSONArray(dumpTable(d, "cash", new String[]{"id", "type", "cat", "amount", "note", "date"})));
        o.put("clients", new JSONArray(dumpTable(d, "clients", new String[]{"id", "name", "phone"})));
        o.put("client_tx", new JSONArray(dumpTable(d, "client_tx", new String[]{"id", "client_id", "dir", "amount", "note", "date"})));
        o.put("debts", new JSONArray(dumpTable(d, "debts", new String[]{"id", "person", "dir", "amount", "paid", "note", "date"})));
        o.put("items", new JSONArray(dumpTable(d, "items", new String[]{"id", "name", "qty", "buy", "sell"})));
        o.put("sales", new JSONArray(dumpTable(d, "sales", new String[]{"id", "item", "qty", "price", "buy", "total", "cost", "mode", "client", "date"})));
        return o.toString(2).getBytes("UTF-8");
    }

    private void restore(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        JSONObject o = new JSONObject(new String(bos.toByteArray(), "UTF-8"));
        SQLiteDatabase d = db.getWritableDatabase();
        d.beginTransaction();
        try {
            d.execSQL("DELETE FROM cash"); d.execSQL("DELETE FROM clients"); d.execSQL("DELETE FROM client_tx");
            d.execSQL("DELETE FROM debts"); d.execSQL("DELETE FROM items"); d.execSQL("DELETE FROM sales");
            ins(d, "cash", o.optJSONArray("cash"), new String[]{"id", "type", "cat", "amount", "note", "date"});
            ins(d, "clients", o.optJSONArray("clients"), new String[]{"id", "name", "phone"});
            ins(d, "client_tx", o.optJSONArray("client_tx"), new String[]{"id", "client_id", "dir", "amount", "note", "date"});
            ins(d, "debts", o.optJSONArray("debts"), new String[]{"id", "person", "dir", "amount", "paid", "note", "date"});
            ins(d, "items", o.optJSONArray("items"), new String[]{"id", "name", "qty", "buy", "sell"});
            ins(d, "sales", o.optJSONArray("sales"), new String[]{"id", "item", "qty", "price", "buy", "total", "cost", "mode", "client", "date"});
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
    }

    private void ins(SQLiteDatabase d, String table, JSONArray a, String[] cols) throws Exception {
        if (a == null) return;
        for (int i = 0; i < a.length(); i++) {
            JSONObject r = a.getJSONObject(i);
            android.content.ContentValues cv = new android.content.ContentValues();
            for (String col : cols) {
                Object v = r.opt(col);
                if (v == null) { cv.put(col, ""); continue; }
                String s = String.valueOf(v);
                try {
                    if (s.matches("-?\\d+")) cv.put(col, Long.parseLong(s));
                    else cv.put(col, Double.parseDouble(s));
                } catch (Exception e) { cv.put(col, s); }
            }
            d.insert(table, null, cv);
        }
    }

    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        try {
            if (req == REQ_RESTORE) {
                new AlertDialog.Builder(this)
                    .setMessage("الاستعادة ستحذف كل البيانات الحالية وتستبدلها بالنسخة الاحتياطية. متابعة؟")
                    .setPositiveButton("استعادة", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            try {
                                InputStream in = getContentResolver().openInputStream(uri);
                                restore(in);
                                in.close();
                                Toast.makeText(ReportsActivity.this, "تمت الاستعادة بنجاح", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(ReportsActivity.this, "فشلت الاستعادة: ملف غير صالح", Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
            } else {
                OutputStream out = getContentResolver().openOutputStream(uri);
                byte[] bytes = (req == REQ_XLSX) ? buildXlsx() : buildBackup();
                out.write(bytes);
                out.close();
                Toast.makeText(this, "تم الحفظ بنجاح", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "حدث خطأ أثناء الحفظ", Toast.LENGTH_LONG).show();
        }
    }
}
