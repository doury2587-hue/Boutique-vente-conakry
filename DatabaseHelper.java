package com.boutiqueconakry.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "boutique_conakry.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "purchase REAL NOT NULL," +
                "sale REAL NOT NULL," +
                "stock INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_id INTEGER," +
                "product_name TEXT NOT NULL," +
                "qty INTEGER NOT NULL," +
                "unit_sale REAL NOT NULL," +
                "unit_purchase REAL NOT NULL," +
                "profit REAL NOT NULL," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "label TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "created_at INTEGER NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS products");
        db.execSQL("DROP TABLE IF EXISTS sales");
        db.execSQL("DROP TABLE IF EXISTS expenses");
        onCreate(db);
    }

    public long addProduct(String name, double purchase, double sale, int stock) {
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("purchase", purchase); v.put("sale", sale); v.put("stock", stock);
        return getWritableDatabase().insert("products", null, v);
    }

    public Cursor products() {
        return getReadableDatabase().rawQuery("SELECT * FROM products ORDER BY name COLLATE NOCASE", null);
    }

    public boolean deleteProduct(long id) {
        return getWritableDatabase().delete("products", "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean addStock(long id, int qty) {
        return getWritableDatabase().execSQL("UPDATE products SET stock=stock+? WHERE id=?",
                new Object[]{qty, id}) == null;
    }

    public boolean sell(long id, int qty) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT name,purchase,sale,stock FROM products WHERE id=?",
                new String[]{String.valueOf(id)});
        if (!c.moveToFirst()) { c.close(); return false; }
        String name = c.getString(0);
        double purchase = c.getDouble(1);
        double sale = c.getDouble(2);
        int stock = c.getInt(3);
        c.close();
        if (qty <= 0 || stock < qty) return false;

        ContentValues saleRow = new ContentValues();
        saleRow.put("product_id", id);
        saleRow.put("product_name", name);
        saleRow.put("qty", qty);
        saleRow.put("unit_sale", sale);
        saleRow.put("unit_purchase", purchase);
        saleRow.put("profit", (sale - purchase) * qty);
        saleRow.put("created_at", System.currentTimeMillis());
        db.insert("sales", null, saleRow);

        db.execSQL("UPDATE products SET stock=stock-? WHERE id=?", new Object[]{qty, id});
        return true;
    }

    public long addExpense(String label, double amount) {
        ContentValues v = new ContentValues();
        v.put("label", label); v.put("amount", amount); v.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("expenses", null, v);
    }

    public Cursor sales() {
        return getReadableDatabase().rawQuery("SELECT * FROM sales ORDER BY created_at DESC", null);
    }

    public Cursor expenses() {
        return getReadableDatabase().rawQuery("SELECT * FROM expenses ORDER BY created_at DESC", null);
    }

    public double sum(String column, String table) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(" + column + "),0) FROM " + table, null);
        double x = c.moveToFirst() ? c.getDouble(0) : 0;
        c.close();
        return x;
    }

    public int stockCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(stock),0) FROM products", null);
        int x = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return x;
    }

    public int productCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM products", null);
        int x = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return x;
    }

    public double todaySales() {
        long start = startOfToday();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(qty*unit_sale),0) FROM sales WHERE created_at>=?",
                new String[]{String.valueOf(start)});
        double x = c.moveToFirst() ? c.getDouble(0) : 0; c.close(); return x;
    }

    public double todayProfit() {
        long start = startOfToday();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(profit),0) FROM sales WHERE created_at>=?",
                new String[]{String.valueOf(start)});
        double x = c.moveToFirst() ? c.getDouble(0) : 0; c.close(); return x;
    }

    private long startOfToday() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
