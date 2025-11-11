package br.edu.utfpr.cc.setac.wotsiapp.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.edu.utfpr.cc.setac.wotsiapp.R
import br.edu.utfpr.cc.setac.wotsiapp.databinding.ActivityProfileEditBinding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applySideSystemBarsPadding
import br.edu.utfpr.cc.setac.wotsiapp.extensions.applyStatusBarPadding
import com.bumptech.glide.Glide
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private val viewModel: ProfileEditViewModel by viewModel()
    private val analytics: FirebaseAnalytics by inject()
    private var capturedImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.uploadProfileImage(it)
            loadImage(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            capturedImageUri?.let { uri ->
                viewModel.uploadProfileImage(uri)
                loadImage(uri)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val wic = WindowInsetsControllerCompat(window, window.decorView)
        wic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.applyStatusBarPadding()
        binding.root.applySideSystemBarsPadding()

        setupObservers()
        setupListeners()

        // Log screen view
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, "ProfileEdit")
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "ProfileEditActivity")
        })
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
            binding.fabChangePhoto.isEnabled = !isLoading
        }

        viewModel.user.observe(this) { user ->
            user?.let {
                binding.etDisplayName.setText(it.displayName)
                binding.etEmail.setText(it.email)

                if (!it.photoUrl.isNullOrEmpty()) {
                    loadImage(it.photoUrl)
                }
            }
        }

        viewModel.imageUrl.observe(this) { url ->
            url?.let {
                loadImage(it)
            }
        }

        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }

        viewModel.profileUpdated.observe(this) { updated ->
            if (updated) {
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.fabChangePhoto.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        binding.btnSave.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString().trim()
            viewModel.updateProfile(displayName)
        }
    }

    private fun checkPermissionsAndShowDialog() {
        // Modern photo picker doesn't require permissions on Android 13+
        // We only need camera permission when using camera
        showImageSourceDialog()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.camera),
            getString(R.string.gallery)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.select_image_source)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        capturedImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun loadImage(imageSource: Any) {
        Glide.with(this)
            .load(imageSource)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .centerCrop()
            .into(binding.ivProfile)
    }
}

