package com.example.autosmscards;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PaymentParser {
    static boolean trustedSender(String sender) {
        if (sender == null) return false;
        String s = sender.toLowerCase();
        return s.contains("jawali") || s.contains("jaib") || sender.contains("جوالي") || sender.contains("جيب");
    }

    static ParsedPayment parse(String sender, String body) {
        if (!trustedSender(sender) || body == null) return null;

        ParsedPayment p = parseJawali(sender, body);
        if (p != null) return p;

        p = parseJaib(sender, body);
        if (p != null) return p;

        return parseFallback(sender, body);
    }

    private static ParsedPayment parseJawali(String sender, String body) {
        // مثال:
        // استلمت مبلغ 100 YER من 774583505 رصيدك هو 2478.34
        Pattern pattern = Pattern.compile("استلمت\\s+مبلغ\\s+(\\d+(?:\\.\\d+)?)\\s*YER\\s+من\\s+(\\d+)");
        Matcher m = pattern.matcher(body);
        if (m.find()) {
            int amount = toIntAmount(m.group(1));
            String phone = m.group(2);
            if (amount > 0 && phone.length() >= 7) {
                return new ParsedPayment("Jawali", amount, phone, "");
            }
        }
        return null;
    }

    private static ParsedPayment parseJaib(String sender, String body) {
        // مثال:
        // اضيف 250ر.ي تحويل مشترك رص:15598.76ر.ي من أسامه عبدالحق-738119852
        Pattern pattern = Pattern.compile("اضيف\\s+(\\d+(?:\\.\\d+)?)\\s*ر\\.?ي.*?من\\s+(.+?)-(\\d+)");
        Matcher m = pattern.matcher(body);
        if (m.find()) {
            int amount = toIntAmount(m.group(1));
            String name = m.group(2).trim();
            String phone = m.group(3).trim();
            if (amount > 0 && phone.length() >= 7) {
                return new ParsedPayment("Jaib", amount, phone, name);
            }
        }
        return null;
    }

    private static ParsedPayment parseFallback(String sender, String body) {
        int amount = 0;
        String phone = "";

        Pattern amountPattern = Pattern.compile("(?:مبلغ|اضيف)\\s*(\\d+(?:\\.\\d+)?)");
        Matcher ma = amountPattern.matcher(body);
        if (ma.find()) amount = toIntAmount(ma.group(1));

        Pattern phonePattern = Pattern.compile("(7\\d{8}|\\d{9,12})");
        Matcher mp = phonePattern.matcher(body);
        String last = "";
        while (mp.find()) last = mp.group(1);
        phone = last;

        if (amount > 0 && phone.length() >= 7) {
            String provider = sender.toLowerCase().contains("jaib") || sender.contains("جيب") ? "Jaib" : "Jawali";
            return new ParsedPayment(provider, amount, phone, "");
        }
        return null;
    }

    private static int toIntAmount(String value) {
        try {
            double d = Double.parseDouble(value);
            return (int) Math.round(d);
        } catch (Exception e) {
            return 0;
        }
    }
}
