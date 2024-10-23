package com.app.pustakam.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
fun createHttpClient(engine: HttpClientEngine): HttpClient {
    return HttpClient(engine) {
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }
        install(ContentNegotiation) {
            json(
                json = Json {
                    explicitNulls = false
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                }
            )
        }
    }
}

private fun getBaseUrl(): String = "https://notesapp-s8wpnlgb.b4a.run"
private fun getBaseUrlDev(): String = "http//localhost:8080"

fun getUrl(): String = getBaseUrl()
expect fun getNetworkEngine(): HttpClientEngine
