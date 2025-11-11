package br.edu.utfpr.cc.setac.wotsiapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ActivityEmailVerificationBinding
import br.edu.utfpr.cc.setac.wotsiapp.ui.conversations.ConversationsActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnResend.isEnabled = !isLoading
            binding.btnContinue.isEnabled = !isLoading
        }

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.VerificationEmailSent -> {
                    Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
                }
                is AuthViewModel.AuthState.EmailVerified -> {
                    Toast.makeText(this, "Email verified!", Toast.LENGTH_SHORT).show()
                    navigateToConversations()
                }
                is AuthViewModel.AuthState.EmailNotVerified -> {
                    Toast.makeText(this, "Please verify your email to continue", Toast.LENGTH_SHORT).show()
                }
                is AuthViewModel.AuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setupListeners() {
        binding.btnResend.setOnClickListener {
            viewModel.resendVerificationEmail()
        }

        binding.btnContinue.setOnClickListener {
            viewModel.reloadUser()
        }

        // Handle back button
        onBackPressedDispatcher.addCallback(this) {
            // Prevent going back to login/signup
            // User must verify email or sign out
            viewModel.signOut()
            finish()
        }
    }

    private fun navigateToConversations() {
        startActivity(Intent(this, ConversationsActivity::class.java))
        finish()
    }
}

