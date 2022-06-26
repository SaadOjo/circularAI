package com.example.circularai

import java.time.LocalDateTime

//default set to null to allow deserialization from firebase datasnapshot
data class recycled_entry(var epoch: Long? = null, val plastic: Int? = null, val metal: Int? = null, val paper: Int? = null, val glass: Int? = null ){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "epoch" to epoch,
            "plastic" to plastic,
            "metal" to metal,
            "paper" to paper,
            "glass" to glass
        )
    }
}
