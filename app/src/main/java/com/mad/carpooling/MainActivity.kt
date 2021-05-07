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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.data.User
import com.mad.carpooling.ui.SharedViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User
    private var userState: FirebaseUser? = null
    private val RC_SIGN_IN: Int = 1
    private val model: SharedViewModel by viewModels()

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
                R.id.nav_show_profile, R.id.nav_trip_list
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        auth = Firebase.auth
        auth.addAuthStateListener { authState ->
            userState = authState.currentUser
            if (userState == null) {
                Log.d("AuthListener", "null user")
            } else {

                initDrawerHeader(navView)
            }
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

    private fun initDrawerHeader(navView: NavigationView) {
        val headerView: View = navView.getHeaderView(0)
        val ivProfileHeader: ImageView = headerView.findViewById(R.id.nav_header_profile_pic)
        val tvFullNameHeader: TextView = headerView.findViewById(R.id.nav_header_fullName)
        val tvNicknameHeader: TextView = headerView.findViewById(R.id.nav_header_nickname)

        if (auth.currentUser != null)
            model.getCurrentUser().observe(this, { currentUser ->
                // Update the UI
                tvFullNameHeader.text = currentUser.fullname
                tvNicknameHeader.text = currentUser.nickname
                Glide.with(this).load(currentUser.imageUserRef).into(ivProfileHeader)
            })
        //TODO logout button
        ivProfileHeader.setOnLongClickListener {
            logout()
            true
        }


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
                    val db = Firebase.firestore
                    //TODO check if user exists
                    db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
                        if (document != null) {
                            Log.d("LOGIN", "User login")
                            // timestamp of latest login -> it triggers the observer and loads the user data
                            val updates = hashMapOf<String, Any>(
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                            db.collection("users").document(user.uid).update(updates)
                                .addOnCompleteListener {
                                    Snackbar.make(
                                        findViewById(R.id.triplist_rv),
                                        "Login successful",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Log.d("LOGIN", "New user signed up")
                            val newUser = User(
                                uid = user.uid,
                                fullname = if (user.displayName != null) user.displayName else "Fullname",
                                email = if (user.email != null) user.email else "email@address.com",
                                imageUserRef = if (user.photoUrl != null) user.photoUrl!!.toString() else null
                            )
                            db.collection("users").document(user.uid).set(newUser, SetOptions.merge())
                                .addOnSuccessListener {
                                    Snackbar.make(
                                        findViewById(R.id.triplist_rv),
                                        "Login successful",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                        }
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
