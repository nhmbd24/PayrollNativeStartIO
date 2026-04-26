package com.payroll.nativeapp;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;
import org.json.*;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

public class MainActivity extends Activity {
    private LinearLayout root, memberList, tableRows;
    private HorizontalScrollView tableScroll;
    private String currentMember = "Member 1";
    private ArrayList<String> members = new ArrayList<>();
    private final String[] months = {"يناير","فبراير","مارس","أبريل","مايو","يونيو","يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"};
    private final String[] heads = {"شهر","الاسم","اساسي","إعانة","انتقال","حافز","علاوة","إضافي","إجمالي","سلف","مسحوبات","أخرى","صافي","التوقيع"};
    private Map<String, String[][]> data = new HashMap<>();
    private StartAppAd interstitialAd;
    private int selectedRow = -1, selectedCol = -1;
    private boolean columnMode = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        StartAppSDK.init(this, "203832106", true);
        StartAppSDK.setTestAdsEnabled(true); // Remove before live publish
        interstitialAd = new StartAppAd(this);
        interstitialAd.loadAd();
        loadState();
        buildUI();
        new Handler().postDelayed(() -> { if (interstitialAd.isReady()) interstitialAd.showAd(); }, 5000);
    }

    private void buildUI() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(20,20,20));
        setContentView(root);
        buildToolbar();
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.HORIZONTAL); root.addView(body, new LinearLayout.LayoutParams(-1,0,1));
        buildMembers(body); buildTable(body);
    }

    private void buildToolbar() {
        LinearLayout bar = new LinearLayout(this); bar.setPadding(8,8,8,8); bar.setGravity(Gravity.CENTER); bar.setBackgroundColor(Color.rgb(30,30,30)); bar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(bar, new LinearLayout.LayoutParams(-1,-2));
        addBtn(bar,"+ عضو", v -> addMember());
        addBtn(bar,"Copy", v -> copyMember());
        addBtn(bar,"Single", v -> { columnMode=false; toast("Single select"); });
        addBtn(bar,"Column", v -> { columnMode=true; toast("Column select"); });
        addBtn(bar,"🎁 Reward", v -> showReward());
        addBtn(bar,"Ad", v -> { interstitialAd.loadAd(); new Handler().postDelayed(() -> { if(interstitialAd.isReady()) interstitialAd.showAd(); },1000); });
    }

    private void buildMembers(LinearLayout body) {
        ScrollView sideScroll = new ScrollView(this); sideScroll.setBackgroundColor(Color.rgb(245,245,245));
        memberList = new LinearLayout(this); memberList.setPadding(8,8,8,8); memberList.setOrientation(LinearLayout.VERTICAL); sideScroll.addView(memberList);
        body.addView(sideScroll, new LinearLayout.LayoutParams(dp(130),-1));
        renderMembers();
    }

    private void renderMembers() {
        memberList.removeAllViews();
        TextView title = label("قائمة الموظفين", 16, true); title.setTextColor(Color.BLACK); memberList.addView(title);
        for (String m: members) {
            TextView tv = label(m, 14, true); tv.setPadding(8,12,8,12); tv.setBackgroundColor(m.equals(currentMember)? Color.rgb(33,150,243):Color.WHITE); tv.setTextColor(m.equals(currentMember)?Color.WHITE:Color.BLACK);
            tv.setOnClickListener(v -> { saveState(); currentMember = m; renderMembers(); renderTable(); });
            tv.setOnLongClickListener(v -> { renameMember(m); return true; });
            memberList.addView(tv, new LinearLayout.LayoutParams(-1,-2));
        }
    }

    private void buildTable(LinearLayout body) {
        tableScroll = new HorizontalScrollView(this); ScrollView vscroll = new ScrollView(this); tableRows = new LinearLayout(this); tableRows.setOrientation(LinearLayout.VERTICAL); tableRows.setPadding(10,10,10,10); tableRows.setBackgroundColor(Color.WHITE); vscroll.addView(tableRows); tableScroll.addView(vscroll); body.addView(tableScroll, new LinearLayout.LayoutParams(0,-1,1)); renderTable();
    }

    private void renderTable() {
        tableRows.removeAllViews();
        EditText marquee = edit("", 16, true); marquee.setHint("Marquee / text"); marquee.setGravity(Gravity.CENTER); tableRows.addView(marquee, new LinearLayout.LayoutParams(dp(1100), dp(44)));
        LinearLayout headerTop = new LinearLayout(this); headerTop.setOrientation(LinearLayout.HORIZONTAL); headerTop.setGravity(Gravity.CENTER); tableRows.addView(headerTop, new LinearLayout.LayoutParams(dp(1100), dp(75)));
        EditText h1 = edit("مؤسسة فطيرة وشكولاتة التجارية\nلصاحبها / فواز ضيدان الشمري",15,true); EditText h2 = edit("مؤسسة فطيرة وشكولاتة التجارية\nلجميع أشهر عام ٢٠٢٦ م",15,true);
        headerTop.addView(h1,new LinearLayout.LayoutParams(0,-1,1)); headerTop.addView(h2,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout headRow = new LinearLayout(this); headRow.setOrientation(LinearLayout.HORIZONTAL); tableRows.addView(headRow);
        for (int c=0;c<heads.length;c++) headRow.addView(cell(heads[c],0,c,true));
        String[][] arr = getData(currentMember);
        for (int r=0;r<12;r++) { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); tableRows.addView(row); for(int c=0;c<heads.length;c++) row.addView(cell(arr[r][c], r, c, false)); }
    }

    private EditText cell(String text, int r, int c, boolean head) {
        EditText e = edit(text, head?13:14, head); e.setSingleLine(false); e.setMinHeight(dp(head?42:36)); e.setBackgroundColor(head?Color.rgb(242,242,242):Color.WHITE); e.setOnFocusChangeListener((v,has)-> { if(!has) saveCell(r,c,((EditText)v).getText().toString(), head); });
        e.setOnClickListener(v -> { selectedRow=r; selectedCol=c; if(columnMode && !head) toast("Selected column from row " + (r+1)); });
        return e;
    }

    private EditText edit(String s, int sp, boolean bold) { EditText e = new EditText(this); e.setText(s); e.setTextSize(sp); e.setTextColor(Color.BLACK); e.setGravity(Gravity.CENTER); e.setPadding(4,1,4,1); e.setTypeface(Typeface.DEFAULT, bold?Typeface.BOLD:Typeface.NORMAL); e.setSelectAllOnFocus(false); return e; }
    private TextView label(String s,int sp,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setGravity(Gravity.CENTER); t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL); return t; }
    private void addBtn(LinearLayout bar,String s,View.OnClickListener l){ Button b=new Button(this); b.setText(s); b.setTextSize(11); b.setOnClickListener(l); bar.addView(b); }

    private String[][] getData(String m) { if(!data.containsKey(m)){ String[][] a=new String[12][heads.length]; for(int r=0;r<12;r++){ for(int c=0;c<heads.length;c++) a[r][c]= c==0?months[r]:(c==1?"Employee Name":(c==13?"":"0")); } data.put(m,a);} return data.get(m); }
    private void saveCell(int r,int c,String val,boolean head){ if(!head && r>=0) { getData(currentMember)[r][c]=val; saveState(); } }
    private void addMember(){ final EditText input=new EditText(this); input.setHint("Member Name"); new AlertDialog.Builder(this).setTitle("Add Member").setView(input).setPositiveButton("OK",(d,w)->{ String n=input.getText().toString(); if(n.length()>0){ members.add(n); currentMember=n; saveState(); renderMembers(); renderTable(); }}).setNegativeButton("Cancel",null).show(); }
    private void copyMember(){ String n=currentMember+" Copy"; members.add(n); String[][] old=getData(currentMember), cp=new String[12][heads.length]; for(int i=0;i<12;i++) cp[i]=old[i].clone(); data.put(n,cp); currentMember=n; saveState(); renderMembers(); renderTable(); }
    private void renameMember(String old){ final EditText input=new EditText(this); input.setText(old); new AlertDialog.Builder(this).setTitle("Rename / Delete").setView(input).setPositiveButton("Save",(d,w)->{ String n=input.getText().toString(); if(n.length()>0&&!n.equals(old)){ int idx=members.indexOf(old); members.set(idx,n); data.put(n,data.remove(old)); currentMember=n; saveState(); renderMembers(); renderTable(); }}).setNegativeButton("Delete",(d,w)->{ members.remove(old); data.remove(old); if(members.size()==0) members.add("Member 1"); currentMember=members.get(0); saveState(); renderMembers(); renderTable(); }).show(); }
    private void showReward(){ StartAppAd ad=new StartAppAd(this); ad.loadAd(StartAppAd.AdMode.REWARDED_VIDEO); new Handler().postDelayed(() -> { if(ad.isReady()) ad.showAd(); else toast("Reward ad loading, try again"); }, 1200); }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }

    private void loadState(){ SharedPreferences sp=getSharedPreferences("payroll",0); try{ JSONArray jm=new JSONArray(sp.getString("members","[\"Member 1\"]")); members.clear(); for(int i=0;i<jm.length();i++) members.add(jm.getString(i)); currentMember=sp.getString("current",members.get(0)); JSONObject all=new JSONObject(sp.getString("data","{}")); for(String m:members){ if(all.has(m)){ JSONArray rows=all.getJSONArray(m); String[][] a=new String[12][heads.length]; for(int r=0;r<12;r++){ JSONArray row=rows.getJSONArray(r); for(int c=0;c<heads.length;c++) a[r][c]=row.optString(c,""); } data.put(m,a); } } }catch(Exception e){ members.clear(); members.add("Member 1"); currentMember="Member 1"; } }
    private void saveState(){ try{ JSONObject all=new JSONObject(); for(String m:members){ JSONArray rows=new JSONArray(); String[][] a=getData(m); for(int r=0;r<12;r++){ JSONArray row=new JSONArray(); for(int c=0;c<heads.length;c++) row.put(a[r][c]); rows.put(row);} all.put(m,rows);} getSharedPreferences("payroll",0).edit().putString("members",new JSONArray(members).toString()).putString("current",currentMember).putString("data",all.toString()).apply(); }catch(Exception e){} }
    @Override protected void onPause(){ super.onPause(); saveState(); StartAppAd.onPause(this); }
    @Override protected void onResume(){ super.onResume(); StartAppAd.onResume(this); }
}
