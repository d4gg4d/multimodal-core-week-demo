package com.nitor.multimodalcoreweekdemo.intents

import com.speechly.client.slu.Segment

class IntentParser {

    var latestSegment: Segment? = null
    var latestFinalSegment: Segment? = null

    fun updateLatestSegment(segment: Segment) {
        latestSegment = segment
        if (segment.isFinal) {
            latestFinalSegment = segment
        }
    }

    fun resolveAction(): IntentAction? {
        return when (latestFinalSegment?.intent?.intent) {
            "report" -> HourReportAction(latestFinalSegment!!)
            else -> null
        }
    }

    fun reset() {
        latestFinalSegment = null
        latestSegment = null
    }
}