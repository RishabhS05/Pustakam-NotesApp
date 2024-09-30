package com.app.pustakam.data.network
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun getNetworkEngine(): HttpClientEngine = OkHttp.create()
