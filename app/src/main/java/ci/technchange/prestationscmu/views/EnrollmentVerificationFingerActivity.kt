package ci.technchange.prestationscmu.views

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.famoco.biometryservicelibrary.data.EnrolledFinger
import ci.technchange.prestationscmu.R
import ci.technchange.prestationscmu.ui.composables.MainCard
import ci.technchange.prestationscmu.ui.theme.MainTheme
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ci.technchange.prestationscmu.core.DatabaseHelperKt
import ci.technchange.prestationscmu.core.dbHelper
import com.famoco.biometryservicelibrary.enums.Finger
import com.famoco.biometryservicelibrary.enums.Hand
import com.famoco.biometryservicelibrary.enums.TemplateType
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.famoco.biometryservicelibrary.enums.SensorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class EnrollmentVerificationFingerActivity : ComponentActivity() {
    private val viewModel: EnrollmentViewModel by viewModels()
    //private lateinit var matricule: String




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Récupération du matricule
        val matricule = intent.getStringExtra("MATRICULE") ?: ""

        // Récupérer l'ID temporaire des empreintes à vérifier
        // Récupérer le matricule de l'utilisateur
        val emptyTemplates = emptyArray<EnrolledFinger>()
        //val enrolledFingers = loadEnrolledFingerprints()


        //matricule = intent.getStringExtra("MATRICULE") ?: ""
        //viewModel.connectService(applicationContext)
        try {
            viewModel.connectService(applicationContext)
            //viewModel.connectSensor()
            // Attendre que le service soit connecté
            Thread.sleep(500)
            //viewModel.connectSensor()
            // Attendre que le capteur se connecte
            Thread.sleep(500)

            // Effectuer une vérification vide pour initialiser le protocole
            Timber.d("Initialisation du capteur avec une vérification vide")
            //viewModel.verify(emptyTemplates)

            // Cette vérification échouera, mais elle permettra d'initialiser le protocole de communication
            Timber.d("Initialisation terminée")

        } catch (e: Exception) {
            Timber.e("Erreur lors de la connexion au service biométrique: ${e.message}")
            Toast.makeText(this, "Erreur de connexion au service biométrique", Toast.LENGTH_LONG).show()
            finish() // Terminer l'activité si le service n'est pas disponible
            return
        }
        setContent {
            MainTheme {
                VerificationScreen(context = this, matricule)
            }
        }
    }


    private data class FingerWithMatricule(val finger: EnrolledFinger, val matricule: String)
    // Fonction pour récupérer les empreintes enregistrées de la base de données
    private fun loadEnrolledFingerprints(matricule: String): Pair<Array<EnrolledFinger>, Map<String, String>> {
        val enrolledFingers = mutableListOf<EnrolledFinger>()
        val fingerMatriculeMap = mutableMapOf<String, String>()

        try {
            val dbHelper = dbHelper(this)
            val db = dbHelper.readableDatabase



            val cursor = db.query(
                "empreintes",
                arrayOf("main", "doigt", "template", "matricule"),
                "matricule = ?", // Critère de sélection (clause WHERE)
                arrayOf(matricule), // Les arguments qui remplacent les "?" dans la condition
                null, null, null
            )

            while (cursor.moveToNext()) {
                val handStr = cursor.getString(cursor.getColumnIndexOrThrow("main"))
                val fingerStr = cursor.getString(cursor.getColumnIndexOrThrow("doigt"))
                val templateBlob = cursor.getBlob(cursor.getColumnIndexOrThrow("template"))
                //val matricule = cursor.getString(cursor.getColumnIndexOrThrow("matricule"))

                Timber.d("TTTTTTTSSSSS: ${matricule}")

                // Convertir les chaînes en types d'énumération
                val hand = Hand.valueOf(handStr)
                val finger = Finger.valueOf(fingerStr)

                // Désérialiser le template
                val templates = deserializeTemplates(templateBlob)

                // Créer l'objet EnrolledFinger et l'ajouter à la liste
                enrolledFingers.add(EnrolledFinger(hand, finger, templates))

                val fingerKey = "${hand.name}:${finger.name}:${templates}"
                fingerMatriculeMap[fingerKey] = matricule
            }

            cursor.close()
            db.close()

        } catch (e: Exception) {
            Timber.e("Erreur lors du chargement des empreintes: ${e.message}")
            Toast.makeText(this, "Erreur lors du chargement des empreintes", Toast.LENGTH_SHORT).show()
        }
        Timber.d("mmmmmmmmm: ${fingerMatriculeMap}")
        //return enrolledFingers.toTypedArray()
        return Pair(enrolledFingers.toTypedArray(), fingerMatriculeMap)

    }

    // Fonction pour désérialiser le template d'empreinte
    @Suppress("UNCHECKED_CAST")
    private fun deserializeTemplates(bytes: ByteArray): HashMap<TemplateType, ByteArray?> {
        try {
            val byteArrayInputStream = ByteArrayInputStream(bytes)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)
            return objectInputStream.readObject() as HashMap<TemplateType, ByteArray?>
        } catch (e: Exception) {
            Timber.e("Erreur de désérialisation: ${e.message}")
            return HashMap()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnectSensor()
        //viewModel.disconnectService(applicationContext)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.disconnectSensor()
        //viewModel.disconnectService(applicationContext)
    }

    fun extractRawData(key: String): String? {
        // Extraire la partie RAW entre les accolades dans la clé
        val rawRegex = Regex("RAW=\\[B@([a-fA-F0-9]+)") // Regex pour extraire l'adresse mémoire ou la représentation RAW
        val matchResult = rawRegex.find(key)

        // Si on trouve une correspondance, retourner l'adresse ou l'ID du tableau de bytes
        return matchResult?.groups?.get(1)?.value
    }

    @Composable
    fun VerificationScreen(context: Context, matricule: String, viewModel: EnrollmentViewModel = viewModel()) {
        val verificationResultState = remember { mutableStateOf("") }
        val lastMatchingScore = viewModel.lastMatchingScore.collectAsState()
        val lastMatchedFinger = viewModel.lastEnrolledFinger.collectAsState()
        val isVerifying = remember { mutableStateOf(false) }
        val sensorState = viewModel.sensorState.collectAsState()
        val error = viewModel.error.collectAsState()

        // Mémoriser les matricules des empreintes
        val fingersMatriculeMap = remember { mutableStateOf<Map<String, String>>(mapOf()) }

        // Effet pour gérer les résultats de vérification
        LaunchedEffect(lastMatchingScore.value) {
            if (isVerifying.value && lastMatchingScore.value != null) {
                isVerifying.value = false
                val score = lastMatchingScore.value
                if (score != null && score > 0) {
                    verificationResultState.value = "Vérification réussie avec un score de $score"



                    fingersMatriculeMap.value.forEach { (key, value) ->
                        //println("Clé : $key, Valeur : $value")
                    }

                    //delay(300)
                    if (matricule != null) {
                        val agent = getAgentByMatricule(context, matricule)
                        if (agent != null) {
                            delay(300)
                            Timber.e("Info agent: ${agent.toString()}")
                            // Créer l'intent et passer les données de l'agent
                            val intent = Intent(context, MainActivity::class.java).apply {
                                putExtra("AGENT_NOM", agent.nom)
                                putExtra("AGENT_PRENOM", agent.prenom)
                                putExtra("AGENT_MATRICULE", agent.matricule)
                                putExtra("AGENT_TELEPHONE", agent.telephone)
                                putExtra("AGENT_CENTRE", agent.centreSante)
                                // Ajouter d'autres informations si nécessaire
                            }
                            context.startActivity(intent)
                            if (context is Activity) {
                                context.finish()
                            }
                        } else {
                            verificationResultState.value = "Agent non trouvé pour cette empreinte"
                        }
                    } else {
                        // Pas de matricule trouvé, ouvrir MainActivity sans données
                        delay(300)
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                        if (context is Activity) {
                            context.finish()
                        }
                    }
                } else {
                    verificationResultState.value = "Empreinte non reconnue"
                }
            }
        }

        // Effet pour gérer les erreurs
        LaunchedEffect(error.value) {
            if (error.value != null && isVerifying.value) {
                isVerifying.value = false
                verificationResultState.value = "Erreur: Empreinte Inconnue."
                Timber.e(" ${error.value}")
                Timber.e("${error.toString()}")
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Vérification d'empreinte",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Afficher l'état du capteur
            /*Text(
                text = when (sensorState.value) {
                    SensorState.CONNECTED -> "Capteur connecté"
                    SensorState.DISCONNECTED -> "Capteur déconnecté"
                    null -> "État du capteur inconnu"
                },
                fontSize = 14.sp,
                color = if (sensorState.value == SensorState.CONNECTED) Color.Green else Color.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )*/

            Button(
                onClick = {
                    isVerifying.value = true
                    verificationResultState.value = ""
                    viewModel.connectService(applicationContext)
                    //viewModel.connectSensor()
                    // Attendre que le service soit connecté
                    Thread.sleep(500)

                    viewModel.viewModelScope.launch {
                        // Ne tentez la vérification que si le capteur est connecté
                        //if (sensorState.value == SensorState.CONNECTED) {
                        val (enrolledFingers, matriculeMap) = loadEnrolledFingerprints(matricule)
                        fingersMatriculeMap.value = matriculeMap

                        if (enrolledFingers.isEmpty()) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Aucune empreinte enregistrée",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isVerifying.value = false
                            }
                            return@launch
                        }
                        try {
                            viewModel.verify(enrolledFingers)
                        }catch (e: Exception){
                            Timber.e("Erreur ouverture: ${e.message}")
                        }
                        //viewModel.verify(enrolledFingers)
                        /*} else {
                            withContext(Dispatchers.Main) {
                                isVerifying.value = false
                                verificationResultState.value = "Le capteur n'est pas connecté"
                                Toast.makeText(
                                    context,
                                    "Le capteur n'est pas connecté, veuillez réessayer",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }*/
                    }
                },
                enabled = !isVerifying.value,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Vérifier l'empreinte",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp
                )
            }

            if (verificationResultState.value.isNotEmpty()) {
                Text(
                    text = verificationResultState.value,
                    fontSize = 16.sp,
                    color = if (verificationResultState.value.contains("réussie")) Color.Green else Color.Red,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }

    private fun getAgentByMatricule(context: Context, matricule: String): Agent? {
        var agent: Agent? = null

        try {
            val dbHelper = dbHelper(context)
            val db = dbHelper.readableDatabase

            val cursor = db.query(
                "agents_inscription",
                null,  // Toutes les colonnes
                "matricule = ?",
                arrayOf(matricule),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                agent = Agent(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nom = cursor.getString(cursor.getColumnIndexOrThrow("nom")),
                    prenom = cursor.getString(cursor.getColumnIndexOrThrow("prenom")),
                    telephone = cursor.getString(cursor.getColumnIndexOrThrow("telephone")),
                    matricule = cursor.getString(cursor.getColumnIndexOrThrow("matricule")),
                    centreSante = cursor.getString(cursor.getColumnIndexOrThrow("centre_sante")),
                    photoPath = cursor.getString(cursor.getColumnIndexOrThrow("photo_path")),
                    empreintes = cursor.getInt(cursor.getColumnIndexOrThrow("empreintes")),
                    photoFacadePath = cursor.getString(cursor.getColumnIndexOrThrow("photo_facade_path")),
                    photoInterieurPath = cursor.getString(cursor.getColumnIndexOrThrow("photo_interieur_path")),
                    latitudeFacade = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude_facade"))) null
                    else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude_facade")),
                    longitudeFacade = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude_facade"))) null
                    else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude_facade")),
                    latitudeInterieur = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude_interieur"))) null
                    else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude_interieur")),
                    longitudeInterieur = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude_interieur"))) null
                    else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude_interieur")),
                    dateInscription = cursor.getString(cursor.getColumnIndexOrThrow("date_inscription"))
                )
            }

            cursor.close()
            db.close()

        } catch (e: Exception) {
            Timber.e("Erreur lors de la récupération de l'agent: ${e.message}")
        }

        return agent
    }








    /*private fun verifyFingerprint(templates: Array<EnrolledFinger>) {
        if (templates.isEmpty()) {
            Toast.makeText(this, "Aucune empreinte enregistrée à vérifier", Toast.LENGTH_SHORT).show()
            return
        }

        // Lancer la vérification avec le viewModel
        viewModel.verify(templates)
    }*/
}

