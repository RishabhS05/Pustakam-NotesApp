package com.app.pustakam.core.network
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

import io.ktor.client.engine.okhttp.OkHttp


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
