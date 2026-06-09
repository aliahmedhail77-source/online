package com.example.autosmscards;

import android.content.Context;
import android.telephony.SmsManager;

import java.security.MessageDigest;
import java.util.UUID;

class SmsProcessor {
    static void processIncomingSms(Context context, String sender, String body) {
        if (!CardStore.isAutoSendEnabled(context)) return;
        if (!PaymentParser.trustedSender(sender)) return;

        ParsedPayment payment = PaymentParser.parse(sender, body);
        if (payment == null) {
            addLog(context, sender, "", 0, "مرفوض", "تعذر فهم الرسالة", "");
            return;
        }

        String eventId = sha256(sender + "|" + body);
        if (CardStore.isProcessed(context, eventId)) return;

        CardStore.markProcessed(context, eventId);

        CardItem card = CardStore.takeAvailableCard(context, payment.amount, payment.customerPhone);
        if (card == null) {
            String noStock = "تم استلام مبلغ " + payment.amount + " ريال، لكن كروت فئة " + payment.amount + " غير متوفرة حاليًا. يرجى التواصل مع الإدارة.";
            trySendSms(payment.customerPhone, noStock);
            addLog(context, sender, payment.customerPhone, payment.amount, "معلق", "لا توجد كروت متاحة من نفس الفئة", "");
            return;
        }

        String reply = "تم استلام مبلغ " + payment.amount + " ريال بنجاح.\n"
                + "كرتك من فئة " + payment.amount + ":\n"
                + card.code + "\n\n"
                + "شكرًا لاستخدامك خدمتنا.";

        boolean sent = trySendSms(payment.customerPhone, reply);

        if (sent) {
            addLog(context, sender, payment.customerPhone, payment.amount, "تم الإرسال", "تم إرسال الكرت تلقائيًا", card.code);
        } else {
            addLog(context, sender, payment.customerPhone, payment.amount, "فشل الإرسال", "تم حجز الكرت لكن فشل إرسال SMS", card.code);
        }
    }

    private static boolean trySendSms(String phone, String text) {
        try {
            if (phone == null || phone.trim().isEmpty()) return false;
            SmsManager sms = SmsManager.getDefault();
            if (text.length() > 160) {
                sms.sendMultipartTextMessage(phone, null, sms.divideMessage(text), null, null);
            } else {
                sms.sendTextMessage(phone, null, text, null, null);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void addLog(Context context, String sender, String phone, int amount, String status, String message, String cardCode) {
        CardStore.addLog(context, new OperationLog(
                UUID.randomUUID().toString(),
                sender == null ? "" : sender,
                phone == null ? "" : phone,
                amount,
                status,
                message,
                cardCode == null ? "" : cardCode,
                CardStore.now()
        ));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(value.hashCode());
        }
    }
}
