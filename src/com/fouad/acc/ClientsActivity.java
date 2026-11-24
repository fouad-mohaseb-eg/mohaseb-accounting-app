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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ClientsActivity extends Activity {
    private Db db;
    private LinearLayout list;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16)); root.setBackground(Ui.bgGradient());

        TextView head = new TextView(this);
        head.setText("العملاء وحساباتهم");
        head.setTextSize(22);
        head.setTextColor(Ui.INK);
        head.setPadding(0, 0, 0, 20);
        root.addView(head);

        android.widget.Button add = new android.widget.Button(this); Ui.style(add);
        add.setText("إضافة عميل");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addDialog(); }
        });
        root.addView(add);

        TextView hint = new TextView(this);
        hint.setText("اضغط على العميل لفتح حسابه، واضغط مطولًا لحذفه");
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

        final EditText name = new EditText(this);
        name.setHint("اسم العميل");
        f.addView(name);

        final EditText phone = new EditText(this);
        phone.setHint("رقم الهاتف (اختياري)");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        f.addView(phone);

        new AlertDialog.Builder(this)
            .setTitle("عميل جديد")
            .setView(f)
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String n = name.getText().toString().trim();
                    if (n.length() == 0) return;
                    ContentValues cv = new ContentValues();
                    cv.put("name", n);
                    cv.put("phone", phone.getText().toString().trim());
                    db.getWritableDatabase().insert("clients", null, cv);
                    refresh();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void refresh() {
        list.removeAllViews();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT id,name,phone FROM clients ORDER BY name", null);
        while (c.moveToNext()) {
            final long id = c.getLong(0);
            final String name = c.getString(1);
            double bal = db.clientBalance(id);
            TextView t = new TextView(this);
            t.setText(name + "  —  " + Util.signed(bal));
            t.setTextSize(18);
            t.setTextColor(bal > 0.004 ? Ui.GREEN : (bal < -0.004 ? Ui.RED : Ui.INK));
            Ui.row(t);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(ClientsActivity.this, ClientDetailActivity.class);
                    i.putExtra("id", id);
                    i.putExtra("name", name);
                    startActivity(i); Ui.enter(ClientsActivity.this);
                }
            });
            t.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(ClientsActivity.this)
                        .setMessage("حذف العميل «" + name + "» وكل حركات حسابه؟ (حركات الخزنة المرتبطة به تبقى كما هي)")
                        .setPositiveButton("حذف", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                db.getWritableDatabase().delete("clients", "id=" + id, null);
                                db.getWritableDatabase().delete("client_tx", "client_id=" + id, null);
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
            empty.setText("لا يوجد عملاء بعد");
            empty.setTextColor(Ui.GRAY);
            list.addView(empty);
        }
    }

    protected void onResume() { super.onResume(); refresh(); }
}
