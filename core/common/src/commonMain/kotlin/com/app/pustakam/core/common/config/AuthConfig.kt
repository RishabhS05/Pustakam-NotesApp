package com.app.pustakam.core.common.config

object AuthConfig {
    const val BYPASS_AUTH = false
    const val LOCAL_USER_ID = "local-user"

    fun bypassAuth(): Boolean = BYPASS_AUTH
    fun localUserId(): String = LOCAL_USER_ID
}
