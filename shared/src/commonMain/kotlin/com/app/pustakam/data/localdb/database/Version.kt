package com.app.pustakam.data.localdb.database

object Version {
    fun nextVersion(
        ownerId: String? ,
        currentVersion: String? = ""
    ): String {

        // No previous version -> create the first one
        if (ownerId.isNullOrBlank()){
            return "v0.0.1"
        }
        if(!ownerId.isNullOrBlank() && currentVersion.isNullOrBlank()) {
            return "$ownerId-v0.0.1"
        }

        val regex = Regex("""^(.+)-v(\d+)\.(\d+)\.(\d+)$""")
        val match = regex.matchEntire(currentVersion!!)
            ?: return "$ownerId-v0.0.1"

        var major = match.groupValues[2].toInt()
        var minor = match.groupValues[3].toInt()
        var patch = match.groupValues[4].toInt()

        patch++

        if (patch > 999) {
            patch = 0
            minor++

            if (minor > 999) {
                minor = 0
                major++
            }
        }

        return "$ownerId-v$major.$minor.$patch"
    }
}