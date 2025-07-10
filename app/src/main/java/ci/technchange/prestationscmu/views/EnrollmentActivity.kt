package ci.technchange.prestationscmu.views

//  $$$$$$$$$$$$$$

//$$$$$$$

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import ci.technchange.prestationscmu.R
import ci.technchange.prestationscmu.core.DatabaseHelperKt
import ci.technchange.prestationscmu.core.dbHelper
import ci.technchange.prestationscmu.ui.theme.MainTheme
import com.famoco.biometryservicelibrary.BiometryServiceAccess
import com.famoco.biometryservicelibrary.data.EnrolledFinger
import com.famoco.biometryservicelibrary.enums.Finger
import com.famoco.biometryservicelibrary.enums.Hand
import com.famoco.biometryservicelibrary.enums.TemplateType
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

class EnrollmentActivity : ComponentActivity() {
    var phoneNumber: String = ""
    var nom: String = ""
    var prenom: String = ""
    var centre: String = ""
    var photoPath: String = ""
    var idUser = 0L;


    private val viewModel: EnrollmentViewModel by viewModels()

    data class Utilisateur(
        val id: Int = 0,
        val phoneNumber: String,
        val nom: String,
        val prenom: String,
        val centre: String
    )

    data class Empreinte(
        val id: Int = 0,
        val userId: Int,
        val main: String,
        val doigt: String,
        val rawTemplate: ByteArray
    )


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_recuperer_empreinte_main)

        // IMPORTANT: Réinitialiser l'état des empreintes avant de connecter le service
        if (viewModel.enrolledFingerArray.value.isNotEmpty()) {
            // Si des empreintes existent déjà, on les efface
            Timber.d("Empreintes existantes détectées au démarrage, tentative d'effacement...")
            //viewModel.clearAllFingerprints()
        }
        viewModel.clearAllFingerprintsWithReflection()
        viewModel.connectService(applicationContext)

        phoneNumber = intent.getStringExtra("TELEPHONE").toString()
        nom = intent.getStringExtra("NOM").toString()
        prenom = intent.getStringExtra("PRENOM").toString()
        centre = intent.getStringExtra("CENTRE_SANTE").toString()
        photoPath = intent.getStringExtra("PHOTO_PATH").toString()



        // Configuration des éléments UI du layout XML
        //configureUI()
        setContent {
            MainTheme {
                EnrollmentScreen()
            }
        }
    }

    private fun logEnrolledFingers() {
        if (viewModel.enrolledFingerArray.value.isEmpty()) {
            Timber.d("Aucune empreinte enregistrée")
        } else {
            viewModel.enrolledFingerArray.value.forEach { finger ->
                Timber.d("Empreinte: Main=${finger.hand}, Doigt=${finger.finger}, Templates=${finger.templates}")
            }
        }
    }

    private fun configureUI() {
        // Bouton Retour

        /*val sensorState: SensorState by viewModel.sensorState.collectAsStateWithLifecycle()

        val lastEnrolledFinger by viewModel.lastEnrolledFinger.collectAsStateWithLifecycle()
        val lastMatchingScore by viewModel.lastMatchingScore.collectAsStateWithLifecycle()*/

        // Bouton Enregistrer Empreintes


        // Boutons pour effacer les empreintes
        findViewById<Button>(R.id.effacerIndexDroit).setOnClickListener {
            Toast.makeText(this, "Index droit effacé", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.effacerIndexGauche).setOnClickListener {
            Toast.makeText(this, "Index gauche effacé", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.effacerPouceDroit).setOnClickListener {
            Toast.makeText(this, "Pouce droit effacé", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.effacerPouceGauche).setOnClickListener {
            Toast.makeText(this, "Pouce gauche effacé", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.BtnImageIndexDroit).setOnClickListener {
            viewModel.enroll(TemplateType.RAW, viewModel.enrolledFingerArray.value)
            //viewModel.enrollSpecificFinger(applicationContext, TemplateType.RAW, viewModel.enrolledFingerArray.value)
            //val enrolledToFinger = EnrolledFinger(Hand.RIGHT, Finger.INDEX, hashMapOf());
            //viewModel.enroll(TemplateType.RAW, arrayOf(enrolledToFinger))
            Toast.makeText(this, "INDEX droit capturé", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.BtnImageIndexGauche).setOnClickListener {
            viewModel.enroll(TemplateType.RAW, viewModel.enrolledFingerArray.value)
            //val enrolledToFinger = EnrolledFinger(Hand.LEFT, Finger.INDEX, hashMapOf());
            //viewModel.enroll(TemplateType.RAW, arrayOf(enrolledToFinger))
            //viewModel.enrollSpecificFinger(applicationContext, TemplateType.RAW, viewModel.enrolledFingerArray.value)
            Toast.makeText(this, " INDEX gauche capturé", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.BtnImagePouceDroit).setOnClickListener {
            viewModel.enroll(TemplateType.RAW, viewModel.enrolledFingerArray.value)
            //val enrolledToFinger = EnrolledFinger(Hand.RIGHT, Finger.THUMB, hashMapOf());
            //viewModel.enroll(TemplateType.RAW, arrayOf(enrolledToFinger))
            Toast.makeText(this, "Pouce droit capturé", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.BtnImagePouceGauche).setOnClickListener {
            viewModel.enroll(TemplateType.RAW, viewModel.enrolledFingerArray.value)
            //val enrolledToFinger = EnrolledFinger(Hand.LEFT, Finger.THUMB, hashMapOf());
            //viewModel.enrollSpecificFinger(applicationContext, TemplateType.RAW, viewModel.enrolledFingerArray.value)
            //viewModel.enroll(TemplateType.RAW, arrayOf(enrolledToFinger))
            Toast.makeText(this, "Pouce gauche capturé", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearAllFingerprints()
        viewModel.disconnectSensor()
        viewModel.disconnectService(applicationContext)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.disconnectSensor()
        viewModel.disconnectService(applicationContext)
    }



    fun serializeHashMap(map: HashMap<TemplateType, ByteArray?>): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val objectStream = ObjectOutputStream(byteStream)
        objectStream.writeObject(map)
        objectStream.close()
        return byteStream.toByteArray()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("StringFormatInvalid", "StateFlowValueCalledInComposition")
    @Composable
    fun EnrollmentScreen(viewModel: EnrollmentViewModel = viewModel()){
        val capturedFingerprints by viewModel.capturedFingerprints // Observer le nombre d'empreintes
        Timber.d("nombre d'empreinte"+viewModel.enrolledFingerArray.value.size)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titre
            Text(
                text = "Enregistrement des empreintes",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Sous-titre
            Text(
                text = "Veuillez enregistrer les empreintes des doigts suivants (au moins 2) :",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Grille des empreintes
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(4) { index ->
                    val fingerName = when (index) {
                        0 -> "Index gauche"
                        1 -> "Index droit"
                        2 -> "Pouce gauche"
                        else -> "Pouce droit"
                    }
                    val myFinger = when (index) {
                        0 -> Finger.INDEX
                        1 -> Finger.INDEX
                        2 -> Finger.THUMB
                        else -> Finger.THUMB
                    }
                    val myHand = when (index) {
                        0 -> Hand.LEFT
                        1 -> Hand.RIGHT
                        2 -> Hand.LEFT
                        else -> Hand.RIGHT
                    }
                    FingerCard(
                        title = fingerName,
                        onClick = {
                            Timber.i("doigt choisi: " + fingerName + "-" + myHand + "-" + myFinger)
                            val nbrEmp = viewModel.enrolledFingerArray.value.size
                            viewModel.enroll2(
                                myHand,
                                myFinger,
                                TemplateType.RAW,
                                viewModel.enrolledFingerArray.value
                            )
                            if (viewModel.enrolledFingerArray.value.size > nbrEmp)
                                viewModel.incrementFingerprintCount()
                            else
                                Timber.i("pas d'empreinte enregistrée")
                        },
                        onDeleteClick = { logEnrolledFingers() }
                    )
                }
            }
            // Bouton Enregistrer
            // Dans la méthode EnrollmentScreen du fichier EnrollmentActivity.kt, remplacez le code du bouton "Terminer" :

            Button(

                onClick = {
                    if (viewModel.enrolledFingerArray.value.size < 2) {
                        // Afficher un avertissement mais permettre quand même de continuer
                        Toast.makeText(
                            this@EnrollmentActivity,
                            "Attention: Moins de 2 empreintes enregistrées. Il est recommandé d'en avoir au moins 2.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // Stockage temporaire des empreintes dans la base de données avec un identifiant unique
                    val tempId = System.currentTimeMillis().toString()

                    try {
                        // Créer ou accéder à la base de données
                        val dbHelper = dbHelper(this@EnrollmentActivity);
                        val db = dbHelper.getWritableDatabase();

                        val createTableQuerys = "CREATE TABLE IF NOT EXISTS temp_empreintes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "temp_id TEXT NOT NULL, " +
                                "main TEXT NOT NULL, " +
                                "doigt TEXT NOT NULL, " +
                                "template BLOB NOT NULL)"
                        db.execSQL(createTableQuerys)

                        var indexDroit = false
                        var indexGauche = false
                        var pouceDroit = false
                        var pouceGauche = false

                        // IMPORTANT: Garder trace des empreintes réellement traitées pendant cette session
                        //val processedFingerprints = mutableSetOf<String>()

                        // Stocker chaque empreinte capturée
                        viewModel.enrolledFingerArray.value.forEach { enrolledFinger ->
                            try {
                                // Créer une clé unique pour cette empreinte




                                // Sérialiser les données biométriques
                                val templateBytes = serializeHashMap(enrolledFinger.templates)

                                // Insérer dans la base de données temporaire
                                val values = ContentValues().apply {
                                    put("temp_id", tempId)
                                    put("main", enrolledFinger.hand.name)
                                    put("doigt", enrolledFinger.finger.name)
                                    put("template", templateBytes)
                                }

                                Timber.d("Enrollement hand"+enrolledFinger.hand)
                                Timber.d("Enrollement finger"+enrolledFinger.finger)

                                // Déterminer quel doigt est enregistré
                                when {
                                    enrolledFinger.hand == Hand.RIGHT && enrolledFinger.finger == Finger.INDEX -> indexDroit = true
                                    enrolledFinger.hand == Hand.LEFT && enrolledFinger.finger == Finger.INDEX -> indexGauche = true
                                    enrolledFinger.hand == Hand.RIGHT && enrolledFinger.finger == Finger.THUMB -> pouceDroit = true
                                    enrolledFinger.hand == Hand.LEFT && enrolledFinger.finger == Finger.THUMB -> pouceGauche = true
                                }


                                db.insert("temp_empreintes", null, values)
                                Timber.d("Empreinte temporaire stockée: Main=${enrolledFinger.hand.name}, Doigt=${enrolledFinger.finger.name}, templates=${enrolledFinger.templates}, bites=${templateBytes}")


                            } catch (e: Exception) {
                                Timber.e("Erreur lors de la sérialisation/stockage de l'empreinte: ${e.message}")
                                Log.d("TAG", "EnrollmentScreen: erreur stockage")
                            }
                        }

                        db.close()




                        // Parcourir les empreintes capturées
                        /*viewModel.enrolledFingerArray.value.forEach { finger ->
                            when {
                                finger.hand == Hand.RIGHT && finger.finger == Finger.INDEX -> indexDroit =
                                    true

                                finger.hand == Hand.LEFT && finger.finger == Finger.INDEX -> indexGauche =
                                    true

                                finger.hand == Hand.RIGHT && finger.finger == Finger.THUMB -> pouceDroit =
                                    true

                                finger.hand == Hand.LEFT && finger.finger == Finger.THUMB -> pouceGauche =
                                    true
                            }
                        }*/

                        // Créer un Intent pour retourner uniquement l'identifiant et les informations de base
                        val returnIntent = Intent()
                        returnIntent.putExtra("EMPREINTES_VALIDES", true)
                        returnIntent.putExtra("TEMP_ID", tempId)
                        returnIntent.putExtra(
                            "NB_EMPREINTES",
                            viewModel.enrolledFingerArray.value.size
                        )
                        returnIntent.putExtra("INDEX_DROIT", indexDroit)
                        returnIntent.putExtra("INDEX_GAUCHE", indexGauche)
                        returnIntent.putExtra("POUCE_DROIT", pouceDroit)
                        returnIntent.putExtra("POUCE_GAUCHE", pouceGauche)

                        // Log pour le débogage
                        Timber.d("Empreintes envoyées à InscriptionStepper: IndexD=$indexDroit, IndexG=$indexGauche, PouceD=$pouceDroit, PouceG=$pouceGauche")

                        // Définir le résultat et terminer l'activité
                        setResult(Activity.RESULT_OK, returnIntent)

                        //viewModel.clearAllFingerprints()
                        //prepareForFingerprintEnrollment()
                        finish()

                    } catch (e: Exception) {
                        Log.d("TAG", "EnrollmentScreen: erreur creation: ${e.message}")
                        Timber.e("Erreur lors du traitement des empreintes: ${e.message}")
                        Toast.makeText(
                            this@EnrollmentActivity,
                            "Erreur lors du traitement des empreintes: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                //colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = true
            ) {
                Text(
                    text = "<- Terminer",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp
                )
            }
        }

    }

    private fun prepareForFingerprintEnrollment() {
        // Déconnecter le service s'il est en cours d'exécution
        try {
            val context = applicationContext
            //BiometryServiceAccess.disconnectService(context)
            BiometryServiceAccess.disconnectSensor()
            BiometryServiceAccess.disconnectService(applicationContext)



            // Petit délai pour s'assurer que le service est bien arrêté
            Thread.sleep(500)
        } catch (e: Exception) {
            Log.e("TAG", "Erreur lors de la déconnexion du service: ${e.message}")
        }
    }
    @Composable
    fun FingerCard(title: String, onClick: () -> Unit, onDeleteClick: () -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Card(
                modifier = Modifier
                    .size(120.dp, 140.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_empreinte), // Pour PNG
                        contentDescription = "Empreinte digitale",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Text(
                text = title,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = onDeleteClick,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(100.dp)
                    .height(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(
                    text = "Effacer",
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
    }


    fun saveUserAndFingerprints(
        context: Context,
        phoneNumber: String,
        nom: String,
        prenom: String,
        centre: String,
        enrolledFingerArray: List<Empreinte>
    ) {
        val dbHelper = DatabaseHelperKt.getInstance(context)
        val db = dbHelper.writableDatabase
        var userId = 0L;

        db.beginTransaction()
        try {
            // Insérer l'utilisateur
            val userValues = ContentValues().apply {
                put("phoneNumber", phoneNumber)
                put("nom", nom)
                put("prenom", prenom)
                put("centre", centre)
            }

            userId = db.insert("utilisateurs", null, userValues)

            if (userId == -1L) {
                throw Exception("Échec de l'insertion de l'utilisateur")
            }

            // Insérer les empreintes
            for (fingerprint in enrolledFingerArray) {
                val fingerprintValues = ContentValues().apply {
                    put("userId", userId.toInt())
                    put("main", fingerprint.main)
                    put("doigt", fingerprint.doigt)
                    put("rawTemplate", fingerprint.rawTemplate)

                }
                db.insert("empreintes", null, fingerprintValues)
            }

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
            db.close()
            idUser = userId
        }
    }
}

