package com.example.todolistapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

// Объект для управления темной и светлой темами приложения
object ThemeManager {

    private const val PREF_NAME = "theme_prefs"     // Имя файла с настройками
    private const val PREF_DARK_MODE = "dark_mode"  // Ключ для хранения состояния темы

    // Получаем SharedPreferences с приватным режимом доступа
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Проверяем, включена ли тёмная тема (по умолчанию - false)
    fun isDarkMode(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREF_DARK_MODE, false)
    }

    // Устанавливаем тёмную или светлую тему
    fun setDarkMode(context: Context, isDark: Boolean) {
        // Сохраняем выбор в SharedPreferences
        getSharedPreferences(context).edit {
            putBoolean(PREF_DARK_MODE, isDark)
        }

        // Выбираем соответствующий режим AppCompatDelegate
        val mode = if (isDark) {
            AppCompatDelegate.MODE_NIGHT_YES   // Тёмная тема
        } else {
            AppCompatDelegate.MODE_NIGHT_NO    // Светлая тема
        }

        // Устанавливаем глобально для приложения
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
