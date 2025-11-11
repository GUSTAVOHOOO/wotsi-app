package br.edu.utfpr.cc.setac.wotsiapp.ui.conversations

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.edu.utfpr.cc.setac.wotsiapp.R
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ActivityConversationsBinding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applySideSystemBarsPadding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applyStatusBarPadding
import br.edu.utfpr.cc.setac.wotsiapp.ui.auth.LoginActivity
import br.edu.utfpr.cc.setac.wotsiapp.ui.chat.ChatActivity
import br.edu.utfpr.cc.setac.wotsiapp.ui.users.UsersActivity
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConversationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationsBinding
    private val viewModel: ConversationsViewModel by viewModel()
    private val analytics: FirebaseAnalytics by inject()
    private lateinit var adapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val wic = WindowInsetsControllerCompat(window, window.decorView)
        wic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

        binding = ActivityConversationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.toolbar.applyStatusBarPadding()
        binding.root.applySideSystemBarsPadding()

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Log screen view
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "Conversations")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "ConversationsActivity")
        })
    }

    private fun setupRecyclerView() {
        adapter = ConversationsAdapter(
            onConversationClick = { conversation ->
                val otherUserId = viewModel.getOtherParticipantId(conversation)
                if (otherUserId != null) {
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.id)
                        putExtra(ChatActivity.EXTRA_CONVERSATION_NAME,
                            viewModel.getOtherParticipantName(conversation))
                        putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUserId)
                    }
                    startActivity(intent)
                }
            },
            getParticipantName = { conversation ->
                viewModel.getOtherParticipantName(conversation)
            },
            getParticipantPhotoUrl = { conversation ->
                viewModel.getOtherParticipantPhotoUrl(conversation)
            },
            getUnreadCount = { conversation ->
                viewModel.getUnreadCount(conversation)
            }
        )
        binding.rvConversations.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.conversations.observe(this) { conversations ->
            adapter.submitList(conversations)
            binding.swipeRefresh.isRefreshing = false

            if (conversations.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvConversations.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvConversations.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(this, UsersActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            // RecyclerView is already listening to real-time updates
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_conversations, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, br.edu.utfpr.cc.setac.wotsiapp.ui.profile.ProfileEditActivity::class.java))
                true
            }
            R.id.action_sign_out -> {
                viewModel.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

