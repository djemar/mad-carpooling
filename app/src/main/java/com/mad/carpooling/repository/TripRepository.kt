package com.mad.carpooling.repository

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.model.Trip
import com.mad.carpooling.model.User
import com.mad.carpooling.ui.trip_edit.TripEditFragment
import com.mad.carpooling.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TripRepository {

/*    @ExperimentalCoroutinesApi
    suspend fun loadOthersTrips(currentUser: LiveData<User>): Flow<HashMap<String, Trip>> =
        callbackFlow {
            val db = Firebase.firestore
            val currentUserRef =
                FirebaseFirestore.getInstance().document("users/${currentUser.value?.uid}")

            val subscription = db.collection("trips").whereNotEqualTo("owner", currentUserRef)
                .whereEqualTo("visibility", true)
                //.whereEqualTo("finished", false)
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        offer(HashMap())
                        Log.e("loadTrips() exception => ", e.toString())
                        return@addSnapshotListener
                    }
                    val tripsMap: HashMap<String, Trip> = HashMap()
                    for (doc in value!!) {
                        tripsMap[doc.id] = doc.toObject(Trip::class.java)
                    }
                    offer(tripsMap.filterValues { t -> !t.finished && t.timestamp > Timestamp.now() } as HashMap<String, Trip>)
                }
            awaitClose { subscription.remove() }
        }*/

    @ExperimentalCoroutinesApi
    fun loadOthersTrips(currentUser: LiveData<User>): Flow<HashMap<String, Trip>> {
        val db = Firebase.firestore
        val currentUserRef =
            FirebaseFirestore.getInstance().document("users/${currentUser.value?.uid}")
        val tripsMap = HashMap<String, Trip>()

        return db.collection("trips").whereNotEqualTo("owner", currentUserRef)
            .whereEqualTo("visibility", true)
            .getDataFlow { querySnapshot ->
                for (doc in querySnapshot!!) {
                    tripsMap[doc.id] = doc.toObject(Trip::class.java)
                }
                return@getDataFlow tripsMap
            }
    }

    suspend fun getUserDoc(childName: String)
            : Result<User?> = withContext(Dispatchers.IO) {
        try {
            val data = Firebase.firestore
                .collection("users")
                .document(childName)
                .get()
                .await()
            return@withContext Result.success(data.toObject(User::class.java))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun getTrip(childName: String) : Result<Trip?> = withContext(Dispatchers.IO){
        try {
            val data = Firebase.firestore
                .collection("trips")
                .document(childName)
                .get()
                .await()
            return@withContext Result.success(data.toObject(Trip::class.java))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun arrayUnionTrip(childName: String, field: String, user: String): Boolean{
        return try{
            Firebase.firestore
                .collection("trips")
                .document(childName)
                .update(field, FieldValue.arrayUnion(user))
                .await()
            true
        } catch (e: Exception){
            false
        }
    }

    suspend fun arrayRemoveTrip(childName: String, field: String, user: String): Boolean{
        return try{
            Firebase.firestore
                .collection("trips")
                .document(childName)
                .update(field, FieldValue.arrayRemove(user))
                .await()
            true
        } catch (e: Exception){
            false
        }
    }

    suspend fun getFieldTrip(childName: String, field: String): Result<ArrayList<String>> = withContext(Dispatchers.IO){
        try{
            val data = Firebase.firestore
                .collection("trips")
                .document(childName)
                .get()
                .await()
                .get(field)
            return@withContext Result.success(data) as Result<ArrayList<String>>
        } catch (e: Exception){
            return@withContext Result.failure(e)
        }
    }

    suspend fun incrementTrip(childName: String, field: String, value: Long) : Boolean{
        return try{
            Firebase.firestore
                .collection("trips")
                .document(childName)
                .update(field, FieldValue.increment(value))
                .await()
            true
        } catch (e: Exception){
            false
        }
    }

    @ExperimentalCoroutinesApi
    fun Query.getQuerySnapshotFlow(): Flow<QuerySnapshot?> {
        return callbackFlow {
            val listenerRegistration =
                addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (firebaseFirestoreException != null) {
                        cancel(
                            message = "error fetching collection data",
                            cause = firebaseFirestoreException
                        )
                        return@addSnapshotListener
                    }
                    this.trySend(querySnapshot).isSuccess
                }
            awaitClose {
                Log.d("Trip repo", "cancelling the listener on collection")
                listenerRegistration.remove()
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun <T> Query.getDataFlow(mapper: (QuerySnapshot?) -> T): Flow<T> {
        return getQuerySnapshotFlow()
            .map {
                return@map mapper(it)
            }
    }

    suspend fun updateTrip(trip: Trip) : Boolean{
        return try {
            Firebase.firestore
                .collection("trips")
                .document(trip.id)
                .set(trip)
                .await()
            true
        }catch(e: Exception){
            false
        }
    }

    suspend fun removeFavTrip(user: String, tripId: String): Boolean{
        return try{
            Firebase.firestore
                .collection("users").document(user)
                .update("favTrips", FieldValue.arrayRemove(tripId))
                .await()
            true
        }catch (e: Exception){
            false
        }
    }

    fun getNewTripId() : String{
        return try {
            val data = Firebase.firestore
                .collection("trips")
                .document()
                .id
            data
        }catch(e: Exception) {
            e.toString()
        }
    }

    suspend fun createTrip(trip: Trip) : Boolean{
        return try {
            val data = Firebase.firestore
                .collection("trips")
                .document()
                .set(trip)
                .await()
            true
        }catch(e: Exception){
            false
        }
    }


}