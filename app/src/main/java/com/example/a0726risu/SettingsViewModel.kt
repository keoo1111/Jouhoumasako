package com.example.a0726risu

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences(AppConstants.PREFS_SETTINGS, Context.MODE_PRIVATE)

    private val _fontSize = MutableStateFlow(loadFontSize())
    open val fontSize: StateFlow<FontSize> = _fontSize

    private fun loadFontSize(): FontSize {
        val savedName = sharedPreferences.getString(AppConstants.KEY_FONT_SIZE, FontSize.MEDIUM.name)
        return FontSize.values().find { it.name == savedName } ?: FontSize.MEDIUM
    }

    open fun setFontSize(size: FontSize) {
        sharedPreferences.edit {
            putString(AppConstants.KEY_FONT_SIZE, size.name)
        }
        _fontSize.value = size
    }
}
