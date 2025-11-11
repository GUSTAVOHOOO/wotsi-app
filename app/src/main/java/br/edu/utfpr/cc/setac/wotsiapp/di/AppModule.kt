package br.edu.utfpr.cc.setac.wotsiapp.di

import br.edu.utfpr.cc.setac.wotsiapp.data.repository.AuthRepository
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.ChatRepository
import br.edu.utfpr.cc.setac.wotsiapp.data.repository.UserRepository
import br.edu.utfpr.cc.setac.wotsiapp.ui.auth.AuthViewModel
import br.edu.utfpr.cc.setac.wotsiapp.ui.chat.ChatViewModel
import br.edu.utfpr.cc.setac.wotsiapp.ui.conversations.ConversationsViewModel
import br.edu.utfpr.cc.setac.wotsiapp.ui.profile.ProfileEditViewModel
import br.edu.utfpr.cc.setac.wotsiapp.ui.users.UsersViewModel
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val firebaseModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }
    single { Firebase.storage }
    single { Firebase.analytics }
    single { Firebase.crashlytics }
}

val repositoryModule = module {
    single { AuthRepository(get(), get()) }
    single { ChatRepository(get(), get()) }
    single { UserRepository(get()) }
}

val viewModelModule = module {
    viewModel { AuthViewModel(get()) }
    viewModel { ConversationsViewModel(get(), get()) }
    viewModel { ChatViewModel(get(), get(), get(), get(), get()) }
    viewModel { UsersViewModel(get(), get(), get()) }
    viewModel { ProfileEditViewModel(get(), get(), get()) }
}

val appModule = listOf(firebaseModule, repositoryModule, viewModelModule)



