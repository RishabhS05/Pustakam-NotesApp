package com.app.pustakam.data.sync


import kotlinx.serialization.Serializable
@Serializable
enum class SyncStatus { LOCAL_ONLY, SYNCED, PENDING_UPDATE, PENDING_DELETE }