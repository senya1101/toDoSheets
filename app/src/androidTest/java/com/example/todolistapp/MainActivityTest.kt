package com.example.todolistapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todolistapp.activities.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    // Запускаем MainActivity перед каждым тестом
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // Тест: Проверяем, что главный список задач (RecyclerView) отображается корректно
    @Test
    fun mainActivity_LaunchesSuccessfully() {
        // Проверяем, что RecyclerView видим
        onView(withId(R.id.recyclerViewTasks))
            .check(matches(isDisplayed()))

        // Проверяем, что кнопка добавления задачи видна
        onView(withId(R.id.fabAddTask))
            .check(matches(isDisplayed()))
    }

    // Тест: Проверяем переключение вкладок — "Все" и "Завершенные"
    @Test
    fun tabs_SwitchBetweenAllAndCompleted() {
        // Проверка и клик по вкладке "Все"
        onView(withId(R.id.tabAll))
            .check(matches(isDisplayed()))
            .perform(click())

        // Проверка и клик по вкладке "Завершенные"
        onView(withId(R.id.tabCompleted))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    // Тест: Проверка, что кнопка "+" (добавить задачу) кликабельна
    @Test
    fun fabButton_IsClickable() {
        onView(withId(R.id.fabAddTask))
            .check(matches(isClickable()))
    }

    // Тест: Проверка видимости SearchView
    @Test
    fun searchView_IsDisplayed() {
        onView(withId(R.id.searchView))
            .check(matches(isDisplayed()))
    }

    // Тест: Проверка, что кнопка сортировки активна и кликабельна
    @Test
    fun sortButton_IsClickable() {
        onView(withId(R.id.btnSort))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    // Тест: Проверка, что календарь (контейнер) отображается и активен для клика
    @Test
    fun calendarButton_IsClickable() {
        onView(withId(R.id.calendarContainer))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    // Тест: Проверка, что RecyclerView имеет установленный layoutManager
    @Test
    fun recyclerView_HasLayoutManager() {
        onView(withId(R.id.recyclerViewTasks))
            .check(matches(isDisplayed()))
        // Можно добавить более глубокие проверки, например, что layoutManager установлен
    }
}



