package com.app.pustakam.data.network
import com.app.pustakam.util.log_d
import com.app.pustakam.util.log_i
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

import java.util.concurrent.TimeUnit


 internal  actual  fun platformHttpClient(config : HttpClientConfig<*>.()-> Unit): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
        config()
    }
}
