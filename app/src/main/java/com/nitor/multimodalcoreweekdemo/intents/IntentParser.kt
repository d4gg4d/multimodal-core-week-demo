package com.nitor.multimodalcoreweekdemo.intents

import com.nitor.multimodalcoreweekdemo.services.HoursReportingService
import com.speechly.client.slu.Segment

class IntentParser(private val hoursReporting: HoursReportingService) {

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
            "report" -> HourReportAction(latestFinalSegment!!, hoursReporting)
            else -> null
        }
    }

    fun reset() {
        latestFinalSegment = null
        latestSegment = null
    }
}