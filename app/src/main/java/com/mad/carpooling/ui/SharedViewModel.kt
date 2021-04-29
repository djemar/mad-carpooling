package com.mad.carpooling.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.Trip

class SharedViewModel : ViewModel() {

    private val trips: MutableLiveData<HashMap<String, Trip>> by lazy {
        MutableLiveData<HashMap<String, Trip>>().also {
            loadTrips()
        }
    }

    fun getTrips(): LiveData<HashMap<String, Trip>> {
        return trips
    }

    private fun loadTrips(){
        // Do an asynchronous operation to fetch trips.
        val db = Firebase.firestore
        db.collection("trips").also {
            it.addSnapshotListener{
                value, e ->
                if (e != null) {
                    trips.value = null
                    Log.e("loadTrips() exception => ", e.toString())
                    return@addSnapshotListener
                }
                val tripsMap : HashMap<String, Trip> = HashMap()
                for (doc in value!!) {
                    tripsMap[doc.id] = doc.toObject(Trip::class.java)
                }
                trips.value = tripsMap
            }
        }
    }

}