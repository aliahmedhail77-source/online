package com.example.autosmscards;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int REQ_PERMS = 10;
    private static final int REQ_FILE = 20;

    LinearLayout root;
    LinearLayout content;
    int importAmount = 100;

    int purple = 0xff6D4BB3;
    int bg = 0xff111016;
    int card = 0xff1E1B29;
    int text = 0xffF4F1FF;
    int muted = 0xffB9B3C9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionsIfNeeded();
        buildLayout();
        showHome();
    }

    private void requestPermissionsIfNeeded() {
        ArrayList<String> perms = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.RECEIVE_SMS);
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_SMS);
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.SEND_SMS);
        if (!perms.isEmpty()) requestPermissions(perms.toArray(new String[0]), REQ_PERMS);
    }

    private void buildLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        setContentView(root);

        TextView header = txt("نظام إرسال الكروت التلقائي", 20, true);
        header.setGravity(Gravity.CENTER);
        header.setTextColor(0xffffffff);
        header.setBackgroundColor(purple);
        header.setPadding(10, 28, 10, 18);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 16, 16, 16);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(card);
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        nav.addView(navButton("الرئيسية", v -> showHome()));
        nav.addView(navButton("الكروت", v -> showCards()));
        nav.addView(navButton("استيراد", v -> showImport()));
        nav.addView(navButton("السجلات", v -> showLogs()));
        nav.addView(navButton("الإعدادات", v -> showSettings()));
    }

    private Button navButton(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(text);
        b.setTextSize(12);
        b.setBackgroundColor(card);
        b.setOnClickListener(l);
        b.setAllCaps(false);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        return b;
    }

    private TextView txt(String s, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(text);
        v.setGravity(Gravity.RIGHT);
        if (bold) v.setTypeface(null, 1);
        return v;
    }

    private TextView small(String s) {
        TextView v = txt(s, 13, false);
        v.setTextColor(muted);
        return v;
    }

    private TextView box(String title, String value) {
        TextView v = txt(title + "\n" + value, 16, true);
        v.setPadding(18, 18, 18, 18);
        v.setBackgroundColor(card);
        return v;
    }

    private void clear() {
        content.removeAllViews();
    }

    private void addSpace() {
        Space s = new Space(this);
        content.addView(s, new LinearLayout.LayoutParams(1, 14));
    }

    private void showHome() {
        clear();

        boolean enabled = CardStore.isAutoSendEnabled(this);
        content.addView(box("حالة الإرسال التلقائي", enabled ? "مفعّل" : "متوقف"));
        addSpace();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(row, new LinearLayout.LayoutParams(-1, -2));

        row.addView(box("متاح 100", String.valueOf(CardStore.availableCount(this, 100))), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(box("متاح 200", String.valueOf(CardStore.availableCount(this, 200))), new LinearLayout.LayoutParams(0, -2, 1));

        addSpace();
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(row2, new LinearLayout.LayoutParams(-1, -2));
        row2.addView(box("متاح 250", String.valueOf(CardStore.availableCount(this, 250))), new LinearLayout.LayoutParams(0, -2, 1));
        row2.addView(box("السجلات", String.valueOf(CardStore.loadLogs(this).size())), new LinearLayout.LayoutParams(0, -2, 1));

        addSpace();
        content.addView(small("القاعدة: المبلغ الوارد يرسل كرتًا من نفس الفئة فقط. 100 يرسل كرت 100، و200 يرسل كرت 200، بدون دمج أو استبدال."));
    }

    private void showCards() {
        clear();
        content.addView(txt("الكروت المخزنة", 20, true));
        addSpace();

        ArrayList<CardItem> cards = CardStore.loadCards(this);
        if (cards.isEmpty()) {
            content.addView(small("لا توجد كروت. استخدم صفحة الاستيراد أو الإضافة اليدوية."));
            return;
        }

        for (CardItem c : cards) {
            String line = "فئة: " + c.amount + " ريال\n"
                    + "الكرت: " + c.code + "\n"
                    + "الحالة: " + (c.sold ? "مباع إلى " + c.buyerPhone : "متاح");
            TextView item = box(line, "");
            content.addView(item);
            addSpace();
        }
    }

    private void showImport() {
        clear();

        content.addView(txt("استيراد أو إضافة كروت", 20, true));
        addSpace();

        Spinner spinner = new Spinner(this);
        String[] amounts = {"100", "200", "250", "500", "1000"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, amounts);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                importAmount = Integer.parseInt(amounts[position]);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        content.addView(small("اختر الفئة التي ستضاف تحتها الكروت:"));
        content.addView(spinner);

        addSpace();
        Button fileBtn = new Button(this);
        fileBtn.setText("استيراد من ملف TXT");
        fileBtn.setAllCaps(false);
        fileBtn.setOnClickListener(v -> openTxtFile());
        content.addView(fileBtn);

        addSpace();
        Button manualBtn = new Button(this);
        manualBtn.setText("إضافة يدوية / لصق عدة كروت");
        manualBtn.setAllCaps(false);
        manualBtn.setOnClickListener(v -> showManualAddDialog());
        content.addView(manualBtn);

        addSpace();
        content.addView(small("صيغة ملف TXT: كل سطر يحتوي كرتًا واحدًا فقط. التطبيق يتجاهل الأسطر الفارغة والكروت المكررة لنفس الفئة."));
    }

    private void showLogs() {
        clear();
        content.addView(txt("سجلات العمليات", 20, true));
        addSpace();

        ArrayList<OperationLog> logs = CardStore.loadLogs(this);
        if (logs.isEmpty()) {
            content.addView(small("لا توجد عمليات حتى الآن."));
            return;
        }

        for (OperationLog l : logs) {
            String line = "الحالة: " + l.status + "\n"
                    + "المصدر: " + l.sender + "\n"
                    + "الرقم: " + l.customerPhone + "\n"
                    + "المبلغ: " + l.amount + "\n"
                    + "الكرت: " + (l.cardCode.isEmpty() ? "-" : l.cardCode) + "\n"
                    + "الوقت: " + l.createdAt + "\n"
                    + "ملاحظة: " + l.message;
            content.addView(box(line, ""));
            addSpace();
        }
    }

    private void showSettings() {
        clear();
        content.addView(txt("الإعدادات", 20, true));
        addSpace();

        Switch sw = new Switch(this);
        sw.setText("تشغيل الإرسال التلقائي");
        sw.setTextColor(text);
        sw.setTextSize(17);
        sw.setChecked(CardStore.isAutoSendEnabled(this));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> CardStore.setAutoSendEnabled(this, isChecked));
        content.addView(sw);

        addSpace();
        content.addView(small("التطبيق يتعامل فقط مع الرسائل القادمة من Jawali / جوالي / Jaib / جيب. أي مرسل آخر يتم تجاهله."));

        addSpace();
        Button clearBtn = new Button(this);
        clearBtn.setText("حذف كل البيانات");
        clearBtn.setAllCaps(false);
        clearBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("تأكيد")
                .setMessage("هل تريد حذف الكروت والسجلات؟")
                .setPositiveButton("نعم", (d, w) -> {
                    CardStore.clearAll(this);
                    Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
                    showHome();
                })
                .setNegativeButton("لا", null)
                .show());
        content.addView(clearBtn);
    }

    private void openTxtFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQ_FILE);
    }

    private void showManualAddDialog() {
        final EditText input = new EditText(this);
        input.setMinLines(6);
        input.setGravity(Gravity.TOP | Gravity.RIGHT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("كل سطر = كرت واحد");

        new AlertDialog.Builder(this)
                .setTitle("إضافة كروت فئة " + importAmount)
                .setView(input)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    ArrayList<String> lines = new ArrayList<>();
                    for (String line : input.getText().toString().split("\\r?\\n")) lines.add(line);
                    int added = CardStore.importCards(this, importAmount, lines, "manual");
                    Toast.makeText(this, "تمت إضافة " + added + " كرت", Toast.LENGTH_LONG).show();
                    showHome();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            importFromUri(uri);
        }
    }

    private void importFromUri(Uri uri) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
            reader.close();

            int added = CardStore.importCards(this, importAmount, lines, getFileName(uri));
            Toast.makeText(this, "تم استيراد " + added + " كرت", Toast.LENGTH_LONG).show();
            showHome();
        } catch (Exception e) {
            Toast.makeText(this, "فشل قراءة الملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = "txt";
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
                cursor.close();
            }
        } catch (Exception ignored) {}
        return result;
    }
}
