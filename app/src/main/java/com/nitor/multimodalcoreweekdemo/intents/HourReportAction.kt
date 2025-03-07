package com.nitor.multimodalcoreweekdemo.intents

import android.util.Log
import com.nitor.multimodalcoreweekdemo.services.HoursReporting
import com.speechly.client.slu.Entity
import com.speechly.client.slu.Segment

private const val TAG = "HourReportingAction"

class HourReportAction(override val segment: Segment, private val hoursReporting: HoursReporting) : IntentAction {

    override fun process(): String {
        Log.d(TAG, "Final transcript is: ${segment.words.values.joinToString(" ", transform = { it.value })}")
        Log.d(TAG, "reporting hours with entities: ${segment.entities}")

        val project = segment.getEntityByType("project")
        val hours = segment.getEntityByType("hours")
        val timePeriod = segment.getEntityByType("time_period")

        val projectName = validateTargetProject(project)
        val reportingHours = validateReportingHours(hours, timePeriod)

        //
        // TODO here should be the parsing logic of all reporting hours that we want list of (project Alias, hours)
        // - sanity check for the request

        return when {
            allParametersValid(projectName, reportingHours) -> {
                val isSuccess = hoursReporting.sendHours(projectName!!, reportingHours!!)
                return if (isSuccess) {
                    "Marking $reportingHours hours to $projectName"
                } else {
                    "Failed to send hour report"
                }
            }
            else ->  "Can't parse reporting hour request parameters"
        }
    }

    private fun validateTargetProject(raw: Entity?): String? {
        Log.d(TAG, "parsing reporting target from '${raw?.value}'")
        return raw?.value
    }

    private fun validateReportingHours(hours: Entity?, timePeriod: Entity?): Number? {
        return when {
            hours?.value != null -> parseHours(hours.value)
            timePeriod?.value != null -> parseTimePeriod(timePeriod.value)
            else -> null
        }
    }

    private fun parseHours(raw: String): Number? {
        Log.d(TAG, "parsing hours from '${raw}'")
        return raw.toBigDecimalOrNull()
    }

    private fun parseTimePeriod(raw: String): Number? {
        Log.d(TAG, "parsing time period from '${raw}'")
        return when(raw) {
            "AFTERNOON", "MORNING", "HALF DAY" -> 3.5F
            "WHOLE DAY", "FULL DAY", "DAY" -> 7.5F
            else -> null
        }
    }

    private fun allParametersValid(targetProject: String?, hours: Number?): Boolean {
        return targetProject?.isNotEmpty() == true &&
                hours != null && hours.toFloat() > 0.0F
    }
}
