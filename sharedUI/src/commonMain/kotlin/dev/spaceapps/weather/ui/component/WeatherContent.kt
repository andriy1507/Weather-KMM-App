package dev.spaceapps.weather.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.spaceapps.weather.feature.weather.presentation.CurrentWeatherUi
import dev.spaceapps.weather.feature.weather.presentation.WeatherError
import dev.spaceapps.weather.ui.message
import org.jetbrains.compose.resources.stringResource
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.cloudiness
import weatherapp.sharedui.generated.resources.error_title
import weatherapp.sharedui.generated.resources.feels_like
import weatherapp.sharedui.generated.resources.high_low
import weatherapp.sharedui.generated.resources.humidity
import weatherapp.sharedui.generated.resources.pressure
import weatherapp.sharedui.generated.resources.retry
import weatherapp.sharedui.generated.resources.sunrise
import weatherapp.sharedui.generated.resources.sunset
import weatherapp.sharedui.generated.resources.updated_at
import weatherapp.sharedui.generated.resources.visibility
import weatherapp.sharedui.generated.resources.wind

/** Full weather display shared by the current-location and city screens. Scrollable (for pull-to-refresh). */
@Composable
fun WeatherContent(
    weather: CurrentWeatherUi,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        header()

        AsyncImage(
            model = weather.iconUrl,
            contentDescription = weather.conditionText,
            modifier = Modifier.size(128.dp),
        )
        Text(weather.temperature, style = MaterialTheme.typography.displayLarge)
        Text(weather.conditionText, style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(Res.string.feels_like, weather.feelsLike) + "  ·  " +
                stringResource(Res.string.high_low, weather.maxTemperature, weather.minTemperature),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        WeatherDetails(weather)

        Text(
            text = stringResource(Res.string.updated_at, weather.updatedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeatherDetails(weather: CurrentWeatherUi) {
    val details = buildList {
        add(Detail(stringResource(Res.string.humidity), weather.humidity))
        add(Detail(stringResource(Res.string.wind), weather.wind, windDirectionDegrees = weather.windDirectionDegrees))
        add(Detail(stringResource(Res.string.pressure), weather.pressure))
        weather.visibility?.let { add(Detail(stringResource(Res.string.visibility), it)) }
        add(Detail(stringResource(Res.string.cloudiness), weather.cloudiness))
        add(Detail(stringResource(Res.string.sunrise), weather.sunrise))
        add(Detail(stringResource(Res.string.sunset), weather.sunset))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        details.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { DetailCard(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class Detail(
    val label: String,
    val value: String,
    val windDirectionDegrees: Int? = null,
)

@Composable
private fun DetailCard(detail: Detail, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = detail.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(detail.value, style = MaterialTheme.typography.titleLarge)
                detail.windDirectionDegrees?.let { degrees ->
                    // Meteorological degrees say where the wind comes from; the arrow shows where it blows.
                    Text(
                        text = "↑",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp).rotate(degrees + 180f),
                    )
                }
            }
        }
    }
}

/** Full-screen error with retry. Scrollable so pull-to-refresh also works from it. */
@Composable
fun ErrorContent(
    error: WeatherError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    primaryAction: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.error_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(error.message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        primaryAction()
        OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
    }
}

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Sample data for previews. */
internal val previewWeather = CurrentWeatherUi(
    location = "Kyiv, UA",
    conditionText = "Broken clouds",
    conditionId = 803,
    iconUrl = "https://openweathermap.org/img/wn/04d@4x.png",
    temperature = "12°",
    feelsLike = "11°",
    minTemperature = "11°",
    maxTemperature = "14°",
    humidity = "71%",
    pressure = "1015 hPa",
    wind = "4.1 m/s",
    windDirectionDegrees = 240,
    visibility = "10 km",
    cloudiness = "75%",
    sunrise = "07:00",
    sunset = "18:00",
    updatedAt = "10:02",
)
