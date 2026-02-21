package com.cyin.daily_push_up

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cyin.daily_push_up.data.CachedStats
import com.cyin.daily_push_up.data.PushUpEntry
import com.cyin.daily_push_up.data.PushUpRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PushUpRepository(MyApplication.database.pushUpDao())

    val stats: LiveData<CachedStats?> = repo.statsLiveData

    private val _entries = MutableLiveData<List<PushUpEntry>>(emptyList())
    val entries: LiveData<List<PushUpEntry>> = _entries

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _pushupCount = MutableLiveData(30)
    val pushupCount: LiveData<Int> = _pushupCount

    private val _validateResult = MutableLiveData<Boolean?>()
    val validateResult: LiveData<Boolean?> = _validateResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        refresh()
    }

    fun refresh() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repo.sync()
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message
            }
            _entries.value = repo.getAllEntries()
            _isLoading.value = false
        }
    }

    fun adjustPushups(delta: Int) {
        val current = _pushupCount.value ?: 30
        val newValue = (current + delta).coerceAtLeast(0)
        _pushupCount.value = newValue
    }

    fun validate() {
        val count = _pushupCount.value ?: return
        _isLoading.value = true
        viewModelScope.launch {
            val result = repo.validateToday(count)
            if (result.isSuccess) {
                _validateResult.value = result.getOrNull()
                _entries.value = repo.getAllEntries()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
            _isLoading.value = false
        }
    }

    fun clearValidateResult() {
        _validateResult.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
