package com.nitor.multimodalcoreweekdemo.intents

import com.speechly.client.slu.Segment

interface IntentAction {

    val segment: Segment

    fun process(): String
}