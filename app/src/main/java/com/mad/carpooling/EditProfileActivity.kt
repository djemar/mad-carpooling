package com.mad.carpooling

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast


class EditProfileActivity : AppCompatActivity() {
    private lateinit var fullNameEditText : EditText
    private lateinit var nicknameEditText : EditText
    private lateinit var emailEditText : EditText
    private lateinit var locationEditText : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        fullNameEditText = findViewById<EditText>(R.id.edit_fullName)
        nicknameEditText = findViewById<EditText>(R.id.edit_nickname)
        emailEditText = findViewById<EditText>(R.id.edit_email)
        locationEditText = findViewById<EditText>(R.id.edit_location)
        val cameraButton = findViewById<ImageButton>(R.id.camera_button)
        registerForContextMenu(cameraButton)
        cameraButton.setOnClickListener { openContextMenu(cameraButton)}
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
            R.id.context_camera -> {
                Toast.makeText(this, "Open Camera", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
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