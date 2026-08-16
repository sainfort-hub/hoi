package ch.santa.santatab.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.santa.santatab.SantaTabApp
import ch.santa.santatab.data.model.AppMode
import ch.santa.santatab.data.model.AppSettings
import ch.santa.santatab.data.model.BrokerSettings
import ch.santa.santatab.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _form = MutableStateFlow(AppSettings())
    val form: StateFlow<AppSettings> = _form.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            _form.value = repository.settings.first()
        }
    }

    fun setMode(mode: AppMode) = update { it.copy(mode = mode) }

    fun updateBroker(transform: (BrokerSettings) -> BrokerSettings) =
        update { it.copy(broker = transform(it.broker)) }

    fun addHost(host: String) {
        val clean = host.trim()
        if (clean.isEmpty() || clean in _form.value.directHosts) return
        update { it.copy(directHosts = it.directHosts + clean) }
    }

    fun removeHost(host: String) = update { it.copy(directHosts = it.directHosts - host) }

    fun save() {
        viewModelScope.launch {
            repository.save(_form.value)
            _saved.value = true
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        _form.value = transform(_form.value)
        _saved.value = false
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SantaTabApp
                SettingsViewModel(app.container.settingsRepository)
            }
        }
    }
}
