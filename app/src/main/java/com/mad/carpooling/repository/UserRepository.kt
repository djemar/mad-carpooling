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

class UserRepository {

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

    suspend fun setUserDoc(childName: String, newUser: User) : Boolean
    {
        return try{
            Firebase.firestore
                .collection("users")
                .document(childName)
                .set(newUser)
                .await()
            true
        }catch (e: Exception){
            false
        }
    }

    suspend fun arrayUnionUser(childName: String, field: String, trip: String): Boolean{
        return try{
            Firebase.firestore
                .collection("users")
                .document(childName)
                .update(field, FieldValue.arrayUnion(trip))
                .await()
            true
        } catch (e: Exception){
            false
        }
    }

    suspend fun arrayRemoveUser(childName: String, field: String, trip: String): Boolean{
        return try{
            Firebase.firestore
                .collection("users")
                .document(childName)
                .update(field, FieldValue.arrayRemove(trip))
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
}