fun getUserAndFingerprints(context: Context, phoneNumber: String): Pair<EnrollmentActivity.Utilisateur?, List<EnrollmentActivity.Empreinte>> {
    val dbHelper = DatabaseHelperKt.getInstance(context)
    val db = dbHelper.readableDatabase

    var user: EnrollmentActivity.Utilisateur? = null
    val fingerprints = mutableListOf<EnrollmentActivity.Empreinte>()

    // Récupérer l'utilisateur
    val userCursor: Cursor = db.rawQuery(
        "SELECT * FROM utilisateurs WHERE phoneNumber = ? LIMIT 1",
        arrayOf(phoneNumber)
    )

    if (userCursor.moveToFirst()) {
        val userId = userCursor.getInt(userCursor.getColumnIndexOrThrow("id"))
        user = EnrollmentActivity.Utilisateur(
            id = userId,
            phoneNumber = userCursor.getString(userCursor.getColumnIndexOrThrow("phoneNumber")),
            nom = userCursor.getString(userCursor.getColumnIndexOrThrow("nom")),
            prenom = userCursor.getString(userCursor.getColumnIndexOrThrow("prenom")),
            centre = userCursor.getString(userCursor.getColumnIndexOrThrow("centre"))
        )

        // Récupérer les empreintes associées
        val fingerprintCursor: Cursor = db.rawQuery(
            "SELECT * FROM empreintes WHERE userId = ?",
            arrayOf(userId.toString())
        )

        while (fingerprintCursor.moveToNext()) {
            val fingerprint = EnrollmentActivity.Empreinte(
                id = fingerprintCursor.getInt(fingerprintCursor.getColumnIndexOrThrow("id")),
                userId = userId,
                main = fingerprintCursor.getString(fingerprintCursor.getColumnIndexOrThrow("main")),
                doigt = fingerprintCursor.getString(fingerprintCursor.getColumnIndexOrThrow("doigt")),
                rawTemplate = fingerprintCursor.getBlob(
                    fingerprintCursor.getColumnIndexOrThrow(
                        "rawTemplate"
                    )
                )
            )
            fingerprints.add(fingerprint)
        }
        fingerprintCursor.close()
    }
    userCursor.close()
    db.close()

    return Pair(user, fingerprints)
}




data class Agent(
    val id: Int,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val matricule: String,
    val centreSante: String,
    val photoPath: String?,
    val empreintes: Int,
    val photoFacadePath: String?,
    val photoInterieurPath: String?,
    val latitudeFacade: Double?,
    val longitudeFacade: Double?,
    val latitudeInterieur: Double?,
    val longitudeInterieur: Double?,
    val dateInscription: String?
)