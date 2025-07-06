package com.example.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.app.data.EntryRepository
import com.example.app.model.Entry
import com.example.app.util.LocationUtil
import javax.inject.Inject

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val repo: EntryRepository
) : ViewModel() {

    private val _uid = MutableStateFlow("")
    val uid: StateFlow<String> = _uid.asStateFlow()

    val entries: StateFlow<List<Entry>> = _uid
        .filter { it.isNotEmpty() }
        .flatMapLatest { uid ->
            repo.flow(uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setUserId(userId: String) {
        _uid.value = userId
    }

    fun saveEntry(
        title: String,
        content: String,
        location: LocationUtil.LocationData?,
        photoUri: Uri?,
        audioUri: Uri?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val entry = Entry(
                title = title,
                content = content,
                lat = location?.lat,
                lon = location?.lon,
                place = location?.place
            )

            repo.add(_uid.value, entry, photoUri, audioUri)
                .onSuccess {
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            repo.delete(_uid.value, entryId)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
