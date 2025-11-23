package com.example.todolistapp.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.todolistapp.R
import com.example.todolistapp.activities.TimeTrackerActivity
import com.example.todolistapp.models.Task
import java.text.SimpleDateFormat
import java.util.Locale

// Адаптер для отображения списка задач, реализует RecyclerView
// Внутри используется DiffUtil для оптимизации обновлений
class TaskAdapter(
    private val onTaskChecked: (Task, Boolean) -> Unit = { _, _ -> }, // Обработчик чекбокса (выполнено/нет)
    private val onTaskEdit: (Task) -> Unit = {}, // Обработчик редактирования задачи
    private val onTaskDelete: (Task) -> Unit = {} // Обработчик удаления задачи
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DIFF_CALLBACK) {

    // ViewHolder для одного элемента списка
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Находим нужные UI элементы внутри itemView
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxTask)
        val title: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        val description: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        val btnDelete: AppCompatImageView = itemView.findViewById(R.id.btnDelete) // Кнопка удалить
        val btnTimer: AppCompatImageView = itemView.findViewById(R.id.btnTimer) // Кнопка таймера
        val btnEdit: AppCompatImageView = itemView.findViewById(R.id.btnEdit) // Кнопка редактировать
        val tvDeadline: TextView = itemView.findViewById(R.id.tvDeadline) // Дата дедлайна
        val tagsContainer: ChipGroup? = itemView.findViewById(R.id.tagsContainer) // Контейнер для тегов

        // Метод привязки данных задачи к UI
        @SuppressLint("SetTextI18n")
        fun bind(task: Task) {
            // Лог для отладки, показывает при привязке, какой таск обрабатывается
            Log.d("TAG_BIND", "Binding task: ${task.title} (ID: ${task.id}), tags: ${task.tags}")
            title.text = task.title // Устанавливаем название

            // Если есть описание, показываем его, иначе скрываем
            if (task.description.isNotEmpty()) {
                description.visibility = View.VISIBLE
                description.text = task.description
            } else {
                description.visibility = View.GONE
            }

            // Перед установкой слушателя отключаем предыдущий (чтобы не было срабатываний при обновлении)
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = task.isCompleted // Статус задачи
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                // Обновляем модель задачи и вызываем колбек для сохранения
                task.isCompleted = isChecked
                onTaskChecked(task, isChecked)
            }

            // Работа с датой дедлайна
            task.deadline?.let { deadline ->
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                tvDeadline.text = dateFormat.format(deadline)
                tvDeadline.visibility = View.VISIBLE
                // Цвет меняется в зависимости от того, прошёл дедлайн или нет
                if (deadline < System.currentTimeMillis()) {
                    tvDeadline.setTextColor("#F44336".toColorInt()) // Красный, если прошёл
                } else {
                    tvDeadline.setTextColor("#FF9800".toColorInt()) // Оранжевый, если ещё не прошёл
                }
            } ?: run {
                // Если нет дедлайна — убираем текст и ничего не показываем
                tvDeadline.visibility = View.GONE
            }

            // Работа с тегами
            Log.d("TAG_BIND", "tagsContainer is ${if (tagsContainer == null) "NULL" else "NOT NULL"}")
            tagsContainer?.let { container ->
                Log.d("TAG_CHIP", "tagsContainer found for ${task.title}")
                container.removeAllViews() // Очищаем предыдущие теги
                if (task.tags.isNotEmpty()) {
                    Log.d("TAG_CHIP", "Setting visibility VISIBLE, ${task.tags.size} tags")
                    container.visibility = View.VISIBLE
                    // Создаём и добавляем чипы для каждого тега
                    task.tags.forEach { tagName ->
                        Log.d("TAG_CHIP", "Creating chip for: $tagName")
                        val chip = Chip(container.context).apply {
                            text = tagName // Название тега
                            isCloseIconVisible = false // Иконка закрытия, тут не нужно
                            isCheckable = false // Можно свернуть или оставить раскрытым, сейчас - нет
                            setTextColor(Color.WHITE)
                            textSize = 13f
                            // Цвет фона зависит от эмодзи в начале названия
                            val backgroundColor = when {
                                tagName.startsWith("🏢") -> "#FF6B6B".toColorInt()
                                tagName.startsWith("🏠") -> "#FF8787".toColorInt()
                                tagName.startsWith("⭐") -> "#FFA500".toColorInt()
                                tagName.startsWith("⚡") -> "#FF5252".toColorInt()
                                tagName.startsWith("📚") -> "#5C6BC0".toColorInt()
                                tagName.startsWith("🏋️") -> "#26A69A".toColorInt()
                                tagName.startsWith("🛒") -> "#AB47BC".toColorInt()
                                tagName.startsWith("🤝") -> "#EC407A".toColorInt()
                                else -> "#2196F3".toColorInt()
                            }
                            // Применяем цвет фона
                            chipBackgroundColor = android.content.res.ColorStateList.valueOf(backgroundColor)
                            // Внутри — немного отступов, центрирование
                            setPadding(8, 0, 8, 0)
                            gravity = android.view.Gravity.CENTER
                            // Размер
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                        Log.d("TAG_CHIP", "Adding chip to container")
                        container.addView(chip) // Добавляем чип в контейнер
                    }
                } else {
                    Log.d("TAG_CHIP", "No tags, setting visibility GONE")
                    container.visibility = View.GONE // Там, где тегов нет — скрываем группу
                }
            } ?: run {
                Log.e("TAG_CHIP", "❌ tagsContainer is NULL!") // На всякий случай
            }

            // Обработка кнопок
            btnEdit.setOnClickListener { onTaskEdit(task) }
            btnDelete.setOnClickListener { onTaskDelete(task) }
            btnTimer.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, TimeTrackerActivity::class.java)
                intent.putExtra("TASK_NAME", task.title)
                context.startActivity(intent)
            }
        }
    }

    // Создаём ViewHolder, разворачивая XML layout элемента
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    // Привязываем конкретную задачу к ViewHolder
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = currentList[position]
        holder.bind(task)
    }

    // DiffUtil для обновления списка — сравнить по id и содержимому
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem.id == newItem.id // Проверка по id
            }
            override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem.id == newItem.id &&
                        oldItem.title == newItem.title &&
                        oldItem.description == newItem.description &&
                        oldItem.isCompleted == newItem.isCompleted &&
                        oldItem.categoryId == newItem.categoryId &&
                        oldItem.deadline == newItem.deadline
            }
        }
    }
}
