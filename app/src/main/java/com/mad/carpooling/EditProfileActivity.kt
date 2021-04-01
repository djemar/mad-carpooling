package com.mad.carpooling

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONObject


class EditProfileActivity : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etNickname: EditText
    private lateinit var etEmail: EditText
    private lateinit var etLocation: EditText
    private lateinit var ivEditProfilePic: ImageView
    private var REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etFullName = findViewById<EditText>(R.id.et_fullName)
        etNickname = findViewById<EditText>(R.id.et_nickname)
        etEmail = findViewById<EditText>(R.id.et_email)
        etLocation = findViewById<EditText>(R.id.et_location)
        ivEditProfilePic = findViewById<ImageView>(R.id.et_profile_pic)

        initProfile()

        val btnCamera = findViewById<ImageButton>(R.id.btn_camera)
        registerForContextMenu(btnCamera)
        btnCamera.setOnClickListener { openContextMenu(btnCamera) }
    }

    private fun initProfile() {
        etFullName.setText(intent.getStringExtra("fullName"))
        etNickname.setText(intent.getStringExtra("nickname"))
        etEmail.setText(intent.getStringExtra("email"))
        etLocation.setText(intent.getStringExtra("location"))
        val image = intent.getParcelableExtra("profilePic") as Bitmap?
        if (image != null)
            ivEditProfilePic.setImageBitmap(image)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.ctx_menu_camera, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ctx_gallery -> {
                Toast.makeText(this, "Open Gallery", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.ctx_camera -> {
                dispatchTakePictureIntent()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Toast.makeText(this, "Error opening the camera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                setResult(Activity.RESULT_OK, Intent().also {
                    it.putExtra("save_fullName", etFullName.text.toString())
                    it.putExtra("save_nickname", etNickname.text.toString())
                    it.putExtra("save_email", etEmail.text.toString())
                    it.putExtra("save_location", etLocation.text.toString())
                    it.putExtra("save_profilePic", (ivEditProfilePic.drawable).toBitmap())
                })

                saveToSharedPref()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun saveToSharedPref() {
        val filename = "profile_pic_img"
        val fileContents = (ivEditProfilePic.drawable).toBitmap()
        val fileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE)
        fileContents?.compress(Bitmap.CompressFormat.PNG, 85, fileOutputStream)
        fileOutputStream.close()

        val jsonObj = JSONObject()
        jsonObj.put("json_fullName", etFullName.text.toString())
        jsonObj.put("json_nickname", etNickname.text.toString())
        jsonObj.put("json_email", etEmail.text.toString())
        jsonObj.put("json_location", etLocation.text.toString())
        jsonObj.put("json_profilePic", filename)

        val sharedPref = this.getSharedPreferences("profile_pref", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.saved_profile_data), jsonObj.toString())
            apply()
        }
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_edit_profile, menu)

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            ivEditProfilePic.setImageBitmap(imageBitmap)
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("state_fullName", etFullName.text.toString())
        outState.putString("state_nickname", etNickname.text.toString())
        outState.putString("state_email", etEmail.text.toString())
        outState.putString("state_location", etLocation.text.toString())
        outState.putParcelable("state_profilePic", (ivEditProfilePic.drawable).toBitmap())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        etFullName.setText(savedInstanceState.getString("state_fullName"))
        etNickname.setText(savedInstanceState.getString("state_nickname"))
        etEmail.setText(savedInstanceState.getString("state_email"))
        etLocation.setText(savedInstanceState.getString("state_location"))
        ivEditProfilePic.setImageBitmap(savedInstanceState.getParcelable("state_profilePic"))
    }

}