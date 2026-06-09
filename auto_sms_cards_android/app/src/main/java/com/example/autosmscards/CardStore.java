package com.example.autosmscards;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

class CardStore {
    private static final String PREF = "auto_sms_cards_store";
    private static final String KEY_CARDS = "cards";
    private static final String KEY_LOGS = "logs";
    private static final String KEY_PROCESSED = "processed";
    private static final String KEY_AUTO_SEND = "auto_send_enabled";
    private static final String KEY_TRUSTED_CONTACTS = "trusted_contacts";

    static final int[] DEFAULT_AMOUNTS = new int[]{50, 100, 150, 200, 250, 300, 500};

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    static boolean isAutoSendEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_SEND, true);
    }

    static void setAutoSendEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_SEND, enabled).apply();
    }

    static ArrayList<CardItem> loadCards(Context context) {
        ArrayList<CardItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(context).getString(KEY_CARDS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new CardItem(
                        o.optString("id"),
                        o.optInt("amount"),
                        o.optString("code"),
                        o.optBoolean("sold"),
                        o.optString("buyerPhone"),
                        o.optString("soldAt"),
                        o.optString("source")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    static void saveCards(Context context, ArrayList<CardItem> cards) {
        try {
            JSONArray arr = new JSONArray();
            for (CardItem c : cards) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("amount", c.amount);
                o.put("code", c.code);
                o.put("sold", c.sold);
                o.put("buyerPhone", c.buyerPhone == null ? "" : c.buyerPhone);
                o.put("soldAt", c.soldAt == null ? "" : c.soldAt);
                o.put("source", c.source == null ? "" : c.source);
                arr.put(o);
            }
            prefs(context).edit().putString(KEY_CARDS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static ArrayList<OperationLog> loadLogs(Context context) {
        ArrayList<OperationLog> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(context).getString(KEY_LOGS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new OperationLog(
                        o.optString("id"),
                        o.optString("sender"),
                        o.optString("customerPhone"),
                        o.optInt("amount"),
                        o.optString("status"),
                        o.optString("message"),
                        o.optString("cardCode"),
                        o.optString("createdAt")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    static void saveLogs(Context context, ArrayList<OperationLog> logs) {
        try {
            JSONArray arr = new JSONArray();
            for (OperationLog l : logs) {
                JSONObject o = new JSONObject();
                o.put("id", l.id);
                o.put("sender", l.sender);
                o.put("customerPhone", l.customerPhone);
                o.put("amount", l.amount);
                o.put("status", l.status);
                o.put("message", l.message);
                o.put("cardCode", l.cardCode);
                o.put("createdAt", l.createdAt);
                arr.put(o);
            }
            prefs(context).edit().putString(KEY_LOGS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static HashSet<String> loadProcessed(Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_PROCESSED, new HashSet<String>()));
    }

    static boolean isProcessed(Context context, String id) {
        return loadProcessed(context).contains(id);
    }

    static void markProcessed(Context context, String id) {
        HashSet<String> set = loadProcessed(context);
        set.add(id);
        prefs(context).edit().putStringSet(KEY_PROCESSED, set).apply();
    }

    static int importCards(Context context, int amount, ArrayList<String> codes, String source) {
        ArrayList<CardItem> cards = loadCards(context);
        HashSet<String> existing = new HashSet<>();
        for (CardItem c : cards) existing.add(c.amount + "|" + c.code.trim());

        int added = 0;
        for (String raw : codes) {
            String code = raw == null ? "" : raw.trim();
            if (code.isEmpty()) continue;
            String key = amount + "|" + code;
            if (existing.contains(key)) continue;
            cards.add(0, new CardItem(UUID.randomUUID().toString(), amount, code, false, "", "", source));
            existing.add(key);
            added++;
        }
        saveCards(context, cards);
        return added;
    }

    static CardItem takeAvailableCard(Context context, int amount, String buyerPhone) {
        ArrayList<CardItem> cards = loadCards(context);
        CardItem selected = null;
        for (CardItem c : cards) {
            if (c.amount == amount && !c.sold) {
                c.sold = true;
                c.buyerPhone = buyerPhone;
                c.soldAt = now();
                selected = c;
                break;
            }
        }
        if (selected != null) saveCards(context, cards);
        return selected;
    }

    static void addLog(Context context, OperationLog log) {
        ArrayList<OperationLog> logs = loadLogs(context);
        logs.add(0, log);
        while (logs.size() > 500) logs.remove(logs.size() - 1);
        saveLogs(context, logs);
    }

    static int availableCount(Context context, int amount) {
        int count = 0;
        for (CardItem c : loadCards(context)) if (c.amount == amount && !c.sold) count++;
        return count;
    }

    static int soldCount(Context context, int amount) {
        int count = 0;
        for (CardItem c : loadCards(context)) if (c.amount == amount && c.sold) count++;
        return count;
    }

    static ArrayList<TrustedContact> loadTrustedContacts(Context context) {
        ArrayList<TrustedContact> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(context).getString(KEY_TRUSTED_CONTACTS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new TrustedContact(
                        o.optString("id"),
                        o.optString("name"),
                        o.optString("tripleName"),
                        o.optString("phone"),
                        o.optBoolean("active", true)
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    static void saveTrustedContacts(Context context, ArrayList<TrustedContact> list) {
        try {
            JSONArray arr = new JSONArray();
            for (TrustedContact c : list) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("name", c.name);
                o.put("tripleName", c.tripleName);
                o.put("phone", c.phone);
                o.put("active", c.active);
                arr.put(o);
            }
            prefs(context).edit().putString(KEY_TRUSTED_CONTACTS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    static void addTrustedContact(Context context, String name, String phone) {
        ArrayList<TrustedContact> list = loadTrustedContacts(context);
        String triple = NameUtils.tripleName(name);
        for (TrustedContact c : list) {
            if (c.tripleName.equals(triple)) {
                c.name = name.trim();
                c.phone = phone.trim();
                c.active = true;
                saveTrustedContacts(context, list);
                return;
            }
        }
        list.add(0, new TrustedContact(UUID.randomUUID().toString(), name.trim(), triple, phone.trim(), true));
        saveTrustedContacts(context, list);
    }

    static TrustedContact findTrustedContactByTripleName(Context context, String rawName) {
        String incomingTriple = NameUtils.tripleName(rawName);
        if (incomingTriple.isEmpty()) return null;
        for (TrustedContact c : loadTrustedContacts(context)) {
            if (c.active && c.tripleName.equals(incomingTriple)) return c;
        }
        return null;
    }

    static void deleteTrustedContact(Context context, String id) {
        ArrayList<TrustedContact> list = loadTrustedContacts(context);
        ArrayList<TrustedContact> next = new ArrayList<>();
        for (TrustedContact c : list) if (!c.id.equals(id)) next.add(c);
        saveTrustedContacts(context, next);
    }

    static String now() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(new Date());
    }

    static void clearAll(Context context) {
        prefs(context).edit().clear().apply();
    }
}
