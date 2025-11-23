package com.example.todolistapp.utils

// sealed class - запечатанный класс (ограниченная иерархия наследников)
// Используется для безопасного представления результатов операций (успех/ошибка)
// Аналог Either, Try или Result из функционального программирования
sealed class Result {
    // Success - успешный результат операции
    // message: Any - может содержать любой тип данных:
    //   - String (путь к файлу)
    //   - BackupData (данные бэкапа)
    //   - Int (количество обработанных элементов)
    //   - и т.д.
    data class Success(val message: Any) : Result()

    // Error - ошибка при выполнении операции
    // message: String - текст ошибки для пользователя (например: "Файл не найден")
    data class Error(val message: String) : Result()
}

