package com.keyserdsoze.cardreader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyserdsoze.cardreader.data.GoogleAccountIdentity
import com.keyserdsoze.cardreader.data.GoogleAccountStore
import com.keyserdsoze.cardreader.data.LocalCardStore
import com.keyserdsoze.cardreader.data.cloud.DriveSyncRepository
import com.keyserdsoze.cardreader.data.cloud.DriveTransport
import com.keyserdsoze.cardreader.data.cloud.GoogleDriveAccessTokenProvider
import com.keyserdsoze.cardreader.model.CardEntry
import com.keyserdsoze.cardreader.model.CodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data class Success(val cardCount: Int) : SyncState
    data class Error(val message: String) : SyncState
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalCardStore(application)
    private val accountStore = GoogleAccountStore(application)
    val document = store.document
    private val _account = MutableStateFlow(accountStore.read())
    val account: StateFlow<GoogleAccountIdentity?> = _account.asStateFlow()
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun add(name: String, value: String, format: CodeFormat, colorArgb: Int, notes: String) {
        store.add(name, value, format, colorArgb, notes)
    }

    fun update(card: CardEntry) = store.update(card)
    fun delete(id: String) = store.delete(id)

    fun setConnectedAccount(identity: GoogleAccountIdentity) {
        accountStore.write(identity)
        _account.value = identity
        _syncState.value = SyncState.Idle
    }

    fun disconnectLocally() {
        accountStore.clear()
        _account.value = null
        _syncState.value = SyncState.Idle
    }

    fun reportSyncError(message: String) {
        _syncState.value = SyncState.Error(message)
    }

    fun syncNow() {
        val identity = _account.value ?: run {
            _syncState.value = SyncState.Error("Connect a Google account first")
            return
        }
        if (_syncState.value == SyncState.Running) return
        _syncState.value = SyncState.Running
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val provider = GoogleDriveAccessTokenProvider(getApplication()) { identity.email }
                DriveSyncRepository(store, DriveTransport(provider)).sync()
            }.onSuccess {
                _syncState.value = SyncState.Success(it.cardCount)
            }.onFailure {
                _syncState.value = SyncState.Error(it.message ?: "Google Drive sync failed")
            }
        }
    }
}
