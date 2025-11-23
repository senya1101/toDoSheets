package com.example.todolistapp.widget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.todolistapp.R
import com.example.todolistapp.database.AppDatabase
import com.example.todolistapp.models.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// Класс фабрики, которая создаёт отдельные элементы списка для виджета задач
// RemoteViewsFactory используется системой Android для отображения коллекций (ListView) внутри виджета
class TaskWidgetViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks = listOf<Task>() // Список активных задач, которые мы покажем в виджете

    // Вызывается при создании фабрики — загрузим данные
    override fun onCreate() {
        loadTasks()
    }

    // Вызывается когда данные виджета обновляются — подгружаем свежие задачи
    override fun onDataSetChanged() {
        loadTasks()
    }

    // Загрузка задач из базы данных (синхронно внутри runBlocking, т.к. интерфейс не suspend)
    private fun loadTasks() {
        runBlocking {
            val database = AppDatabase.getDatabase(context)
            val taskDao = database.taskDao()
            // Берём все активные задачи (не выполненные)
            tasks = taskDao.getAllActiveTasks().first()
        }
    }

    // Возвращает количество задач для списка в виджете
    override fun getCount(): Int = tasks.size

    // Создаёт отдельный элемент списка RemoteViews по позиции
    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        val task = tasks[position]
        // Заполняем заголовок и описание в элементе списка виджета
        views.setTextViewText(R.id.widgetTaskTitle, task.title)
        views.setTextViewText(R.id.widgetTaskDescription, task.description)
        return views
    }

    // Возвращает view, показываемое во время загрузки (null — используем дефолт)
    override fun getLoadingView(): RemoteViews? = null

    // Количество различных типов view в списке — 1, т.к. все одинаковые
    override fun getViewTypeCount(): Int = 1

    // Уникальный ID элемента списка — используем id задачи из базы
    override fun getItemId(position: Int): Long = tasks[position].id

    // Говорим, что у элементов списка стабильные (постоянные) id
    override fun hasStableIds(): Boolean = true

    // Освобождаем ресурсы — очищаем список при уничтожении фабрики
    override fun onDestroy() {
        tasks = emptyList()
    }
}
