package com.qrscanner.sheets

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import androidx.appcompat.app.AppCompatActivity
import com.qrscanner.sheets.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCROLL_TO_GOOGLE = "scroll_to_google"
    }

    private lateinit var binding: ActivityHelpBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        makeStep3UrlClickable()

        // If launched from Settings, scroll to the Configuring Google section
        if (intent.getBooleanExtra(EXTRA_SCROLL_TO_GOOGLE, false)) {
            binding.tvSectionConfigGoogle.post {
                binding.scrollView.smoothScrollTo(0, binding.tvSectionConfigGoogle.top)
            }
        }
    }

    private fun makeStep3UrlClickable() {
        val url = "https://s.id/gappscr"
        val fullText = getString(R.string.help_gstep3_desc)
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(url)
        if (start >= 0) {
            spannable.setSpan(
                URLSpan(url),
                start,
                start + url.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.tvHelpGstep3Desc.text = spannable
        binding.tvHelpGstep3Desc.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
