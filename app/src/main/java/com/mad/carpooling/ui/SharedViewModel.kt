package com.mad.carpooling.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.data.Trip
import com.mad.carpooling.data.User

class SharedViewModel : ViewModel() {

    private val trips: MutableLiveData<HashMap<String, Trip>> by lazy {
        MutableLiveData<HashMap<String, Trip>>().also {
            loadTrips()
        }
    }

    private val currentUser: MutableLiveData<User> by lazy {
        MutableLiveData<User>().also {
            loadUser()
        }
    }

    private fun loadUser() {
        // Do an asynchronous operation to fetch user.
        val db = Firebase.firestore
        db.collection("users").addSnapshotListener { value, e ->
            if (e != null) {
                currentUser.value = null
                Log.e("loadUser() exception => ", e.toString())
                return@addSnapshotListener
            }
            var user = User()
            for (doc in value!!) {
                if(doc.id == Firebase.auth.currentUser?.uid)
                     user = doc.toObject(User::class.java)
            }
            currentUser.value = user
        }

    }

    fun getCurrentUser(): LiveData<User> {
        return currentUser
    }

    private fun loadTrips() {
        // Do an asynchronous operation to fetch trips.
        val db = Firebase.firestore
        db.collection("trips").addSnapshotListener { value, e ->
            if (e != null) {
                trips.value = null
                Log.e("loadTrips() exception => ", e.toString())
                return@addSnapshotListener
            }
            val tripsMap: HashMap<String, Trip> = HashMap()
            for (doc in value!!) {
                tripsMap[doc.id] = doc.toObject(Trip::class.java)
            }
            trips.value = tripsMap
        }
    }

    fun getTrips(): LiveData<HashMap<String, Trip>> {
        return trips
    }


}

