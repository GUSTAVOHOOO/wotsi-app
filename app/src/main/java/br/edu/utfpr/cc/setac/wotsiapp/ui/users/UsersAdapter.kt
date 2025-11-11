package br.edu.utfpr.cc.setac.wotsiapp.ui.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.utfpr.cc.setac.wotsiapp.R
import br.edu.utfpr.cc.setac.wotsiapp.data.model.User
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ItemUserBinding
import com.bumptech.glide.Glide

class UsersAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvName.text = user.displayName
            binding.tvEmail.text = user.email

            // Show online status
            if (user.isOnline) {
                binding.tvStatus.text = binding.root.context.getString(R.string.online)
                binding.tvStatus.setTextColor(
                    binding.root.context.getColor(R.color.success)
                )
            } else {
                binding.tvStatus.text = binding.root.context.getString(R.string.offline)
                binding.tvStatus.setTextColor(
                    binding.root.context.getColor(R.color.text_secondary)
                )
            }

            // Load avatar
            if (user.photoUrl != null) {
                Glide.with(binding.root.context)
                    .load(user.photoUrl)
                    .into(binding.ivAvatar)
            }

            binding.root.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}

