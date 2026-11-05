package dev.busung.roots24

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.busung.roots24.ui.theme.RootS24Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val installViewModel by viewModels<InstallViewModel>()
    private var resumedOnce = false
    private var accentColor by mutableStateOf(AccentColor.Dynamic)
    private var themeMode by mutableStateOf(AppThemeMode.System)
    private var shizukuMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        accentColor = AppPreferences.accentColor(this)
        themeMode = AppPreferences.themeMode(this)
        shizukuMode = AppPreferences.shizukuMode(this)
        setContent {
            RootS24Theme(accentColor = accentColor, themeMode = themeMode) {
                RootApp(
                    installViewModel = installViewModel,
                    accentColor = accentColor,
                    themeMode = themeMode,
                    shizukuMode = shizukuMode,
                    onAccentColorChanged = { color ->
                        AppPreferences.setAccentColor(this, color)
                        accentColor = color
                    },
                    onThemeModeChanged = { mode ->
                        AppPreferences.setThemeMode(this, mode)
                        themeMode = mode
                    },
                    onShizukuModeChanged = { enabled ->
                        AppPreferences.setShizukuMode(this, enabled)
                        shizukuMode = enabled
                    },
                    openInstaller = {
                        startActivity(
                            Intent(this, InstallActivity::class.java)
                                .putExtra(InstallActivity.EXTRA_INSTALL_REQUEST_ID, UUID.randomUUID().toString()),
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (resumedOnce) installViewModel.refresh() else resumedOnce = true
    }
}

private enum class AppPage(@StringRes val label: Int, val icon: ImageVector) {
    Overview(R.string.nav_overview, Icons.Rounded.Home),
    History(R.string.nav_history, Icons.Rounded.History),
    Settings(R.string.nav_settings, Icons.Rounded.Settings),
}

private const val KERNEL_SU_PACKAGE = "me.weishu.kernelsu"
private const val KERNEL_SU_NEXT_PACKAGE = "com.rifsxd.ksunext"
private const val KERNEL_SU_URL =
    "https://github.com/tiann/KernelSU/releases/download/v3.3.0/KernelSU_v3.3.0_32601-release.apk"
private const val KERNEL_SU_HOME_URL = "https://kernelsu.org/"
private const val SHIZUKU_MANAGER_PACKAGE = "moe.shizuku.manager"
private const val SHIZUKU_MANAGER_URL = "https://github.com/thedjchi/Shizuku/releases/"

private fun openKernelSuManager(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(KERNEL_SU_PACKAGE)
        ?: context.packageManager.getLaunchIntentForPackage(KERNEL_SU_NEXT_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(KERNEL_SU_URL)))
    }
}

private data class LanguageOption(@StringRes val label: Int, val tag: String)

private val languageOptions = listOf(
    LanguageOption(R.string.language_system, ""),
    LanguageOption(R.string.language_korean, "ko"),
    LanguageOption(R.string.language_english, "en"),
    LanguageOption(R.string.language_japanese, "ja"),
    LanguageOption(R.string.language_chinese, "zh-CN"),
    LanguageOption(R.string.language_chinese_traditional, "zh-TW"),
    LanguageOption(R.string.language_turkish, "tr"),
    LanguageOption(R.string.language_brazillian_portuguese, "pt-BR"),
    LanguageOption(R.string.language_russian, "ru"),
    LanguageOption(R.string.language_vietnamese, "vi"),
    LanguageOption(R.string.language_uzbek, "uz"),
)

private fun openShizukuManager(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_MANAGER_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_MANAGER_URL)))
    }
}

private fun clickHaptic(view: View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        },
    )
}

@Composable
private fun DialogDimAmount(amount: Float) {
    val window = (LocalView.current.parent as DialogWindowProvider).window
    SideEffect { window.setDimAmount(amount) }
}

