package dev.spaceapps.weather.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.back
import weatherapp.sharedui.generated.resources.ic_arrow_back

@Composable
fun IconAction(icon: DrawableResource, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = contentDescription)
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconAction(Res.drawable.ic_arrow_back, stringResource(Res.string.back), onClick)
}
