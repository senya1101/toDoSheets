package com.example.todolistapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.example.todolistapp.R
import com.example.todolistapp.database.AppDatabase
import com.example.todolistapp.models.Task
import com.example.todolistapp.repository.TaskRepository
import com.example.todolistapp.utils.BackupManager
import com.example.todolistapp.utils.DataExporter
import com.example.todolistapp.utils.Result
import com.example.todolistapp.utils.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Экран отладки (для разработки и тестирования)
// Позволяет быстро добавлять тестовые данные, проверять БД, делать бэкапы
class DebugActivity : AppCompatActivity() {

    // TextView для вывода логов (что происходит в реальном времени)
    private lateinit var logTextView: TextView

    // Кнопки для различных действий
    private lateinit var btnAddTasks: Button // Добавить 100 тестовых задач
    private lateinit var btnExportJSON: Button // Экспорт данных в JSON
    private lateinit var btnCheckDB: Button // Проверить состояние БД
    private lateinit var btnClearDB: Button // Очистить всю БД
    private lateinit var btnBackup: Button // Создать резервную копию БД
    private lateinit var btnRestore: Button // Восстановить БД из копии
    private lateinit var btnBack: ImageView // Кнопка назад

    // ScrollView для прокрутки логов
    private lateinit var scrollView: ScrollView

    // Репозиторий для работы с БД
    private lateinit var repository: TaskRepository

