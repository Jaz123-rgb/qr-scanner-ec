// MainActivity.kt
package com.jp.scantwo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.jp.scantwo.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showCamera()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, getString(R.string.cancelled), Toast.LENGTH_SHORT).show()
        } else {
            handleScanResult(result.contents)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private var scanAction: String? = null

    private val apiService: ApiEntryExit by lazy {
        RetrofitClient.apiService
    }

    private fun handleScanResult(contents: String) {
        scanAction?.let { action ->
            registerEntryExit(contents, action)
        }
        binding.textResult.text = contents
    }

    private fun showCamera() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt(getString(R.string.scan_a_barcode))
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(true)
        scanLauncher.launch(options)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()
        initViews()
    }

    private fun initViews() {
        binding.fab.setOnClickListener {
            checkCameraPermission()
        }
        binding.buttonEntry.setOnClickListener {
            scanAction = "entry"
            checkCameraPermission()
        }
        binding.buttonExit.setOnClickListener {
            scanAction = "exit"
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCamera()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Toast.makeText(this, getString(R.string.allow_camera_permission), Toast.LENGTH_SHORT).show()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun initBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun registerEntryExit(uniqueIdPayment: String, action: String) {
        lifecycleScope.launch {
            try {
                val request = EntryExitRequest(uniqueIdPayment, action)
                val response = apiService.registerEntryExit(request)
                handleResponse(response)
            } catch (e: Exception) {
                Log.e("MainActivity", "Exception: ${e.message}", e)
                runOnUiThread {
                    binding.textStatus.text = "Ocurrió una excepción. Verifica los logs para más detalles."
                }
            }
        }
    }

    private fun handleResponse(response: Response<EntryExitResponse>) {
        if (response.isSuccessful) {
            response.body()?.let { responseBody ->
                val message = responseBody.message
                val time = responseBody.time
                val nombre = responseBody.user.nombre
                displayUserInfo("Éxito: $message a las $time", nombre)
            }
        } else {
            response.errorBody()?.let { errorBody ->
                val errorJson = JSONObject(errorBody.string())
                val errorMessage = errorJson.getString("error")
                val user = errorJson.getJSONObject("user")
                val nombre = user.getString("nombre")
                displayUserInfo("Error: $errorMessage", nombre)
            }
        }
    }

    private fun displayUserInfo(message: String, nombre: String) {
        val userInfo = "Nombre: $nombre"
        runOnUiThread {
            binding.textStatus.text = "$message\n$userInfo"
        }
    }
}
