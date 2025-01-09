package com.app.pustakam.android.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.io.File
import java.io.IOException

fun Activity.goToAppSetting() {
    val i = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    startActivity(i)
}
