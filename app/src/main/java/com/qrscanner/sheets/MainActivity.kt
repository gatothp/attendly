package com.qrscanner.sheets

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
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

    // -------------------------------------------------------------------------
    // Camera scanner — square viewfinder via SquareScanActivity
    // -------------------------------------------------------------------------

    private val scanLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents?.let { handleScanResult(it) }
        }

    // -------------------------------------------------------------------------
    // Gallery image picker
    // -------------------------------------------------------------------------

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { decodeQrFromUri(it) }
        }

    // -------------------------------------------------------------------------
    // Runtime permission for gallery (Android 13+)
    // -------------------------------------------------------------------------

    private val mediaPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) galleryLauncher.launch("image/*")
            else Toast.makeText(this, getString(R.string.msg_gallery_permission_denied), Toast.LENGTH_SHORT).show()
        }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Short-press → camera scan; long-press → pick from gallery
        binding.cardScan.setOnClickListener { startCameraScan() }
        binding.cardScan.setOnLongClickListener {
            openGalleryPicker()
            true
        }

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
    // Camera scan (square viewfinder)
    // -------------------------------------------------------------------------

    private fun startCameraScan() {
        if (!isConfigured()) return

        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(getString(R.string.msg_scan_prompt))
            .setCaptureActivity(SquareScanActivity::class.java)
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(false)
        scanLauncher.launch(options)
    }

    // -------------------------------------------------------------------------
    // Gallery QR picker
    // -------------------------------------------------------------------------

    private fun openGalleryPicker() {
        if (!isConfigured()) return

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED ->
                galleryLauncher.launch("image/*")
            else ->
                mediaPermissionLauncher.launch(permission)
        }
    }

    /**
     * Decode a QR code from a content [Uri] using ML Kit.
     * Runs the heavy work on IO then calls [handleScanResult] on Main.
     */
    private fun decodeQrFromUri(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val image = InputImage.fromFilePath(applicationContext, uri)
                    val scanner = BarcodeScanning.getClient()
                    // Tasks API — block on IO thread
                    val barcodes = com.google.android.gms.tasks.Tasks.await(scanner.process(image))
                    barcodes.firstOrNull { it.valueType == Barcode.TYPE_TEXT || it.rawValue != null }
                        ?.rawValue
                }
            }

            result
                .onSuccess { value ->
                    if (value != null) {
                        handleScanResult(value)
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.msg_no_qr_found), Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_no_qr_found), Toast.LENGTH_SHORT).show()
                }
        }
    }

    // -------------------------------------------------------------------------
    // Guard: ensure the app is configured before scanning
    // -------------------------------------------------------------------------

    private fun isConfigured(): Boolean {
        val saveLocally = prefs().getBoolean("save_locally", false)
        val scriptUrl = prefs().getString("script_url", "").orEmpty()
        return if (!saveLocally && scriptUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_configure_sheet), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            false
        } else true
    }

    // -------------------------------------------------------------------------
    // Common result handler
    // -------------------------------------------------------------------------

    private fun handleScanResult(scannedText: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        // Increment and persist the scan counter
        val scanNumber = prefs().getInt("scan_counter", 0) + 1
        prefs().edit().putInt("scan_counter", scanNumber).apply()

        if (prefs().getBoolean("save_locally", false)) {
            saveLocally(timestamp, scannedText, scanNumber)
        } else {
            saveToSheets(timestamp, scannedText, scanNumber)
        }
    }

    // -------------------------------------------------------------------------
    // Save locally (offline)
    // -------------------------------------------------------------------------

    private fun saveLocally(timestamp: String, id: String, scanNumber: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.insert(timestamp, id)
            withContext(Dispatchers.Main) {
                binding.tvLastScanLabel.text = getString(R.string.label_scan_number, scanNumber)
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

    private fun saveToSheets(timestamp: String, id: String, scanNumber: Int) {
        binding.cardScan.isEnabled = false

        val scriptUrl = prefs().getString("script_url", "").orEmpty()
        val sheetName = prefs().getString("sheet_name", "Sheet1").orEmpty()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                SheetsHelper.appendRow(scriptUrl, sheetName, timestamp, id)
            }

            binding.cardScan.isEnabled = true

            result.onSuccess {
                binding.tvLastScanLabel.text = getString(R.string.label_scan_number, scanNumber)
                binding.tvLastScan.text = getString(R.string.label_last_scan_detail, id, timestamp)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.msg_saved_to_sheet),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                // Roll back the counter increment since the save failed
                prefs().edit().putInt("scan_counter", scanNumber - 1).apply()
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
