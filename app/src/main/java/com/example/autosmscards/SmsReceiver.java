package com.example.autosmscards;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null) return;

        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");

        if (pdus == null) return;

        String sender = "";
        StringBuilder body = new StringBuilder();

        for (Object pdu : pdus) {
            SmsMessage msg;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                msg = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                msg = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (msg == null) continue;
            sender = msg.getDisplayOriginatingAddress();
            body.append(msg.getMessageBody());
        }

        SmsProcessor.processIncomingSms(context, sender, body.toString());
    }
}
