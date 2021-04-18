package com.mad.carpooling.ui.trip_edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.mad.carpooling.R

class TripEditFragment : Fragment() {

    private lateinit var tripEditViewModel: TripEditViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        tripEditViewModel =
                ViewModelProvider(this).get(TripEditViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_trip_details, container, false)
        return root
    }
}