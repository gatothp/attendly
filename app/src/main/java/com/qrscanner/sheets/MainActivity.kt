package com.qrscanner.sheets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.qrscanner.sheets.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db by lazy { ScanDatabase(applicationContext) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private val scanLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents?.let { handleScanResult(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dark mode preference before inflating layout
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardScan.setOnClickListener { startScan() }

        binding.btnSendManual.setOnClickListener { submitManualId() }
        binding.etManualId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { submitManualId(); true } else false
        }

        binding.cardTable.setOnClickListener {
            startActivity(Intent(this, TableActivity::class.java))
        }
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.cardHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        binding.cardInfo.setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
        }
    }

    // -------------------------------------------------------------------------
    // Manual ID entry
    // -------------------------------------------------------------------------

    private fun submitManualId() {
        val id = binding.etManualId.text.toString().trim()
        if (id.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_enter_id), Toast.LENGTH_SHORT).show()
            return
        }
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etManualId.windowToken, 0)
        binding.etManualId.text?.clear()
        handleScanResult(id)
    }

    // -------------------------------------------------------------------------
    // Scanning
    // -------------------------------------------------------------------------

    private fun startScan() {
        val saveLocally = prefs().getBoolean("save_locally", false)
        val scriptUrl = prefs().getString("script_url", "").orEmpty()

        if (!saveLocally && scriptUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_configure_sheet), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(getString(R.string.msg_scan_prompt))
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(false)
        scanLauncher.launch(options)
    }

    private fun handleScanResult(scannedText: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        if (prefs().getBoolean("save_locally", false)) {
            saveLocally(timestamp, scannedText)
        } else {
            saveToSheets(timestamp, scannedText)
        }
    }

    // -------------------------------------------------------------------------
    // Save locally (offline)
    // -------------------------------------------------------------------------

    private fun saveLocally(timestamp: String, id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.insert(timestamp, id)
            withContext(Dispatchers.Main) {
                binding.tvLastScan.text = getString(R.string.label_last_scan_detail, id, timestamp)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.msg_saved_locally),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Save directly to Sheets via Apps Script
    // -------------------------------------------------------------------------

    private fun saveToSheets(timestamp: String, id: String) {
        binding.cardScan.isEnabled = false

        val scriptUrl = prefs().getString("script_url", "").orEmpty()
        val sheetName = prefs().getString("sheet_name", "Sheet1").orEmpty()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                SheetsHelper.appendRow(scriptUrl, sheetName, timestamp, id)
            }

            binding.cardScan.isEnabled = true

            result.onSuccess {
                binding.tvLastScan.text = getString(R.string.label_last_scan_detail, id, timestamp)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.msg_saved_to_sheet),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.msg_failed_prefix, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun prefs() = getSharedPreferences("prefs", MODE_PRIVATE)
}
