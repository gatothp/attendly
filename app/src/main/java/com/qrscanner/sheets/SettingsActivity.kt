package com.qrscanner.sheets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.qrscanner.sheets.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy { getSharedPreferences("prefs", MODE_PRIVATE) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

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

        // Restore language selection
        val currentLang = LocaleHelper.getSavedLanguage(this)
        if (currentLang == LocaleHelper.LANG_ENGLISH) {
            binding.radioEnglish.isChecked = true
        } else {
            binding.radioIndonesian.isChecked = true
        }

        // Build "Paste ... sheet name. See help" as a single line with clickable "See help"
        setupScriptUrlHelpText()

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

        // Language selection — restart app to apply
        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val newLang = when (checkedId) {
                R.id.radioEnglish -> LocaleHelper.LANG_ENGLISH
                else -> LocaleHelper.LANG_INDONESIAN
            }
            val prevLang = LocaleHelper.getSavedLanguage(this)
            if (newLang != prevLang) {
                LocaleHelper.saveLanguage(this, newLang)
                restartApp()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndFinish()
            }
        })
    }

    private fun setupScriptUrlHelpText() {
        val hint = getString(R.string.hint_script_url_help)
        val seeHelp = getString(R.string.label_see_help)
        val full = "$hint $seeHelp"
        val spannable = SpannableString(full)
        val start = hint.length + 1  // position right after the space
        val primaryColor = getColor(R.color.colorPrimary)

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@SettingsActivity, HelpActivity::class.java)
                    intent.putExtra(HelpActivity.EXTRA_SCROLL_TO_GOOGLE, true)
                    startActivity(intent)
                }
                override fun updateDrawState(ds: TextPaint) {
                    ds.color = primaryColor
                    ds.isFakeBoldText = true
                    ds.isUnderlineText = false
                }
            },
            start,
            full.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvScriptUrlHelp.text = spannable
        binding.tvScriptUrlHelp.movementMethod = LinkMovementMethod.getInstance()
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

    /** Restart the entire app so the new locale takes effect everywhere. */
    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
