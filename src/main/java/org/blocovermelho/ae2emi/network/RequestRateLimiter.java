package org.blocovermelho.ae2emi.network;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.LongSupplier;

/** Shared token bucket for both terminal actions, keyed by live connection. */
final class RequestRateLimiter<K> {
    private static final class Bucket {
        long updated;
        long credit;

        Bucket(long updated, long credit) {
            this.updated = updated;
            this.credit = credit;
        }
    }

    private final Map<K, Bucket> buckets = new WeakHashMap<>();
    private final LongSupplier clock;
    private final long interval;
    private final long capacity;

    RequestRateLimiter(int burst, long intervalNanos, LongSupplier clock) {
        if (burst <= 0 || intervalNanos <= 0) {
            throw new IllegalArgumentException("Rate limits must be positive");
        }
        this.interval = intervalNanos;
        this.capacity = Math.multiplyExact(burst, intervalNanos);
        this.clock = clock;
    }

    synchronized boolean tryAcquire(K connection) {
        long now = clock.getAsLong();
        Bucket bucket = buckets.computeIfAbsent(connection, key -> new Bucket(now, capacity));
        long elapsed = now - bucket.updated;
        if (elapsed > 0) {
            // Clamp before addition, so long idle periods cannot overflow the credit.
            bucket.credit += Math.min(elapsed, capacity - bucket.credit);
            bucket.updated = now;
        }
        if (bucket.credit < interval) {
            return false;
        }
        bucket.credit -= interval;
        return true;
    }
}
