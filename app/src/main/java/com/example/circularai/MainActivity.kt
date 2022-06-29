package com.example.circularai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.circularai.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.random.Random



class MainActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    /*
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }
     */

    private var user: FirebaseUser? = null
    private val permissions = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    )

    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private lateinit var f1: detector_fragment
    private lateinit var f2: map_fragment
    private lateinit var f3: fragment_history
    private lateinit var f4: debug_fragment

    private lateinit var auth: FirebaseAuth


    private lateinit var binding: ActivityMainBinding

    /*
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            user = FirebaseAuth.getInstance().currentUser!!

            supportFragmentManager.beginTransaction().apply {
                replace(R.id.fragment, f1)
                commit()
            }

            // ...
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }

     */

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(getLayoutInflater())
        setContentView(binding.root)

        auth = Firebase.auth

        val email = "saad@gmail.com"
        val password = "123456"
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("MAIN", "createUserWithEmail:success")
                    binding.mainActivityWelcomeTv.text = "Welcome, " + user?.displayName
                    user = auth.currentUser
                    f1 = detector_fragment()
                    f2 = map_fragment()
                    f3 = fragment_history()
                    f4 = debug_fragment()


                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragment, f1)
                        commit()
                    }

                    //updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("MAIN", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    //updateUI(null)
                }
            }


/*
        if (user == null) {
            // Choose authentication providers
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build()
            )

            // Create and launch sign-in intent
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.ic_launcher_foreground)
                .setTheme(R.style.Theme_CircularAI)
                .build()
            signInLauncher.launch(signInIntent)
        }

*/

        binding.imageView.setOnClickListener {
            val menu = PopupMenu(this, it)
            menu.setOnMenuItemClickListener(this)
            menu.inflate(R.menu.application_menu)
            menu.show()
        }




        val nav_view = binding.bottomNavigationView

        nav_view.setOnItemSelectedListener { item ->
            when (item.getItemId()) {
                R.id.nav_camera -> replaceFragment(f1)
                R.id.nav_map -> replaceFragment(f2)
                R.id.hist_map -> replaceFragment(f3)
                R.id.nav_debug -> replaceFragment(f4)

                else -> {
                    replaceFragment(f1)
                }
            }
        }

    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment, fragment)
            addToBackStack(null)
            commit()
            return true
        }
    }

    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        } else {
            start_gps()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            start_gps()
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun start_gps(): Boolean {
        Toast.makeText(this, "Permissions have been granted", Toast.LENGTH_LONG).show()
        return true
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.getItemId()) {
                R.id.sign_out -> AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener { finish() }
                else -> {
                }
            }
            return true
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
           // reload();
        }
    }
}
