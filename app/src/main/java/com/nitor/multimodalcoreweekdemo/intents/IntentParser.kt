package com.nitor.multimodalcoreweekdemo.intents

import android.util.Log
import com.nitor.multimodalcoreweekdemo.services.HoursReporting
import com.speechly.client.slu.Segment

private const val TAG:String = "IntentParser"

class IntentParser(private val hoursReporting: HoursReporting) {

    var segments: MutableList<Segment> = mutableListOf()
    var latestFinalSegment: Segment? = null

    fun updateLatestSegment(segment: Segment) {
        segments.add(segment)
        if (segment.isFinal) {
            latestFinalSegment = segment
        }
    }

    fun resolveAction(): IntentAction? {
        Log.d(TAG, "prosessed segments:")
        segments.forEach { _it ->
            Log.d(TAG, "(${_it.contextId} : ${_it.segmentId} : ${_it.isFinal}), words = ${_it.words.values.joinToString(" ", transform = { it.value })}")
        }
        return when (latestFinalSegment?.intent?.intent) {
            "report" -> HourReportAction(latestFinalSegment!!, hoursReporting)
            else -> null
        }
    }

    fun reset() {
        segments = mutableListOf()
        latestFinalSegment = null
    }
}