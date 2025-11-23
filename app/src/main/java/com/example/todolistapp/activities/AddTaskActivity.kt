package com.example.todolistapp.activities

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.View
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.todolistapp.R
import com.example.todolistapp.database.AppDatabase
import com.example.todolistapp.models.Task
import com.example.todolistapp.models.Tag
import com.example.todolistapp.models.TaskTagCrossRef
import com.example.todolistapp.repository.TaskRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.delay

// Экран создания и редактирования задачи
class AddTaskActivity : AppCompatActivity() {
    // UI элементы
    private lateinit var btnBack: ImageView // Кнопка "Назад"
    private lateinit var editTextTask: EditText // Поле ввода названия задачи
    private lateinit var editTextDescription: EditText // Поле ввода описания
    private lateinit var buttonAdd: MaterialButton // Кнопка "Добавить" или "Сохранить"
    private lateinit var buttonCancel: MaterialButton // Кнопка "Отмена"
    private lateinit var deadlineContainer: LinearLayout // Контейнер для выбора дедлайна
    private lateinit var tvDeadline: TextView // Текст с выбранной датой дедлайна
    private lateinit var btnClearDeadline: ImageView // Кнопка очистки дедлайна (крестик)
    private lateinit var chipsGroup: ChipGroup // Группа чипов для выбора тегов

    // Репозиторий для работы с БД
    private lateinit var repository: TaskRepository

    // Множество ID выбранных тегов (используем Set чтобы не было дубликатов)
    private val selectedTagIds = mutableSetOf<Long>()

    // Выбранный дедлайн в миллисекундах (null если не выбран)
    private var selectedDeadline: Long? = null

    // Задача для редактирования (null если создаём новую)
    private var task: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Анимация появления экрана снизу вверх (красиво выглядит)
        window.enterTransition = Slide(Gravity.BOTTOM).apply { duration = 300 }
        // Анимация закрытия экрана сверху вниз
        window.exitTransition = Slide(Gravity.BOTTOM).apply { duration = 250 }

        setContentView(R.layout.activity_add_task)

        // Настраиваем полноэкранный режим (контент под статус-баром)
        setupWindowInsets()

        // Инициализируем БД и репозиторий
        val database = AppDatabase.getDatabase(this)
        repository = TaskRepository(database.taskDao(), database.tagDao(), database.taskTagDao())

