package ci.technchange.prestationscmu.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun InfoDialog(
    icon: @Composable (() -> Unit)? = null,
    dialogTitle: String,
    dialogText: String,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = icon,
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        })
}