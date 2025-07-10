package ci.technchange.prestationscmu.views

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.famoco.biometryservicelibrary.BiometryServiceAccess
import com.famoco.biometryservicelibrary.data.EnrolledFinger
import com.famoco.biometryservicelibrary.enums.BiometryServiceState
import com.famoco.biometryservicelibrary.enums.Finger
import com.famoco.biometryservicelibrary.enums.Hand
import com.famoco.biometryservicelibrary.enums.SensorState
import com.famoco.biometryservicelibrary.enums.SkinTone
import com.famoco.biometryservicelibrary.enums.TemplateType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay


class EnrollmentViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _capturedFingerprints = mutableStateOf(0)
    val capturedFingerprints: State<Int> = _capturedFingerprints

    private val _fingerIdMap = mutableMapOf<EnrolledFinger, String>()

    fun storeFingerIdMapping(finger: EnrolledFinger, idKey: String) {
        _fingerIdMap[finger] = idKey
    }

    fun getIdKeyForFinger(finger: EnrolledFinger): String? {
        return _fingerIdMap[finger]
    }

    fun incrementFingerprintCount() {
        _capturedFingerprints.value = _capturedFingerprints.value + 1
    }

    val biometryServiceState = BiometryServiceAccess.biometryServiceState
    //val biometryServiceState: StateFlow<BiometryServiceState>

    val sensorState = BiometryServiceAccess.sensorState

    var enrolledFingerArray = BiometryServiceAccess.enrolledFingerArray
    //private val _enrolledFingerArray = MutableStateFlow<List<EnrolledFinger>>(emptyList())
    //val enrolledFingerArray: StateFlow<List<EnrolledFinger>> get() = _enrolledFingerArray


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


    fun clearAllFingerprints() {
        Timber.i("clearAllFingerprints - Tentative d'effacement des empreintes")
        BiometryServiceAccess.disconnectSensor()
        viewModelScope.launch {
            try {
                // Déconnecter et reconnecter le capteur pour réinitialiser les empreintes
                disconnectSensor()
                connectSensor()


                // Réinitialiser notre compteur d'empreintes
                _capturedFingerprints.value = 0

                Timber.d("Tentative de réinitialisation du capteur terminée")
            } catch (e: Exception) {
                Timber.e("Erreur lors de la tentative d'effacement des empreintes: ${e.message}")
            }
        }
    }

    fun clearAllFingerprintsWithReflection() {
        Timber.i("clearAllFingerprints - Tentative d'effacement complète des empreintes avec réflexion")
        viewModelScope.launch {
            try {
                // Obtenir la classe de BiometryServiceAccess
                val biometryServiceAccessClass = BiometryServiceAccess::class.java

                // 1. Réinitialiser le tableau d'empreintes
                val enrolledFingerArrayField = biometryServiceAccessClass.getDeclaredField("enrolledFingerArray")
                enrolledFingerArrayField.isAccessible = true
                val enrolledFingerArrayStateFlow = enrolledFingerArrayField.get(BiometryServiceAccess) as MutableStateFlow<Array<EnrolledFinger>>
                enrolledFingerArrayStateFlow.value = emptyArray()



                // 8. Petit délai pour assurer la déconnexion complète
                Thread.sleep(300)

                // 9. Réinitialiser notre compteur local
                _capturedFingerprints.value = 0

                // 11. Vérifier que la réinitialisation a fonctionné
                Timber.d("Empreintes réinitialisées via réflexion. Nouveau nombre: ${enrolledFingerArrayStateFlow.value.size}")

            } catch (e: Exception) {
                Timber.e("Erreur lors de la réinitialisation complète via réflexion: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Déconnecte le capteur d'empreintes en utilisant la réflexion pour accéder aux méthodes privées.
     */
    fun disconnectSensorWithReflection() {
        Timber.i("disconnectSensorWithReflection - Tentative de déconnexion du capteur via réflexion")
        viewModelScope.launch {
            try {
                // Obtenir la classe de BiometryServiceAccess
                val biometryServiceAccessClass = BiometryServiceAccess::class.java

                // Accéder à la méthode disconnectSensor via réflexion
                val disconnectSensorMethod = biometryServiceAccessClass.getDeclaredMethod("disconnectSensor")
                disconnectSensorMethod.isAccessible = true

                // Accéder au champ _sensorState pour vérifier l'état actuel
                val sensorStateField = biometryServiceAccessClass.getDeclaredField("_sensorState")
                sensorStateField.isAccessible = true
                val sensorStateFlow = sensorStateField.get(BiometryServiceAccess) as MutableStateFlow<SensorState>

                // Exécuter la déconnexion via réflexion
                disconnectSensorMethod.invoke(BiometryServiceAccess)

                // Attendre un peu pour assurer la déconnexion
                delay(500)

                // Vérifier si la déconnexion a réussi
                val currentState = sensorStateFlow.value
                Timber.d("État du capteur après déconnexion: $currentState")

                if (currentState == SensorState.DISCONNECTED) {
                    Timber.d("Déconnexion du capteur réussie via réflexion")
                } else {
                    Timber.w("La déconnexion via réflexion n'a pas changé l'état du capteur")
                }

            } catch (e: Exception) {
                Timber.e("Erreur lors de la déconnexion du capteur via réflexion: ${e.message}")
                e.printStackTrace()
            }
        }
    }


    /**
     * Connecte le capteur d'empreintes en utilisant la réflexion pour accéder aux méthodes privées.
     */
    fun connectSensorWithReflection() {
        Timber.i("connectSensorWithReflection - Tentative de connexion du capteur via réflexion")
        viewModelScope.launch {
            try {
                // Obtenir la classe de BiometryServiceAccess
                val biometryServiceAccessClass = BiometryServiceAccess::class.java

                // Accéder à la méthode connectSensor via réflexion
                val connectSensorMethod = biometryServiceAccessClass.getDeclaredMethod("connectSensor")
                connectSensorMethod.isAccessible = true

                // Accéder au service (pour vérifier s'il est initialisé)
                val serviceField = biometryServiceAccessClass.getDeclaredField("service")
                serviceField.isAccessible = true
                val service = serviceField.get(BiometryServiceAccess)

                // Vérifier si le service est disponible avant de tenter la connexion
                if (service == null) {
                    Timber.w("Le service BiometryService n'est pas initialisé, impossible de connecter le capteur")
                    return@launch
                }

                // Exécuter la connexion via réflexion
                connectSensorMethod.invoke(BiometryServiceAccess)

                // Attendre un peu pour que la connexion s'établisse
                delay(1000)

                // Vérifier si la connexion a réussi en utilisant le StateFlow public
                val currentState = BiometryServiceAccess.sensorState.value
                Timber.d("État du capteur après tentative de connexion: $currentState")

                if (currentState == SensorState.CONNECTED) {
                    Timber.d("Connexion au capteur réussie via réflexion")
                } else {
                    Timber.w("La connexion via réflexion n'a pas réussi ou est en cours")
                }

            } catch (e: Exception) {
                Timber.e("Erreur lors de la connexion du capteur via réflexion: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun safeConnectService(context: Context) {
        Timber.i("safeConnectService - Tentative de connexion sécurisée au service")
        viewModelScope.launch {
            try {
                // Vérifier l'état initial
                val initialState = BiometryServiceAccess.biometryServiceState.value
                Timber.d("État initial du service: $initialState")

                // Utiliser la méthode standard du SDK
                BiometryServiceAccess.connectService(context)

                // Attendre un moment pour que la connexion s'établisse
                delay(1000)

                // Vérifier et analyser l'état après connexion
                val currentState = BiometryServiceAccess.biometryServiceState.value
                Timber.d("État du service après connexion: $currentState")

                if (currentState == BiometryServiceState.IDLE) {
                    Timber.d("✅ Service biométrique connecté avec succès (IDLE) - prêt à utiliser")
                } else if (currentState == BiometryServiceState.DISCONNECTED) {
                    Timber.d("⚠️ Service encore en cours de connexion (CONNECTING) - attente prolongée...")

                    // Attendre encore un peu et revérifier
                    delay(1500)
                    val finalState = BiometryServiceAccess.biometryServiceState.value

                    if (finalState == BiometryServiceState.IDLE) {
                        Timber.d("✅ Service finalement connecté avec succès après attente prolongée")
                    } else {
                        Timber.w("⚠️ Service toujours dans l'état: $finalState après attente prolongée")
                    }
                } else if (currentState == BiometryServiceState.BLOCKED) {
                    Timber.e("❌ Erreur de connexion au service (ERROR)")

                    // Tentative de reconnexion en cas d'erreur
                    Timber.d("Tentative de reconnexion après erreur...")
                    delay(500)
                    BiometryServiceAccess.connectService(context)
                } else {
                    Timber.w("⚠️ État du service inattendu: $currentState")
                }

            } catch (e: Exception) {
                Timber.e("❌ Exception lors de la connexion au service: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Connecte au service biométrique en utilisant la réflexion pour accéder aux méthodes privées.
     * @param context Le contexte de l'application nécessaire pour la connexion au service
     */
    fun connectServiceWithReflection(context: Context) {
        Timber.i("connectServiceWithReflection - Tentative de connexion au service via réflexion")
        viewModelScope.launch {
            try {
                // Obtenir la classe de BiometryServiceAccess
                val biometryServiceAccessClass = BiometryServiceAccess::class.java

                // Accéder à la méthode connectService via réflexion
                val connectServiceMethod = biometryServiceAccessClass.getDeclaredMethod("connectService", Context::class.java)
                connectServiceMethod.isAccessible = true

                // Accéder au champ biometryServiceState pour vérifier l'état actuel
                val biometryServiceStateField = biometryServiceAccessClass.getDeclaredField("_biometryServiceState")
                biometryServiceStateField.isAccessible = true
                val biometryServiceStateFlow = biometryServiceStateField.get(BiometryServiceAccess) as MutableStateFlow<BiometryServiceState>

                // Vérifier l'état actuel avant connexion
                val initialState = biometryServiceStateFlow.value
                Timber.d("État du service avant connexion: $initialState")

                // Exécuter la connexion au service via réflexion
                connectServiceMethod.invoke(BiometryServiceAccess, context)

                // Attendre un peu pour que la connexion s'établisse
                delay(1000)

                // Vérifier l'état après tentative de connexion
                val currentState = biometryServiceStateFlow.value
                Timber.d("État du service après tentative de connexion: $currentState")

                // Vérifier si un service est disponible
                val serviceField = biometryServiceAccessClass.getDeclaredField("service")
                serviceField.isAccessible = true
                val service = serviceField.get(BiometryServiceAccess)

                if (service != null) {
                    Timber.d("Service biométrique initialisé avec succès via réflexion")
                } else {
                    Timber.w("Le service biométrique n'a pas pu être initialisé via réflexion")
                }

                // Vérifier l'état toutes les secondes pendant un certain temps
                var serviceReady = false
                for (iteration in 0 until 5) {
                    delay(1000)
                    val updatedState = biometryServiceStateFlow.value
                    Timber.d("État du service (itération ${iteration+1}): $updatedState")

                    if (updatedState == BiometryServiceState.IDLE) {
                        Timber.d("Le service est maintenant en état IDLE, prêt à utiliser")
                        serviceReady = true
                        break  // Maintenant le break est correct car il est dans une boucle for
                    }
                }

                if (!serviceReady) {
                    Timber.w("Le service n'a pas atteint l'état IDLE après 5 secondes")
                }

            } catch (e: Exception) {
                Timber.e("Erreur lors de la connexion au service via réflexion: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun verifyAllFingerprintsWithTracking(
        enrolledFingers: Array<EnrolledFinger>,
        fingersMatriculeMap: Map<String, String>,
        onMatchFound: (matricule: String, score: Int) -> Unit,
        onNoMatchFound: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Préparer le mapping des doigts aux matricules
                val fingerMatriculeMap = mutableMapOf<String, String>()
                fingersMatriculeMap.forEach { (key, matricule) ->
                    val parts = key.split(":")
                    if (parts.size >= 3) {
                        val handFinger = "${parts[1]}:${parts[2]}"
                        fingerMatriculeMap[handFinger] = matricule
                    }
                }

                Timber.d("Démarrage de la vérification avec ${enrolledFingers.size} empreintes")

                // Configurer la collecte de résultats avant de lancer la vérification
                val resultJob = launch {
                    // Collecter les résultats de correspondance
                    try {
                        // Collecter l'empreinte correspondante
                        lastEnrolledFinger.collect { matchedFinger ->
                            // Collecter le score de correspondance
                            val score = lastMatchingScore.value

                            if (matchedFinger != null && score != null && score > 0) {
                                Timber.d("Correspondance détectée! Doigt: ${matchedFinger.hand.name} ${matchedFinger.finger.name}, Score: $score")

                                // Trouver le matricule associé
                                val key = "${matchedFinger.hand.name}:${matchedFinger.finger.name}"
                                val matricule = fingerMatriculeMap[key]

                                if (matricule != null) {
                                    Timber.d("Matricule associé trouvé: $matricule")

                                    // Empreinte identifiée, appeler le callback et arrêter
                                    onMatchFound(matricule, score)
                                    this.cancel()
                                } else {
                                    Timber.w("Pas de matricule trouvé pour la clé: $key")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("Erreur lors de la collecte des résultats: ${e.message}")
                        if (e is kotlinx.coroutines.CancellationException) {
                            Timber.d("Collecte annulée normalement")
                        } else {
                            Timber.e("Exception inattendue", e)
                        }
                    }
                }

                // Surveiller les erreurs
                val errorJob = launch {
                    error.collect { failure ->
                        if (failure != null) {
                            Timber.e("Erreur de vérification: $failure")
                            resultJob.cancel()
                            onNoMatchFound()
                            this.cancel()
                        }
                    }
                }

                // Lancer une seule vérification avec toutes les empreintes
                verify(enrolledFingers)

                // Attendre un temps raisonnable pour la vérification
                delay(10000) // 10 secondes maximum pour la vérification

                // Si on arrive ici sans avoir annulé les jobs, c'est qu'aucune correspondance n'a été trouvée
                resultJob.cancel()
                errorJob.cancel()
                onNoMatchFound()

            } catch (e: Exception) {
                Timber.e("Exception lors de la vérification: ${e.message}")
                onNoMatchFound()
            }
        }
    }


    /*fun clearAllFingerprintsWithReflection() {
        Timber.i("clearAllFingerprints - Tentative d'effacement des empreintes avec réflexion")
        viewModelScope.launch {
            try {
                // Utiliser la réflexion pour accéder au champ privé _enrolledFingerArray
                val biometryServiceAccessClass = BiometryServiceAccess::class.java
                val enrolledFingerArrayField = biometryServiceAccessClass.getDeclaredField("_enrolledFingerArray")
                enrolledFingerArrayField.isAccessible = true

                // Récupérer l'objet MutableStateFlow
                val enrolledFingerArrayStateFlow = enrolledFingerArrayField.get(BiometryServiceAccess) as MutableStateFlow<Array<EnrolledFinger>>

                // Définir sa valeur à un tableau vide
                enrolledFingerArrayStateFlow.value = emptyArray()

                // Réinitialiser le compteur local
                _capturedFingerprints.value = 0

                // Continuer avec le cycle normal de déconnexion/reconnexion
                disconnectSensor()
                Thread.sleep(300)
                connectSensor()

                Timber.d("Empreintes réinitialisées avec succès via réflexion")
            } catch (e: Exception) {
                Timber.e("Erreur lors de la réinitialisation des empreintes via réflexion: ${e.message}")
                e.printStackTrace()
            }
        }
    }*/



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

    fun enroll2(hand: Hand,  finger: Finger,templateType: TemplateType, templates: Array<EnrolledFinger>) {
        Timber.i("enroll")
        viewModelScope.launch {
            //BiometryServiceAccess.enroll(templateType = templateType, enrolledFingerArray = templates)
            BiometryServiceAccess.enroll(hand, finger, templateType = TemplateType.RAW, enrolledFingerArray = templates )
            //fun enroll( hand: Hand = Hand.UNSPECIFIED, finger: Finger = Finger.UNSPECIFIED, templateType: TemplateType = TemplateType.RAW, enrolledFingerArray: Array<EnrolledFinger> )
        }
    }

    fun verify(templates: Array<EnrolledFinger>) {
        Timber.i("verify with ${templates.size} templates")
        try {
            viewModelScope.launch {
                BiometryServiceAccess.verify(templates)
            }
        }catch (e: Exception){
            Timber.e("Erreur ouverture22: ${e.message}")
        }

    }

    fun disconnectService(context: Context) {
        Timber.i("disconnectService")
        viewModelScope.launch {
            BiometryServiceAccess.disconnectService(context)
        }
    }



    fun getLibraryVersion() = BiometryServiceAccess.getLibraryVersion()

    fun setSkinTone(context: Context, skinTone: SkinTone) {
        viewModelScope.launch {
            BiometryServiceAccess.setSkinTone(context, skinTone)
        }
    }
}