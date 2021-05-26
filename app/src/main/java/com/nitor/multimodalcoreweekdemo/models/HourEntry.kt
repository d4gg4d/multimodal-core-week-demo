package com.nitor.multimodalcoreweekdemo.models

import com.google.gson.annotations.SerializedName
import java.util.*

class HourEntry (
    @SerializedName("hours")
    val hours: Double,
    @SerializedName("localDate")
    val localDate: Date,
    @SerializedName("projectId")
    val projectId: String,
    @SerializedName("description")
    val description: String
)



