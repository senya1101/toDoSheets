package com.example.todolistapp.activities

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.transition.Explode
import android.transition.Fade
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.todolistapp.R

// Экран для фокус-таймера Pomodoro
class TimeTrackerActivity : AppCompatActivity() {

    // --- View-элементы для управления таймером и отображения информации ---
    private lateinit var btnBack: ImageView         // кнопка назад
    private lateinit var textViewTimer: TextView    // основной таймер на экране
    private lateinit var btnPlayPause: ImageView    // кнопка старт/пауза
    private lateinit var btnSkip: ImageView         // кнопка "пропустить" этап
    private lateinit var textViewMode: TextView     // название текущего режима (раб./перерыв)
    private lateinit var textViewTaskName: TextView // название задачи под таймером
    private lateinit var indicator1: android.view.View // индикаторы сессий
    private lateinit var indicator2: android.view.View
    private lateinit var indicator3: android.view.View
    private lateinit var indicator4: android.view.View

    // Таймер обратного отсчёта: CountDownTimer отменяем при завершении/паузе
    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private var timeLeftInMillis: Long = 25 * 60 * 1000 // дефолтное значение помодоро: 25 минут

    // Переменная для восстановления исходного режима "Не беспокоить"
    private var previousInterruptionFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL

    // Моды таймера: работа, короткий перерыв, длинный перерыв
    private enum class TimerMode { FOCUS, SHORT_BREAK, LONG_BREAK }
    private var currentMode = TimerMode.FOCUS
    private var completedSessions = 0 // сколько фокус-сессий прошло

