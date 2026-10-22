package com.shilapi.xcertplay.network

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BonjourProbeQueueTest {
    private var now = 0L
    private fun queue() = BonjourProbeQueue<String>(7, 1_500) { now }

    @Test fun reachableIpv4RunsAfterOneIpv6FailureInsteadOfAllSeven() {
        val queue = queue()
        queue.offer("IPv6")
        val failed = requireNotNull(queue.poll(0))
        // The first connect attempt times out while the other family is discovered.
        now += 3_000_000_000L
        queue.offer("IPv4")
        assertTrue(queue.retry(failed))

        val next = requireNotNull(queue.poll(0))
        assertEquals("IPv4", next.value)
        assertEquals(1, next.number)
        assertNull(queue.poll(0))

        now += 1_500_000_000L
        val retry = requireNotNull(queue.poll(0))
        assertEquals("IPv6", retry.value)
        assertEquals(2, retry.number)
    }

    @Test fun newlyDiscoveredIpv6AlsoBypassesAPendingIpv4Retry() {
        val queue = queue()
        queue.offer("IPv4")
        assertTrue(queue.retry(requireNotNull(queue.poll(0))))
        now += 100_000_000L
        queue.offer("IPv6")
        assertEquals("IPv6", requireNotNull(queue.poll(0)).value)
        assertNull(queue.poll(0))
    }

    @Test fun singleFamilyKeepsSevenAttemptsAndTheFullRecoveryDelay() {
        val queue = queue()
        queue.offer("IPv4")
        for (number in 1..7) {
            val attempt = requireNotNull(queue.poll(0))
            assertEquals(number, attempt.number)
            if (number < 7) {
                assertTrue(queue.retry(attempt))
                now += 1_499_000_000L
                assertNull(queue.poll(0))
                now += 1_000_000L
            } else {
                assertFalse(queue.retry(attempt))
            }
        }
        now += 60_000_000_000L
        assertNull(queue.poll(0))
    }

    @Test fun closingClearsBothDiscoveredAndDelayedWork() {
        val queue = queue()
        queue.offer("IPv6")
        assertTrue(queue.retry(requireNotNull(queue.poll(0))))
        queue.offer("IPv4")
        queue.clear()
        assertNull(queue.poll(0))
        now += 60_000_000_000L
        assertNull(queue.poll(0))
    }

    @Test fun retryOrderingSurvivesMonotonicClockWraparound() {
        now = Long.MAX_VALUE - 1_000_000_000L
        val queue = queue()
        queue.offer("IPv6")
        assertTrue(queue.retry(requireNotNull(queue.poll(0))))
        queue.offer("IPv4")
        assertEquals("IPv4", requireNotNull(queue.poll(0)).value)
        assertNull(queue.poll(0))
        now += 1_500_000_000L
        assertEquals("IPv6", requireNotNull(queue.poll(0)).value)
    }

    @Test fun newlyDiscoveredAddressWakesTheWorkerBeforeTheRetryDeadline() {
        val queue = BonjourProbeQueue<String>(7, 60_000)
        queue.offer("IPv6")
        assertTrue(queue.retry(requireNotNull(queue.poll(0))))
        val started = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val received = AtomicReference<BonjourProbeQueue.Attempt<String>>()
        val worker = Thread {
            started.countDown()
            try {
                received.set(queue.poll(60_000))
            } finally {
                finished.countDown()
            }
        }
        worker.start()
        try {
            assertTrue(started.await(1, TimeUnit.SECONDS))
            queue.offer("IPv4")
            assertTrue(finished.await(1, TimeUnit.SECONDS))
            assertEquals("IPv4", received.get()?.value)
        } finally {
            worker.interrupt()
            worker.join(1_000)
        }
    }

    @Test fun clearingAndInterruptingStopsTheWorkerWithRetriesStillPending() {
        val queue = BonjourProbeQueue<String>(7, 60_000)
        queue.offer("IPv6")
        assertTrue(queue.retry(requireNotNull(queue.poll(0))))
        val started = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val interrupted = AtomicBoolean()
        val worker = Thread {
            started.countDown()
            try {
                queue.poll(60_000)
            } catch (_: InterruptedException) {
                interrupted.set(true)
            } finally {
                finished.countDown()
            }
        }
        worker.start()
        try {
            assertTrue(started.await(1, TimeUnit.SECONDS))
            queue.clear()
            worker.interrupt()
            assertTrue(finished.await(1, TimeUnit.SECONDS))
            assertTrue(interrupted.get())
            assertNull(queue.poll(0))
        } finally {
            worker.interrupt()
            worker.join(1_000)
        }
    }
}
