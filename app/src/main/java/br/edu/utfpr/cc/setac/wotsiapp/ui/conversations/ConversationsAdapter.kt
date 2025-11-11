package br.edu.utfpr.cc.setac.wotsiapp.ui.conversations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.utfpr.cc.setac.wotsiapp.data.model.Conversation
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ItemConversationBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ConversationsAdapter(
    private val onConversationClick: (Conversation) -> Unit,
    private val getParticipantName: (Conversation) -> String,
    private val getParticipantPhotoUrl: (Conversation) -> String?,
    private val getUnreadCount: (Conversation) -> Int
) : ListAdapter<Conversation, ConversationsAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            val participantName = getParticipantName(conversation)
            val participantPhotoUrl = getParticipantPhotoUrl(conversation)
            val unreadCount = getUnreadCount(conversation)

            binding.tvName.text = participantName
            binding.tvLastMessage.text = conversation.lastMessage
            binding.tvTime.text = formatTime(conversation.lastMessageTimestamp.toDate())

            // Load avatar
            if (participantPhotoUrl != null) {
                Glide.with(binding.root.context)
                    .load(participantPhotoUrl)
                    .into(binding.ivAvatar)
            }

            // Show unread badge
            if (unreadCount > 0) {
                binding.tvUnreadBadge.visibility = View.VISIBLE
                binding.tvUnreadBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            } else {
                binding.tvUnreadBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onConversationClick(conversation)
            }
        }

        private fun formatTime(date: Date): String {
            val now = Calendar.getInstance()
            val messageTime = Calendar.getInstance().apply { time = date }

            return when {
                now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                }
                now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> {
                    "Yesterday"
                }
                else -> {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                }
            }
        }
    }

    class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}

