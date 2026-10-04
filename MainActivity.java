package com.boutiqueconakry.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    DatabaseHelper db;
    LinearLayout root, content;
    TextView title;
    final NumberFormat money = NumberFormat.getInstance(Locale.US);
    final SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DatabaseHelper(this);
        buildShell();
        home();
    }

    TextView tv(String s, float size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setPadding(20, 14, 20, 14);
        return t;
    }
    Button btn(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); return b;
    }
    String gnf(double x) { return money.format(Math.round(x)).replace(",", " ") + " GNF"; }

    void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        title = tv("BOUTIQUE CONAKRY", 22); title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xFFFFFFFF); title.setBackgroundColor(0xFF1565C0);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this); scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"Accueil","Produits","Dépenses","Historique"};
        View.OnClickListener[] ls = {v->home(),v->products(),v->expenses(),v->history()};
        for(int i=0;i<labels.length;i++){ Button b=btn(labels[i]); b.setTextSize(12); b.setOnClickListener(ls[i]);
            nav.addView(b,new LinearLayout.LayoutParams(0,60,1)); }
        root.addView(nav);
        setContentView(root);
    }

    void setContent(String t) {
        title.setText(t); content.removeAllViews();
    }
    TextView heading(String s) {
        TextView t=tv(s,19); t.setTypeface(null,Typeface.BOLD); content.addView(t); return t;
    }

    void home() {
        setContent("BOUTIQUE CONAKRY");
        heading("Tableau de bord");
        addStat("Chiffre d'affaires", gnf(db.sum("qty*unit_sale","sales")));
        addStat("Bénéfice brut", gnf(db.sum("profit","sales")));
        addStat("Dépenses", gnf(db.sum("amount","expenses")));
        addStat("Bénéfice net", gnf(db.sum("profit","sales")-db.sum("amount","expenses")));
        addStat("Stock restant", db.stockCount()+" article(s)");
        addStat("Produits", db.productCount()+" produit(s)");
        addStat("Aujourd'hui", "CA: "+gnf(db.todaySales())+" • bénéfice: "+gnf(db.todayProfit()));
        Button report=btn("📱 Générer et partager le rapport WhatsApp");
        report.setOnClickListener(v->shareReport());
        content.addView(report);
        Button add=btn("➕ Ajouter un produit");
        add.setOnClickListener(v->showAddProduct());
        content.addView(add);
        Button ex=btn("💸 Ajouter une dépense");
        ex.setOnClickListener(v->showAddExpense());
        content.addView(ex);
    }

    void addStat(String a,String b) {
        TextView t=tv(a+"\n"+b,17); t.setTypeface(null,Typeface.BOLD);
        t.setBackgroundColor(0xFFEAF2FF); content.addView(t,new LinearLayout.LayoutParams(-1,-2));
    }

    void products() {
        setContent("PRODUITS & STOCK");
        Button add=btn("➕ Ajouter un produit"); add.setOnClickListener(v->showAddProduct()); content.addView(add);
        Cursor c=db.products();
        while(c.moveToNext()){
            long id=c.getLong(0); String name=c.getString(1); double p=c.getDouble(2), s=c.getDouble(3); int stock=c.getInt(4);
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            TextView t=tv(name+"\nAchat: "+gnf(p)+"   Vente: "+gnf(s)+"\nStock: "+stock+"   Marge/unité: "+gnf(s-p),16);
            t.setTypeface(null,Typeface.BOLD); card.addView(t);
            LinearLayout actions=new LinearLayout(this);
            Button sell=btn("Vendre"); sell.setOnClickListener(v->showSell(id,name,stock));
            Button restock=btn("+ Stock"); restock.setOnClickListener(v->showRestock(id,name));
            Button del=btn("Supprimer"); del.setOnClickListener(v->confirmDelete(id,name));
            actions.addView(sell,new LinearLayout.LayoutParams(0,60,1));
            actions.addView(restock,new LinearLayout.LayoutParams(0,60,1));
            actions.addView(del,new LinearLayout.LayoutParams(0,60,1));
            card.addView(actions);
            content.addView(card);
        }
        c.close();
    }

    EditText input(String hint) {
        EditText e=new EditText(this); e.setHint(hint); e.setPadding(20,8,20,8); return e;
    }

    void showAddProduct() {
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        EditText n=input("Nom du produit"); EditText p=input("Prix d'achat (GNF)"); EditText s=input("Prix de vente (GNF)"); EditText q=input("Quantité en stock");
        p.setInputType(2); s.setInputType(2); q.setInputType(2);
        l.addView(n);l.addView(p);l.addView(s);l.addView(q);
        new AlertDialog.Builder(this).setTitle("Nouveau produit").setView(l)
            .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
                try{ db.addProduct(n.getText().toString().trim(),Double.parseDouble(p.getText().toString()),Double.parseDouble(s.getText().toString()),Integer.parseInt(q.getText().toString())); products(); }
                catch(Exception e){toast("Vérifie les valeurs.");}
            }).show();
    }

    void showSell(long id,String name,int stock) {
        EditText q=input("Quantité à vendre (stock: "+stock+")"); q.setInputType(2);
        new AlertDialog.Builder(this).setTitle("Vendre: "+name).setView(q)
            .setNegativeButton("Annuler",null).setPositiveButton("Vendre",(d,w)->{
                try { if(db.sell(id,Integer.parseInt(q.getText().toString()))) {toast("Vente enregistrée."); products();} else toast("Stock insuffisant.");}
                catch(Exception e){toast("Quantité invalide.");}
            }).show();
    }

    void showRestock(long id,String name) {
        EditText q=input("Quantité à ajouter"); q.setInputType(2);
        new AlertDialog.Builder(this).setTitle("Ajouter du stock: "+name).setView(q)
            .setNegativeButton("Annuler",null).setPositiveButton("Ajouter",(d,w)->{
                try { int x=Integer.parseInt(q.getText().toString()); if(x>0){ db.addStock(id,x); products(); } }
                catch(Exception e){toast("Quantité invalide.");}
            }).show();
    }

    void confirmDelete(long id,String name) {
        new AlertDialog.Builder(this).setTitle("Supprimer ?").setMessage(name)
            .setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->{db.deleteProduct(id);products();}).show();
    }

    void expenses() {
        setContent("DÉPENSES");
        Button add=btn("➕ Ajouter une dépense"); add.setOnClickListener(v->showAddExpense()); content.addView(add);
        Cursor c=db.expenses();
        while(c.moveToNext()){
            content.addView(tv(c.getString(1)+"\n"+gnf(c.getDouble(2))+" • "+date.format(new Date(c.getLong(3))),16));
        }
        c.close();
        content.addView(tv("Total dépenses: "+gnf(db.sum("amount","expenses")),18));
    }

    void showAddExpense() {
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        EditText n=input("Motif (transport, électricité...)"); EditText a=input("Montant (GNF)"); a.setInputType(2);
        l.addView(n);l.addView(a);
        new AlertDialog.Builder(this).setTitle("Nouvelle dépense").setView(l)
            .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
                try{db.addExpense(n.getText().toString().trim(),Double.parseDouble(a.getText().toString()));expenses();}
                catch(Exception e){toast("Vérifie le montant.");}
            }).show();
    }

    void history() {
        setContent("HISTORIQUE");
        heading("Ventes");
        Cursor s=db.sales();
        while(s.moveToNext()){
            content.addView(tv("🛒 "+s.getString(2)+" × "+s.getInt(3)+"\nCA: "+gnf(s.getInt(3)*s.getDouble(4))+" • bénéfice: "+gnf(s.getDouble(6))+"\n"+date.format(new Date(s.getLong(7))),15));
        }
        s.close();
        heading("Dépenses");
        Cursor e=db.expenses();
        while(e.moveToNext()) content.addView(tv("💸 "+e.getString(1)+" : "+gnf(e.getDouble(2))+"\n"+date.format(new Date(e.getLong(3))),15));
        e.close();
    }

    void shareReport() {
        double ca=db.sum("qty*unit_sale","sales"), brut=db.sum("profit","sales"), dep=db.sum("amount","expenses"), net=brut-dep;
        StringBuilder r=new StringBuilder();
        r.append("🛒 *BILAN BOUTIQUE CONAKRY*\\n\\n");
        r.append("💰 CA: ").append(gnf(ca)).append("\\n");
        r.append("📈 Bénéfice brut: ").append(gnf(brut)).append("\\n");
        r.append("💸 Dépenses: ").append(gnf(dep)).append("\\n");
        r.append(net>=0 ? "🟢 Bénéfice net: " : "🔴 Perte nette: ").append(gnf(net)).append("\\n");
        r.append("📦 Stock restant: ").append(db.stockCount()).append(" article(s)\\n");
        r.append("📅 Aujourd'hui — CA: ").append(gnf(db.todaySales())).append("\\n");
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,r.toString());
        startActivity(Intent.createChooser(i,"Partager le rapport avec WhatsApp"));
    }

    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
