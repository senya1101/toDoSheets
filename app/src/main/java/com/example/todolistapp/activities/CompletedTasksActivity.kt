package com.example.todolistapp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolistapp.adapters.CompletedTaskAdapter
import com.example.todolistapp.database.AppDatabase
import com.example.todolistapp.databinding.ActivityCompletedTasksBinding
import com.example.todolistapp.repository.TaskRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

// Экран со списком завершённых (выполненных) задач
class CompletedTasksActivity : AppCompatActivity() {

    // ViewBinding для доступа к элементам layout (проще чем findViewById)
    private lateinit var binding: ActivityCompletedTasksBinding

    // Адаптер для отображения завершённых задач в списке
    private lateinit var completedTasksAdapter: CompletedTaskAdapter

    // Репозиторий для работы с БД
    private lateinit var repository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем ViewBinding (автоматически генерируется из XML layout)
        binding = ActivityCompletedTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем БД и репозиторий
        val database = AppDatabase.getDatabase(this)
        repository = TaskRepository(
            database.taskDao(),
            database.tagDao(),
            database.taskTagDao()
        )

        // Настраиваем адаптер с обработчиками действий
        setupAdapter()

        // Подключаем адаптер к RecyclerView
        setupRecyclerView()

        // Подписываемся на изменения списка завершённых задач
        observeCompletedTasks()
    }

    // Настройка адаптера с обработчиками кнопок "Восстановить" и "Удалить"
    private fun setupAdapter() {
        completedTasksAdapter = CompletedTaskAdapter(
            // Обработчик восстановления задачи (возврат в активные)
            onRestore = { task ->
                lifecycleScope.launch {
                    // Снимаем флаг "завершена"
                    task.isCompleted = false

                    // Сохраняем изменения в БД
                    repository.update(task)

                    // Обновляем виджет на главном экране
                    repository.updateWidget(this@CompletedTasksActivity)

                    // Показываем уведомление
                    Snackbar.make(binding.root, "Задача восстановлена", Snackbar.LENGTH_SHORT).show()
                }
            },
            // Обработчик удаления задачи
            onDelete = { task ->
                lifecycleScope.launch {
                    // Удаляем задачу из БД
                    repository.delete(task)

                    // Обновляем виджет
                    repository.updateWidget(this@CompletedTasksActivity)

                    // Показываем Snackbar с кнопкой "Отменить" (можно вернуть удалённую задачу)
                    Snackbar.make(binding.root, "Задача удалена", Snackbar.LENGTH_LONG)
                        .setAction("Отменить") {
                            // Если пользователь нажал "Отменить" - возвращаем задачу в БД
                            lifecycleScope.launch {
                                repository.insert(task)
                                repository.updateWidget(this@CompletedTasksActivity)
                            }
                        }
                        .show()
                }
            }
        )
    }

    // Подключение адаптера к RecyclerView
    private fun setupRecyclerView() {
        binding.recyclerViewCompletedTasks.apply {
            // Список вертикальный (по умолчанию)
            layoutManager = LinearLayoutManager(this@CompletedTasksActivity)

            // Подключаем адаптер
            adapter = completedTasksAdapter
        }
    }

    // Подписка на изменения списка завершённых задач из БД
    private fun observeCompletedTasks() {
        lifecycleScope.launch {
            // Слушаем Flow из репозитория (автоматически обновляется при изменении БД)
            repository.allCompletedTasks.collect { tasks ->
                // Обновляем список в адаптере
                completedTasksAdapter.submitList(tasks)
            }
        }
    }
}
