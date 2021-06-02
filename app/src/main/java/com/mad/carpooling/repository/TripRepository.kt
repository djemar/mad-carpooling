package com.mad.carpooling.repository

import android.util.Log
import androidx.lifecycle.LiveData
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
    fun loadOthersTrips(currentUser: LiveData<User>):Flow<HashMap<String, Trip>> {
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

    suspend fun getDataFromFireStore(childName : String)
            : Result<User?> = withContext(Dispatchers.IO){
        try{
            val data = Firebase.firestore
                .collection("users")
                .document(childName)
                .get()
                .await()
            return@withContext Result.success(data.toObject(User::class.java))
        }catch (e : Exception){
            return@withContext Result.failure(e)
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
                    offer(querySnapshot)
                }
            awaitClose {
                Log.d("Trip repo","cancelling the listener on collection")
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
}