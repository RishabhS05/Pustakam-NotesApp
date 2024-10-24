package com.app.pustakam.data.network

import io.ktor.client.HttpClient



expect fun createHttpClient(): HttpClient

private fun getBaseUrl(): String = "https://notesapp-s8wpnlgb.b4a.run"
private fun getBaseUrlDev(): String = "http//localhost:8080"

fun getUrl(): String = getBaseUrl()
