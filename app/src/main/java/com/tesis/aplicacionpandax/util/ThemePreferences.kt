package com.tesis.aplicacionpandax.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate // Mantener para las constantes
import androidx.preference.PreferenceManager

object ThemePreferences {
    private const val PREF_NIGHT_MODE = "pref_night_mode"

    // Constantes para los modos
    const val MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO
    const val MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES
    const val MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // O usar -1 si prefieres

    // Función saveThemePreference MODIFICADA
    fun saveThemePreference(context: Context, mode: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putInt(PREF_NIGHT_MODE, mode).apply()
        // Ya NO se llama a AppCompatDelegate.setDefaultNightMode(mode) aquí
    }

    fun getCurrentThemeMode(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(PREF_NIGHT_MODE, MODE_SYSTEM) // Sigue al sistema por defecto
    }

    fun modeToText(mode: Int): String {
        return when (mode) {
            MODE_LIGHT -> "Claro"
            MODE_DARK -> "Oscuro"
            else -> "Seguir Sistema"
        }
    }
}