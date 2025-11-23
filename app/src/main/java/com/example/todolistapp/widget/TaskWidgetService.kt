package com.example.todolistapp.widget

import android.content.Intent
import android.widget.RemoteViewsService

// RemoteViewsService - специальный Service для виджетов с ListView/GridView
// Это посредник между виджетом и данными из БД
// Работает в отдельном процессе (launcher process) для безопасности
class TaskWidgetService : RemoteViewsService() {

    // onGetViewFactory() - единственный метод который нужно переопределить
    // Вызывается системой когда виджет запрашивает данные для ListView
    // intent - Intent который был передан в setRemoteAdapter() из TaskWidgetProvider
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        // Возвращаем factory который будет создавать RemoteViews для каждой задачи
        // applicationContext - используем application context (не activity!) для предотвращения утечек памяти
        // TaskWidgetViewsFactory - наша реализация RemoteViewsFactory (аналог RecyclerView.Adapter)
        return TaskWidgetViewsFactory(this.applicationContext)
    }
}

