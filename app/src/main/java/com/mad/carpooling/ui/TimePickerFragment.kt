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
import java.text.SimpleDateFormat
import java.util.*

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
        tvTime.text =
            (SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault())).format(
                SimpleDateFormat("HH:mm", Locale.getDefault()).parse("$hourOfDay:$minute")!!
            )
    }
}
