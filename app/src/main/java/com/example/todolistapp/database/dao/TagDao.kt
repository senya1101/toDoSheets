package com.example.todolistapp.database.dao

import androidx.room.*
import com.example.todolistapp.models.Tag
import kotlinx.coroutines.flow.Flow

// @Dao - Data Access Object (интерфейс для работы с таблицей tags в БД)
// Room генерирует реализацию автоматически во время компиляции
@Dao
interface TagDao {

    // @Insert - вставляет новый тег в таблицу tags
    // suspend - асинхронная функция для корутин (не блокирует UI поток)
    // Возвращает Long - id созданной записи (auto-generated primary key)
    @Insert
    suspend fun insertTag(tag: Tag): Long

    // @Update - обновляет существующий тег в БД
    // Room находит запись по id (primary key) и обновляет все поля
    // suspend - выполняется асинхронно
    @Update
    suspend fun updateTag(tag: Tag)

    // @Delete - удаляет тег из БД
    // Room находит запись по id и удаляет её
    // suspend - выполняется асинхронно
    @Delete
    suspend fun deleteTag(tag: Tag)

    // @Query - выполняет SQL запрос для получения всех тегов
    // Возвращает Flow<List<Tag>> - реактивный поток данных
    // Flow автоматически эмитит новый список при ЛЮБЫХ изменениях в таблице tags
    // (при insert, update, delete любого тега)
    // НЕ suspend - Flow сам управляет асинхронностью и подписками
    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<Tag>>

    // @Query с параметром :tagId (значение подставляется из аргумента функции)
    // SELECT * FROM tags WHERE id = 5 (если tagId = 5)
    // Возвращает Flow<Tag?> - либо найденный тег, либо null
    // ? означает nullable тип - тег может не существовать
    // Flow автоматически обновляется при изменении этого конкретного тега
    @Query("SELECT * FROM tags WHERE id = :tagId")
    fun getTagById(tagId: Long): Flow<Tag?>
}
