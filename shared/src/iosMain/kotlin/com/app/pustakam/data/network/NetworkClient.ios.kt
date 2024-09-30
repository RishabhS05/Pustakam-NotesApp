package com.app.pustakam.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun getNetworkEngine(): HttpClientEngine = Darwin.create()
