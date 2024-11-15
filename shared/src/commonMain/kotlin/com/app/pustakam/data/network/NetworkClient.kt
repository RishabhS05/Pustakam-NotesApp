package com.app.pustakam.data.network

import io.ktor.client.HttpClient



expect fun createHttpClient(): HttpClient

private fun getBaseUrl(): String = "https://notesapp-s8wpnlgb.b4a.run"
private fun getBaseUrlDev(): String = "http://192.168.68.107:3000"

fun getUrl(): String = getBaseUrl()
