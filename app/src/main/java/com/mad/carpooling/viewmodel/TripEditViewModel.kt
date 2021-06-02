package com.mad.carpooling.viewmodel

import androidx.lifecycle.ViewModel
import com.mad.carpooling.model.Trip

class TripEditViewModel : ViewModel() {

    private lateinit var trip : Trip

    fun getTrip() : Trip {
        return trip
    }

    fun setTrip(trip: Trip){
        this.trip = trip
    }

}