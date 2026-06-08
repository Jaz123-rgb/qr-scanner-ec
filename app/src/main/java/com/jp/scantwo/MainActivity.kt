package com.jp.scantwo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import android.app.AlertDialog
import android.util.Log
import android.widget.TextView
import com.google.gson.Gson
import com.jp.scantwo.databinding.ActivityMainBinding
import com.jp.scantwo.databinding.ItemInfoRowBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showCamera()
        else Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
    }

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) performCheckIn(result.contents)
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonScan.setOnClickListener { checkCameraPermission() }
        binding.buttonLogout.setOnClickListener {
            LoginActivity.clearToken(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> showCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ->
                Toast.makeText(this, getString(R.string.allow_camera_permission), Toast.LENGTH_SHORT).show()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showCamera() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.scan_hint))
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    private fun performCheckIn(uuid: String) {
        val token = LoginActivity.getToken(this)
        if (token == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.buttonScan.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.checkIn("Bearer $token", uuid)
                val code = response.code()
                Log.d("CheckIn", "HTTP $code")
                when (code) {
                    200 -> {
                        val data = response.body()?.data
                        if (data != null) showSuccess(data)
                        else showResult(getString(R.string.checkin_error))
                    }
                    409 -> {
                        val rawBody = response.errorBody()?.string().orEmpty()
                        Log.d("CheckIn", "409 body: $rawBody")
                        val data = try {
                            Gson().fromJson(rawBody, CheckInResponse::class.java)?.data
                        } catch (ex: Exception) {
                            Log.e("CheckIn", "parse error: ${ex.message}")
                            null
                        }
                        try {
                            showAlreadyCheckedIn(data)
                        } catch (ex: Exception) {
                            Log.e("CheckIn", "dialog error: ${ex.javaClass.simpleName} — ${ex.message}", ex)
                            showResult(getString(R.string.already_checked_in))
                        }
                    }
                    401 -> {
                        LoginActivity.clearToken(this@MainActivity)
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                    else -> {
                        Log.e("CheckIn", "unexpected code $code")
                        showResult(getString(R.string.checkin_error))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CheckIn", "outer catch: ${e.javaClass.simpleName} — ${e.message}", e)
                showResult(getString(R.string.connection_error))
            } finally {
                binding.buttonScan.isEnabled = true
            }
        }
    }

    private fun showSuccess(data: CheckInData) {
        binding.textHint.visibility = View.GONE
        binding.cardResult.visibility = View.VISIBLE

        binding.textName.text = data.fullName.uppercase()
        binding.textStatus.text = getString(R.string.checkin_success)
        binding.textStatus.setTextColor(ContextCompat.getColor(this, R.color.on26_black))
        binding.textTime.text = data.checkedInAt?.let { formatTime(it) }.orEmpty()

        binding.tagServidor.visibility = if (data.isServidor) View.VISIBLE else View.GONE

        setRow(binding.rowChurch, "IGLESIA", data.churchOrigin)
        setRow(binding.rowMail, "CORREO", data.mail)
        setRow(binding.rowPhone, "TELÉFONO", data.phoneNumber)

        val pago = buildPaymentText(data.statusPayment, data.porcentPayment, data.amount)
        setRow(binding.rowPayment, "PAGO", pago)
    }

    private fun setRow(row: ItemInfoRowBinding, label: String, value: String?) {
        row.root.visibility = if (!value.isNullOrBlank()) View.VISIBLE else View.GONE
        row.rowLabel.text = label
        row.rowValue.text = value.orEmpty()
    }

    private fun buildPaymentText(status: String?, percent: Double?, amount: Double?): String? {
        if (status == null && percent == null && amount == null) return null
        val parts = mutableListOf<String>()
        status?.let { parts.add(it.uppercase()) }
        percent?.let { if (it > 0) parts.add("${it.toInt()}%") }
        amount?.let { if (it > 0) parts.add("$${it.toInt()}") }
        return parts.joinToString(" · ").ifBlank { null }
    }

    private fun showAlreadyCheckedIn(data: CheckInData?) {
        val time = data?.checkedInAt?.let { formatTime(it) }.orEmpty()

        val dialogView = layoutInflater.inflate(R.layout.dialog_already_registered, null) as android.view.View
        (dialogView.findViewById<TextView>(R.id.dialogName)).text = data?.fullName.orEmpty()
        val tvTime = dialogView.findViewById<TextView>(R.id.dialogTime)
        tvTime.text = if (time.isNotEmpty()) "Check-in original: $time" else ""
        tvTime.visibility = if (time.isNotEmpty()) View.VISIBLE else View.GONE

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<android.widget.Button>(R.id.dialogBtnClose).setOnClickListener {
            dialog.dismiss()
            if (data != null) showSuccess(data) else binding.cardResult.visibility = View.GONE
        }
        if (!isFinishing && !isDestroyed) {
            dialog.show()
        }
    }

    private fun showResult(status: String) {
        binding.textHint.visibility = View.GONE
        binding.cardResult.visibility = View.VISIBLE
        binding.textName.text = ""
        binding.textName.visibility = View.GONE
        binding.tagServidor.visibility = View.GONE
        binding.textStatus.text = status
        binding.textStatus.setTextColor(ContextCompat.getColor(this, R.color.on26_error))
        binding.textTime.text = ""
        binding.textTime.visibility = View.GONE
        listOf(binding.rowChurch, binding.rowMail, binding.rowPhone, binding.rowPayment)
            .forEach { it.root.visibility = View.GONE }
    }

    private fun formatTime(isoTime: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val output = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            output.format(input.parse(isoTime)!!)
        } catch (e: Exception) {
            isoTime
        }
    }
}
