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
    int importAmount = 50;

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
        TextView v = txt(title + (value.isEmpty() ? "" : "\n" + value), 16, true);
        v.setPadding(18, 18, 18, 18);
        v.setBackgroundColor(card);
        return v;
    }

    private Button actionButton(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setOnClickListener(l);
        return b;
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

        content.addView(small("الفئات الافتراضية: 50، 100، 150، 200، 250، 300، 500. ويمكن إضافة فئات أخرى من صفحة الاستيراد."));
        addSpace();

        for (int amount : CardStore.DEFAULT_AMOUNTS) {
            content.addView(box("فئة " + amount + " ريال",
                    "المتاح: " + CardStore.availableCount(this, amount)
                            + " | المباع: " + CardStore.soldCount(this, amount)));
            addSpace();
        }

        content.addView(small("قاعدة البيع: كل مبلغ يرسل كرتًا من نفس الفئة فقط، بدون دمج أو استبدال."));
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
            content.addView(box(line, ""));
            addSpace();
        }
    }

    private void showImport() {
        clear();

        content.addView(txt("استيراد أو إضافة كروت", 20, true));
        addSpace();

        Spinner spinner = new Spinner(this);
        String[] amounts = {"50", "100", "150", "200", "250", "300", "500"};
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
        content.addView(actionButton("استيراد من ملف TXT", v -> openTxtFile()));

        addSpace();
        content.addView(actionButton("إضافة يدوية / لصق عدة كروت", v -> showManualAddDialog()));

        addSpace();
        content.addView(actionButton("إضافة فئة جديدة غير موجودة", v -> showNewAmountDialog()));

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
                    + "الرقم: " + (l.customerPhone.isEmpty() ? "-" : l.customerPhone) + "\n"
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
        content.addView(actionButton("الأسماء الموثوقة لوَن كاش", v -> showTrustedNames()));

        addSpace();
        content.addView(small("التطبيق يتعامل مع Jawali / جوالي / Jaib / جيب / ONE Cash / ون كاش. في ONE Cash يتم الإرسال فقط إذا طابق الاسم الثلاثي اسمًا محفوظًا في الأسماء الموثوقة."));

        addSpace();
        content.addView(actionButton("حذف كل البيانات", v -> new AlertDialog.Builder(this)
                .setTitle("تأكيد")
                .setMessage("هل تريد حذف الكروت والسجلات والأسماء الموثوقة؟")
                .setPositiveButton("نعم", (d, w) -> {
                    CardStore.clearAll(this);
                    Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
                    showHome();
                })
                .setNegativeButton("لا", null)
                .show()));
    }

    private void showTrustedNames() {
        clear();
        content.addView(txt("الأسماء الموثوقة - ONE Cash", 20, true));
        addSpace();

        content.addView(actionButton("إضافة اسم موثوق", v -> showAddTrustedContactDialog()));
        addSpace();

        content.addView(small("المطابقة تتم على الاسم الثلاثي فقط. مثال: إذا وصلت الرسالة من ONE Cash باسم: غالب احمد علي ه، سيطابق التطبيق: غالب احمد علي."));
        addSpace();

        ArrayList<TrustedContact> list = CardStore.loadTrustedContacts(this);
        if (list.isEmpty()) {
            content.addView(small("لا توجد أسماء موثوقة. أضف الاسم الثلاثي ورقم الهاتف الذي سيرسل إليه الكرت."));
            return;
        }

        for (TrustedContact c : list) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(18, 18, 18, 18);
            item.setBackgroundColor(card);

            item.addView(txt("الاسم: " + c.name, 16, true));
            item.addView(small("الاسم الثلاثي المطابق: " + c.tripleName));
            item.addView(small("رقم الإرسال: " + c.phone));

            Button del = actionButton("حذف", v -> {
                CardStore.deleteTrustedContact(this, c.id);
                showTrustedNames();
            });
            item.addView(del);
            content.addView(item);
            addSpace();
        }
    }

    private void showAddTrustedContactDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 10, 10, 10);

        final EditText name = new EditText(this);
        name.setHint("الاسم كما يظهر في رسالة ون كاش");
        name.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText phone = new EditText(this);
        phone.setHint("رقم الهاتف لإرسال الكرت");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);

        box.addView(name);
        box.addView(phone);

        new AlertDialog.Builder(this)
                .setTitle("إضافة اسم موثوق")
                .setView(box)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String n = name.getText().toString().trim();
                    String p = phone.getText().toString().trim();
                    if (n.isEmpty() || p.isEmpty()) {
                        Toast.makeText(this, "الاسم والرقم مطلوبان", Toast.LENGTH_LONG).show();
                        return;
                    }
                    CardStore.addTrustedContact(this, n, p);
                    Toast.makeText(this, "تم حفظ الاسم الثلاثي: " + NameUtils.tripleName(n), Toast.LENGTH_LONG).show();
                    showTrustedNames();
                })
                .setNegativeButton("إلغاء", null)
                .show();
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

    private void showNewAmountDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("مثال: 750");

        new AlertDialog.Builder(this)
                .setTitle("إضافة فئة جديدة")
                .setView(input)
                .setPositiveButton("اعتماد الفئة", (dialog, which) -> {
                    int amount = 0;
                    try { amount = Integer.parseInt(input.getText().toString().trim()); } catch (Exception ignored) {}
                    if (amount <= 0) {
                        Toast.makeText(this, "أدخل فئة صحيحة", Toast.LENGTH_LONG).show();
                        return;
                    }
                    importAmount = amount;
                    Toast.makeText(this, "تم اختيار فئة " + amount + ". استخدم الإضافة اليدوية لإدخال الكروت.", Toast.LENGTH_LONG).show();
                    showManualAddDialog();
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
