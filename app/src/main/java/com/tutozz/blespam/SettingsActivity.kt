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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.bumptech.glide.Glide
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var isSettingsChanged = false
    private lateinit var saveButton: Button
    private var pendingLanguage: String? = null
    private var pendingAnimationEnabled: Boolean? = null
    private var pendingTheme: String? = null
    private var originalLanguage: String? = null
    private var originalAnimationEnabled: Boolean? = null
    private var originalTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)

        originalLanguage = sharedPref.getString("language", Locale.getDefault().language) ?: "en"
        originalAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        originalTheme = sharedPref.getString("theme", "auto") ?: "auto"
        setAppLanguage(originalLanguage!!)
        setAppTheme(originalTheme!!)

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
                pendingTheme?.let { theme ->
                    sharedPref.edit {
                        putString("theme", theme)
                        commit()
                    }
                    setAppTheme(theme)
                }
                isSettingsChanged = false
                saveButton.visibility = View.GONE
                pendingLanguage = null
                pendingAnimationEnabled = null
                pendingTheme = null
                restartApp()
            }
        }

        val languageSpinner = findViewById<Spinner>(R.id.language_spinner)
        setupLanguageSpinner(languageSpinner)

        val themeSpinner = findViewById<Spinner>(R.id.theme_spinner)
        setupThemeSpinner(themeSpinner)

        val logoImageView = findViewById<ImageView>(R.id.logo)
        val animationSwitch = findViewById<Switch>(R.id.switch_logo_animation)

        val isAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        animationSwitch.isChecked = isAnimationEnabled
        updateLogoImage(logoImageView, isAnimationEnabled)

        var lastClickTime = 0L
        logoImageView.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 500) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(AppConfig.ICONCLICK))
                startActivity(intent)
            }
            lastClickTime = currentTime
        }


        animationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalAnimationEnabled) {
                isSettingsChanged = true
                pendingAnimationEnabled = isChecked
                saveButton.visibility = View.VISIBLE
            } else {
                pendingAnimationEnabled = null
                checkIfSettingsChanged()
            }
        }
    }

    private fun setupThemeSpinner(spinner: Spinner) {
        val themeCodes = resources.getStringArray(R.array.theme_codes)
        val themeNames = resources.getStringArray(R.array.theme_names)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            themeNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val currentTheme = sharedPref.getString("theme", "auto") ?: "auto"
        val position = themeCodes.indexOfFirst { it == currentTheme }.coerceAtLeast(0)
        spinner.setSelection(position)

        spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTheme = themeCodes[position]
                if (selectedTheme != originalTheme) {
                    isSettingsChanged = true
                    pendingTheme = selectedTheme
                    saveButton.visibility = View.VISIBLE
                } else {
                    pendingTheme = null
                    checkIfSettingsChanged()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun checkIfSettingsChanged() {
        isSettingsChanged = pendingLanguage != null || pendingAnimationEnabled != null || pendingTheme != null
        saveButton.visibility = if (isSettingsChanged) View.VISIBLE else View.GONE
    }

    private fun setAppTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
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
                    checkIfSettingsChanged()
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