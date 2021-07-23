package com.mad.carpooling.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mad.carpooling.R
import com.mad.carpooling.model.Trip
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object TripUtils {
    fun checkStopsValidity(stops: ArrayList<String>): Boolean {
        stops.forEach { s ->
            val stop = s.split(",")
            stop.forEach { s -> if (s.isEmpty()) return false }
        }
        return true
    }

    fun calcDuration(stops: ArrayList<String>): String {
        val str1 =
            stops.get(0).split(",").get(2) + "T" + stops.get(0).split(",").get(3)
        val str2 = stops.get(stops.size - 1).split(",")
            .get(2) + "T" + stops.get(stops.size - 1).split(",").get(3)

        val d1 = LocalDateTime.parse(str1, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val d2 = LocalDateTime.parse(str2, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val s = Duration.between(d1, d2).toMillis() / 1000
        var d: String = if (s / 86400 > 0) {
            String.format("%d days %d h %02d min", s / 86400, (s % 86400) / 3600, (s % 3600) / 60)
        } else {
            String.format("%d h %02d min", s / 3600, (s % 3600) / 60)
        }
        if (s / 86400 < 2) d = d.replace("s", "")
        return d
    }

    fun changeStateFab(fab: FloatingActionButton, trip: Trip, context: Context) {
        if (trip.visibility) {
            fab.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_sharp_visibility
                )
            )
            fab.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.green_300)
        } else {
            fab.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_baseline_visibility_off
                )
            )
            fab.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.red_300)
        }
    }
}