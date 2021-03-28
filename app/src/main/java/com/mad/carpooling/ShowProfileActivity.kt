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
import org.json.JSONObject





class ShowProfileActivity : AppCompatActivity() {
    private lateinit var fullNameTextView: TextView
    private lateinit var nicknameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var profilePic: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_profile)

        fullNameTextView = findViewById<TextView>(R.id.text_fullName)
        nicknameTextView = findViewById<TextView>(R.id.text_nickname)
        emailTextView = findViewById<TextView>(R.id.text_email)
        locationTextView = findViewById<TextView>(R.id.text_location)
        profilePic = findViewById<ImageView>(R.id.profile_pic)

        initProfile()
    }

    private fun initProfile() {
        val sharedPref = this.getSharedPreferences("pref",Context.MODE_PRIVATE) ?: return
        val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
        if(jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            fullNameTextView.text = jsonObject.getString("json_fullName")
            nicknameTextView.text = jsonObject.getString("json_nickname")
            emailTextView.text = jsonObject.getString("json_email")
            locationTextView.text = jsonObject.getString("json_location")
            profilePic.setImageBitmap(BitmapFactory.decodeStream(openFileInput(jsonObject.getString("json_profilePic"))))
        }
    }

    fun editProfile() {
        val intentEditProfileActivity = Intent(this, EditProfileActivity::class.java)
            .also {
                it.putExtra("fullName", fullNameTextView.text.toString())
                it.putExtra("nickname", nicknameTextView.text.toString())
                it.putExtra("email", emailTextView.text.toString())
                it.putExtra("location", locationTextView.text.toString())
                if (profilePic.drawable is VectorDrawable)
                    it.putExtra("profilePic", null as Bitmap?)
                else
                    it.putExtra("profilePic", (profilePic.drawable as BitmapDrawable).bitmap)

            }
        startActivityForResult(intentEditProfileActivity, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            fullNameTextView.text = (data?.getStringExtra("save_fullName"))
            nicknameTextView.text = (data?.getStringExtra("save_nickname"))
            emailTextView.text = (data?.getStringExtra("save_email"))
            locationTextView.text = (data?.getStringExtra("save_location"))
            val newPicture = data?.getParcelableExtra("save_profilePic") as Bitmap?
            if (newPicture != null) {
                profilePic.setImageBitmap(newPicture)
            }
        } else if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "RESULT_CANCELED", Toast.LENGTH_SHORT).show()
        }
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
        inflater.inflate(R.menu.profile_menu, menu)
        return true
    }
}