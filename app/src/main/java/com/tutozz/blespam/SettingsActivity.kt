package com.tutozz.blespam

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
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
    private var isSettingsChanged = false
    private lateinit var saveButton: Button
    private var pendingLanguage: String? = null
    private var pendingAnimationEnabled: Boolean? = null
    private var originalLanguage: String? = null
    private var originalAnimationEnabled: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)

        originalLanguage = sharedPref.getString("language", Locale.getDefault().language) ?: "en"
        originalAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        setAppLanguage(originalLanguage!!)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val versionTextView = findViewById<TextView>(R.id.text_app_version)
        versionTextView.text = getString(R.string.app_version, getAppVersion())

        saveButton = findViewById<Button>(R.id.save_button)
        saveButton.visibility = View.GONE
        saveButton.setOnClickListener {
            if (isSettingsChanged) {
                pendingLanguage?.let { lang ->
                    sharedPref.edit {
                        putString("language", lang)
                        commit()
                    }
                    setAppLanguage(lang)
                }
                pendingAnimationEnabled?.let { isEnabled ->
                    sharedPref.edit {
                        putBoolean("logo_animation", isEnabled)
                        commit()
                    }
                    updateLogoImage(findViewById(R.id.logo), isEnabled)
                }
                isSettingsChanged = false
                saveButton.visibility = View.GONE
                pendingLanguage = null
                pendingAnimationEnabled = null
                restartApp()
            }
        }

        val languageSpinner = findViewById<Spinner>(R.id.language_spinner)
        setupLanguageSpinner(languageSpinner)

        val logoImageView = findViewById<ImageView>(R.id.logo)
        val animationSwitch = findViewById<Switch>(R.id.switch_logo_animation)

        val isAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        animationSwitch.isChecked = isAnimationEnabled
        updateLogoImage(logoImageView, isAnimationEnabled)

        var logoClickCount = 0
        var lastClickTime = 0L
        logoImageView.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastClickTime > 2000) {
                logoClickCount = 0
            }
            lastClickTime = currentTime
            logoClickCount++
            if (logoClickCount >= 5) {
                logoClickCount = 0
                if (ICONCLICK != null) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(ICONCLICK)
                    }
                    startActivity(intent)
                }
            }
        }

        animationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalAnimationEnabled) {
                isSettingsChanged = true
                pendingAnimationEnabled = isChecked
                saveButton.visibility = View.VISIBLE
            } else {
                pendingAnimationEnabled = null
                if (pendingLanguage == null) {
                    isSettingsChanged = false
                    saveButton.visibility = View.GONE
                }
            }
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

        val currentLang = sharedPref.getString("language", Locale.getDefault().language) ?: "en"
        val position = languageCodes.indexOfFirst { it == currentLang }.coerceAtLeast(0)
        spinner.setSelection(position)

        spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLang = languageCodes[position]
                if (selectedLang != originalLanguage) {
                    isSettingsChanged = true
                    pendingLanguage = selectedLang
                    saveButton.visibility = View.VISIBLE
                } else {
                    pendingLanguage = null
                    if (pendingAnimationEnabled == null) {
                        isSettingsChanged = false
                        saveButton.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun setAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
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

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}