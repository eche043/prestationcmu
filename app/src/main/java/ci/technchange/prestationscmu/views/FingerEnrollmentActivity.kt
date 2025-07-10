package ci.technchange.prestationscmu.views

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.famoco.biometryservicelibrary.enums.TemplateType
import com.famoco.biometryservicelibrary.data.EnrolledFinger
import com.famoco.biometryservicelibrary.enums.Hand
import com.famoco.biometryservicelibrary.enums.Finger
//import ci.technchange.prestationscmu.R
import timber.log.Timber

class FingerEnrollmentActivity : ComponentActivity() {
    private val viewModel: FingerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Vérifiez les données d'intent si nécessaire
        intent?.extras?.let { bundle ->
            // Traitez les extras ici
        } ?: run {
            Timber.w("Aucun extra dans l'intent")
        }

        setContent {
            MaterialTheme {
                FingerEnrollmentScreen(
                    onEnrollComplete = { fingers ->
                        fingers?.let {
                            viewModel.enroll(TemplateType.RAW, it)
                            finish()
                        } ?: Timber.e("Liste de doigts null")
                    }
                )
            }
        }
    }
}
@Composable
fun FingerEnrollmentScreen(
    onEnrollComplete: (Array<EnrolledFinger>) -> Unit
) {
    val enrolledFingers = remember { mutableStateListOf<EnrolledFinger>() }
    val fingersToCapture = remember {
        listOf(
            Pair(Hand.RIGHT, Finger.THUMB),
            Pair(Hand.RIGHT, Finger.INDEX),
            Pair(Hand.LEFT, Finger.THUMB),
            Pair(Hand.LEFT, Finger.INDEX)
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Capture de 4 doigts spécifiques",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))


        Text("Doigts à capturer :")
        fingersToCapture.forEachIndexed { index, (hand, finger) ->
            val captured = index < enrolledFingers.size
            Text(
                text = "${index + 1}. ${getHandName(hand)} ${getFingerName(finger)} ${if (captured) "✓" else ""}",
                color = if (captured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                try {
                    if (enrolledFingers.size < fingersToCapture.size) {
                        val (hand, finger) = fingersToCapture[enrolledFingers.size]
                        enrolledFingers.add(EnrolledFinger(hand, finger))

                        if (enrolledFingers.size == fingersToCapture.size) {
                            onEnrollComplete(enrolledFingers.toTypedArray())
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Erreur lors de l'ajout d'un doigt")
                }
            },
            enabled = enrolledFingers.size < fingersToCapture.size
        ) {
            Text(if (enrolledFingers.isEmpty()) "Commencer la capture" else "Capturer le doigt suivant")
        }
    }
}

// Fonctions d'aide pour traduire les noms en français
fun getHandName(hand: Hand): String {
    return when (hand) {
        Hand.RIGHT -> "Main droite"
        Hand.LEFT -> "Main gauche"
        else -> hand.name
    }
}

fun getFingerName(finger: Finger): String {
    return when (finger) {
        Finger.THUMB -> "Pouce"
        Finger.INDEX -> "Index"
        Finger.MIDDLE -> "Majeur"
        Finger.RING -> "Annulaire"
        Finger.LITTLE -> "Auriculaire"
        else -> finger.name
    }
}
/*@Composable
fun FingerEnrollmentScreen(
    onEnrollComplete: (Array<EnrolledFinger>) -> Unit
) {
    val enrolledFingers = remember { mutableListOf<EnrolledFinger>() }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Capture 4 Specific Fingers",
            style = MaterialTheme.typography.titleLarge)

        /*Button(onClick = {
            enrolledFingers.add(EnrolledFinger(Hand.RIGHT, Finger.THUMB))
            if (enrolledFingers.size == 4) {
                onEnrollComplete(enrolledFingers.toTypedArray())
            }
        }) **/

        Button(onClick = {
            try {
                enrolledFingers.add(EnrolledFinger(Hand.RIGHT, Finger.THUMB))
                if (enrolledFingers.size == 4) {
                    onEnrollComplete(enrolledFingers.toTypedArray())
                }
            } catch (e: Exception) {
                Timber.e(e, "Erreur lors de l'ajout d'un doigt")
            }
        }) {
            Text("Capture Finger")
        }
    }
}**/


@Preview
@Composable
private fun PreviewFingerEnrollment() {
    MaterialTheme {
        FingerEnrollmentScreen(
            onEnrollComplete = { fingers ->
                Timber.d("Prévisualisation: ${fingers.size} doigts enregistrés")
            }
        )
    }
}

/*@Preview(showBackground = true)
@Composable
fun FingerEnrollmentScreenPreview() {
    MaterialTheme {
        FingerEnrollmentScreen(
            onEnrollComplete = { /* Ne rien faire pour la prévisualisation */ }
        )
    }
}**/



/*package ci.technchange.prestationscmu.views

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.xtooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.famoco.biometryservicelibrary.enums.TemplateType
import com.famoco.biometryservicelibrary.data.EnrolledFinger
import ci.technchange.prestationscmu.R
import com.famoco.biometryservicesample.ui.theme.MainTheme
import timber.log.Timber

class FingerEnrollmentActivity : ComponentActivity() {

    private val viewModel: FingerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainTheme {
                FingerEnrollmentScreen()
            }
        }
    }

    @Composable
    fun FingerEnrollmentScreen() {
        val enrolledFingers = remember { mutableListOf<EnrolledFinger>() }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Capture 4 Specific Fingers", style = MaterialTheme.typography.titleLarge)

            // Vous pouvez ajouter un formulaire ou une interface pour capturer les empreintes
            Button(onClick = {
                // Simule l'enrôlement d'une empreinte (ici vous aurez une logique réelle d'enrôlement)
                enrolledFingers.add(EnrolledFinger("Right Hand", "Thumb")) // exemple
                if (enrolledFingers.size == 4) {
                    // Après capture de 4 doigts
                    viewModel.enroll(TemplateType.RAW, enrolledFingers.toTypedArray())
                    // Retour à l'activité précédente
                    finish()
                }
            }) {
                Text("Capture Finger")
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun FingerEnrollmentScreenPreview() {
        MainTheme {
            FingerEnrollmentScreen()
        }
    }
}**/
