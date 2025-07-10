package ci.technchange.prestationscmu.views

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famoco.biometryservicelibrary.BiometryServiceAccess
import com.famoco.biometryservicelibrary.data.EnrolledFinger
import com.famoco.biometryservicelibrary.enums.BiometryServiceState
import com.famoco.biometryservicelibrary.enums.SensorState
import com.famoco.biometryservicelibrary.enums.SkinTone
import com.famoco.biometryservicelibrary.enums.TemplateType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class FingerViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val biometryServiceState = BiometryServiceAccess.biometryServiceState
    val sensorState = BiometryServiceAccess.sensorState
    val enrolledFingerArray = BiometryServiceAccess.enrolledFingerArray
    val lastEnrolledFinger = BiometryServiceAccess.lastEnrolledFinger
    val lastMatchingScore = BiometryServiceAccess.lastMatchingScore
    val error = BiometryServiceAccess.error

    init {
        viewModelScope.launch {
            BiometryServiceAccess.biometryServiceState.collect {
                if (it == BiometryServiceState.IDLE) {
                    connectSensor()
                }
            }
        }

        viewModelScope.launch {
            BiometryServiceAccess.sensorState.collect {
                if (it == SensorState.CONNECTED) {
                    _isLoading.value = false
                }
            }
        }

        viewModelScope.launch {
            BiometryServiceAccess.error.collect {
                if (it != null) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun connectService(context: Context) {
        Timber.i("connectService")
        viewModelScope.launch {
            BiometryServiceAccess.connectService(context)
        }
    }

    fun connectSensor() {
        Timber.i("connectSensor")
        viewModelScope.launch {
            BiometryServiceAccess.connectSensor()
        }
    }

    fun disconnectSensor() {
        Timber.i("disconnectSensor")
        viewModelScope.launch {
            BiometryServiceAccess.disconnectSensor()
        }
    }

    fun enroll(templateType: TemplateType, templates: Array<EnrolledFinger>) {
        Timber.i("enroll")
        viewModelScope.launch {
            BiometryServiceAccess.enroll(templateType = templateType, enrolledFingerArray = templates)
        }
    }

    fun verify(templates: Array<EnrolledFinger>) {
        Timber.i("verify with ${templates.size} templates")
        viewModelScope.launch {
            BiometryServiceAccess.verify(templates)
        }
    }

    fun disconnectService(context: Context) {
        Timber.i("disconnectService")
        viewModelScope.launch {
            BiometryServiceAccess.disconnectService(context)
        }
    }

    fun enrollSpecificFinger(context: Context, templateType: TemplateType, templates: Array<EnrolledFinger>) {
        viewModelScope.launch {
            BiometryServiceAccess.chooseFinger(context, templateType, templates)
        }
    }

    fun getLibraryVersion() = BiometryServiceAccess.getLibraryVersion()

    fun setSkinTone(context: Context, skinTone: SkinTone) {
        viewModelScope.launch {
            BiometryServiceAccess.setSkinTone(context, skinTone)
        }
    }
}