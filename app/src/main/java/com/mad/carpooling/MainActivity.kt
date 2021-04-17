package com.mad.carpooling

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

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

        initDrawerHeader(navView)
    }

    private fun initDrawerHeader(navView: NavigationView) {
        val headerView: View = navView.getHeaderView(0)
        val ivProfileHeader: ImageView = headerView.findViewById(R.id.nav_header_profile_pic)
        val tvFullNameHeader: TextView = headerView.findViewById(R.id.nav_header_fullName)
        val tvNicknameHeader: TextView = headerView.findViewById(R.id.nav_header_nickname)
        val sharedPref = getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE) ?: return
        val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            tvFullNameHeader.text = jsonObject.getString("json_fullName.group05.lab1")
            tvNicknameHeader.text = jsonObject.getString("json_nickname.group05.lab1")
            ivProfileHeader.setImageBitmap(
                BitmapFactory.decodeStream(
                    openFileInput(
                        jsonObject.getString(
                            "json_profilePic.group05.lab1"
                        )
                    )
                )
            )
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