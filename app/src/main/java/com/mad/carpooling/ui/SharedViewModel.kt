package com.mad.carpooling.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
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

    private val myTrips: MutableLiveData<HashMap<String, Trip>> by lazy {
        MutableLiveData<HashMap<String, Trip>>().also {
            loadMyTrips()
        }
    }

    private val boughtTrips: MutableLiveData<HashMap<String, Trip>> by lazy {
        MutableLiveData<HashMap<String, Trip>>().also {
            loadBoughtTrips()
        }
    }

    private val interestTrips: MutableLiveData<HashMap<String, Trip>> by lazy {
        MutableLiveData<HashMap<String, Trip>>().also {
            loadInterestedTrips()
        }
    }

    private fun loadInterestedTrips() {
        val db = Firebase.firestore

        db.collection("trips").whereArrayContains("interestedPeople", currentUser.value?.uid!!)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    interestTrips.postValue(HashMap())
                    Log.e("loadInterestedTrips() exception => ", e.toString())
                    return@addSnapshotListener
                }
                val tripsMap: HashMap<String, Trip> = HashMap()
                for (doc in value!!) {
                    tripsMap[doc.id] = doc.toObject(Trip::class.java)
                }
                interestTrips.postValue(tripsMap.filterValues { t -> t.timestamp>Timestamp.now() } as HashMap<String, Trip>?)
            }
    }

    private fun loadBoughtTrips() {
        val db = Firebase.firestore

        db.collection("trips").whereArrayContains("acceptedPeople", currentUser.value?.uid!!)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    boughtTrips.postValue(HashMap())
                    Log.e("loadBoughtTrips() exception => ", e.toString())
                    return@addSnapshotListener
                }
                val tripsMap: HashMap<String, Trip> = HashMap()
                for (doc in value!!) {
                    tripsMap[doc.id] = doc.toObject(Trip::class.java)
                }
                boughtTrips.postValue(tripsMap)
            }
    }

    private fun loadMyTrips() {
        val db = Firebase.firestore
        val currentUserRef =
            FirebaseFirestore.getInstance().document("users/${currentUser.value?.uid}")
        db.collection("trips").whereEqualTo("owner", currentUserRef)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    myTrips.postValue(HashMap())
                    Log.e("loadMyTrips() exception => ", e.toString())
                    return@addSnapshotListener
                }
                val tripsMap: HashMap<String, Trip> = HashMap()
                for (doc in value!!) {
                    tripsMap[doc.id] = doc.toObject(Trip::class.java)
                }
                myTrips.postValue(tripsMap)
            }
    }

    private val othersTrips: MutableLiveData<HashMap<String, Trip>> by lazy {
        MutableLiveData<HashMap<String, Trip>>().also {
            loadOthersTrips()
        }
    }

    private fun loadOthersTrips() {
        val db = Firebase.firestore
        val currentUserRef =
            FirebaseFirestore.getInstance().document("users/${currentUser.value?.uid}" )

        db.collection("trips").whereNotEqualTo("owner", currentUserRef)
            .whereEqualTo("visibility", true)
            //.whereEqualTo("finished", false)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    othersTrips.postValue(HashMap())
                    Log.e("loadTrips() exception => ", e.toString())
                    return@addSnapshotListener
                }
                val tripsMap: HashMap<String, Trip> = HashMap()
                for (doc in value!!) {
                    tripsMap[doc.id] = doc.toObject(Trip::class.java)
                }
                othersTrips.postValue(tripsMap.filterValues { t -> !t.finished && t.timestamp>Timestamp.now() } as HashMap<String, Trip>?)
            }
    }

    private val currentUser: MutableLiveData<User> by lazy {
        MutableLiveData<User>().also {
            loadUser()
        }
    }


    private fun loadTrips() {
        // Do an asynchronous operation to fetch trips.
        val db = Firebase.firestore
        db.collection("trips").addSnapshotListener { value, e ->
            if (e != null) {
                trips.postValue(HashMap())
                Log.e("loadTrips() exception => ", e.toString())
                return@addSnapshotListener
            }
            val tripsMap: HashMap<String, Trip> = HashMap()
            for (doc in value!!) {
                tripsMap[doc.id] = doc.toObject(Trip::class.java)
            }
            trips.postValue(tripsMap)
        }
    }

    fun getTrips(): LiveData<HashMap<String, Trip>> {
        return trips
    }

    fun getMyTrips(): LiveData<HashMap<String, Trip>> {
        return myTrips
    }
    fun getBoughtTrips(): LiveData<HashMap<String, Trip>> {
        return boughtTrips
    }

    fun getInterestedTrips(): LiveData<HashMap<String, Trip>> {
        return interestTrips
    }

    fun getOthersTrips(): LiveData<HashMap<String, Trip>> {
        return othersTrips
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
                if (doc.id == Firebase.auth.currentUser?.uid)
                    user = doc.toObject(User::class.java)
            }
            currentUser.postValue(user)
        }

    }

    fun getCurrentUser(): LiveData<User> {
        return currentUser
    }


}

