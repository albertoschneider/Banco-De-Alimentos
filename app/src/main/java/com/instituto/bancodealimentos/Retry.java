package com.instituto.bancodealimentos;

import android.os.Handler;
import android.os.Looper;

/** Helper simples de retry com backoff exponencial. */
public final class Retry {
    private Retry() {}

    @FunctionalInterface
    public interface Job { void run(); }

    public static class Backoff {
        private final Handler h = new Handler(Looper.getMainLooper());
        private int attempt = 0;
        private final int maxAttempts;
        private final long baseMs; // p.ex.: 300ms

        public Backoff(int maxAttempts, long baseMs) {
            this.maxAttempts = Math.max(1, maxAttempts);
            this.baseMs = Math.max(50, baseMs);
        }

        /** Reseta a contagem de tentativas. */
        public void reset() { attempt = 0; }

        /** Agenda a execução do job com backoff exponencial. */
        public void schedule(Job job) {
            if (attempt >= maxAttempts) return;
            long delay = (long) (baseMs * Math.pow(2, attempt)); // 300, 600, 1200, ...
            attempt++;
            h.postDelayed(job::run, delay);
        }

        /** Ainda podemos tentar de novo? */
        public boolean canRetry() { return attempt < maxAttempts; }
    }
}
