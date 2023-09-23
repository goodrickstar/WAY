package etc.syndicate.locations

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import etc.syndicate.locations.databinding.LoginActivityBinding

class LoginActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: LoginActivityBinding

    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            Logger.i("signInLauncher()")
            if (result.resultCode == RESULT_OK) auth.currentUser?.let {
                launchApp()
            }
        }

    private fun signIn() {
        val providers = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())
        val signInIntent =
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers)
                .build()
        signInLauncher.launch(signInIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.i("onCreate()")
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth.currentUser?.let {
            launchApp()
        }
        binding.signInButton.setOnClickListener{
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            signIn()
        }
    }
    fun launchApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }


}