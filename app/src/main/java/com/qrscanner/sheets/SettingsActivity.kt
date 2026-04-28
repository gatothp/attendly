package com.qrscanner.sheets

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.qrscanner.sheets.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { getSharedPreferences("prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Restore saved values
        binding.etScriptUrl.setText(prefs.getString("script_url", ""))
        binding.etSheetName.setText(prefs.getString("sheet_name", "Sheet1"))
        binding.switchSaveLocally.isChecked = prefs.getBoolean("save_locally", false)

        // Restore dark mode toggle
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        binding.switchDarkMode.isChecked = isDarkMode

        // Save the toggle immediately when changed
        binding.switchSaveLocally.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("save_locally", isChecked).apply()
        }

        // Apply dark mode immediately when toggled
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndFinish()
            }
        })
    }

    private fun saveAndFinish() {
        val url = binding.etScriptUrl.text.toString().trim()
        val name = binding.etSheetName.text.toString().trim().ifEmpty { "Sheet1" }

        if (url.isNotEmpty() && !url.startsWith("https://script.google.com/")) {
            Toast.makeText(this, getString(R.string.msg_invalid_script_url), Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit()
            .putString("script_url", url)
            .putString("sheet_name", name)
            .apply()

        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        saveAndFinish()
        return true
    }
}
