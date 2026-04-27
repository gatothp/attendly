package com.qrscanner.sheets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.qrscanner.sheets.databinding.ActivityInfoBinding

class InfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        binding.tvVersion.text = "Version ${pInfo.versionName}"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
