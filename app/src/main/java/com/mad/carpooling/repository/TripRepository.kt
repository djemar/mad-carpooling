package com.mad.carpooling.repository

import androidx.lifecycle.LiveData
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.model.Trip
import com.mad.carpooling.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TripRepository {

    @ExperimentalCoroutinesApi
    suspend fun loadOthersTrips(currentUser: LiveData<User?>): Flow<Result<HashMap<String, Trip>>> =
        callbackFlow {
            val db = Firebase.firestore
            val currentUserRef =
                FirebaseFirestore.getInstance().document("users/${currentUser.value?.uid}")

            val subscription = db.collection("trips").whereNotEqualTo("owner", currentUserRef)
                .whereEqualTo("visibility", true)
                //.whereEqualTo("finished", false)
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        val tripsMap: HashMap<String, Trip> = HashMap()
                        for (doc in snapshot!!) {
                            tripsMap[doc.id] = doc.toObject(Trip::class.java)
                        }
                        val filteredMap: HashMap<String, Trip> =
                            tripsMap.filterValues { t -> !t.finished && t.timestamp > Timestamp.now() } as HashMap<String, Trip>
                        this.trySend(Result.success(filteredMap)).isSuccess
                    } else {
                        cancel(
                            message = "error fetching collection othersTrip data",
                            cause = e
                        )
                    }
                }
            awaitClose { subscription.remove() }
        }

    @ExperimentalCoroutinesApi
    suspend fun loadMyTrips(currentUser: LiveData<User?>): Flow<Result<HashMap<String, Trip>>> =
        callbackFlow {
            val db = Firebase.firestore
            val currentUserRef =
                FirebaseFirestore.getInstance().document("users/${currentUser.value?.uid}")

            val subscription = db.collection("trips").whereEqualTo("owner", currentUserRef)
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        val tripsMap: HashMap<String, Trip> = HashMap()
                        for (doc in snapshot!!) {
                            tripsMap[doc.id] = doc.toObject(Trip::class.java)
                        }
                        this.trySend(Result.success(tripsMap)).isSuccess
                    } else {
                        cancel(
                            message = "error fetching collection myTrips data",
                            cause = e
                        )
                    }
                }
            awaitClose { subscription.remove() }
        }

    @ExperimentalCoroutinesApi
    suspend fun loadInterestedTrips(): Flow<Result<HashMap<String, Trip>>> =
        callbackFlow {
            val db = Firebase.firestore

            val subscription = db.collection("trips")
                .whereArrayContains("interestedPeople", Firebase.auth.uid.toString())
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        val tripsMap: HashMap<String, Trip> = HashMap()
                        for (doc in snapshot!!) {
                            tripsMap[doc.id] = doc.toObject(Trip::class.java)
                        }
                        this.trySend(Result.success(tripsMap)).isSuccess
                    } else {
                        cancel(
                            message = "error fetching collection interestedTrips data",
                            cause = e
                        )
                    }
                }
            awaitClose { subscription.remove() }
        }

    @ExperimentalCoroutinesApi
    suspend fun loadBoughtTrips(): Flow<Result<HashMap<String, Trip>>> =
        callbackFlow {
            val db = Firebase.firestore

            val subscription = db.collection("trips")
                .whereArrayContains("acceptedPeople", Firebase.auth.uid.toString())
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        val tripsMap: HashMap<String, Trip> = HashMap()
                        for (doc in snapshot!!) {
                            tripsMap[doc.id] = doc.toObject(Trip::class.java)
                        }
                        this.trySend(Result.success(tripsMap)).isSuccess
                    } else {
                        cancel(
                            message = "error fetching collection boughtTrips data",
                            cause = e
                        )
                    }
                }
            awaitClose { subscription.remove() }
        }

    @ExperimentalCoroutinesApi
    suspend fun loadTrips(): Flow<Result<HashMap<String, Trip>>> =
        callbackFlow {
            val db = Firebase.firestore

            val subscription = db.collection("trips")
                //.whereEqualTo("finished", false)
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        val tripsMap: HashMap<String, Trip> = HashMap()
                        for (doc in snapshot!!) {
                            tripsMap[doc.id] = doc.toObject(Trip::class.java)
                        }
                        this.trySend(Result.success(tripsMap)).isSuccess
                    } else {
                        cancel(
                            message = "error fetching collection myTrips data",
                            cause = e
                        )
                    }
                }
            awaitClose { subscription.remove() }
        }

    suspend fun getTrip(childName: String): Result<Trip?> = withContext(Dispatchers.IO) {
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

    suspend fun arrayUnionTrip(childName: String, field: String, user: String): Boolean {
        return try {
            Firebase.firestore
                .collection("trips")
                .document(childName)
                .update(field, FieldValue.arrayUnion(user))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun arrayRemoveTrip(childName: String, field: String, user: String): Boolean {
        return try {
            Firebase.firestore
                .collection("trips")
                .document(childName)
                .update(field, FieldValue.arrayRemove(user))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun arrayAddTrip(childName: String, field: String, user: String): Boolean{
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

    suspend fun incrementTrip(childName: String, field: String, value: Long): Boolean {
        return try {
            Firebase.firestore
                .collection("trips")
                .document(childName)
                .update(field, FieldValue.increment(value))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun terminateTrip(childName: String) : Boolean{
        return try {
            Firebase.firestore
                .collection("trips")
                .document(childName)
                .update("finished", true)
                .await()
            true
        }catch(e: Exception){
            false
        }
    }

    /*    @ExperimentalCoroutinesApi
    fun loadOthersTrips(currentUser: LiveData<User?>): Flow<HashMap<String, Trip>> {
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
}
