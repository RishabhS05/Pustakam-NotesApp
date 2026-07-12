package com.app.pustakam.bridge

import kotlinx.coroutines.Job

class Closeable(private val job: Job) {
    fun close() = job.cancel()
}