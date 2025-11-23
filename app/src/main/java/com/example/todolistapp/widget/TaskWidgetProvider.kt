package com.example.todolistapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.todolistapp.R
import com.example.todolistapp.activities.MainActivity

// AppWidgetProvider - базовый класс для создания виджетов Android (живут на главном экране)
// Виджет - это мини-окно приложения которое показывает задачи без открытия самого приложения
class TaskWidgetProvider : AppWidgetProvider() {

    // onUpdate() вызывается системой когда:
    // 1. Виджет добавляется на главный экран впервые
    // 2. Время обновления виджета истекло (по расписанию)
    // 3. Приложение явно запрашивает обновление через AppWidgetManager
    override fun onUpdate(
        context: Context,                      // Контекст приложения
        appWidgetManager: AppWidgetManager,     // Менеджер виджетов для обновления
        appWidgetIds: IntArray                  // Массив id всех экземпляров виджета (пользователь может добавить несколько)
    ) {
        // Проходим по всем экземплярам виджета и обновляем каждый
        // Пример: если пользователь добавил виджет дважды - обновятся оба
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // Обновляет один экземпляр виджета
    @Suppress("DEPRECATION")  // Подавляет предупреждение о deprecated API (работает на всех версиях)
    private fun updateAppWidget(
        context: Context,                   // Контекст для доступа к ресурсам и сервисам
        appWidgetManager: AppWidgetManager,  // Менеджер для обновления виджета
        appWidgetId: Int                    // Уникальный id конкретного экземпляра виджета
    ) {
        // RemoteViews - специальный объект для работы с UI виджета
        // Виджеты не могут использовать обычные View - только RemoteViews
        // context.packageName - имя пакета приложения (com.example.todolistapp)
        // R.layout.widget_task_list - XML layout виджета
        val views = RemoteViews(context.packageName, R.layout.widget_task_list)

        // Создаём Intent для открытия MainActivity при клике на виджет
        val intent = Intent(context, MainActivity::class.java)

        // PendingIntent - обёртка над Intent для отложенного запуска системой
        // Когда пользователь кликнет на виджет - система запустит MainActivity
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,  // requestCode (не используется здесь)
            intent,
            // FLAG_UPDATE_CURRENT - обновляет существующий PendingIntent
            // FLAG_IMMUTABLE - нельзя изменить после создания (требование Android 12+)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Привязываем PendingIntent к контейнеру виджета
        // При клике на R.id.widgetContainer - откроется MainActivity
        views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

        // Intent для запуска TaskWidgetService
        // TaskWidgetService - RemoteViewsService который загружает список задач из БД
        val serviceIntent = Intent(context, TaskWidgetService::class.java)

        // setRemoteAdapter() привязывает ListView виджета к RemoteViewsService
        // TaskWidgetService будет создавать RemoteViews для каждой задачи в списке
        // Это как RecyclerView.Adapter, но для виджетов
        views.setRemoteAdapter(R.id.widgetListView, serviceIntent)

        // Обновляем виджет с новыми RemoteViews
        // appWidgetManager применяет изменения к виджету на главном экране
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Уведомляем виджет что данные ListView изменились
        // Это заставит виджет перезагрузить список задач через TaskWidgetService
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView)
    }
}
