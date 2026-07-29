package com.app.pustakam.core.model.models

import com.app.pustakam.util.getCurrentTimestamp
import kotlinx.serialization.Serializable

@Serializable
data class Tag(val id : String, val label : String?, val color : String? ){
    constructor( label: String?, color: String? ) : this(
        id = "${getCurrentTimestamp()}",
        label = label,
        color = color
    )
}