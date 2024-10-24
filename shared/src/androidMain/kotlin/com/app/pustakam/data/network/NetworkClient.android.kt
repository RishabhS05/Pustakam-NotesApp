package com.app.pustakam.data.network


import com.app.pustakam.domain.repositories.BaseRepository.UserData.token
import io.ktor.client.HttpClient

import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
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
        install(DefaultRequest) {
                headers.apply {
                    append(headerAuth, token)
            }
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                   println(message)
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

//class NetworkInterceptor : Interceptor {
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val request = chain.request()
//        val builder = request.newBuilder().method(request.method, request.body).addHeader("Authorization", "Bearer " + token)
//        return chain.proceed(builder.build())
//    }
//}