package com.mad.carpooling.ui.trip_edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mad.carpooling.data.Trip

class TripEditViewModel : ViewModel() {

    private lateinit var trip : Trip

    fun getTrip() : Trip{
        return trip
    }

    fun setTrip(trip: Trip){
        this.trip = trip
    }

}