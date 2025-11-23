package com.example.todolistapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.todolistapp.R
import com.example.todolistapp.activities.MainActivity

object TaskNotificationManager {
    private const val CHANNEL_ID = "todo_channel"
    private const val CHANNEL_NAME = "Todo Notifications" // Имя канала, видно в настройках Android

    // Создаёт канал уведомлений (требуется для Android 8.0 и выше)
    // Вызывать нужно один раз при старте приложения — обычно в Application.onCreate или первой Activity
    // На устройствах с Android ниже 8.0 каналы не нужны — функция ничего не сделает
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Важность канала — средний приоритет (уведомление видно, но без звука и вибрации)
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            // Создаём канал с указанным id, именем и уровнем важности
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
            channel.description = "Уведомления о задачах" // Описание показывается в настройках

            // Получаем системный менеджер уведомлений
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Регистрируем канал (если вызвать несколько раз — ничего страшного, система игнорирует дубли)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Показывает уведомление с напоминанием о задаче (например, по таймеру или дедлайну)
    fun showTaskReminder(context: Context, taskTitle: String, taskId: Int) {
        // Создаём Intent, который откроет MainActivity при клике на уведомление
        val intent = Intent(context, MainActivity::class.java).apply {
            // Если приложение закрыто — создаём новую задачу в стеке
            // Если уже есть задачи — очищаем их, чтобы открытие было "чистым"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Передаём id задачи, чтобы можно было обработать переход к ней в MainActivity
            putExtra("TASK_ID", taskId)
        }

        // PendingIntent оборачивает Intent для отложенного запуска системой при клике на уведомление
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId, // requestCode — делаем уникальным через id задачи, чтобы уведомления не путались
            intent,
            // Обновляем существующий PendingIntent, если он есть, и делаем его неизменяемым (требование Android 12+)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Создаём само уведомление, поддерживаемое на всех версиях Android
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)                  // Иконка в статусбаре
            .setContentTitle("Напоминание о задаче")            // Заголовок
            .setContentText(taskTitle)                           // Текст — название задачи
            .setContentIntent(pendingIntent)                     // Обработчик клика по уведомлению
            .setAutoCancel(true)                                 // Уведомление исчезнет при клике
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)   // Приоритет для старых версий Android
            .build()

        // Получаем менеджер уведомлений и показываем уведомление с id = taskId
        // id уведомления позволит обновлять или удалять конкретное уведомление в будущем
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(taskId, notification)
    }
}
