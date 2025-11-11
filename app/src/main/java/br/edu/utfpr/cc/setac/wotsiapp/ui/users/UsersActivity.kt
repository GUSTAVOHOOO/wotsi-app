package br.edu.utfpr.cc.setac.wotsiapp.ui.users

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ActivityUsersBinding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applySideSystemBarsPadding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applyStatusBarPadding
import br.edu.utfpr.cc.setac.wotsiapp.ui.chat.ChatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private val viewModel: UsersViewModel by viewModel()
    private val analytics: FirebaseAnalytics by inject()
    private lateinit var adapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val wic = WindowInsetsControllerCompat(window, window.decorView)
        wic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.toolbar.applyStatusBarPadding()
        binding.root.applySideSystemBarsPadding()

        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.loadUsers()

        // Log screen view
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "Users")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "UsersActivity")
        })
    }

    private fun setupRecyclerView() {
        adapter = UsersAdapter(
            onUserClick = { user ->
                viewModel.startConversation(user)
            }
        )
        binding.rvUsers.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.users.observe(this) { users ->
            adapter.submitList(users)

            if (users.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvUsers.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvUsers.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }

        viewModel.conversationCreated.observe(this) { conversationId ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
                putExtra(ChatActivity.EXTRA_CONVERSATION_NAME, "Chat")
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupListeners() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            viewModel.searchUsers(query)
        }
    }
}

