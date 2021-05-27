package com.nitor.multimodalcoreweekdemo.models

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

class HourEntry (
    @SerializedName("hours")
    val hours: Double,
    @SerializedName("date")
    val date: String,
    @SerializedName("projectId")
    val projectId: String,
    @SerializedName("description")
    val description: String
)



