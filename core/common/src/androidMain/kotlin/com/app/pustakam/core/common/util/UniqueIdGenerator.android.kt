package com.app.pustakam.core.common.util
import java.util.UUID
actual fun generateUUID(): String {
return UUID.randomUUID().toString()
}