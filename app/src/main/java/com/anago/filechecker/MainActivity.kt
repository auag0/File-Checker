package com.anago.filechecker

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvResult: MaterialTextView = findViewById(R.id.tvResult)
        viewModel.result.observe(this) { result ->
            tvResult.text = result ?: "Unknown"
        }

        val btnOpenFile: MaterialButton = findViewById(R.id.btnOpenFile)
        btnOpenFile.setOnClickListener {
            openLauncher.launch(arrayOf("*/*"))
        }
    }

    private val openLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { safeUri ->
                viewModel.handleOpenFileFromUri(safeUri)
            }
        }
}