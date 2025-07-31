package com.app.pustakam.android.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ImageCompressionWorker(
    private val context : Context,
    private val params: WorkerParameters,
) :  CoroutineWorker(context, params){
    private  fun  compressImage() {

    }

    override suspend fun doWork(): Result {
        return try {
            compressImage()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}