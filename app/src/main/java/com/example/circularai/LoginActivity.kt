package com.example.circularai

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val button = findViewById<Button>(R.id.login_button).setOnClickListener(
            {
                sendMessage()
            }
        )
    }

    fun sendMessage() {
        val email = findViewById<EditText>(R.id.email_tv).text.toString()
        val password = findViewById<EditText>(R.id.password_tv).text.toString()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("EMAIL", email)
            putExtra("PASSWORD", password)
        }

        startActivity(intent)
    }
}