package com.example.todolistapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolistapp.R
import com.example.todolistapp.models.Task

// Адаптер для списка выполненных задач
// Наследуемся от ListAdapter, чтобы удобно обновлять список через DiffUtil
class CompletedTaskAdapter(
    private val onRestore: (Task) -> Unit, // Колбек для восстановления задачи
    private val onDelete: (Task) -> Unit   // Колбек для удаления задачи
) : ListAdapter<Task, CompletedTaskAdapter.CompletedTaskViewHolder>(TaskDiffCallback()) {

    // ViewHolder хранит ссылки на нужные в одном элементе списка View
    class CompletedTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textViewTaskTitle)         // Название задачи
        val description: TextView = itemView.findViewById(R.id.textViewTaskDescription) // Описание
        val btnRestore: AppCompatImageView = itemView.findViewById(R.id.btnRestore)  // Кнопка восстановления
        val btnDelete: AppCompatImageView = itemView.findViewById(R.id.btnDelete)    // Кнопка удаления
    }

    // Создаём новый ViewHolder, надувая layout для одного элемента
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedTaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_task_completed,
            parent,
            false
        )
        return CompletedTaskViewHolder(view)
    }

    // Связываем данные задачи с ViewHolder, вызывается при каждой прокрутке
    override fun onBindViewHolder(holder: CompletedTaskViewHolder, position: Int) {
        val task = getItem(position) // Получаем задачу по позиции

        holder.title.text = task.title // Назначаем название

        // Если описание есть, показываем и заполняем, иначе скрываем блок
        if (task.description.isNotEmpty()) {
            holder.description.visibility = View.VISIBLE
            holder.description.text = task.description
        } else {
            holder.description.visibility = View.GONE
        }

        // Кнопка восстановления вызывает колбек из Activity
        holder.btnRestore.setOnClickListener {
            onRestore(task)
        }

        // Кнопка удаления вызывает колбек из Activity
        holder.btnDelete.setOnClickListener {
            onDelete(task)
        }
    }

    // DiffUtil помогает понять, что изменилось в списке, чтобы не перерисовывать всё
    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        // Проверяем, это один и тот же объект (используем уникальный ID)
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        // Проверяем, изменились ли данные задачи (содержимое)
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            // data class имеет удобный equals, сравниваем все поля
            return oldItem == newItem
        }

        // Можно сделать частичное обновление без полного обновления элемента,
        // Но сейчас это не реализовано - проще сделать полное обновление
    }
}
