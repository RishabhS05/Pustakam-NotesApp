package com.app.pustakam.core.common.util
import platform.Foundation.NSUUID
actual fun generateUUID(): String {
    return NSUUID().UUIDString()
}