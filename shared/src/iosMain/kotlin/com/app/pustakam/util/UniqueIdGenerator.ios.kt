package com.app.pustakam.util
import platform.Foundation.NSUUID
actual fun generateUUID(): String {
    return NSUUID().UUIDString()
}