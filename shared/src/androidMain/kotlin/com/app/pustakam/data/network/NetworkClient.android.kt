package com.app.pustakam.data.network
import com.app.pustakam.util.log_d
import com.app.pustakam.util.log_i
import io.ktor.client.HttpClient

import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

import java.util.concurrent.TimeUnit

actual fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(5, TimeUnit.SECONDS)
            }
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    log_d("HTTPS_Client: ",message)
                }

            }
        }
        install(ContentNegotiation) {
            json(json = Json {
                explicitNulls = false
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }
}