@Composable
private fun RootApp(
    installViewModel: InstallViewModel,
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    shizukuMode: Boolean,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onShizukuModeChanged: (Boolean) -> Unit,
    openInstaller: () -> Unit,
) {
    val installState by installViewModel.state.collectAsStateWithLifecycle()
    val history by installViewModel.history.collectAsStateWithLifecycle()
    var selectedPage by remember { mutableStateOf(AppPage.Overview) }
    var showInstallConfirmation by remember { mutableStateOf(false) }
    val device = remember { DeviceSnapshot.current() }
    val view = LocalView.current

    if (showInstallConfirmation) {
        AlertDialog(
            onDismissRequest = { showInstallConfirmation = false },
            icon = { Icon(painterResource(R.drawable.ic_app_logo), contentDescription = null) },
            title = {
                DialogDimAmount(0.34f)
                Text(stringResource(R.string.install_confirm_title))
            },
            text = { Text(stringResource(R.string.install_confirm_body)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    clickHaptic(view)
                    showInstallConfirmation = false
                    openInstaller()
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clickHaptic(view)
                    showInstallConfirmation = false
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                AppPage.entries.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = {
                            clickHaptic(view)
                            selectedPage = page
                        },
                        icon = { Icon(page.icon, contentDescription = null) },
                        label = { Text(stringResource(page.label)) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        AnimatedContent(targetState = selectedPage, label = "page") { page ->
            when (page) {
                AppPage.Overview -> OverviewPage(
                    padding = padding,
                    device = device,
                    installState = installState,
                    onInstall = { showInstallConfirmation = true },
                )
                AppPage.History -> HistoryPage(
                    padding,
                    history,
                )
                AppPage.Settings -> SettingsPage(
                    padding = padding,
                    accentColor = accentColor,
                    themeMode = themeMode,
                    shizukuMode = shizukuMode,
                    onAccentColorChanged = onAccentColorChanged,
                    onThemeModeChanged = onThemeModeChanged,
                    onShizukuModeChanged = onShizukuModeChanged,
                )
            }
        }
    }
}

@Composable
private fun OverviewPage(
    padding: PaddingValues,
    device: DeviceSnapshot,
    installState: InstallUiState,
    onInstall: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        R.string.version_format,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
        }
        item { InstallStatusCard(installState, onInstall) }
        item { DeviceCard(device) }
        item { HowItWorksCard() }
        item { KernelSuCard() }
    }
}

@Composable
private fun InstallStatusCard(installState: InstallUiState, onInstall: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val managerInstalled = remember(installState) {
        context.packageManager.getLaunchIntentForPackage(KERNEL_SU_PACKAGE) != null ||
            context.packageManager.getLaunchIntentForPackage(KERNEL_SU_NEXT_PACKAGE) != null
    }
    Card(
        onClick = {
            clickHaptic(view)
            when {
                installState.busy -> Unit
                installState.phase == InstallPhase.Installed -> {
                    openKernelSuManager(context)
                }
                !installState.supported -> Unit
                else -> onInstall()
            }
        },
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = expressiveClickableCardShape(interactionSource),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                installState.busy -> LoadingIndicator(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                installState.phase == InstallPhase.Installed -> Icon(
                    Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(44.dp),
                )
                installState.phase == InstallPhase.Failed -> Icon(
                    Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(44.dp),
                )
                else -> Icon(
                    Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(44.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (installState.phase == InstallPhase.Installed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_kernelsu),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.status_rooted),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    Text(
                        text = when (installState.phase) {
                            InstallPhase.Ready -> stringResource(R.string.status_not_rooted)
                            else -> installState.message
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = when {
                        installState.phase == InstallPhase.Installed -> stringResource(
                            if (managerInstalled) {
                                R.string.install_tap_open_manager
                            } else {
                                R.string.install_tap_manager
                            },
                        )
                        installState.phase == InstallPhase.Failed && installState.supported ->
                            stringResource(R.string.install_tap_retry)
                        !installState.supported -> stringResource(R.string.home_unsupported_hint)
                        else -> stringResource(R.string.install_tap_start)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoRow(Icons.Rounded.Memory, stringResource(R.string.device), "${device.manufacturer} ${device.model} (${device.device})")
            InfoRow(Icons.Rounded.Code, stringResource(R.string.firmware), device.fingerprint)
            InfoRow(Icons.Rounded.Info, stringResource(R.string.system), "Android ${device.androidRelease} (API ${device.sdk})")
            InfoRow(Icons.Rounded.Info, stringResource(R.string.kernel), device.kernelRelease)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HowItWorksCard() {
    val steps = listOf(
        Triple(Icons.Rounded.Security, R.string.step_device_title, R.string.step_device_detail),
        Triple(Icons.Rounded.Inventory2, R.string.step_stage_title, R.string.step_stage_detail),
        Triple(Icons.Rounded.Memory, R.string.step_exploit_title, R.string.step_exploit_detail),
        Triple(Icons.Rounded.CheckCircle, R.string.step_ksu_title, R.string.step_ksu_detail),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.how_it_works), style = MaterialTheme.typography.titleMedium)
            steps.forEach { (icon, title, detail) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.size(36.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KernelSuCard() {
    val context = LocalContext.current
    val view = LocalView.current
    Card(
        onClick = {
            clickHaptic(view)
            openKernelSuManager(context)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_kernelsu),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.kernelsu_card_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.kernelsu_card_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.Link, contentDescription = null)
        }
    }
}

@Composable
private fun HistoryPage(
    padding: PaddingValues,
    history: List<InstallHistoryEntry>,
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.history_empty_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(history, key = { it.id }) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = when (entry.result) {
                        InstallRunResult.Succeeded -> MaterialTheme.colorScheme.primaryContainer
                        InstallRunResult.Failed -> MaterialTheme.colorScheme.errorContainer
                        InstallRunResult.Running -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = when (entry.result) {
                            InstallRunResult.Succeeded -> Icons.Rounded.CheckCircle
                            InstallRunResult.Failed -> Icons.Rounded.Error
                            InstallRunResult.Running -> Icons.Rounded.History
                        },
                        contentDescription = null,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                when (entry.result) {
                                    InstallRunResult.Succeeded -> R.string.history_succeeded
                                    InstallRunResult.Failed -> R.string.history_failed
                                    InstallRunResult.Running -> R.string.history_running
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                                .format(Date(entry.startedAtMillis)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(
                                if (entry.usedShizuku) {
                                    R.string.history_shizuku_used
                                } else {
                                    R.string.history_shizuku_not_used
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppVersionText(
    style: TextStyle,
    color: Color,
) {
    Text(
        text = stringResource(
            R.string.version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
        ),
        style = style,
        color = color,
    )
}

@Composable
private fun SettingsPage(
    padding: PaddingValues,
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    shizukuMode: Boolean,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onShizukuModeChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showShizukuMissingDialog by remember { mutableStateOf(false) }
    var languageMenuTop by remember { mutableStateOf(32.dp) }
    var colorMenuTop by remember { mutableStateOf(32.dp) }
    val density = LocalDensity.current
    val currentLanguageTag = AppPreferences.languageTag(context)

    if (showShizukuMissingDialog) {
        AlertDialog(
            onDismissRequest = { showShizukuMissingDialog = false },
            icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            title = {
                DialogDimAmount(0.34f)
                Text(stringResource(R.string.shizuku_not_running_title))
            },
            text = { Text(stringResource(R.string.shizuku_not_running_body)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    clickHaptic(view)
                    showShizukuMissingDialog = false
                    openShizukuManager(context)
                }) {
                    Text(stringResource(R.string.action_download_shizuku))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clickHaptic(view)
                    showShizukuMissingDialog = false
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showLanguageDialog) {
        SideChoiceMenu(
            choices = languageOptions.map { stringResource(it.label) },
            selectedIndex = languageOptions.indexOfFirst { languageMatches(it, currentLanguageTag) }
                .coerceAtLeast(0),
            topOffset = languageMenuTop,
            onSelected = { index ->
                showLanguageDialog = false
                AppPreferences.setLanguage(context, languageOptions[index].tag)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showColorDialog) {
        val colors = AccentColor.entries
        SideChoiceMenu(
            choices = colors.map { accentLabel(it) },
            selectedIndex = colors.indexOf(accentColor),
            topOffset = colorMenuTop,
            onSelected = { index ->
                showColorDialog = false
                onAccentColorChanged(colors[index])
            },
            onDismiss = { showColorDialog = false },
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 18.dp)) {
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
                AppVersionText(
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionLabel(stringResource(R.string.appearance)) }
        item {
            ThemeModeSelector(themeMode, onThemeModeChanged)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsCard(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        colorMenuTop = with(density) { coordinates.positionInWindow().y.toDp() }
                    },
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.material_color),
                    description = stringResource(R.string.material_color_description),
                    value = accentLabel(accentColor),
                    position = SettingsCardPosition.Top,
                    onClick = {
                        clickHaptic(view)
                        showColorDialog = true
                    },
                )
                SettingsCard(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        languageMenuTop = with(density) { coordinates.positionInWindow().y.toDp() }
                    },
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.language),
                    description = stringResource(R.string.language_description),
                    value = languageLabel(currentLanguageTag),
                    position = SettingsCardPosition.Middle,
                    onClick = {
                        clickHaptic(view)
                        showLanguageDialog = true
                    },
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.VerifiedUser,
                    title = stringResource(R.string.shizuku_mode),
                    description = stringResource(R.string.shizuku_mode_description),
                    checked = shizukuMode,
                    position = SettingsCardPosition.Bottom,
                    onCheckedChange = { enabled ->
                        clickHaptic(view)
                        if (!enabled) {
                            onShizukuModeChanged(false)
                        } else {
                            scope.launch {
                                ShizukuController.pingUntilRunning()
                                if (ShizukuController.isRunning()) {
                                    onShizukuModeChanged(true)
                                    if (!ShizukuController.isGranted()) {
                                        ShizukuController.requestPermission()
                                    }
                                } else {
                                    showShizukuMissingDialog = true
                                }
                            }
                        }
                    },
                )
            }
        }
        item { SectionLabel(stringResource(R.string.about)) }
        item {
            SettingsCard(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.about),
                description = stringResource(R.string.about_description),
                value = "",
                position = SettingsCardPosition.GroupedSingle,
                onClick = {
                    clickHaptic(view)
                    showAboutDialog = true
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, top = 6.dp, bottom = 2.dp),
    )
}

private enum class SettingsCardPosition {
    Single,
    GroupedSingle,
    Top,
    Middle,
    Bottom,
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    value: String,
    position: SettingsCardPosition = SettingsCardPosition.Single,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    Card(
        onClick = {
            clickHaptic(view)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource, position),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    position: SettingsCardPosition = SettingsCardPosition.Single,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    Card(
        onClick = {
            clickHaptic(view)
            onCheckedChange(!checked)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource, position),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun ThemeModeSelector(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    val view = LocalView.current
    val themeModes = AppThemeMode.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        themeModes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = themeMode == mode,
                onCheckedChange = {
                    clickHaptic(view)
                    onThemeModeChanged(mode)
                },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    themeModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Icon(
                    imageVector = when (mode) {
                        AppThemeMode.System -> Icons.Rounded.BrightnessAuto
                        AppThemeMode.Light -> Icons.Rounded.LightMode
                        AppThemeMode.Dark -> Icons.Rounded.DarkMode
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(themeModeLabel(mode), maxLines = 1)
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            DialogDimAmount(0.34f)
            Text(stringResource(R.string.about_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.about_body))
                AppVersionText(
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Surface(
                    onClick = {
                        clickHaptic(view)
                        uriHandler.openUri(KERNEL_SU_HOME_URL)
                    },
                    color = Color.Transparent,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_kernelsu), contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.kernelsu_card_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.kernelsu_card_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.Link, contentDescription = stringResource(R.string.open_github))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clickHaptic(view)
                onDismiss()
            }) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun expressiveClickableCardShape(
    interactionSource: MutableInteractionSource,
    position: SettingsCardPosition = SettingsCardPosition.Single,
): RoundedCornerShape {
    val pressed by interactionSource.collectIsPressedAsState()
    val topRadius by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            position == SettingsCardPosition.Single -> 16.dp
            position in setOf(SettingsCardPosition.GroupedSingle, SettingsCardPosition.Top) -> 24.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clickable-card-top-corner",
    )
    val bottomRadius by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            position == SettingsCardPosition.Single -> 16.dp
            position in setOf(SettingsCardPosition.GroupedSingle, SettingsCardPosition.Bottom) -> 24.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clickable-card-bottom-corner",
    )
    return RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
}

@Composable
private fun SideChoiceMenu(
    choices: List<String>,
    selectedIndex: Int,
    topOffset: Dp,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.34f else 0f,
        animationSpec = tween(durationMillis = if (visible) 160 else 180),
        label = "menu-scrim",
    )

    fun closeMenu(afterAnimation: () -> Unit) {
        if (closing) return
        closing = true
        visible = false
        coroutineScope.launch {
            delay(MENU_EXIT_WAIT_MILLIS)
            afterAnimation()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    Popup(
        onDismissRequest = { closeMenu(onDismiss) },
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val estimatedHeight = 16.dp + 56.dp * choices.size
            val constrainedTop = minOf(
                topOffset,
                maxHeight - estimatedHeight - 24.dp,
            ).coerceAtLeast(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { closeMenu(onDismiss) },
                    ),
            )
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = constrainedTop, end = 18.dp),
                enter = scaleIn(
                    animationSpec = keyframes {
                        durationMillis = 200
                        1.025f at 95
                        0.995f at 155
                    },
                    initialScale = 0.94f,
                    transformOrigin = TransformOrigin(1f, 0f),
                ),
                exit = scaleOut(
                    animationSpec = tween(durationMillis = MENU_EXIT_ANIMATION_MILLIS),
                    targetScale = 0.86f,
                    transformOrigin = TransformOrigin(1f, 0.5f),
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 160,
                        delayMillis = 20,
                    ),
                ),
            ) {
                Surface(
                    modifier = Modifier
                        .width(196.dp)
                        .heightIn(max = 620.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(choices) { index, choice ->
                            val selected = index == selectedIndex
                            Surface(
                                onClick = {
                                    clickHaptic(view)
                                    closeMenu { onSelected(index) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = if (selected) {
                                    MaterialTheme.shapes.extraLarge
                                } else {
                                    MaterialTheme.shapes.medium
                                },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Text(
                                        text = choice,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val MENU_EXIT_ANIMATION_MILLIS = 180
private const val MENU_EXIT_WAIT_MILLIS = 200L

@Composable
private fun languageLabel(tag: String): String =
    languageOptions.firstOrNull { languageMatches(it, tag) }
        ?.let { stringResource(it.label) }
        ?: stringResource(R.string.language_system)

private fun languageMatches(option: LanguageOption, currentTag: String): Boolean {
    if (option.tag.isEmpty()) return currentTag.isEmpty()
    return currentTag == option.tag || currentTag.startsWith("${option.tag}-")
}

@Composable
private fun accentLabel(color: AccentColor): String = when (color) {
    AccentColor.Dynamic -> stringResource(R.string.color_dynamic)
    AccentColor.Blue -> stringResource(R.string.color_blue)
    AccentColor.Violet -> stringResource(R.string.color_violet)
    AccentColor.Green -> stringResource(R.string.color_green)
    AccentColor.Orange -> stringResource(R.string.color_orange)
}

@Composable
private fun themeModeLabel(themeMode: AppThemeMode): String = when (themeMode) {
    AppThemeMode.System -> stringResource(R.string.theme_system)
    AppThemeMode.Light -> stringResource(R.string.theme_light)
    AppThemeMode.Dark -> stringResource(R.string.theme_dark)
}
