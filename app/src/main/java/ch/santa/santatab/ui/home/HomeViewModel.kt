package ch.santa.santatab.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.santa.santatab.SantaTabApp
import ch.santa.santatab.data.model.Scene
import ch.santa.santatab.domain.DiffuserRepository

class HomeViewModel(private val repository: DiffuserRepository) : ViewModel() {

    val diffusers = repository.diffusers
    val connectionState = repository.connectionState
    val scenes: List<Scene> = repository.scenes

    fun setPower(id: String, on: Boolean) = repository.setPower(id, on)
    fun setStep(id: String, step: Int) = repository.setStep(id, step)
    fun applyScene(scene: Scene) = repository.applyScene(scene)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SantaTabApp
                HomeViewModel(app.container.diffuserRepository)
            }
        }
    }
}
