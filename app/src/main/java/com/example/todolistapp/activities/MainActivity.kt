package com.example.todolistapp.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp.R
import com.example.todolistapp.adapters.TaskAdapter
import com.example.todolistapp.database.AppDatabase
import com.example.todolistapp.database.DatabaseInitializer
import com.example.todolistapp.models.Task
import com.example.todolistapp.repository.TaskRepository
import com.example.todolistapp.utils.DataExporter
import com.example.todolistapp.utils.Result
import com.example.todolistapp.utils.TaskNotificationManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

// Главный экран — список задач
class MainActivity : AppCompatActivity() {

    // Репозиторий работы с базой данных
    private lateinit var repository: TaskRepository

    // Адаптер для отображения задач
    private lateinit var taskAdapter: TaskAdapter

    // UI элементы
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var tabAll: TextView
    private lateinit var tabCompleted: TextView
    private lateinit var calendarContainer: FrameLayout
    private lateinit var ivCalendar: ImageView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var btnSort: ImageButton
    private lateinit var btnMenu: ImageButton

    // Выбранная дата для фильтрации
    private var selectedDate: Calendar = Calendar.getInstance()

    // Флаг: показываем завершённые или активные задачи
    private var showingCompletedTasks = false

    // Текущая сортировка ("date" или "title")
    private var currentSortOrder = "date"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаём канал уведомлений для Android 8+ (обязательно для пушей)
        TaskNotificationManager.createNotificationChannel(this)

        setContentView(R.layout.activity_main)

        // Инициализация базы и репозитория
        val database = AppDatabase.getDatabase(this)
        repository = TaskRepository(
            database.taskDao(),
            database.tagDao(),
            database.taskTagDao()
        )

        // Инициализация UI элементов
        initViews()
        // Настройка RecyclerView и его адаптера
        setupRecyclerView()
        // Настройка слушателей для кнопок и элементов
        setupListeners()
        // Настройка поиска
        setupSearch()
        // Настройка сортировки
        setupSort()
        // Автоматический бэкап при запуске
        createAutoBackup()

