package com.fouad.acc;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Db extends SQLiteOpenHelper {
    // cash.cat: SALE, COLLECT (client payment in), REPAY (debt settle, either way),
    // INCOME (counts in profit), DEPOSIT (capital, no profit),
    // EXPENSE (counts in profit), PURCHASE (stock purchase, no profit), DRAW (owner, no profit)
    public Db(Context c) { super(c, "acc.db", null, 2); }

    public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE cash(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT,cat TEXT,amount REAL,note TEXT,date TEXT)");
        d.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,phone TEXT)");
        d.execSQL("CREATE TABLE client_tx(id INTEGER PRIMARY KEY AUTOINCREMENT,client_id INTEGER,dir TEXT,amount REAL,note TEXT,date TEXT)");
        d.execSQL("CREATE TABLE debts(id INTEGER PRIMARY KEY AUTOINCREMENT,person TEXT,dir TEXT,amount REAL,paid REAL DEFAULT 0,note TEXT,date TEXT)");
        d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE,qty REAL,buy REAL,sell REAL)");
        d.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,item TEXT,qty REAL,price REAL,buy REAL,total REAL,cost REAL,mode TEXT,client TEXT,date TEXT)");
    }
    public void onUpgrade(SQLiteDatabase d, int a, int b) {}

    public double cashBalance() { return sum("SELECT COALESCE(SUM(CASE WHEN type='IN' THEN amount ELSE -amount END),0) FROM cash", null); }

    private double cashCat(String type, String cat, String since) {
        return sum("SELECT COALESCE(SUM(amount),0) FROM cash WHERE type='" + type + "' AND cat='" + cat + "'" + (since == null ? "" : since), null);
    }
    public double periodIncome(String sc)  { return cashCat("IN", "INCOME", sc); }
    public double periodExpense(String sc) { return cashCat("OUT", "EXPENSE", sc); }

    public double periodSalesTotal(String sc) { return sum("SELECT COALESCE(SUM(total),0) FROM sales WHERE 1=1" + (sc == null ? "" : sc), null); }
    public double periodSalesCost(String sc)  { return sum("SELECT COALESCE(SUM(cost),0) FROM sales WHERE 1=1" + (sc == null ? "" : sc), null); }
    public double periodSalesCash(String sc)   { return sum("SELECT COALESCE(SUM(total),0) FROM sales WHERE mode='CASH'" + (sc == null ? "" : sc), null); }
    public double periodSalesCredit(String sc) { return sum("SELECT COALESCE(SUM(total),0) FROM sales WHERE mode='CREDIT'" + (sc == null ? "" : sc), null); }

    public double clientBalance(long id) { return sum("SELECT COALESCE(SUM(CASE WHEN dir='له' THEN amount ELSE -amount END),0) FROM client_tx WHERE client_id=" + id, null); }
    public double clientsReceivable() { return sum("SELECT COALESCE(SUM(x.b),0) FROM (SELECT client_id, SUM(CASE WHEN dir='له' THEN amount ELSE -amount END) b FROM client_tx GROUP BY client_id) x WHERE x.b > 0", null); }
    public double debtsRemaining() { return sum("SELECT COALESCE(SUM(amount-paid),0) FROM debts", null); }
    public double stockValue()  { return sum("SELECT COALESCE(SUM(qty*buy),0) FROM items", null); }
    public double stockProfit() { return sum("SELECT COALESCE(SUM(qty*(sell-buy)),0) FROM items", null); }
    public double itemQty(long id) { return sum("SELECT COALESCE(qty,0) FROM items WHERE id=" + id, null); }

    private double sum(String q, String[] a) {
        Cursor c = getReadableDatabase().rawQuery(q, a);
        double v = 0;
        if (c.moveToFirst()) v = c.getDouble(0);
        c.close();
        return v;
    }
}
