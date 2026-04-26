package com.rentcollection.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isSeedingUsers: Boolean = false,
    val seedSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.login(email, password)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
                }
        }
    }

    /** Create the two default accounts (owner@rent.com / father@rent.com). */
    fun setupDefaultAccounts() {
        viewModelScope.launch {
            _state.update { it.copy(isSeedingUsers = true, error = null) }
            try {
                authRepository.seedUsers()
                _state.update { it.copy(isSeedingUsers = false, seedSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSeedingUsers = false, error = e.message) }
            }
        }
    }

    fun clearSeedSuccess() = _state.update { it.copy(seedSuccess = false) }
}
