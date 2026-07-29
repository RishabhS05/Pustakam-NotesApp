package com.app.pustakam.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal expect  fun platformHttpClient (config: HttpClientConfig<*>.()-> Unit): HttpClient
fun createHttpClient(authTokenProvider:()-> String? ): HttpClient = platformHttpClient {
    val appJson = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint= true
        // 🔧 C8: sealed NoteContentModel discriminates on "type" (TEXT/MEDIA/LINK/LOCATION);
        //       the ContentType property serializes as "contentType" to avoid the clash
        classDiscriminator = "type"
        // 🔧 C8: server nulls coerce to defaults instead of throwing on non-null fields
        coerceInputValues = true
    }
    install(ContentNegotiation) { json(appJson) }
    install(Logging ){
        level= LogLevel.INFO
        logger = Logger.DEFAULT
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
    }
    defaultRequest {
        authTokenProvider()?.takeIf { it.isNotBlank() }?.let{
            headers.append(HttpHeaders.Authorization, "Bearer $it")
        }
    }
}

//private fun getBaseUrl(): String = "https://notesapp-s8wpnlgb.b4a.run"
private fun getBaseUrl(): String = "https://unarmored-bucket-yo-yo.ngrok-free.dev/"
private fun getBaseUrlDev(): String =
    //"http://192.168.68.103:3000"
"http://192.168.31.4:3000"
fun getUrl(): String = getBaseUrl()
