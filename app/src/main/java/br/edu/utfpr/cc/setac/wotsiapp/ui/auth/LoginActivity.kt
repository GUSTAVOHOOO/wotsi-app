package br.edu.utfpr.cc.setac.wotsiapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ActivityLoginBinding
import br.edu.utfpr.cc.setac.wotsiapp.ui.conversations.ConversationsActivity
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModel()
    private val analytics: FirebaseAnalytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check if user is already logged in and email verified
        if (viewModel.isUserLoggedIn) {
            if (viewModel.currentUser?.isEmailVerified == true) {
                navigateToConversations()
                return
            } else {
                navigateToEmailVerification()
                return
            }
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()

        // Log screen view
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "Login")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "LoginActivity")
        })
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
        }

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.SignInSuccess -> {
                    if (state.user.isEmailVerified) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        navigateToConversations()
                    } else {
                        navigateToEmailVerification()
                    }
                }
                is AuthViewModel.AuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is AuthViewModel.AuthState.PasswordResetEmailSent -> {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInput(email, password)) {
                viewModel.signIn(email, password)
            }
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.tilEmail.error = "Enter your email first"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Invalid email"
            } else {
                viewModel.sendPasswordResetEmail(email)
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun navigateToConversations() {
        startActivity(Intent(this, ConversationsActivity::class.java))
        finish()
    }

    private fun navigateToEmailVerification() {
        startActivity(Intent(this, EmailVerificationActivity::class.java))
        finish()
    }
}

