package com.qrscanner.sheets

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qrscanner.sheets.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        // Restore saved values
        binding.etScriptUrl.setText(prefs.getString("script_url", ""))
        binding.etSheetName.setText(prefs.getString("sheet_name", "Sheet1"))
        binding.switchSaveLocally.isChecked = prefs.getBoolean("save_locally", false)

        // Save the toggle immediately when changed (independent of SAVE SETTINGS button)
        binding.switchSaveLocally.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("save_locally", isChecked).apply()
        }

        binding.btnSave.setOnClickListener {
            val url = binding.etScriptUrl.text.toString().trim()
            val name = binding.etSheetName.text.toString().trim().ifEmpty { "Sheet1" }

            if (url.isNotEmpty() && !url.startsWith("https://script.google.com/")) {
                Toast.makeText(this, getString(R.string.msg_invalid_script_url), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("script_url", url)
                .putString("sheet_name", name)
                .apply()

            Toast.makeText(this, getString(R.string.msg_settings_saved), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
