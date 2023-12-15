package com.anago.filechecker

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _result: MutableLiveData<String?> = MutableLiveData(null)
    val result: LiveData<String?> = _result

    fun handleOpenFileFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _result.postValue(null)

            try {
                app.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                    val buffer = ByteArray(8)
                    inputStream.read(buffer, 0, 8)

                    if (buffer.isEmpty()) {
                        Log.e("FileChecker", "ファイルの読み込みに失敗しました！")
                        _result.postValue("failed to read file!")
                        return@launch
                    }

                    checkFileSignature(buffer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FileChecker", "ファイルの読み込み時に例外が発生しました！\n${e.message}")
            }
        }
    }

    private fun isSignatureMatch(buffer: ByteArray, signature: List<Int?>): Boolean {
        if (buffer.size < signature.size) {
            return false
        }
        signature.forEachIndexed { index, byte1 ->
            val byte2 = buffer[index]
            if (byte2 != byte1?.toByte() && byte1 != null) {
                return false
            }
        }
        return true
    }

    private fun checkFileSignature(buffer: ByteArray) {
        signatures.forEach { (fileType, possibleSignatures) ->
            possibleSignatures.forEach aa@{ signature ->
                if (isSignatureMatch(buffer, signature)) {
                    _result.postValue(fileType)
                    return@aa
                }
            }
        }
    }

    // https://en.wikipedia.org/wiki/List_of_file_signatures
    private val signatures = listOf(
        "zip" to listOf(
            listOf(0x50, 0x4b, 0x03, 0x04),
            listOf(0x50, 0x4b, 0x05, 0x06),
            listOf(0x50, 0x4b, 0x07, 0x08)
        ),
        "png" to listOf(
            listOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        ),
        "jpg/jpeg" to listOf(
            listOf(0xFF, 0xD8, 0xFF, 0xDB),
            listOf(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01),
            listOf(0xFF, 0xD8, 0xFF, 0xEE),
            listOf(0xFF, 0xD8, 0xFF, 0xE1, null, null, 0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
        ),
        "jpg" to listOf(
            listOf(0xFF, 0xD8, 0xFF, 0xE0)
        ),
        "pdf" to listOf(
            listOf(0x25, 0x50, 0x44, 0x46, 0x2D)
        ),
        "mp3" to listOf(
            listOf(0xFF, 0xFB),
            listOf(0xFF, 0xF3),
            listOf(0xFF, 0xF2),
            listOf(0x49, 0x44, 0x33)
        ),
        "wav" to listOf(
            listOf(0x52, 0x49, 0x46, 0x46, null, null, null, null, 0x57, 0x41, 0x56, 0x45)
        ),
        "avi" to listOf(
            listOf(0x52, 0x49, 0x46, 0x46, null, null, null, null, 0x41, 0x56, 0x49, 0x20)
        ),
        "iso" to listOf(
            listOf(0x43, 0x44, 0x30, 0x30, 0x31)
        ),
        "tar" to listOf(
            listOf(0x75, 0x73, 0x74, 0x61, 0x72, 0x00, 0x30, 0x30),
            listOf(0x75, 0x73, 0x74, 0x61, 0x72, 0x20, 0x20, 0x00)
        ),
        "7z" to listOf(
            listOf(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C)
        ),
        "xz" to listOf(
            listOf(0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00)
        ),
        "gz" to listOf(
            listOf(0x1F, 0x8B)
        ),
        "mkv" to listOf(
            listOf(0x1A, 0x45, 0xDF, 0xA3)
        ),
        "mp4" to listOf(
            listOf(0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D),
            listOf(0x66, 0x74, 0x79, 0x70, 0x4D, 0x53, 0x4E, 0x56)
        ),
        "sqlite" to listOf(
            listOf(
                0x53,
                0x51,
                0x4C,
                0x69,
                0x74,
                0x65,
                0x20,
                0x66,
                0x6F,
                0x72,
                0x6D,
                0x61,
                0x74,
                0x20,
                0x33,
                0x00
            )
        ),
        "gif" to listOf(
            listOf(0x47, 0x49, 0x46, 0x38, 0x37, 0x61),
            listOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)
        ),
        "tif" to listOf(
            listOf(0x49, 0x49, 0x2A, 0x00),
            listOf(0x4D, 0x4D, 0x00, 0x2A)
        ),
        "mz" to listOf(
            listOf(0x4D, 0x5A)
        ),
        "exe" to listOf(
            listOf(0x5A, 0x4D)
        ),
        "rar" to listOf(
            listOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00),
            listOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00)
        ),
        "elf" to listOf(
            listOf(0x7F, 0x45, 0x4C, 0x46)
        ),
        "java class" to listOf(
            listOf(0xCA, 0xFE, 0xBA, 0xBE)
        ),
        "ogg" to listOf(
            listOf(0x4F, 0x67, 0x67, 0x53)
        ),
        "bmp" to listOf(
            listOf(0x42, 0x4D)
        ),
        "crt" to listOf(
            listOf(
                0x43,
                0x36,
                0x34,
                0x20,
                0x43,
                0x41,
                0x52,
                0x54,
                0x52,
                0x49,
                0x44,
                0x47,
                0x45,
                0x20,
                0x20,
                0x20
            ),
            listOf(
                0x2D,
                0x2D,
                0x2D,
                0x2D,
                0x2D,
                0x42,
                0x45,
                0x47,
                0x49,
                0x4E,
                0x20,
                0x43,
                0x45,
                0x52,
                0x54,
                0x49,
                0x46,
                0x49,
                0x43,
                0x41,
                0x54,
                0x45,
                0x2D,
                0x2D,
                0x2D,
                0x2D,
                0x2D
            )
        ),
        "csr" to listOf(
            listOf(
                0x2D,
                0x2D,
                0x2D,
                0x2D,
                0x2D,
                0x42,
                0x45,
                0x47,
                0x49,
                0x4E,
                0x20,
                0x43,
                0x45,
                0x52,
                0x54,
                0x49,
                0x46,
                0x49,
                0x43,
                0x41,
                0x54,
                0x45,
                0x20,
                0x52,
                0x45,
                0x51,
                0x55,
                0x45,
                0x53,
                0x54,
                0x2D,
                0x2D,
                0x2D,
                0x2D,
                0x2D
            )
        ),
        "webp" to listOf(
            listOf(0x52, 0x49, 0x46, 0x46, null, null, null, null, 0x57, 0x45, 0x42, 0x50)
        )
    )
}