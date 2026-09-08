package org.blocovermelho.ae2emi.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {
    @Test
    void burstIsBoundedAndConnectionsHaveIndependentBudgets() {
        var time = new AtomicLong();
        var limiter = new RequestRateLimiter<String>(8, 50, time::get);
        for (int i = 0; i < 8; i++) {
            assertTrue(limiter.tryAcquire("first"));
        }
        for (int i = 0; i < 1000; i++) {
            assertFalse(limiter.tryAcquire("first"));
        }
        assertTrue(limiter.tryAcquire("second"));
        time.set(49);
        assertFalse(limiter.tryAcquire("first"));
        time.set(50);
        assertTrue(limiter.tryAcquire("first"));
        assertFalse(limiter.tryAcquire("first"));
    }

    @Test
    void rejectedFloodDoesNotPostponeRefillAndPartialCreditSurvives() {
        var time = new AtomicLong();
        var limiter = new RequestRateLimiter<String>(1, 50, time::get);
        assertTrue(limiter.tryAcquire("player"));
        for (int i = 1; i < 50; i++) {
            time.set(i);
            assertFalse(limiter.tryAcquire("player"));
        }
        time.set(75);
        assertTrue(limiter.tryAcquire("player"));
        // The bucket is capped at one credit, so idle time cannot fund another burst.
        time.set(100);
        assertFalse(limiter.tryAcquire("player"));
        time.set(125);
        assertTrue(limiter.tryAcquire("player"));
    }

    @Test
    void longIdleTimeRefillsOnlyOneBurstWithoutOverflow() {
        var time = new AtomicLong();
        var limiter = new RequestRateLimiter<String>(2, 50, time::get);
        assertTrue(limiter.tryAcquire("player"));
        time.set(Long.MAX_VALUE);
        assertTrue(limiter.tryAcquire("player"));
        assertTrue(limiter.tryAcquire("player"));
        assertFalse(limiter.tryAcquire("player"));
        // nanoTime subtraction remains correct across the signed long boundary.
        time.set(Long.MIN_VALUE + 49);
        assertTrue(limiter.tryAcquire("player"));
    }

    @Test
    void concurrentNetworkThreadsCannotExceedTheBurst() {
        var limiter = new RequestRateLimiter<String>(8, 50, () -> 0L);
        assertEquals(8, IntStream.range(0, 1000).parallel()
                .filter(i -> limiter.tryAcquire("same connection")).count());
    }
}
