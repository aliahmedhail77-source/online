package com.example.autosmscards;

class NameUtils {
    static String normalizeArabicName(String value) {
        if (value == null) return "";
        String s = value.trim();

        s = s.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا");
        s = s.replace("ة", "ه");
        s = s.replace("ى", "ي");
        s = s.replace("ؤ", "و").replace("ئ", "ي");
        s = s.replace("ـ", "");

        // حذف التشكيل
        s = s.replaceAll("[\\u064B-\\u065F\\u0670]", "");

        // حذف علامات الترقيم وتحويلها لمسافات
        s = s.replaceAll("[^\\p{L}\\p{Nd}\\s]", " ");

        // توحيد المسافات
        s = s.replaceAll("\\s+", " ").trim();

        return s;
    }

    static String tripleName(String value) {
        String s = normalizeArabicName(value);
        if (s.isEmpty()) return "";
        String[] parts = s.split(" ");
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (String p : parts) {
            if (p.trim().isEmpty()) continue;
            if (count > 0) out.append(" ");
            out.append(p.trim());
            count++;
            if (count == 3) break;
        }
        return out.toString();
    }
}
