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
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _result: MutableLiveData<String?> = MutableLiveData(null)
    val result: LiveData<String?> = _result

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var signatures: List<Pair<String, List<List<Int?>>>> = emptyList()

    init {
        loadSignaturesFromAssets()
    }

    private fun loadSignaturesFromAssets() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val inputStream = app.assets.open("signatures.json")
            val rawJson = inputStream.bufferedReader().readText()
            val jsonObject = JSONObject(rawJson)
            val signatureList: ArrayList<Pair<String, List<List<Int?>>>> = ArrayList()
            for (fileType in jsonObject.keys()) {
                val jsonArray = jsonObject.getJSONArray(fileType)
                val bytesList: ArrayList<List<Int?>> = ArrayList()
                for (i in 0 until jsonArray.length()) {
                    val byteJsonArray = jsonArray.getJSONArray(i)
                    val bytes: ArrayList<Int?> = ArrayList()
                    for (ii in 0 until byteJsonArray.length()) {
                        val byte = try {
                            byteJsonArray.getInt(ii)
                        } catch (e: JSONException) {
                            null
                        }
                        bytes.add(byte)
                    }
                    bytesList.add(bytes)
                }
                signatureList.add(Pair(fileType, bytesList))
            }
            signatures = signatureList
            _isLoading.postValue(false)
        }
    }

    fun handleOpenFileFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            _result.postValue(null)

            try {
                app.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                    val buffer = ByteArray(100)
                    inputStream.read(buffer, 0, 100)

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
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun isSignatureMatch(buffer: ByteArray, signature: List<Int?>): Boolean {
        if (buffer.size < signature.size) {
            return false
        }
        signature.forEachIndexed { index, signatureByte ->
            val fileByte = buffer[index]
            if (fileByte != signatureByte?.toByte() && signatureByte != null) {
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
}