package com.app.pustakam.data.network

import com.app.pustakam.domain.repositories.BaseRepository
import io.ktor.client.HttpClient

import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin.create()) {
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }
        install(DefaultRequest) {
            headers.apply {
                append(headerAuth, BaseRepository.token)
            }
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