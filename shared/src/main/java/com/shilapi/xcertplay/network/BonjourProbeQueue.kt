package com.shilapi.xcertplay.network

import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit

/**
 * Keeps a failing mDNS address from monopolizing the single probe worker. In particular, an
 * unreachable IPv6 address must not consume every retry before a discovered IPv4 address runs.
 * Only retries are delayed; discovery can wake the worker immediately, including on KitKat.
 */
internal class BonjourProbeQueue<T>(
    private val maxAttempts: Int,
    private val retryDelayMillis: Long,
    private val nowNanos: () -> Long = System::nanoTime,
) {
    init {
        require(maxAttempts > 0)
        require(retryDelayMillis >= 0)
    }

    private val pending = DelayQueue<Attempt<T>>()

    fun offer(value: T) {
        pending.offer(Attempt(value, 1, nowNanos(), nowNanos))
    }

    fun poll(timeoutMillis: Long): Attempt<T>? =
        pending.poll(timeoutMillis, TimeUnit.MILLISECONDS)

    /** Returns false when this endpoint has exhausted its original retry budget. */
    fun retry(attempt: Attempt<T>): Boolean {
        if (attempt.number >= maxAttempts) return false
        pending.offer(Attempt(
            attempt.value,
            attempt.number + 1,
            nowNanos() + TimeUnit.MILLISECONDS.toNanos(retryDelayMillis),
            nowNanos,
        ))
        return true
    }

    fun clear() = pending.clear()

    class Attempt<T> internal constructor(
        val value: T,
        val number: Int,
        private val readyAtNanos: Long,
        private val nowNanos: () -> Long,
    ) : Delayed {
        override fun getDelay(unit: TimeUnit): Long =
            unit.convert(readyAtNanos - nowNanos(), TimeUnit.NANOSECONDS)

        override fun compareTo(other: Delayed): Int {
            // Subtraction keeps ordering correct across System.nanoTime's signed wraparound.
            val difference = if (other is Attempt<*>) {
                readyAtNanos - other.readyAtNanos
            } else {
                getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS)
            }
            return difference.compareTo(0L)
        }
    }
}
