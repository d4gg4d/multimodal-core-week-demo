package com.nitor.multimodalcoreweekdemo.models

import com.google.gson.annotations.SerializedName

data class Project(
    @SerializedName("customerName")
    val customerName: String,
    @SerializedName("caseName")
    val caseName: String,
    @SerializedName("projectName")
    val projectName: String,
    @SerializedName("projectId")
    val projectId: String
)

