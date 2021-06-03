package com.mad.carpooling.repository

import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.model.Rating
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
    suspend fun loadUser(): Flow<Result<User?>> = callbackFlow {
        // 2.- We create a reference to our data inside Firestore
        val eventDocument =  Firebase.firestore
            .collection("users")

        // 3.- We generate a subscription that is going to let us listen for changes with
        // .addSnapshotListener and then offer those values to the channel that will be collected in our viewmodel
        val subscription = eventDocument.addSnapshotListener { snapshot, e ->
            if(e == null){
                var user: User = User()
                for (doc in snapshot!!) {
                    if (doc.id == Firebase.auth.currentUser?.uid)
                        user = doc.toObject(User::class.java)
                }
                this.trySend(Result.success(user)).isSuccess
            } else{
                cancel(
                    message = "error fetching collection myTrips data",
                    cause = e
                )
            }
        }

        //Finally if collect is not in use or collecting any data we cancel this channel to prevent any leak and remove the subscription listener to the database
        awaitClose { subscription.remove() }

    }

    suspend fun getUserDoc(childName: String)
            : Result<User?> {
        return try {
            val data = Firebase.firestore
                .collection("users")
                .document(childName)
                .get()
                .await()
            Result.success(data.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRef(currentUserUid: String) : Result<DocumentReference?> = withContext(Dispatchers.IO) {
        try {
            val data = FirebaseFirestore.getInstance()
                .document("users/${currentUserUid}")
            return@withContext Result.success(data)
        }catch(e: Exception) {
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

    suspend fun getRatings(childName: String, field: String): Result<Map<String, ArrayList<Any>>> = withContext(Dispatchers.IO){
        try{
            val data = Firebase.firestore
                .collection("ratings")
                .document(childName)
                .get()
                .await()
                .get(field)
            return@withContext Result.success(data) as Result<Map<String, ArrayList<Any>>>
        } catch (e: Exception){
            return@withContext Result.failure(e)
        }
    }

    suspend fun updateRatings(childName: String, role: String, currentUser: String, newArray: ArrayList<Any>): Boolean{
        return try{
            Firebase.firestore
                .collection("ratings")
                .document(childName)
                .update("${role}.${currentUser}", newArray)
                .await()
            true
        }catch(e: Exception){
            return false
    suspend fun signUpUser(uid: String, newUser: User, newRating: Rating): Boolean {
        return try{
            Firebase.firestore
                .collection("users")
                .document(uid)
                .set(newUser, SetOptions.merge())
                .await()
            Firebase.firestore
                .collection("ratings")
                .document(uid)
                .set(newRating, SetOptions.merge())
                .await()
            true
        }catch (e: Exception){
            false
        }
    }

    suspend fun loginUser(uid: String, updates: HashMap<String,Any>): Boolean {
        return try{
            Firebase.firestore
                .collection("users")
                .document(uid)
                .update(updates)
                .await()
            true
        }catch (e: Exception){
            false
        }
    }

    suspend fun checkExistingUser(uid: String): Boolean {
        return try{
            val doc = Firebase.firestore
                .collection("users")
                .document(uid)
                .get()
                .await()
            doc.data != null
        }catch (e: Exception){
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
