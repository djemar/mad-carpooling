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

        tvFullName = findViewById<TextView>(R.id.tv_fullName)
        tvNickname = findViewById<TextView>(R.id.tv_nickname)
        tvEmail = findViewById<TextView>(R.id.tv_email)
        tvLocation = findViewById<TextView>(R.id.tv_location)
        ivProfilePic = findViewById<ImageView>(R.id.iv_profile_pic)

        initProfile()
    }

    private fun initProfile() {
        val sharedPref = this.getSharedPreferences("profile_pref", Context.MODE_PRIVATE) ?: return
        val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            tvFullName.text = jsonObject.getString("json_fullName")
            tvNickname.text = jsonObject.getString("json_nickname")
            tvEmail.text = jsonObject.getString("json_email")
            tvLocation.text = jsonObject.getString("json_location")
            ivProfilePic.setImageBitmap(
                BitmapFactory.decodeStream(
                    openFileInput(
                        jsonObject.getString(
                            "json_profilePic"
                        )
                    )
                )
            )
        }
    }

    fun editProfile() {
        val intentEditProfileActivity = Intent(this, EditProfileActivity::class.java)
            .also {
                it.putExtra("fullName", tvFullName.text.toString())
                it.putExtra("nickname", tvNickname.text.toString())
                it.putExtra("email", tvEmail.text.toString())
                it.putExtra("location", tvLocation.text.toString())
                //it.putExtra("profilePic", (ivProfilePic.drawable as BitmapDrawable).bitmap)
            }
        startActivityForResult(intentEditProfileActivity, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            tvFullName.text = (data?.getStringExtra("save_fullName"))
            tvNickname.text = (data?.getStringExtra("save_nickname"))
            tvEmail.text = (data?.getStringExtra("save_email"))
            tvLocation.text = (data?.getStringExtra("save_location"))
            val newPhoto = data?.getStringExtra("save_profilePic")
            if (newPhoto != null) {
                BitmapFactory.decodeFile(newPhoto)?.also { bitmap ->
                    ivProfilePic.setImageBitmap(bitmap)
                }
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