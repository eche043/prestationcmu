package ci.technchange.prestationscmu.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCard(
    modifier: Modifier,
    colors: CardColors,
    border: BorderStroke? = null,
    title: String,
    painter: Painter,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        border = border,
        colors = colors,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center.also {
                Arrangement.Absolute.spacedBy(
                    30.dp,
                    Alignment.Start
                )
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painter,
                contentDescription = ""
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title)
        }
    }
}