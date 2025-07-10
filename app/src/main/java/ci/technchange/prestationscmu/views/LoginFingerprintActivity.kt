package ci.technchange.prestationscmu.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.famoco.biometryservicelibrary.data.EnrolledFinger
import com.famoco.biometryservicelibrary.enums.Finger
import com.famoco.biometryservicelibrary.enums.Hand
import com.famoco.biometryservicelibrary.enums.SensorState
import com.famoco.biometryservicelibrary.enums.TemplateType
import ci.technchange.prestationscmu.R
import ci.technchange.prestationscmu.core.DatabaseHelperKt
import ci.technchange.prestationscmu.ui.theme.MainTheme
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.util.*

class LoginFingerprintActivity : ComponentActivity() {
    private val viewModel: EnrollmentViewModel by viewModels()
    private var isVerifying = false
    private var matchedUser: AgentInfo? = null

    data class AgentInfo(
        val id: Long,
        val matricule: String,
        val nom: String,
        val prenom: String,
        val telephone: String,
        val centreSante: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("LoginFingerprintActivity created")

        viewModel.connectService(applicationContext)

        setContent {
            MainTheme {
                LoginScreen()
            }
        }
    }

    @Composable
    fun LoginScreen() {
        val sensorState = viewModel.sensorState.collectAsState()
        var instructionText by remember { mutableStateOf("Appuyez sur le capteur d'empreinte pour vous connecter") }
        var statusMessage by remember { mutableStateOf("") }
        var showStatusMessage by remember { mutableStateOf(false) }
        var showRetryButton by remember { mutableStateOf(false) }

        // Observer l'état du capteur
        LaunchedEffect(key1 = sensorState.value) {
            when (sensorState.value) {
                SensorState.CONNECTED -> {
                    Timber.d("Capteur connecté, prêt pour la vérification")
                    if (isVerifying) {
                        instructionText = "Placez votre doigt sur le capteur maintenant"
                    } else {
                        instructionText = "Appuyez sur le capteur d'empreinte pour vous connecter"
                    }
                }
                SensorState.DISCONNECTED -> {
                    Timber.d("Capteur déconnecté")
                    instructionText = "Capteur déconnecté. Veuillez réessayer."
                    showRetryButton = true
                }
                else -> {
                    Timber.d("État du capteur: ${sensorState.value}")
                }
            }
        }

        // Observer le score de correspondance
        LaunchedEffect(key1 = Unit) {
            viewModel.lastMatchingScore.collect { score ->
                Timber.d("Score de correspondance reçu: $score")
                if (score != null) {
                    if (score > 0) {
                        Timber.d("Match trouvé avec score: $score")
                        statusMessage = "Empreinte reconnue! Accès en cours..."
                        showStatusMessage = true
                        showRetryButton = false

                        if (matchedUser != null) {
                            // Authentification réussie
                            val returnIntent = Intent()
                            returnIntent.putExtra("AUTHENTICATED", true)
                            returnIntent.putExtra("AGENT_MATRICULE", matchedUser?.matricule)
                            returnIntent.putExtra("AGENT_NOM", matchedUser?.nom)
                            returnIntent.putExtra("AGENT_PRENOM", matchedUser?.prenom)
                            returnIntent.putExtra("AGENT_TELEPHONE", matchedUser?.telephone)
                            returnIntent.putExtra("AGENT_CENTRE", matchedUser?.centreSante)
                            setResult(Activity.RESULT_OK, returnIntent)
                            finish()
                        } else {
                            statusMessage = "Utilisateur non trouvé dans la base de données"
                            showStatusMessage = true
                            showRetryButton = true
                            isVerifying = false
                        }
                    } else if (score < 0 && isVerifying) {
                        Timber.d("Aucune correspondance trouvée")
                        isVerifying = false
                        statusMessage = "Empreinte non reconnue. Veuillez réessayer."
                        showStatusMessage = true
                        showRetryButton = true
                    }
                }
            }
        }

        // L'interface utilisateur
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val (title, subtitle, fingerprintCard, instructionTextView, statusText, retryButton) = createRefs()

            // Titre
            Text(
                text = "Connexion par empreinte",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )

            // Sous-titre
            Text(
                text = "Veuillez vous identifier avec votre empreinte digitale",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .constrainAs(subtitle) {
                        top.linkTo(title.bottom, margin = 16.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(horizontal = 16.dp)
            )

            // Carte d'empreinte cliquable (similaire à EnrollmentActivity)
            Card(
                modifier = Modifier
                    .size(180.dp, 200.dp)
                    .constrainAs(fingerprintCard) {
                        top.linkTo(subtitle.bottom, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .clickable {
                        // Cette fonction active le lecteur d'empreinte comme dans EnrollmentActivity
                        if (!isVerifying && sensorState.value == SensorState.CONNECTED) {
                            startVerification()
                            instructionText = "Placez votre doigt sur le capteur maintenant"
                            showStatusMessage = false
                        }
                    },
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_empreinte),
                            contentDescription = "Empreinte digitale",
                            modifier = Modifier
                                .size(100.dp)
                                .padding(8.dp)
                        )

                        Text(
                            text = "Appuyez pour scanner",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Instructions
            Text(
                text = instructionText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .constrainAs(instructionTextView) {
                        top.linkTo(fingerprintCard.bottom, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(horizontal = 16.dp)
            )

            // Message de statut
            if (showStatusMessage) {
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    color = if (statusMessage.contains("reconnue")) Color(0xFF4CAF50) else Color(0xFFE91E63),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .constrainAs(statusText) {
                            top.linkTo(instructionTextView.bottom, margin = 16.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .padding(horizontal = 16.dp)
                )
            }

            // Bouton "Réessayer"
            if (showRetryButton) {
                Button(
                    onClick = {
                        showStatusMessage = false
                        showRetryButton = false
                        instructionText = "Appuyez sur le capteur d'empreinte ci-dessus"
                    },
                    modifier = Modifier.constrainAs(retryButton) {
                        bottom.linkTo(parent.bottom, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                ) {
                    Text("Réessayer")
                }
            }
        }
    }

    private fun startVerification() {
        Timber.d("Tentative de démarrage de la vérification")
        if (!isVerifying) {
            isVerifying = true

            try {
                // Récupérer toutes les empreintes enregistrées
                val fingerprintsArray = loadAllFingerprints()

                if (fingerprintsArray.isEmpty()) {
                    isVerifying = false
                    Timber.e("Aucune empreinte trouvée dans la base de données")
                    Toast.makeText(
                        this@LoginFingerprintActivity,
                        "Aucune empreinte enregistrée dans la base de données",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                Timber.d("Lancement de la vérification avec ${fingerprintsArray.size} empreintes")
                // Lancer la vérification - c'est cette fonction qui active le capteur
                //viewModel.verify(fingerprintsArray)
            } catch (e: Exception) {
                Timber.e("Erreur lors du démarrage de la vérification: ${e.message}")
                isVerifying = false
                Toast.makeText(
                    this@LoginFingerprintActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Timber.d("Une vérification est déjà en cours")
        }
    }

    // Les autres fonctions restent inchangées...

    private fun loadAllFingerprints(): Array<EnrolledFinger> {
        val fingerprintsList = mutableListOf<EnrolledFinger>()
        val dbHelper = DatabaseHelperKt.getInstance(this)
        val db = dbHelper.readableDatabase

        try {
            Timber.d("Chargement des empreintes depuis la base de données")
            // Vérifier si la table des empreintes existe
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='empreintes'",
                null
            )
            val tableExists = cursor?.count ?: 0 > 0
            cursor?.close()

            if (!tableExists) {
                Timber.e("La table 'empreintes' n'existe pas dans la base de données")
                return emptyArray()
            }

            // Récupérer toutes les empreintes
            val empreintesCursor = db.query(
                "empreintes",
                null,
                null,
                null,
                null,
                null,
                null
            )

            if (empreintesCursor != null && empreintesCursor.count > 0) {
                Timber.d("${empreintesCursor.count} empreintes trouvées dans la base de données")

                while (empreintesCursor.moveToNext()) {
                    // Récupérer les indices des colonnes
                    val matriculeIndex = empreintesCursor.getColumnIndex("matricule")
                    val mainIndex = empreintesCursor.getColumnIndex("main")
                    val doigtIndex = empreintesCursor.getColumnIndex("doigt")
                    val templateIndex = empreintesCursor.getColumnIndex("template")

                    if (matriculeIndex >= 0 && mainIndex >= 0 && doigtIndex >= 0 && templateIndex >= 0) {
                        val matricule = empreintesCursor.getString(matriculeIndex)
                        val main = empreintesCursor.getString(mainIndex)
                        val doigt = empreintesCursor.getString(doigtIndex)
                        val templateBytes = empreintesCursor.getBlob(templateIndex)

                        try {
                            // Convertir les valeurs de texte en énumérations
                            val hand = try { Hand.valueOf(main) } catch (e: Exception) {
                                Timber.e("Erreur lors de la conversion de la main: $main")
                                Hand.RIGHT // Valeur par défaut
                            }

                            val finger = try { Finger.valueOf(doigt) } catch (e: Exception) {
                                Timber.e("Erreur lors de la conversion du doigt: $doigt")
                                Finger.INDEX // Valeur par défaut
                            }

                            // Désérialiser le template
                            val templates = deserializeHashMap(templateBytes)

                            // Créer l'objet EnrolledFinger
                            val enrolledFinger = EnrolledFinger(
                                hand = hand,
                                finger = finger,
                                templates = templates
                            )

                            fingerprintsList.add(enrolledFinger)

                            // Charger les informations de l'agent
                            val agentInfo = getAgentInfo(matricule)
                            if (agentInfo != null) {
                                matchedUser = agentInfo
                                Timber.d("Informations de l'agent trouvées pour $matricule")
                            }

                            Timber.d("Empreinte chargée: Main=$main, Doigt=$doigt, Matricule=$matricule")
                        } catch (e: Exception) {
                            Timber.e("Erreur lors du traitement de l'empreinte: ${e.message}")
                            e.printStackTrace()
                        }
                    } else {
                        Timber.e("Structure de la table incorrecte")
                    }
                }

                empreintesCursor.close()
            } else {
                Timber.e("Aucune empreinte trouvée dans la base de données")
                if (empreintesCursor != null) {
                    empreintesCursor.close()
                }
            }
        } catch (e: Exception) {
            Timber.e("Erreur lors du chargement des empreintes: ${e.message}")
            e.printStackTrace()
        } finally {
            db.close()
        }

        Timber.d("${fingerprintsList.size} empreintes chargées avec succès")
        return fingerprintsList.toTypedArray()
    }

    private fun getAgentInfo(matricule: String): AgentInfo? {
        val dbHelper = DatabaseHelperKt.getInstance(this)
        val db = dbHelper.readableDatabase

        try {
            val cursor = db.query(
                "agents_inscription",
                null,
                "matricule = ?",
                arrayOf(matricule),
                null, null, null
            )

            if (cursor != null && cursor.moveToFirst()) {
                // Récupérer les indices des colonnes
                val idIndex = cursor.getColumnIndex("id")
                val nomIndex = cursor.getColumnIndex("nom")
                val prenomIndex = cursor.getColumnIndex("prenom")
                val telephoneIndex = cursor.getColumnIndex("telephone")
                val centreSanteIndex = cursor.getColumnIndex("centre_sante")

                // Récupérer les valeurs
                val id = if (idIndex >= 0) cursor.getLong(idIndex) else -1
                val nom = if (nomIndex >= 0) cursor.getString(nomIndex) else ""
                val prenom = if (prenomIndex >= 0) cursor.getString(prenomIndex) else ""
                val telephone = if (telephoneIndex >= 0) cursor.getString(telephoneIndex) else ""
                val centreSante = if (centreSanteIndex >= 0) cursor.getString(centreSanteIndex) else ""

                cursor.close()

                return AgentInfo(id, matricule, nom, prenom, telephone, centreSante)
            }

            cursor?.close()
        } catch (e: Exception) {
            Timber.e("Erreur lors de la récupération des informations de l'agent: ${e.message}")
        } finally {
            db.close()
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeHashMap(bytes: ByteArray): HashMap<TemplateType, ByteArray?> {
        try {
            val byteStream = ByteArrayInputStream(bytes)
            val objectStream = ObjectInputStream(byteStream)
            val map = objectStream.readObject() as HashMap<TemplateType, ByteArray?>
            objectStream.close()
            return map
        } catch (e: Exception) {
            Timber.e("Erreur lors de la désérialisation: ${e.message}")
            e.printStackTrace()
            return HashMap()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnectSensor()
        viewModel.disconnectService(applicationContext)
        Timber.d("LoginFingerprintActivity détruite")
    }
}