    // Инициализация Activity: настройка анимаций, UI, название задачи, запуск интерфейса
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = Explode().apply { duration = 400 }
        window.exitTransition = Fade().apply { duration = 200 }
        setContentView(R.layout.activity_time_tracker)
        setupWindowInsets() // оформление статус-бара и навигации
        initViews()         // findViewById для всех контролов
        setupListeners()    // подключаем клики на кнопки
        // Передаём название задачи по интенту (или дефолт)
        textViewTaskName.text = intent.getStringExtra("TASK_NAME") ?: getString(R.string.default_task_name)
        updateUI()          // обновляем интерфейс перед первым запуском
    }

    // Оформление оконных инсет (статус-бара) — делаем контент под системными элементами
    private fun setupWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    // Инициализация всех View-элементов Activity
    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        textViewTimer = findViewById(R.id.textViewTimer)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnSkip = findViewById(R.id.btnSkip)
        textViewMode = findViewById(R.id.textViewMode)
        textViewTaskName = findViewById(R.id.textViewTaskName)
        indicator1 = findViewById(R.id.indicator1)
        indicator2 = findViewById(R.id.indicator2)
        indicator3 = findViewById(R.id.indicator3)
        indicator4 = findViewById(R.id.indicator4)
    }

    // Обработка нажатий: старт/пауза, переход, выход
    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }                            // завершить Activity
        btnPlayPause.setOnClickListener { if (isRunning) pauseTimer() else startTimer() } // старт/пауза
        btnSkip.setOnClickListener { skipToNext() }                        // переход к следующему этапу
    }

    // --- Режим "не беспокоить" ---
    // Включение DND: проверяем наличие разрешения, сохраняем текущее состояние, включаем режим приоритета
    private fun enableDndMode() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // Нет разрешения: открываем настройки, просим дать доступ вручную
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Нужно дать доступ к \"Не беспокоить\"!", Toast.LENGTH_LONG).show()
            return
        }
        // Сохраняем только валидные значения фильтров — если вдруг был некорректный фильтр, ставим стандарт ALL
        val validFilters = listOf(
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        )
        val currentFilter = notificationManager.currentInterruptionFilter
        previousInterruptionFilter =
            if (currentFilter in validFilters) currentFilter else NotificationManager.INTERRUPTION_FILTER_ALL
        // Включаем приоритетный режим DND
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        Toast.makeText(this, getString(R.string.dnd_enabled), Toast.LENGTH_SHORT).show()
    }

    // Выключение DND: возвращаем прошлый режим, если он валиден, иначе включаем ALL
    private fun disableDndMode() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Нет доступа для выключения DND!", Toast.LENGTH_LONG).show()
            return
        }
        val validFilters = listOf(
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        )
        val restoreFilter =
            if (previousInterruptionFilter in validFilters) previousInterruptionFilter
            else NotificationManager.INTERRUPTION_FILTER_ALL
        notificationManager.setInterruptionFilter(restoreFilter)
        Toast.makeText(this, getString(R.string.dnd_disabled), Toast.LENGTH_SHORT).show()
    }

    // --- Таймер Pomodoro ---
    // Запуск таймера — включает DND и запускает обратный отсчёт
    private fun startTimer() {
        enableDndMode()  // включаем режим "не беспокоить" при запуске фокуса
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()  // обновляем отображение каждую секунду
            }
            override fun onFinish() { onTimerComplete() }
        }.start()
        isRunning = true
        btnPlayPause.setImageResource(R.drawable.ic_pause)
    }

    // Пауза таймера — отменяем обратный отсчёт
    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        btnPlayPause.setImageResource(R.drawable.ic_play)
    }

    // Пропуск этапа — завершает текущий таймер и переходит к следующему режиму
    private fun skipToNext() {
        pauseTimer()
        onTimerComplete()
    }

    // Завершение текущего этапа: выключаем DND, переключаем режим, обновляем UI
    private fun onTimerComplete() {
        disableDndMode() // снимаем режим DND, возвращаем пользователю уведомления
        when (currentMode) {
            TimerMode.FOCUS -> {
                completedSessions++
                updateSessionIndicators() // обновить отображение индикаторов
                if (completedSessions >= 4) {
                    // После 4 фокус-сессий — длинный перерыв, сбрасываем счётчик
                    switchMode(TimerMode.LONG_BREAK)
                    completedSessions = 0
                    updateSessionIndicators()
                } else switchMode(TimerMode.SHORT_BREAK)
            }
            TimerMode.SHORT_BREAK, TimerMode.LONG_BREAK -> switchMode(TimerMode.FOCUS)
        }
    }

    // Переключение между режимами Pomodoro: работа, короткий и длинный перерыв
    private fun switchMode(mode: TimerMode) {
        pauseTimer()
        currentMode = mode
        when (mode) {
            TimerMode.FOCUS -> {
                timeLeftInMillis = 25 * 60 * 1000
                textViewMode.text = getString(R.string.focus_mode)
            }
            TimerMode.SHORT_BREAK -> {
                timeLeftInMillis = 5 * 60 * 1000
                textViewMode.text = getString(R.string.short_break_mode)
            }
            TimerMode.LONG_BREAK -> {
                timeLeftInMillis = 15 * 60 * 1000
                textViewMode.text = getString(R.string.long_break_mode)
            }
        }
        updateTimerText()
        startTimer()
    }

    // Первоначальное обновление UI: таймер и индикаторы
    private fun updateUI() {
        updateTimerText()
        updateSessionIndicators()
    }

    // --- Функции обновления экрана ---
    // Обновляет отображение таймера
    @SuppressLint("DefaultLocale")
    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        textViewTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    // Обновляет индикаторы завершённых фокус-сессий под таймером
    private fun updateSessionIndicators() {
        val indicators = listOf(indicator1, indicator2, indicator3, indicator4)
        indicators.forEachIndexed { index, indicator ->
            val drawableRes = if (index < completedSessions) {
                R.drawable.session_indicator_active      // завершённые сессии — яркие
            } else {
                R.drawable.session_indicator_inactive    // остальные — тусклые
            }
            indicator.setBackgroundResource(drawableRes)
        }
    }

    // Очищаем таймер и возвращаем режим уведомлений при уничтожении Activity
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        disableDndMode()
    }
}
