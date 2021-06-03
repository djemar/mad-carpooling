package com.mad.carpooling.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.model.Rating
import com.mad.carpooling.model.Trip
import com.mad.carpooling.model.User
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SharedViewModel(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository
) : ViewModel() {
/*
    /** Alternative implementation **/
    private val othersTrips: MutableLiveData<HashMap<String, Trip>> by lazy {
        MutableLiveData<HashMap<String, Trip>>().also {
            loadOthersTrips()
        }
    }

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    private val othersTrips: MutableLiveData<HashMap<String, Trip>> by lazy {
        MutableLiveData<HashMap<String, Trip>>().also {
            loadOthersTrips()
        }
    }

    @InternalCoroutinesApi
    @ExperimentalCoroutinesApi
    private fun loadOthersTrips() {
        if(currentUser.value != null){
        viewModelScope.launch {
            tripRepository.loadOthersTrips(currentUser).collect {
                othersTrips.postValue(it)
            }
        }
        }

    }*/

    @ExperimentalCoroutinesApi
    private val othersTrips = liveData<HashMap<String, Trip>>(Dispatchers.IO) {
        //emit(Result.Loading())
        try {
            tripRepository.loadOthersTrips(currentUser).collect {
                emit(it.getOrDefault(HashMap()))
            }
        } catch (e: Exception) {
            emit(HashMap())
            Log.e("ERROR:", e.message!!)
        }
    }

    @ExperimentalCoroutinesApi
    private val myTrips = liveData<HashMap<String, Trip>>(Dispatchers.IO) {
        //emit(Result.Loading())
        try {
            tripRepository.loadMyTrips(currentUser).collect {
                emit(it.getOrDefault(HashMap()))
            }
        } catch (e: Exception) {
            emit(HashMap())
            Log.e("ERROR:", e.message!!)
        }
    }

    @ExperimentalCoroutinesApi
    private val interestTrips = liveData<HashMap<String, Trip>>(Dispatchers.IO) {
        //emit(Result.Loading())
        try {
            tripRepository.loadInterestedTrips().collect {
                emit(it.getOrDefault(HashMap()))
            }
        } catch (e: Exception) {
            emit(HashMap())
            Log.e("ERROR:", e.message!!)
        }
    }

    @ExperimentalCoroutinesApi
    private val boughtTrips = liveData<HashMap<String, Trip>>(Dispatchers.IO) {
        //emit(Result.Loading())
        try {
            tripRepository.loadBoughtTrips().collect {
                emit(it.getOrDefault(HashMap()))
            }
        } catch (e: Exception) {
            emit(HashMap())
            Log.e("ERROR:", e.message!!)
        }
    }

    @ExperimentalCoroutinesApi
    private val trips = liveData<HashMap<String, Trip>>(Dispatchers.IO) {
        //emit(Result.Loading())
        try {
            tripRepository.loadTrips().collect {
                emit(it.getOrDefault(HashMap()))
                Log.d("TRIPS: ", it.getOrNull().toString())
            }
        } catch (e: Exception) {
            emit(HashMap())
            Log.e("ERROR:", e.message!!)
        }
    }

    @ExperimentalCoroutinesApi
    private val currentUser = liveData<User?>(Dispatchers.IO) {
        //emit(Result.Loading())
        try {
            userRepository.loadUser().collect {
                emit(it.getOrNull())
            }
        } catch (e: Exception) {
            emit(null)
            Log.e("ERROR:", e.message!!)
        }
    }


    @ExperimentalCoroutinesApi
    fun getCurrentUserData(): LiveData<User?> {
        return currentUser
    }


    fun getUserDoc(userId: String): LiveData<User?> {
        val result = MutableLiveData<User?>()
        viewModelScope.launch {
            val user = userRepository.getUserDoc(userId)
            if (user.isSuccess) {
                result.postValue(user.getOrNull())
            } else {
                result.postValue(null)
            }
        }
        return result
    }

    fun getUserRef(currentUserUid: String) : LiveData<DocumentReference> {
        val res = MutableLiveData<DocumentReference>()
        viewModelScope.launch{
            val result = userRepository.getUserRef(currentUserUid)
            res.postValue(result.getOrNull())
        }
        return res
    }
    
    fun getRatings(user: String, field: String): LiveData<Map<String, ArrayList<Any>>?>{
        val result = MutableLiveData<Map<String, ArrayList<Any>>?>()
        viewModelScope.launch {
            val map = userRepository.getRatings(user, field)
            if (map.isSuccess) {
                result.postValue(map.getOrNull())
            } else {
                result.postValue(null)
            }
        }
        return result
    }

    fun addInterest(trip: String, fieldTrip: String, fieldUser: String, user: String): LiveData<Boolean>
    {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            if (tripRepository.arrayUnionTrip(trip, fieldTrip, user))
                launch {
                    val res = userRepository.arrayUnionUser(user, fieldUser, trip)
                    result.postValue(res)
                }
        }
        return result
    }

    fun removeInterest(
        trip: String,
        fieldTrip: String,
        fieldUser: String,
        user: String
    ): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            if (tripRepository.arrayRemoveTrip(trip, fieldTrip, user))
                launch {
                    val res = userRepository.arrayRemoveUser(user, fieldUser, trip)
                    result.postValue(res)
                }
        }
        return result
    }

    fun removeAccepted(
        trip: String,
        fieldTrip: String,
        incrementField: String,
        user: String,
        value: Long
    ): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            val res = tripRepository.getFieldTrip(trip, fieldTrip)
            if (res.isSuccess) {
                val tmpArray = res.getOrNull()
                if (tmpArray != null) {
                    if (tmpArray.contains(user)) {
                        if (tripRepository.arrayRemoveTrip(trip, fieldTrip, user))
                            launch {
                                val res = tripRepository.incrementTrip(trip, incrementField, value)
                                result.postValue(res)
                            }
                    }
                }
            } else {
                result.postValue(false)
            }

        }
        return result
    }

    fun addAccepted(trip: String, fieldTrip: String, incrementField: String, user: String, value: Long): LiveData<Boolean>
    {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch{
            val res = tripRepository.getFieldTrip(trip, fieldTrip)
            if (res.isSuccess) {
                val tmpArray = res.getOrNull()
                if (tmpArray != null) {
                    if (tmpArray.contains(user)) {
                        if (tripRepository.arrayAddTrip(trip, "acceptedPeople", user))
                            launch {
                                val res = tripRepository.incrementTrip(trip, incrementField, value)
                                result.postValue(res)
                            }
                    }
                }
            } else {
                result.postValue(false)
            }

    fun getUserDoc(userId: String): LiveData<User?> {
        val result = MutableLiveData<User?>()
        viewModelScope.launch {
            val user = userRepository.getUserDoc(userId)
            if (user.isSuccess) {
                result.postValue(user.getOrNull())
            } else {
                result.postValue(null)
            }
        }
        return result
    }

    fun terminateTrip(trip: String): LiveData<Boolean>
    {
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch{
            val result = tripRepository.terminateTrip(trip)
    fun signUpUser(newUser: User, newRating: Rating) : LiveData<Boolean>
    {
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch {
            val result = userRepository.signUpUser(newUser.uid, newUser, newRating)
            res.postValue(result)
        }
        return res
    }

    fun loginUser(uid: String, updates: HashMap<String,Any>) : LiveData<Boolean>
    {
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch {
            val result = userRepository.loginUser(uid, updates)
            res.postValue(result)
        }
        return res
    }

    fun updateRatings(uid: String, role: String, currentUser: String, newArray: ArrayList<Any>): LiveData<Boolean>{
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch{
            val result = userRepository.updateRatings(uid, role, currentUser, newArray)
            res.postValue(result)
        }
        return res
    fun checkExistingUser(uid: String) : LiveData<Boolean>
    {
        val res = MutableLiveData<Boolean>()
        viewModelScope.launch {
            val result = userRepository.checkExistingUser(uid)
            res.postValue(result)
        }
        return res
    }

    /** Getters **/

    @ExperimentalCoroutinesApi
    fun getTrips(): LiveData<HashMap<String, Trip>> {
        return trips
    }

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    fun getOthersTrips(): LiveData<HashMap<String, Trip>> {
        return othersTrips
    }

    @ExperimentalCoroutinesApi
    fun getMyTrips(): LiveData<HashMap<String, Trip>> {
        return myTrips
    }

    @ExperimentalCoroutinesApi
    fun getInterestedTrips(): LiveData<HashMap<String, Trip>> {
        return interestTrips
    }

    @ExperimentalCoroutinesApi
    fun getBoughtTrips(): LiveData<HashMap<String, Trip>> {
        return boughtTrips
    }

    @ExperimentalCoroutinesApi
    fun getCurrentUser(): LiveData<User?> {
        return currentUser
    }

}