    // Переключатель тёмной темы
    private lateinit var switchDarkMode: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        try {
            // Инициализируем все View элементы
            btnAddTasks = findViewById(R.id.btnAddTasks)
            btnExportJSON = findViewById(R.id.btnExportJSON)
            btnCheckDB = findViewById(R.id.btnCheckDB)
            btnClearDB = findViewById(R.id.btnClearDB)
            btnBackup = findViewById(R.id.btnBackup)
            btnRestore = findViewById(R.id.btnRestore)
            btnBack = findViewById(R.id.btnBack)
            switchDarkMode = findViewById(R.id.switchDarkMode)

            // Инициализируем БД и репозиторий
            val database = AppDatabase.getDatabase(this)
            repository = TaskRepository(
                database.taskDao(),
                database.tagDao(),
                database.taskTagDao()
            )

            // Настраиваем обработчики кнопок
            setupListeners()

            // Пишем в лог что всё загрузилось
            log("✅ DebugActivity успешно загружен")
        } catch (e: Exception) {
            // Если что-то пошло не так - логируем ошибку
            e.printStackTrace()
        }
    }

    // Настройка обработчиков всех кнопок
    private fun setupListeners() {
        // Кнопка добавления 100 тестовых задач (для тестирования производительности)
        btnAddTasks.setOnClickListener {
            addTestTasks()
        }

        // Кнопка экспорта данных в JSON файл
        btnExportJSON.setOnClickListener {
            exportData()
        }

        // Кнопка проверки состояния БД (сколько задач, какие есть)
        btnCheckDB.setOnClickListener {
            checkDatabase()
        }

        // Кнопка очистки БД (удаляет все задачи)
        btnClearDB.setOnClickListener {
            showClearConfirmation() // Сначала показываем диалог подтверждения
        }

        // Кнопка назад
        btnBack.setOnClickListener {
            finish()
        }

        // Кнопка создания резервной копии БД
        btnBackup.setOnClickListener {
            backupDatabase()
        }

        // Кнопка восстановления БД из резервной копии
        btnRestore.setOnClickListener {
            restoreDatabase()
        }

        // Переключатель тёмной темы
        switchDarkMode.isChecked = ThemeManager.isDarkMode(this) // Устанавливаем текущее состояние
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkMode(this, isChecked) // Сохраняем выбор пользователя
            recreate() // Перезагружаем Activity чтобы применить тему
        }
    }

    // Добавляет 100 тестовых задач в БД (для проверки производительности)
    private fun addTestTasks() {
        lifecycleScope.launch {
            log("⏳ Добавляю 100 задач...")
            val startTime = System.currentTimeMillis() // Засекаем время начала

            // Создаём 100 задач в цикле
            repeat(100) { i ->
                val task = Task(
                    title = "Тестовая задача ${i + 1}",
                    description = "Описание задачи номер ${i + 1}",
                    isCompleted = i % 2 == 0 // Каждая вторая задача завершена
                )
                repository.insert(task)
            }

            // Считаем сколько времени заняло
            val duration = System.currentTimeMillis() - startTime
            log("✅ Добавлено 100 задач за ${duration}ms")
        }
    }

    // Экспортирует все активные задачи в JSON файл
    private fun exportData() {
        lifecycleScope.launch {
            log("📁 Экспорт данных...")
            val startTime = System.currentTimeMillis()

            try {
                log("1️⃣ Собираю задачи...")
                // Загружаем все активные задачи из БД
                val allTasks = repository.allActiveTasks.first()
                log("2️⃣ Найдено задач: ${allTasks.size}")

                log("3️⃣ Запускаю экспорт...")
                // Вызываем утилиту экспорта (создаёт JSON файл)
                val result = DataExporter.exportToJson(this@DebugActivity, allTasks, emptyList())
                val duration = System.currentTimeMillis() - startTime
                log("4️⃣ Экспорт завершён")

                // Проверяем результат экспорта
                when (result) {
                    is Result.Success -> {
                        log("✅ Успех!")
                        log("📄 Файл: ${result.message}")

                        // Проверяем размер созданного файла
                        val file = java.io.File(result.message.toString())
                        if (file.exists()) {
                            log("📦 Размер файла: ${file.length()} bytes")
                        }
                        log("⏱️ Время выполнения: ${duration}ms")
                    }
                    is Result.Error -> {
                        log("❌ Ошибка экспорта")
                        log("💬 ${result.message}")
                    }
                }
            } catch (e: Exception) {
                log("❌ Исключение: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Проверяет состояние БД (количество активных и завершённых задач)
    private fun checkDatabase() {
        lifecycleScope.launch {
            log("🔍 Проверка базы данных...")
            val startTime = System.currentTimeMillis()

            try {
                // Загружаем активные и завершённые задачи
                val activeTasks = repository.allActiveTasks.first()
                val completedTasks = repository.allCompletedTasks.first()

                // Считаем статистику
                val activeCount = activeTasks.size
                val completedCount = completedTasks.size
                val totalCount = activeCount + completedCount
                val duration = System.currentTimeMillis() - startTime

                // Выводим результаты
                log("✅ Результаты проверки:")
                log("📋 Всего задач: $totalCount")
                log("⚡ Активных: $activeCount")
                log("✓ Завершённых: $completedCount")
                log("⏱️ Время: ${duration}ms")
            } catch (e: Exception) {
                log("❌ Ошибка проверки БД: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Показывает диалог подтверждения перед очисткой БД
    private fun showClearConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Очистить базу данных?")
            .setMessage("Все задачи будут удалены безвозвратно. Это действие нельзя отменить!")
            .setPositiveButton("Да, удалить") { _, _ ->
                clearDatabase() // Если пользователь подтвердил - очищаем
            }
            .setNegativeButton("Отмена", null) // Кнопка отмены ничего не делает
            .setIcon(android.R.drawable.ic_dialog_alert) // Иконка предупреждения
            .show()
    }

    // Очищает всю БД (удаляет все задачи)
    private fun clearDatabase() {
        lifecycleScope.launch {
            log("🗑️ Очистка базы данных...")
            val startTime = System.currentTimeMillis()
            try {
                // Вызываем метод удаления всех задач
                repository.deleteAll()
                val duration = System.currentTimeMillis() - startTime
                log("✅ База данных полностью очищена")
                log("⏱️ Время: ${duration}ms")
            } catch (e: Exception) {
                log("❌ Ошибка очистки БД: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Создаёт резервную копию БД (сохраняет файл БД в downloads)
    private fun backupDatabase() {
        log("💾 Запускаю резервное копирование...")
        BackupManager.backupDatabase(this) { success, message ->
            log("📦 $message")
            if (success) {
                Toast.makeText(this, "✅ Резервная копия сохранена!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Ошибка резервного копирования", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Восстанавливает БД из резервной копии
    private fun restoreDatabase() {
        // Показываем диалог подтверждения (текущие данные будут заменены)
        AlertDialog.Builder(this)
            .setTitle("⚠️ Восстановить из резервной копии?")
            .setMessage("Текущие данные будут заменены!")
            .setPositiveButton("Восстановить") { _, _ ->
                log("🔄 Восстанавливаю БД из резервной копии...")
                BackupManager.restoreDatabase(this) { success, message ->
                    log("📦 $message")
                    if (success) {
                        Toast.makeText(this, "✅ БД восстановлена! Перезагружаю приложение...", Toast.LENGTH_SHORT).show()

                        // Через 1.5 секунды перезапускаем приложение (чтобы загрузить новые данные)
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }, 1500)
                    } else {
                        Toast.makeText(this, "❌ Ошибка восстановления", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    // Метод для вывода логов в TextView (с timestamp и прокруткой вверх)
    @SuppressLint("SetTextI18n")
    private fun log(message: String) {
        // Дублируем в консоль для отладки
        println("DEBUG LOG: $message")

        runOnUiThread {
            // Добавляем timestamp к каждому сообщению (например "12:34:56.789")
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logLine = "[$timestamp] $message"

            // Добавляем новую строку в начало (новые логи сверху)
            val currentText = logTextView.text.toString()
            val newText = if (currentText.isEmpty()) logLine else "$logLine\n$currentText"
            logTextView.text = newText

            // Ограничиваем количество строк до 50 (чтобы не забивать память)
            val lines = newText.split("\n")
            if (lines.size > 50) {
                logTextView.text = lines.take(50).joinToString("\n")
            }

            // Автоматически прокручиваем ScrollView вверх (к последним логам)
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_UP)
            }
        }
    }
}