        // Инициализация дефолтных тегов (если их ещё нет)
        lifecycleScope.launch {
            DatabaseInitializer.initializeDefaultTags(database.tagDao())
        }
    }

    // Инициализация всех View по их id
    private fun initViews() {
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        tabAll = findViewById(R.id.tabAll)
        tabCompleted = findViewById(R.id.tabCompleted)
        calendarContainer = findViewById(R.id.calendarContainer)
        ivCalendar = findViewById(R.id.ivCalendar)
        fabAddTask = findViewById(R.id.fabAddTask)
        searchView = findViewById(R.id.searchView)
        btnSort = findViewById(R.id.btnSort)
        btnMenu = findViewById(R.id.btnMenu)
    }

    // Настройка RecyclerView и его адаптера
    private fun setupRecyclerView() {
        // Вертикальный список
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)

        // Создаём адаптер с обработчиками
        taskAdapter = TaskAdapter(
            onTaskChecked = { task, isChecked ->
                // Обработка чекбокса ("выполнить/отменить выполнение")
                lifecycleScope.launch {
                    task.isCompleted = isChecked
                    repository.update(task)
                    repository.updateWidget(this@MainActivity)
                    if (isChecked) {
                        showSnackbar("✅ Задача выполнена")
                    } else {
                        showSnackbar("↶ Задача восстановлена")
                    }
                }
            },
            onTaskEdit = { task ->
                // Переход на AddTaskActivity для редактирования
                val intent = Intent(this, AddTaskActivity::class.java)
                intent.putExtra("TASK", task)
                startActivity(intent)
            },
            onTaskDelete = { task ->
                // Удаление задачи
                lifecycleScope.launch {
                    repository.delete(task)
                    repository.updateWidget(this@MainActivity)
                    showSnackbar("✕ Задача удалена")
                }
            }
        )
        recyclerViewTasks.adapter = taskAdapter

        // Обновление активных задач при изменениях в базе
        lifecycleScope.launch {
            repository.allActiveTasks.collect { tasks ->
                if (!showingCompletedTasks) {
                    val tasksWithTags = loadTagsForTasks(tasks)
                    taskAdapter.submitList(tasksWithTags)
                }
            }
        }

        // Обновление завершённых задач
        lifecycleScope.launch {
            repository.allCompletedTasks.collect { tasks ->
                if (showingCompletedTasks) {
                    val tasksWithTags = loadTagsForTasks(tasks)
                    taskAdapter.submitList(tasksWithTags)
                }
            }
        }
    }

    // Загружает теги для задач, чтобы их показать
    private suspend fun loadTagsForTasks(tasks: List<Task>): List<Task> {
        return tasks.map { task ->
            val taskTags = repository.getTaskTagsForEdit(task.id)
            val tagNames = taskTags.mapNotNull { crossRef ->
                try {
                    repository.getTagNameById(crossRef.tagId)
                } catch (_: Exception) {
                    null
                }
            }
            task.tags = tagNames
            task
        }
    }

    // Открывает диалог выбора даты
    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.LightDatePicker)
            .setTitleText("Выберите дату")
            .setSelection(selectedDate.timeInMillis)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            selectedDate = calendar

            lifecycleScope.launch {
                repository.getTasksByDate(selection).collect { tasks ->
                    val tasksWithTags = loadTagsForTasks(tasks)
                    taskAdapter.submitList(tasksWithTags.toMutableList())
                }
            }
            showSnackbar("📅 Показаны задачи с ${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}")
        }
        picker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    // Обработка кликов по UI
    private fun setupListeners() {
        tabAll.setOnClickListener {
            switchToAllTasks()
        }
        tabCompleted.setOnClickListener {
            switchToCompletedTasks()
        }
        calendarContainer.setOnClickListener {
            showDatePicker()
        }
        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }
        btnMenu.setOnClickListener { view ->
            showPopupMenu(view)
        }
    }

    // Настройка поиска
    private fun setupSearch() {
        searchView.setOnClickListener {
            searchView.isIconified = false
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText ?: ""
                lifecycleScope.launch {
                    if (showingCompletedTasks) {
                        repository.allCompletedTasks.collect { tasks ->
                            val filtered = tasks.filter { it.title.contains(query, ignoreCase = true) }
                            val tasksWithTags = loadTagsForTasks(filtered)
                            taskAdapter.submitList(tasksWithTags.toMutableList())
                        }
                    } else {
                        repository.allActiveTasks.collect { tasks ->
                            val filtered = tasks.filter { it.title.contains(query, ignoreCase = true) }
                            val tasksWithTags = loadTagsForTasks(filtered)
                            taskAdapter.submitList(tasksWithTags.toMutableList())
                        }
                    }
                }
                return true
            }
        })
    }

    // Настройка сортировки
    private fun setupSort() {
        btnSort.setOnClickListener {
            val options = arrayOf("По дате", "По названию")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Сортировка")
                .setSingleChoiceItems(options, if (currentSortOrder == "date") 0 else 1) { dialog, which ->
                    currentSortOrder = if (which == 0) "date" else "title"
                    setupRecyclerView()
                    dialog.dismiss()
                    showSnackbar("Отсортировано по ${options[which].lowercase()}")
                }
                .show()
        }
    }

    // Переключение на вкладку "Все задачи"
    private fun switchToAllTasks() {
        if (showingCompletedTasks) {
            showingCompletedTasks = false
            recyclerViewTasks.adapter = taskAdapter
            recyclerViewTasks.scrollToPosition(0)
            tabAll.setTypeface(null, Typeface.BOLD)
            tabCompleted.setTypeface(null, Typeface.NORMAL)
            setupRecyclerView()
        }
    }

    // Переключение на вкладку "Завершённые"
    private fun switchToCompletedTasks() {
        showingCompletedTasks = true
        tabAll.setTypeface(null, Typeface.NORMAL)
        tabCompleted.setTypeface(null, Typeface.BOLD)
        setupRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                exportTasks()
                true
            }
            R.id.action_import -> {
                importTasks()
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this, DebugActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Экспорт задач в JSON
    private fun exportTasks() {
        lifecycleScope.launch {
            try {
                val allTasks = repository.allActiveTasks.first()
                val result = DataExporter.exportToJson(this@MainActivity, allTasks, emptyList())
                when (result) {
                    is Result.Success -> {
                        val fileName = result.message.toString().substringAfterLast("/")
                        showSnackbar("✅ Экспорт выполнен: $fileName")
                    }
                    is Result.Error -> {
                        showSnackbar("❌ ${result.message}")
                    }
                }
            } catch (e: Exception) {
                showSnackbar("❌ Ошибка: ${e.message}")
            }
        }
    }

    // Импорт задач из файла
    private fun importTasks() {
        lifecycleScope.launch {
            try {
                val backups = DataExporter.getBackupsList(this@MainActivity)
                if (backups.isEmpty()) {
                    showSnackbar("❌ Нет доступных бэкапов")
                    return@launch
                }
                val latestBackup = backups.first()
                when (val result = DataExporter.importFromJson(latestBackup.absolutePath)) {
                    is Result.Success -> {
                        val backupData = result.message as? com.example.todolistapp.utils.BackupData
                        if (backupData != null) {
                            backupData.tasks.forEach { task -> repository.insert(task) }
                            showSnackbar("✅ Импорт выполнен: ${backupData.tasks.size} задач")
                        }
                    }
                    is Result.Error -> {
                        showSnackbar("❌ ${result.message}")
                    }
                }
            } catch (e: Exception) {
                showSnackbar("❌ Ошибка: ${e.message}")
            }
        }
    }

    private fun createAutoBackup() {
        lifecycleScope.launch {
            try {
                val allTasks = repository.allActiveTasks.first()
                DataExporter.exportToJson(this@MainActivity, allTasks, emptyList())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(window.decorView, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        createAutoBackup()
    }

    override fun onResume() {
        super.onResume()
        setupRecyclerView()
        // Проверка дедлайнов и уведомления
        checkAndNotifyDeadlines()
    }

    // Проверяет дедлайны задач, и если есть — показывает уведомление
    private fun checkAndNotifyDeadlines() {
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val activeTasks = repository.allActiveTasks.first()

            // Фильтруем задачи, у которых дедлайн в ближайшие 1 час
            val upcomingTasks = activeTasks.filter { task ->
                task.deadline != null && (task.deadline!! in now..(now + 60 * 60 * 1000))
            }

            // Показываем уведомление для каждой
            upcomingTasks.forEach { task ->
                TaskNotificationManager.showTaskReminder(
                    this@MainActivity,
                    task.title,
                    task.id.toInt() // ID уведомления
                )
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.main_menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    exportTasks()
                    true
                }
                R.id.action_import -> {
                    importTasks()
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, DebugActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
