package com.mad.carpooling.viewmodel

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.model.Trip
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository
import com.mad.carpooling.ui.trip_edit.TripEditFragment
import kotlinx.coroutines.launch

class TripEditViewModel(private val tripRepository: TripRepository, private val userRepository : UserRepository) : ViewModel() {

    private lateinit var trip: Trip

    fun getTrip(): Trip {
        return trip
    }

    fun setTrip(trip: Trip) {
        this.trip = trip
    }

    fun updateTrip(tripToUpdate: Trip) : LiveData<Boolean>{
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch{
            val result = tripRepository.updateTrip(tripToUpdate)
            res.postValue(result)
        }
        return res
    }

    fun removeFavTrip(user: String, tripId: String) : LiveData<Boolean>{
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch{
            val result = tripRepository.removeFavTrip(user, tripId)
            res.postValue(result)
        }
        return res
    }

    fun getNewTripId() : String{
        return tripRepository.getNewTripId()
    }

    fun createTrip(tripToCreate: Trip) : LiveData<Boolean>{
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch{
            val result = tripRepository.updateTrip(tripToCreate)
            res.postValue(result)
        }
        return res
    }
}