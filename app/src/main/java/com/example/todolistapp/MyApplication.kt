package com.example.todolistapp

import android.app.Application
import com.example.todolistapp.utils.ThemeManager
import com.example.todolistapp.utils.TaskNotificationManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Устанавливаем тему (тёмный/светлый)
        val isDark = ThemeManager.isDarkMode(this)
        ThemeManager.setDarkMode(this, isDark)

        // Создаём канал уведомлений для Android 8+ (обязательно)
        TaskNotificationManager.createNotificationChannel(this)
    }
}
