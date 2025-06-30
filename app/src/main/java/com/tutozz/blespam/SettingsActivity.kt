package com.tutozz.blespam

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.bumptech.glide.Glide
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        setAppLanguage(sharedPref.getString("language", Locale.getDefault().language) ?: "en")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Set app version
        val versionTextView = findViewById<TextView>(R.id.text_app_version)
        versionTextView.text = getString(R.string.app_version, getAppVersion())

        // Setup language spinner
        val languageSpinner = findViewById<Spinner>(R.id.language_spinner)
        setupLanguageSpinner(languageSpinner)

        // Setup logo animation toggle
        val logoImageView = findViewById<ImageView>(R.id.logo)
        val animationSwitch = findViewById<Switch>(R.id.switch_logo_animation)

        // Load initial logo state
        val isAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        animationSwitch.isChecked = isAnimationEnabled
        updateLogoImage(logoImageView, isAnimationEnabled)

        // Handle switch toggle
        animationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit {
                putBoolean("logo_animation", isChecked)
                apply()
            }
            updateLogoImage(logoImageView, isChecked)
        }
    }

    private fun updateLogoImage(imageView: ImageView, isAnimationEnabled: Boolean) {
        if (isAnimationEnabled) {
            Glide.with(this)
                .load(R.drawable.anim)
                .into(imageView)
        } else {
            Glide.with(this)
                .load(R.drawable.logo)
                .into(imageView)
        }
    }

    private fun setupLanguageSpinner(spinner: Spinner) {
        val languageCodes = resources.getStringArray(R.array.available_language_codes)
        val languageNames = resources.getStringArray(R.array.available_language_names)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languageNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set current language selection
        val currentLang = sharedPref.getString("language", Locale.getDefault().language) ?: "en"
        val position = languageCodes.indexOfFirst { it == currentLang }.coerceAtLeast(0)
        spinner.setSelection(position)

        spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLang = languageCodes[position]
                if (selectedLang != getCurrentLanguage()) {
                    changeLanguage(selectedLang)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun changeLanguage(languageCode: String) {
        sharedPref.edit {
            putString("language", languageCode)
            apply()
        }
        setAppLanguage(languageCode)
        recreate()
    }

    private fun setAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)

        // Update context for activity
        createConfigurationContext(config)
    }

    private fun getCurrentLanguage(): String {
        return sharedPref.getString("language", Locale.getDefault().language) ?: "en"
    }

    fun openBugReportActivity(view: View) {
        val intent = Intent(this, BugReportActivity::class.java)
        startActivity(intent)
    }

    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0"
        }
    }
}