package br.edu.utfpr.cc.setac.wotsiapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ActivitySignUpBinding
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel: AuthViewModel by viewModel()
    private val analytics: FirebaseAnalytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()

        // Log screen view
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "SignUp")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SignUpActivity")
        })
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSignUp.isEnabled = !isLoading
        }

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.SignUpSuccess -> {
                    Toast.makeText(this, "Account created! Please verify your email.", Toast.LENGTH_LONG).show()
                    navigateToEmailVerification()
                }
                is AuthViewModel.AuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setupListeners() {
        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInput(name, email, password)) {
                viewModel.signUp(email, password, name)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            isValid = false
        } else {
            binding.tilName.error = null
        }

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
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun navigateToEmailVerification() {
        startActivity(Intent(this, EmailVerificationActivity::class.java))
        finish()
    }
}

