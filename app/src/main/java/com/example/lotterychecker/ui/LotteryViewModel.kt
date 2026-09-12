package com.example.lotterychecker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lotterychecker.data.LotteryCheckResult
import com.example.lotterychecker.data.LotteryResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LotteryViewModel(
    private val repository: LotteryResultRepository = LotteryResultRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(LotteryUiState())
    val uiState = _uiState.asStateFlow()

    fun checkTicket(ticketNumber: String, drawDate: String) {
        viewModelScope.launch {
            _uiState.value = LotteryUiState(isLoading = true)
            val result = withContext(Dispatchers.IO) {
                repository.getResultSummary(ticketNumber, drawDate)
            }
            _uiState.value = LotteryUiState(isLoading = false, result = result)
        }
    }
}

data class LotteryUiState(
    val isLoading: Boolean = false,
    val result: LotteryCheckResult? = null
)
