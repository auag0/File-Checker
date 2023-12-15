package com.anago.filechecker

import android.os.Bundle
import android.widget.Toast
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
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { safeUri ->
                viewModel.handleOpenFileFromUri(safeUri)
            } ?: Toast.makeText(this, "ファイルが見つかりません！", Toast.LENGTH_SHORT).show()
        }
}