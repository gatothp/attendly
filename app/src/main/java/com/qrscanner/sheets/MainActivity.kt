package com.qrscanner.sheets

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private val scanLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents?.let { handleScanResult(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.btnScan.setOnClickListener { startScan() }
    }

    override fun onResume() {
        super.onResume()
        updateSheetInfo()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_table -> {
                startActivity(Intent(this, TableActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
            .setPrompt("Scan a QR code")
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
                binding.tvLastScan.text = "ID: $id\nTime: $timestamp"
                binding.tvStatus.text = getString(R.string.label_ready)
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
        binding.tvStatus.text = getString(R.string.label_saving)
        binding.btnScan.isEnabled = false

        val scriptUrl = prefs().getString("script_url", "").orEmpty()
        val sheetName = prefs().getString("sheet_name", "Sheet1").orEmpty()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                SheetsHelper.appendRow(scriptUrl, sheetName, timestamp, id)
            }

            binding.btnScan.isEnabled = true

            result.onSuccess {
                binding.tvStatus.text = getString(R.string.label_ready)
                binding.tvLastScan.text = "ID: $id\nTime: $timestamp"
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.msg_saved_to_sheet),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                binding.tvStatus.text = "Error: ${e.message}"
                Toast.makeText(this@MainActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun updateSheetInfo() {
        val saveLocally = prefs().getBoolean("save_locally", false)
        val sheetName = prefs().getString("sheet_name", "").orEmpty()
        binding.tvSheetInfo.text = when {
            saveLocally -> getString(R.string.label_mode_local)
            sheetName.isEmpty() -> getString(R.string.label_not_configured)
            else -> getString(R.string.label_sheet_active, sheetName)
        }
    }

    private fun prefs() = getSharedPreferences("prefs", MODE_PRIVATE)
}
