package br.edu.utfpr.cc.setac.wotsiapp.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import br.edu.utfpr.cc.setac.wotsiapp.R
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ActivityChatBinding
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ToolbarChatBinding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applyNavigationBarPadding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applySideSystemBarsPadding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applyStatusBarPadding
import com.bumptech.glide.Glide
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_CONVERSATION_NAME = "conversation_name"
        const val EXTRA_OTHER_USER_ID = "other_user_id"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var toolbarBinding: ToolbarChatBinding
    private val viewModel: ChatViewModel by viewModel()
    private val analytics: FirebaseAnalytics by inject()
    private lateinit var adapter: MessagesAdapter

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImageMessage(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val wic = WindowInsetsControllerCompat(window, window.decorView)
        wic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbarBinding = ToolbarChatBinding.bind(binding.toolbar.findViewById(R.id.toolbar_content))

        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: run {
            finish()
            return
        }
        val conversationName = intent.getStringExtra(EXTRA_CONVERSATION_NAME) ?: "Chat"
        val otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: run {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.applyStatusBarPadding()
        binding.root.applySideSystemBarsPadding()

        toolbarBinding.tvUserName.text = conversationName
        binding.toolbar.setNavigationOnClickListener { finish() }

        viewModel.setConversationId(conversationId, otherUserId)
        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Log screen view
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "Chat")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "ChatActivity")
        })
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter(
            isMyMessage = { message -> viewModel.isMyMessage(message) }
        )
        binding.rvMessages.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }

            if (messages.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvMessages.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvMessages.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }

        viewModel.sendMessageSuccess.observe(this) { success ->
            if (success) {
                binding.etMessage.text?.clear()
            }
        }

        viewModel.otherUser.observe(this) { user ->
            user?.let {
                toolbarBinding.tvUserName.text = it.displayName.ifEmpty { it.email }

                // Update online status
                if (it.isOnline) {
                    toolbarBinding.tvUserStatus.text = getString(R.string.online)
                } else {
                    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val lastSeenText = getString(R.string.last_seen, dateFormat.format(it.lastSeen.toDate()))
                    toolbarBinding.tvUserStatus.text = lastSeenText
                }

                // Load avatar
                if (!it.photoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(it.photoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(toolbarBinding.ivUserAvatar)
                } else {
                    toolbarBinding.ivUserAvatar.setImageResource(R.drawable.ic_person)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendTextMessage(message)
            }
        }

        binding.btnAttach.setOnClickListener {
            checkPermissionAndPickImage()
        }

        binding.etMessage.addTextChangedListener { text ->
            binding.btnSend.isEnabled = !text.isNullOrBlank()
        }
    }

    private fun checkPermissionAndPickImage() {
        // Modern photo picker doesn't require permissions on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            // For older Android versions, we still need to check permission
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                    pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                else -> {
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }
}

