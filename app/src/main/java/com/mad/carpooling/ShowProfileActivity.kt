package com.mad.carpooling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONObject


class ShowProfileActivity : AppCompatActivity() {
    private lateinit var tvFullName: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvLocation: TextView
    private lateinit var ivProfilePic: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_profile)

        setSupportActionBar(findViewById(R.id.my_toolbar))

        tvFullName = findViewById<TextView>(R.id.tv_fullName)
        tvNickname = findViewById<TextView>(R.id.tv_nickname)
        tvEmail = findViewById<TextView>(R.id.tv_email)
        tvLocation = findViewById<TextView>(R.id.tv_location)
        ivProfilePic = findViewById<ImageView>(R.id.iv_profile_pic)

        initProfile()
    }

    private fun initProfile() {
        val sharedPref = this.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE) ?: return
        val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            tvFullName.text = jsonObject.getString("json_fullName.group05.lab1")
            tvNickname.text = jsonObject.getString("json_nickname.group05.lab1")
            tvEmail.text = jsonObject.getString("json_email.group05.lab1")
            tvLocation.text = jsonObject.getString("json_location.group05.lab1")
            ivProfilePic.setImageBitmap(
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

    fun editProfile() {
        val intentEditProfileActivity = Intent(this, EditProfileActivity::class.java)
            .also {
                it.putExtra("fullName.group05.lab1", tvFullName.text.toString())
                it.putExtra("nickname.group05.lab1", tvNickname.text.toString())
                it.putExtra("email.group05.lab1", tvEmail.text.toString())
                it.putExtra("location.group05.lab1", tvLocation.text.toString())
                //it.putExtra("profilePic", (ivProfilePic.drawable as BitmapDrawable).bitmap)
            }
        startActivityForResult(intentEditProfileActivity, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val sharedPref =
                this.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE) ?: return
            val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
            if (jsonString != null) {
                val jsonObject = JSONObject(jsonString)
                tvFullName.text = (data?.getStringExtra("save_fullName.group05.lab1"))
                tvNickname.text = (data?.getStringExtra("save_nickname.group05.lab1"))
                tvEmail.text = (data?.getStringExtra("save_email.group05.lab1"))
                tvLocation.text = (data?.getStringExtra("save_location.group05.lab1"))
                ivProfilePic.setImageBitmap(
                    BitmapFactory.decodeStream(
                        openFileInput(
                            jsonObject.getString(
                                "json_profilePic.group05.lab1"
                            )
                        )
                    )
                )
            }

        } /*else if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) { //debug
            Toast.makeText(this, "RESULT_CANCELED", Toast.LENGTH_SHORT).show()
        }*/
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit -> {
                editProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_profile, menu)
        return true
    }
}