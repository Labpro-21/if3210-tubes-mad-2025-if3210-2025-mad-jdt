package com.purrytify.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.DailyListeningData
import com.purrytify.mobile.data.room.ListeningSessionRepository
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimeListenedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ListeningSessionRepository

    private val _totalMinutes = MutableStateFlow(0L)
    val totalMinutes: StateFlow<Long> = _totalMinutes.asStateFlow()

    private val _dailyAverage = MutableStateFlow(0L)
    val dailyAverage: StateFlow<Long> = _dailyAverage.asStateFlow()

    private val _currentMonth = MutableStateFlow("")
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    private val _dailyData = MutableStateFlow<List<DailyListeningData>>(emptyList())
    val dailyData: StateFlow<List<DailyListeningData>> = _dailyData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Cache to prevent repeated fetching
    private var lastFetchTime = 0L
    private val cacheValidityDuration = 30_000L // 30 seconds

    init {
        val database = AppDatabase.getDatabase(application)
        val listeningSessionDao = database.listeningSessionDao()
        repository = ListeningSessionRepository(listeningSessionDao)

        // Only load if not already loaded recently
        if (shouldFetchData()) {
            loadTimeListenedData()
        }
    }

    private fun shouldFetchData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return _totalMinutes.value == 0L || (currentTime - lastFetchTime) > cacheValidityDuration
    }

    private fun loadTimeListenedData() {
        if (_isLoading.value) return // Prevent multiple simultaneous loads

        viewModelScope.launch {
            _isLoading.value = true

            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
            val currentYear = calendar.get(Calendar.YEAR)

            // Set current month string
            val monthNames =
                    arrayOf(
                            "January",
                            "February",
                            "March",
                            "April",
                            "May",
                            "June",
                            "July",
                            "August",
                            "September",
                            "October",
                            "November",
                            "December"
                    )
            _currentMonth.value = "${monthNames[currentMonth - 1]} $currentYear"

            try {
                // Get total listening time for the month
                val totalTime = repository.getTotalListeningTimeForMonth(currentMonth, currentYear)
                val totalMinutes = totalTime / (1000 * 60) // Convert to minutes
                _totalMinutes.value = totalMinutes

                // Get daily listening data
                val dailyData = repository.getDailyListeningTimeForMonth(currentMonth, currentYear)
                _dailyData.value = dailyData

                // Calculate daily average
                val daysInMonth = getDaysInMonth(currentMonth, currentYear)
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                val daysElapsed =
                        if (currentMonth == calendar.get(Calendar.MONTH) + 1 &&
                                        currentYear == calendar.get(Calendar.YEAR)
                        ) {
                            currentDay // Current month, use days elapsed
                        } else {
                            daysInMonth // Past month, use full month
                        }

                _dailyAverage.value = if (daysElapsed > 0) totalMinutes / daysElapsed else 0L

                lastFetchTime = System.currentTimeMillis()
            } catch (e: Exception) {
                android.util.Log.e("TimeListenedViewModel", "Error loading time listened data", e)
                _totalMinutes.value = 0L
                _dailyAverage.value = 0L
                _dailyData.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    private fun getDaysInMonth(month: Int, year: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1) // Calendar.MONTH is 0-based
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun refreshData() {
        lastFetchTime = 0L // Reset cache
        loadTimeListenedData()
    }

    // Get the maximum value for chart scaling
    fun getMaxDailyMinutes(): Long {
        return _dailyData.value.maxOfOrNull { it.totalDuration / (1000 * 60) } ?: 1L
    }

    // Get chart data points (normalized for display)
    fun getChartDataPoints(): List<Pair<Int, Float>> {
        val dailyData = _dailyData.value
        val maxMinutes = getMaxDailyMinutes()

        return dailyData.map { data ->
            val day = data.day
            val minutes = data.totalDuration / (1000 * 60)
            val normalizedValue =
                    if (maxMinutes > 0) minutes.toFloat() / maxMinutes.toFloat() else 0f
            Pair(day, normalizedValue)
        }
    }
}
