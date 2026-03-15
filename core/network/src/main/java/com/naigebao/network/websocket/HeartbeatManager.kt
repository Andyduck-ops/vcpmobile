package com.naigebao.network.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class HeartbeatManager(
    private val scope: CoroutineScope,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS
) {
    private val heartbeatJob = AtomicReference<Job?>(null)

    fun start(onHeartbeat: suspend () -> Boolean) {
        stop()
        heartbeatJob.set(
            scope.launch {
                while (true) {
                    delay(intervalMillis)
                    val keepAlive = onHeartbeat()
                    if (!keepAlive) {
                        break
                    }
                }
            }
        )
    }

    fun stop() {
        heartbeatJob.getAndSet(null)?.cancel()
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 30_000L
    }
}
