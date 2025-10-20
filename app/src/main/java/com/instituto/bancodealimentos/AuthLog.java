package com.instituto.bancodealimentos;

import android.util.Log;
import java.util.ArrayDeque;
import java.util.Deque;

public final class AuthLog {
    private static final String TAG = "AUTH";
    private static final int MAX = 200;
    private static final Deque<String> BUF = new ArrayDeque<>(MAX);

    private AuthLog() {}

    public static synchronized void log(String m) {
        String line = System.currentTimeMillis() + " | " + m;
        Log.d(TAG, line);
        if (BUF.size() >= MAX) BUF.removeFirst();
        BUF.addLast(line);
    }

    public static synchronized String dump() {
        StringBuilder sb = new StringBuilder(BUF.size() * 64);
        for (String s : BUF) sb.append(s).append('\n');
        return sb.toString();
    }

    public static synchronized void clear() { BUF.clear(); }
}
