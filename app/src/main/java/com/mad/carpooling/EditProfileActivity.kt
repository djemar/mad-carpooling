package com.mad.carpooling

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*


class EditProfileActivity : AppCompatActivity() {
    private lateinit var fullNameEditText : EditText
    private lateinit var nicknameEditText : EditText
    private lateinit var emailEditText : EditText
    private lateinit var locationEditText : EditText
    private lateinit var profilePicEdit : ImageView
    private var REQUEST_IMAGE_CAPTURE = 1
    private var isPictureChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        fullNameEditText = findViewById<EditText>(R.id.edit_fullName)
        nicknameEditText = findViewById<EditText>(R.id.edit_nickname)
        emailEditText = findViewById<EditText>(R.id.edit_email)
        locationEditText = findViewById<EditText>(R.id.edit_location)

        initProfile()

        val cameraButton = findViewById<ImageButton>(R.id.camera_button)
        registerForContextMenu(cameraButton)
        cameraButton.setOnClickListener { openContextMenu(cameraButton)}
    }

    private fun initProfile() {
        fullNameEditText.setText(intent.getStringExtra("fullName"))
        nicknameEditText.setText(intent.getStringExtra("nickname"))
        emailEditText.setText(intent.getStringExtra("email"))
        locationEditText.setText(intent.getStringExtra("location"))
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View,
                                     menuInfo: ContextMenu.ContextMenuInfo ?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.camera_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.context_gallery -> {
                Toast.makeText(this, "Open Gallery", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.context_camera ->{
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
                setResult(Activity.RESULT_OK, Intent().also{
                    it.putExtra("save_fullName", fullNameEditText.text.toString())
                    it.putExtra("save_nickname", nicknameEditText.text.toString())
                    it.putExtra("save_email", emailEditText.text.toString())
                    it.putExtra("save_location", locationEditText.text.toString())
                    it.putExtra("save_profilePic", isPictureChanged) //TODO
                })
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.edit_profile_menu, menu)

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            profilePicEdit.setImageBitmap(imageBitmap)
            isPictureChanged = true
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("state_fullName", fullNameEditText.text.toString())
        outState.putString("state_nickname", nicknameEditText.text.toString())
        outState.putString("state_email", emailEditText.text.toString())
        outState.putString("state_location", locationEditText.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        fullNameEditText.setText(savedInstanceState.getString("state_fullName"))
        nicknameEditText.setText(savedInstanceState.getString("state_nickname"))
        emailEditText.setText(savedInstanceState.getString("state_email"))
        locationEditText.setText(savedInstanceState.getString("state_location"))
    }

}