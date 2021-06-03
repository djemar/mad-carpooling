package com.mad.carpooling

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.model.Rating
import com.mad.carpooling.model.User
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository
import com.mad.carpooling.viewmodel.SharedViewModel
import com.mad.carpooling.viewmodel.SharedViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User
    private lateinit var appBarLayout: AppBarLayout
    private var userState: FirebaseUser? = null
    private val RC_SIGN_IN: Int = 1
    private val model: SharedViewModel by viewModels {
        SharedViewModelFactory(
            TripRepository(),
            UserRepository()
        )
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_show_profile,
                R.id.nav_trip_list,
                R.id.nav_others_trip_list,
                R.id.nav_bought_trips,
                R.id.nav_interest_trips
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        appBarLayout = findViewById<AppBarLayout>(R.id.appbar_layout)
        if (appBarLayout.layoutParams != null) {    // disable drag on toolbar
            val layoutParams = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            val appBarLayoutBehaviour = AppBarLayout.Behavior()
            appBarLayoutBehaviour.setDragCallback(object : DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                    return false
                }
            })
            layoutParams.behavior = appBarLayoutBehaviour
        }
        auth = Firebase.auth
        auth.addAuthStateListener { authState ->
            userState = authState.currentUser
            if (userState == null) {
                Log.d("AuthListener", "null user")
            } else {
                model.getTrips().observe(this, Observer {
                    model.getTrips().removeObservers(this)
                }
                )
                initDrawerHeader(navView)
            }
        }

        var prof = navView.menu.findItem(R.id.nav_show_profile)
        var myTrips = navView.menu.findItem(R.id.nav_trip_list)
        var intTrips = navView.menu.findItem(R.id.nav_interest_trips)
        var boughtTrips = navView.menu.findItem(R.id.nav_bought_trips)
        var log_item = navView.menu.findItem(R.id.nav_log)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            log_item.title = "Logout"
            log_item.setOnMenuItemClickListener {
                logout()
                true
            }

            prof.isVisible = true
            myTrips.isVisible = true
            intTrips.isVisible = true
            boughtTrips.isVisible = true

        } else {
            log_item.title = "Login"
            log_item.setOnMenuItemClickListener {
                login()
                true
            }

            prof.isVisible = false
            myTrips.isVisible = false
            intTrips.isVisible = false
            boughtTrips.isVisible = false
        }
    }

    private fun login() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    private fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnSuccessListener {
                Snackbar.make(
                    findViewById(R.id.triplist_rv),
                    "Logout successful",
                    Snackbar.LENGTH_SHORT
                ).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            login()
        }
    }

    @ExperimentalCoroutinesApi
    private fun initDrawerHeader(navView: NavigationView) {
        val headerView: View = navView.getHeaderView(0)
        val ivProfileHeader: ImageView = headerView.findViewById(R.id.nav_header_profile_pic)
        val tvFullNameHeader: TextView = headerView.findViewById(R.id.nav_header_fullName)
        val tvNicknameHeader: TextView = headerView.findViewById(R.id.nav_header_nickname)

        if (auth.currentUser != null)
            model.getCurrentUserData().observe(this, { currentUser ->
                // Update the UI
                if (currentUser != null) {
                    tvFullNameHeader.text = currentUser.fullname
                    tvNicknameHeader.text = currentUser.nickname
                    Glide.with(this).load(currentUser.imageUserRef).into(ivProfileHeader)
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = auth.currentUser
                user.reload()
                user.getIdToken(true)
                if (user != null) {
                    model.checkExistingUser(user.uid).observe(this, Observer { userExists ->
                        if (userExists) {
                            Log.d("LOGIN", "User login")
                            // timestamp of latest login -> this triggers the observer and loads the user data
                            val updates = hashMapOf<String, Any>(
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                            model.loginUser(user.uid, updates)
                                .observe(this, Observer { isSuccess ->
                                    if (isSuccess) {
                                        Snackbar.make(
                                            findViewById(R.id.triplist_rv),
                                            "Login successful",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        overridePendingTransition(0, 0)
                                        finish()
                                        overridePendingTransition(0, 0)
                                    }
                                })
                        } else {
                            Log.d("SIGNUP", "New user signed up")
                            val newUser = User(
                                uid = user.uid,
                                fullname = if (user.displayName != null) user.displayName else "Fullname",
                                email = if (user.email != null) user.email else "email@address.com",
                                imageUserRef = if (user.photoUrl != null) user.photoUrl!!.toString() else null
                            )
                            val newRating = Rating()

                            model.signUpUser(newUser, newRating)
                                .observe(this, Observer { isSuccess ->
                                    if (isSuccess) {
                                        Snackbar.make(
                                            findViewById(R.id.triplist_rv),
                                            "Login successful",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        overridePendingTransition(0, 0)
                                        finish()
                                        overridePendingTransition(0, 0)
                                    }
                                })
                        }

                    })
                }

            }

            Log.d("Login result", "Sign in success")
            // ...
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            Log.e("Login result", "Sign in failed")
        }

    }

/*    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }*/

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
