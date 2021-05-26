package com.nitor.multimodalcoreweekdemo.intents

import com.speechly.client.slu.Segment

interface IntentAction {

    val segment: Segment

    fun process(): String
}


class NoAction(override val segment: Segment) : IntentAction {
    override fun process(): String { return "No Action" }
}