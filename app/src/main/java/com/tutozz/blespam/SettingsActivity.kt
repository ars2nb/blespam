package com.tutozz.blespam

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.bumptech.glide.Glide
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.util.Locale
import com.tutozz.blespam.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var isSettingsChanged = false
    private lateinit var saveButton: Button

    private var pendingLanguage: String? = null
    private var pendingAnimationEnabled: Boolean? = null
    private var pendingTheme: String? = null
    private var pendingUseMaterial: Boolean? = null
    private var pendingVibrationEnabled: Boolean? = null

    private var originalLanguage: String = "en"
    private var originalAnimationEnabled: Boolean = false
    private var originalTheme: String = "auto"
    private var originalUseMaterial: Boolean = false
    private var originalVibrationEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)

        originalLanguage = sharedPref.getString("language", Locale.getDefault().language) ?: "en"
        originalAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        originalTheme = sharedPref.getString("theme", "auto") ?: "auto"
        originalUseMaterial = sharedPref.getBoolean("use_material", defaultUseMaterial())
        originalVibrationEnabled = sharedPref.getBoolean("vibration_enabled", true)

        setAppLanguage(originalLanguage)
        setAppTheme(originalTheme)

        super.onCreate(savedInstanceState)

        val useMaterialNow = sharedPref.getBoolean("use_material", defaultUseMaterial())
        setContentView(
            if (useMaterialNow) R.layout.activity_settings_material
            else R.layout.activity_settings_legacy
        )

        val versionTextView = findViewById<TextView>(R.id.text_app_version)
        versionTextView.text = getString(R.string.app_version, getAppVersion())

        saveButton = findViewById(R.id.save_button)
        saveButton.visibility = View.GONE
        saveButton.setOnClickListener {
            if (isSettingsChanged) {
                pendingLanguage?.let { lang ->
                    sharedPref.edit { putString("language", lang) }
                }
                pendingAnimationEnabled?.let { isEnabled ->
                    sharedPref.edit { putBoolean("logo_animation", isEnabled) }
                }
                pendingTheme?.let { theme ->
                    sharedPref.edit { putString("theme", theme) }
                }
                pendingUseMaterial?.let { useMaterial ->
                    sharedPref.edit { putBoolean("use_material", useMaterial) }
                }
                pendingVibrationEnabled?.let { isEnabled ->
                    sharedPref.edit { putBoolean("vibration_enabled", isEnabled) }
                }

                isSettingsChanged = false
                pendingLanguage = null
                pendingAnimationEnabled = null
                pendingTheme = null
                pendingUseMaterial = null
                pendingVibrationEnabled = null
                saveButton.visibility = View.GONE

                restartApp()
            }
        }

        if (useMaterialNow) {
            initMaterialViews()
        } else {
            initLegacyViews()
        }
    }

    private fun defaultUseMaterial(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    @SuppressLint("WrongViewCast")
    private fun initMaterialViews() {
        val languageAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.language_spinner)
        setupLanguageSpinner(languageAutoComplete)

        val themeAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.theme_spinner)
        setupThemeSpinner(themeAutoComplete)

        val logoImageView = findViewById<ImageView>(R.id.logo)
        val animationSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_logo_animation)
        val materialSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.material_switch)
        val vibrationSwitch = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_vibration)

        val isAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        animationSwitch.isChecked = isAnimationEnabled
        updateLogoImage(logoImageView, isAnimationEnabled)

        materialSwitch.isChecked = originalUseMaterial
        materialSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalUseMaterial) {
                pendingUseMaterial = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingUseMaterial = null
                checkIfSettingsChanged()
            }
        }

        val isVibrationEnabled = originalVibrationEnabled
        vibrationSwitch.isChecked = isVibrationEnabled
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalVibrationEnabled) {
                pendingVibrationEnabled = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingVibrationEnabled = null
                checkIfSettingsChanged()
            }
        }

        setupLogoClickListener(logoImageView)
        setupMaterialAnimationSwitch(animationSwitch)
    }

    private fun initLegacyViews() {
        val languageSpinner = findViewById<Spinner>(R.id.language_spinner)
        val themeSpinner = findViewById<Spinner>(R.id.theme_spinner)
        val logoImageView = findViewById<ImageView>(R.id.logo)
        val animationSwitch = findViewById<Switch>(R.id.switch_logo_animation)
        val materialSwitch = findViewById<Switch>(R.id.material_switch)
        val vibrationSwitch = findViewById<Switch>(R.id.switch_vibration)

        val isAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        animationSwitch.isChecked = isAnimationEnabled
        updateLogoImage(logoImageView, isAnimationEnabled)

        materialSwitch.isChecked = originalUseMaterial
        materialSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalUseMaterial) {
                pendingUseMaterial = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingUseMaterial = null
                checkIfSettingsChanged()
            }
        }

        val isVibrationEnabled = originalVibrationEnabled
        vibrationSwitch.isChecked = isVibrationEnabled
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalVibrationEnabled) {
                pendingVibrationEnabled = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingVibrationEnabled = null
                checkIfSettingsChanged()
            }
        }

        setupLanguageSpinnerLegacy(languageSpinner)
        setupThemeSpinnerLegacy(themeSpinner)
        setupLogoClickListener(logoImageView)
        setupLegacyAnimationSwitch(animationSwitch)
    }

    private fun setupLanguageSpinnerLegacy(spinner: Spinner) {
        val languageCodes = resources.getStringArray(R.array.available_language_codes)
        val languageNames = resources.getStringArray(R.array.available_language_names)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter

        val position = languageCodes.indexOf(originalLanguage)
        if (position >= 0) {
            spinner.setSelection(position)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLang = languageCodes[position]
                if (selectedLang != originalLanguage) {
                    pendingLanguage = selectedLang
                    isSettingsChanged = true
                    saveButton.visibility = View.VISIBLE
                } else {
                    pendingLanguage = null
                    checkIfSettingsChanged()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupThemeSpinnerLegacy(spinner: Spinner) {
        val themeCodes = resources.getStringArray(R.array.theme_codes)
        val themeNames = resources.getStringArray(R.array.theme_names)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter

        val position = themeCodes.indexOf(originalTheme)
        if (position >= 0) {
            spinner.setSelection(position)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedTheme = themeCodes[position]
                if (selectedTheme != originalTheme) {
                    pendingTheme = selectedTheme
                    isSettingsChanged = true
                    saveButton.visibility = View.VISIBLE
                } else {
                    pendingTheme = null
                    checkIfSettingsChanged()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupMaterialAnimationSwitch(switch: com.google.android.material.materialswitch.MaterialSwitch) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalAnimationEnabled) {
                pendingAnimationEnabled = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingAnimationEnabled = null
                checkIfSettingsChanged()
            }
        }
    }

    private fun setupLegacyAnimationSwitch(switch: Switch) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != originalAnimationEnabled) {
                pendingAnimationEnabled = isChecked
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingAnimationEnabled = null
                checkIfSettingsChanged()
            }
        }
    }

    private fun setupLogoClickListener(logoImageView: ImageView) {
        var lastClickTime = 0L
        logoImageView.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 500) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(AppConfig.ICONCLICK))
                startActivity(intent)
            }
            lastClickTime = currentTime
        }
    }

    private fun setupLanguageSpinner(autoComplete: MaterialAutoCompleteTextView) {
        val languageCodes = resources.getStringArray(R.array.available_language_codes)
        val languageNames = resources.getStringArray(R.array.available_language_names)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languageNames)
        autoComplete.setAdapter(adapter)
        autoComplete.inputType = 0
        autoComplete.keyListener = null

        val position = languageCodes.indexOf(originalLanguage)
        if (position >= 0) {
            autoComplete.setText(languageNames[position], false)
        }

        autoComplete.setOnClickListener {
            autoComplete.showDropDown()
        }

        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedLang = languageCodes[position]
            val selectedName = languageNames[position]

            if (selectedLang != originalLanguage) {
                pendingLanguage = selectedLang
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingLanguage = null
                checkIfSettingsChanged()
            }
            autoComplete.setText(selectedName, false)
            autoComplete.dismissDropDown()
        }
    }

    private fun setupThemeSpinner(autoComplete: MaterialAutoCompleteTextView) {
        val themeCodes = resources.getStringArray(R.array.theme_codes)
        val themeNames = resources.getStringArray(R.array.theme_names)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, themeNames)
        autoComplete.setAdapter(adapter)
        autoComplete.inputType = 0
        autoComplete.keyListener = null

        val position = themeCodes.indexOf(originalTheme)
        if (position >= 0) {
            autoComplete.setText(themeNames[position], false)
        }

        autoComplete.setOnClickListener {
            autoComplete.showDropDown()
        }

        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedTheme = themeCodes[position]
            val selectedName = themeNames[position]

            if (selectedTheme != originalTheme) {
                pendingTheme = selectedTheme
                isSettingsChanged = true
                saveButton.visibility = View.VISIBLE
            } else {
                pendingTheme = null
                checkIfSettingsChanged()
            }
            autoComplete.setText(selectedName, false)
            autoComplete.dismissDropDown()
        }
    }

    private fun checkIfSettingsChanged() {
        isSettingsChanged = pendingLanguage != null ||
                pendingAnimationEnabled != null ||
                pendingTheme != null ||
                pendingUseMaterial != null ||
                pendingVibrationEnabled != null
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
            val isDarkTheme = (resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            val animResource = if (isDarkTheme) {
                R.drawable.anim_frames_white
            } else {
                R.drawable.anim_frames_black
            }

            imageView.setImageResource(animResource)

            imageView.post {
                (imageView.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
            }
        } else {
            imageView.setImageResource(R.drawable.logo)
        }
    }


    private fun setAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun openBugReportActivity(view: View) {
        val intent = Intent(this, BugReportActivity::class.java)
        startActivity(intent)
    }


    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0"
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
