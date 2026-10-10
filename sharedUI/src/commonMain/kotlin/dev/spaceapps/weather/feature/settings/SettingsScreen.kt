package dev.spaceapps.weather.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.core.settings.UnitSystem
import dev.spaceapps.weather.feature.settings.presentation.SettingsIntent
import dev.spaceapps.weather.feature.settings.presentation.SettingsState
import dev.spaceapps.weather.feature.settings.presentation.SettingsViewModel
import dev.spaceapps.weather.ui.theme.WeatherTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.settings_theme
import weatherapp.sharedui.generated.resources.settings_units
import weatherapp.sharedui.generated.resources.tab_settings
import weatherapp.sharedui.generated.resources.theme_dark
import weatherapp.sharedui.generated.resources.theme_light
import weatherapp.sharedui.generated.resources.theme_system
import weatherapp.sharedui.generated.resources.units_imperial
import weatherapp.sharedui.generated.resources.units_metric

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.tab_settings)) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            RadioSection(
                title = Res.string.settings_units,
                options = UnitSystem.entries,
                selected = state.unitSystem,
                label = { it.label },
                onSelect = { onIntent(SettingsIntent.ChangeUnitSystem(it)) },
            )
            HorizontalDivider()
            RadioSection(
                title = Res.string.settings_theme,
                options = ThemeMode.entries,
                selected = state.themeMode,
                label = { it.label },
                onSelect = { onIntent(SettingsIntent.ChangeThemeMode(it)) },
            )
        }
    }
}

@Composable
private fun <T> RadioSection(
    title: StringResource,
    options: List<T>,
    selected: T,
    label: (T) -> StringResource,
    onSelect: (T) -> Unit,
) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            ListItem(
                modifier = Modifier.selectable(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    role = Role.RadioButton,
                ),
                headlineContent = { Text(stringResource(label(option))) },
                leadingContent = { RadioButton(selected = option == selected, onClick = null) },
            )
        }
    }
}

private val UnitSystem.label: StringResource
    get() = when (this) {
        UnitSystem.METRIC -> Res.string.units_metric
        UnitSystem.IMPERIAL -> Res.string.units_imperial
    }

private val ThemeMode.label: StringResource
    get() = when (this) {
        ThemeMode.SYSTEM -> Res.string.theme_system
        ThemeMode.LIGHT -> Res.string.theme_light
        ThemeMode.DARK -> Res.string.theme_dark
    }

@Preview
@Composable
private fun SettingsScreenPreview() {
    WeatherTheme(ThemeMode.LIGHT) {
        SettingsScreen(state = SettingsState(UnitSystem.METRIC, ThemeMode.DARK), onIntent = {})
    }
}
