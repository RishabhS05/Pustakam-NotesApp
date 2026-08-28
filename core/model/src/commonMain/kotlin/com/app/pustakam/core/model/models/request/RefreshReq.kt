package com.app.pustakam.core.model.models.request

import kotlinx.serialization.Serializable

// 🔐 20-Aug-2026 sync: body of POST /auth/refresh — the server rotates and returns a NEW refresh token
@Serializable
data class RefreshReq(val refreshToken: String)