        // Получаем задачу из Intent если редактируем существующую
        // Для Android 13+ используем новый метод, для старых версий - deprecated
        task = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("TASK", Task::class.java)
        } else {
            @Suppress("DEPRECATION") // Подавляем предупреждение о deprecated методе
            intent.getSerializableExtra("TASK") as? Task
        }

        // Если task != null значит редактируем, иначе создаём новую
        val isEditMode = task != null

        // Инициализируем все View элементы
        initViews()

        // Настраиваем выбор даты дедлайна
        setupDeadlinePicker()

        // Загружаем теги из БД и показываем их
        initializeTags()

        // Если режим редактирования - заполняем поля данными задачи
        if (isEditMode) {
            // Меняем текст кнопки на "Сохранить"
            buttonAdd.apply {
                text = "Сохранить"
                minHeight = 56
                maxLines = 1
                textSize = 16f
            }
            // Заполняем название и описание
            editTextTask.setText(task!!.title)
            editTextDescription.setText(task!!.description)

            // Если есть дедлайн - показываем его
            task!!.deadline?.let { deadline ->
                selectedDeadline = deadline
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                tvDeadline.text = dateFormat.format(Date(deadline))
                tvDeadline.setTextColor(Color.WHITE)
                btnClearDeadline.visibility = View.VISIBLE // Показываем крестик для очистки
            }
        } else {
            // Режим создания новой задачи
            buttonAdd.apply {
                text = "Добавить"
                minHeight = 56
                maxLines = 1
            }
        }

        // Настраиваем обработчики кнопок
        setupListeners(task, isEditMode)

        // Обработка системной кнопки "Назад"
        setupBackPressedHandler()
    }

    // Настройка полноэкранного режима (контент идёт под статус-бар)
    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Для Android 11+ делаем иконки статус-бара тёмными
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        }
    }

    // Инициализация всех View элементов через findViewById
    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        editTextTask = findViewById(R.id.editTextTask)
        editTextDescription = findViewById(R.id.editTextDescription)
        buttonAdd = findViewById(R.id.buttonAdd)
        buttonCancel = findViewById(R.id.buttonCancel)
        deadlineContainer = findViewById(R.id.deadlineContainer)
        tvDeadline = findViewById(R.id.tvDeadline)
        btnClearDeadline = findViewById(R.id.btnClearDeadline)
        chipsGroup = findViewById(R.id.chipsGroup)
    }

    // Настройка выбора дедлайна и кнопки очистки
    private fun setupDeadlinePicker() {
        // По клику на контейнер открываем календарь
        deadlineContainer.setOnClickListener { showDeadlinePicker() }

        // Кнопка очистки дедлайна (крестик)
        btnClearDeadline.setOnClickListener {
            selectedDeadline = null // Убираем дедлайн
            tvDeadline.text = "" // Очищаем текст
            tvDeadline.setTextColor("#B3FFFFFF".toColorInt()) // Возвращаем прозрачный цвет
            btnClearDeadline.visibility = View.GONE // Прячем крестик
        }
    }

    // Показываем диалог выбора даты
    private fun showDeadlinePicker() {
        val calendar = Calendar.getInstance()
        // Если уже выбран дедлайн - показываем его в календаре
        selectedDeadline?.let { calendar.timeInMillis = it }

        val datePickerDialog = DatePickerDialog(
            this, { _, year, month, dayOfMonth ->
                // Устанавливаем время на 23:59:59 выбранного дня (конец дня)
                calendar.set(year, month, dayOfMonth, 23, 59, 59)
                selectedDeadline = calendar.timeInMillis

                // Форматируем дату в читаемый вид (например "25.12.2025")
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                tvDeadline.text = dateFormat.format(calendar.time)
                tvDeadline.setTextColor(Color.WHITE) // Делаем текст белым
                btnClearDeadline.visibility = View.VISIBLE // Показываем крестик для очистки
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Нельзя выбрать дату в прошлом (только сегодня и будущее)
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    // Загружаем популярные теги из БД или создаём новые если их нет
    private fun initializeTags() {
        lifecycleScope.launch {
            try {
                // Список популярных тегов (эмодзи + название)
                val popularTags = listOf(
                    Tag(name = "🏢 Работа"),
                    Tag(name = "🏠 Дом"),
                    Tag(name = "⭐ Срочно"),
                    Tag(name = "🔴 Важно"),
                    Tag(name = "📚 Обучение"),
                    Tag(name = "🏋️ Здоровье"),
                    Tag(name = "🛒 Покупки"),
                    Tag(name = "🤝 Социум")
                )

                // Загружаем все теги из БД
                val allTagsFromDb = repository.getAllTags().first()
                val tags = mutableListOf<Tag>()

                // Проверяем каждый популярный тег: если есть в БД - берём его, если нет - создаём
                for (popularTag in popularTags) {
                    val existingTag = allTagsFromDb.firstOrNull { dbTag -> dbTag.name == popularTag.name }
                    if (existingTag != null) {
                        tags.add(existingTag) // Тег уже есть в БД
                    } else {
                        // Тега нет - создаём новый
                        val newTagId = repository.insertTag(popularTag)
                        val newTag = popularTag.copy(id = newTagId)
                        tags.add(newTag)
                    }
                }

                // Если редактируем задачу - загружаем её теги и отмечаем их как выбранные
                if (task != null) {
                    val taskTags = repository.getTaskTagsForEdit(task!!.id)
                    selectedTagIds.addAll(taskTags.map { crossRef -> crossRef.tagId })
                }

                // Очищаем группу чипов перед добавлением новых
                chipsGroup.removeAllViews()

                // Создаём чип для каждого тега
                for (tagItem in tags) {
                    val chip = Chip(this@AddTaskActivity).apply {
                        text = tagItem.name // Текст чипа (название тега)
                        isCheckable = true // Чип можно выбирать/снимать выбор
                        isChecked = selectedTagIds.contains(tagItem.id) // Если тег уже выбран - отмечаем
                        tag = tagItem.id // Сохраняем ID тега в tag чипа (для удобства)

                        // Обработка выбора/снятия выбора тега
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                selectedTagIds.add(tagItem.id) // Добавляем в выбранные
                            } else {
                                selectedTagIds.remove(tagItem.id) // Убираем из выбранных
                            }
                        }
                    }
                    chipsGroup.addView(chip) // Добавляем чип в группу
                }
            } catch (e: Exception) {
                e.printStackTrace() // Логируем ошибку если что-то пошло не так
            }
        }
    }

    // Настройка обработчиков кнопок
    private fun setupListeners(taskParam: Task?, isEditMode: Boolean) {
        // Кнопка "Назад" - просто закрываем экран
        btnBack.setOnClickListener { finish() }

        // Кнопка "Отмена" - тоже закрываем экран
        buttonCancel.setOnClickListener { finish() }

        // Кнопка "Добавить" или "Сохранить"
        buttonAdd.setOnClickListener {
            // Получаем текст из полей
            val taskTitle = editTextTask.text.toString().trim()
            val taskDescription = editTextDescription.text.toString().trim()

            // Проверка: название обязательно должно быть
            if (taskTitle.isEmpty()) {
                editTextTask.error = "Введите название"
                editTextTask.requestFocus()
                return@setOnClickListener
            }

            // Если редактируем существующую задачу
            if (isEditMode && taskParam != null) {
                // Обновляем поля задачи
                taskParam.title = taskTitle
                taskParam.description = taskDescription
                taskParam.deadline = selectedDeadline

                // Получаем названия выбранных тегов
                val selectedTagNames = chipsGroup.checkedChipIds.mapNotNull { chipId ->
                    val chip = chipsGroup.findViewById<Chip>(chipId)
                    chip?.text?.toString()
                }
                taskParam.tags = selectedTagNames

                lifecycleScope.launch {
                    try {
                        // Сохраняем изменения задачи
                        repository.update(taskParam)

                        // Удаляем старые связи тегов и создаём новые
                        repository.deleteTaskTags(taskParam.id)
                        for (tagId in selectedTagIds) {
                            repository.insertTaskTag(TaskTagCrossRef(taskParam.id, tagId))
                        }

                        // Обновляем виджет на главном экране
                        repository.updateWidget(this@AddTaskActivity)

                        // Показываем уведомление об успехе
                        Snackbar.make(window.decorView, "Задача обновлена", Snackbar.LENGTH_SHORT).show()
                        finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                // Создаём новую задачу
                val newTask = Task(
                    title = taskTitle,
                    description = taskDescription,
                    isCompleted = false,
                    deadline = selectedDeadline
                )

                // Получаем названия выбранных тегов
                val selectedTagNames = chipsGroup.checkedChipIds.mapNotNull { chipId ->
                    val chip = chipsGroup.findViewById<Chip>(chipId)
                    chip?.text?.toString()
                }
                newTask.tags = selectedTagNames

                lifecycleScope.launch {
                    try {
                        // Сохраняем новую задачу в БД
                        repository.insert(newTask)

                        // Получаем ID только что созданной задачи
                        val newTaskId = repository.getLastTaskId()

                        // Создаём связи между задачей и выбранными тегами
                        for (tagId in selectedTagIds) {
                            repository.insertTaskTag(TaskTagCrossRef(newTaskId, tagId))
                        }

                        // Обновляем виджет
                        repository.updateWidget(this@AddTaskActivity)

                        // Показываем уведомление
                        Snackbar.make(window.decorView, "Задача добавлена", Snackbar.LENGTH_SHORT).show()

                        // Возвращаем результат что задача добавлена
                        setResult(RESULT_OK)

                        // Небольшая задержка чтобы Snackbar успел показаться
                        delay(300)
                        finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Обработка системной кнопки "Назад"
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // Просто закрываем экран
            }
        })
    }
}
