package dev.spaceapps.weather.feature.currentweather

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.spaceapps.weather.core.location.AndroidLocationProvider
import dev.spaceapps.weather.ui.util.findActivity

@Composable
internal actual fun rememberLocationPermissionRequester(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    var hasAskedBefore by rememberSaveable { mutableStateOf(false) }
    var awaitingSettings by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasAskedBefore = true
        onResult(grants.values.any { it })
    }

    // Returning from the app's system settings page: report whatever the user chose there.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (awaitingSettings) {
            awaitingSettings = false
            onResult(
                AndroidLocationProvider.LOCATION_PERMISSIONS.any {
                    context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
                },
            )
        }
    }

    return {
        val activity = context.findActivity()
        val showRationale = activity != null && AndroidLocationProvider.LOCATION_PERMISSIONS.any {
            activity.shouldShowRequestPermissionRationale(it)
        }
        // After a denial Android stops showing the dialog ("don't ask again"); the only way is settings.
        if (hasAskedBefore && !showRationale) {
            awaitingSettings = true
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } else {
            launcher.launch(AndroidLocationProvider.LOCATION_PERMISSIONS)
        }
    }
}
