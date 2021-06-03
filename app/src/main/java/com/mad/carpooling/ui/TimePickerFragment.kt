package com.mad.carpooling.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class TimePickerFragment(val tvTime: TextView) : DialogFragment(),
    TimePickerDialog.OnTimeSetListener {

    // private val tvTime = tvTime

    override fun onPause() {
        super.onPause()
        dismissAllowingStateLoss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current time as the default values for the picker
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it
        return TimePickerDialog(
            activity,
            this,
            hour,
            minute,
            DateFormat.is24HourFormat(activity)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        val str = "${checkDigit(hourOfDay)}:${checkDigit(minute)}"
        tvTime.text = LocalTime.parse(str, DateTimeFormatter.ISO_LOCAL_TIME)
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        /* (SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault())).format(
             SimpleDateFormat("HH:mm", Locale.getDefault()).parse("$hourOfDay:$minute")!!
         )*/
    }

    fun checkDigit(number: Int): String {
        return if (number <= 9) "0$number" else number.toString()
    }
}
