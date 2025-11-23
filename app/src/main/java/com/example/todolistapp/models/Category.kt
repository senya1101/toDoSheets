package com.example.todolistapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity - аннотация Room, указывает что это таблица в БД
// tableName - явное указание имени таблицы (по умолчанию было бы "Category")
// Эта таблица будет называться "categories" в SQLite БД
@Entity(tableName = "categories")
// data class - специальный класс Kotlin для хранения данных
// Автоматически генерирует: equals(), hashCode(), toString(), copy(), componentN()
data class Category(
    // @PrimaryKey - помечает поле как первичный ключ (уникальный идентификатор записи)
    // autoGenerate = true - Room автоматически генерирует значение при insert
    // Первая категория получит id=1, вторая id=2, третья id=3 и т.д.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,  // = 0 это дефолтное значение (при создании новой категории)

    // Название категории (например: "Работа", "Личное", "Учёба")
    // String не nullable - категория ВСЕГДА должна иметь название
    val name: String,

    // Цвет категории в HEX формате (например: "#FF6B6B", "#4ECDC4")
    // Используется для визуального отличия категорий в UI
    // Формат: "#RRGGBB" где RR=красный, GG=зелёный, BB=синий (в шестнадцатеричной системе)
    val color: String
)

// Пример использования:
// val workCategory = Category(name = "Работа", color = "#FF6B6B")
// categoryRepository.insert(workCategory)
// Room автоматически установит id (например, 1) и сохранит в БД
