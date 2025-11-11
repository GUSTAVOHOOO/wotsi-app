package br.edu.utfpr.cc.setac.wotsiapp.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.utfpr.cc.setac.wotsiapp.data.model.Message
import br.edu.utfpr.cc.setac.wotsiapp.data.model.MessageType
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ItemMessageReceivedBinding
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ItemMessageSentBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    private val isMyMessage: (Message) -> Boolean
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (isMyMessage(getItem(position))) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(getItem(position))
            is ReceivedMessageViewHolder -> holder.bind(getItem(position))
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            when (message.type) {
                MessageType.TEXT -> {
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.ivImage.visibility = View.GONE
                    binding.tvMessage.text = message.content
                }
                MessageType.IMAGE -> {
                    binding.tvMessage.visibility = View.GONE
                    binding.ivImage.visibility = View.VISIBLE
                    Glide.with(binding.root.context)
                        .load(message.imageUrl)
                        .into(binding.ivImage)
                }
            }

            binding.tvTime.text = formatTime(message.timestamp.toDate())
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {

            when (message.type) {
                MessageType.TEXT -> {
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.ivImage.visibility = View.GONE
                    binding.tvMessage.text = message.content
                }
                MessageType.IMAGE -> {
                    binding.tvMessage.visibility = View.GONE
                    binding.ivImage.visibility = View.VISIBLE
                    Glide.with(binding.root.context)
                        .load(message.imageUrl)
                        .into(binding.ivImage)
                }
            }

            binding.tvTime.text = formatTime(message.timestamp.toDate())
        }
    }

    private fun formatTime(date: Date): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}

