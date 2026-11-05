package dev.busung.roots24

import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.busung.roots24.ui.theme.RootS24Theme
import kotlinx.coroutines.delay
import android.os.SystemClock

class InstallActivity : ComponentActivity() {
    private val installViewModel by viewModels<InstallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val startInstall = savedInstanceState == null && AppPreferences.consumeInstallRequest(
            this,
            intent.getStringExtra(EXTRA_INSTALL_REQUEST_ID),
        )
        intent.removeExtra(EXTRA_INSTALL_REQUEST_ID)
        setContent {
            RootS24Theme(
                accentColor = AppPreferences.accentColor(this),
                themeMode = AppPreferences.themeMode(this),
            ) {
                val installState by installViewModel.state.collectAsStateWithLifecycle()
                var uptimeMillis by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
                var waitingForSafeUptime by remember {
                    mutableStateOf(startInstall && SystemClock.elapsedRealtime() < SAFE_UPTIME_MILLIS)
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        uptimeMillis = SystemClock.elapsedRealtime()
                        if (waitingForSafeUptime && uptimeMillis >= SAFE_UPTIME_MILLIS) {
                            waitingForSafeUptime = false
                            installViewModel.install()
                        }
                        delay(500L)
                    }
                }

                BackHandler(enabled = installState.busy) {}

                LaunchedEffect(startInstall) {
                    if (startInstall && SystemClock.elapsedRealtime() >= SAFE_UPTIME_MILLIS) {
                        installViewModel.install()
                    }
                }

                InstallScreen(
                    installState = installState,
                    uptimeMillis = uptimeMillis,
                    waitingForSafeUptime = waitingForSafeUptime,
                    onStartNow = {
                        waitingForSafeUptime = false
                        installViewModel.install()
                    },
                    onRetry = { installViewModel.install() },
                    onClose = ::finish,
                )
            }
        }
    }

    companion object {
        const val EXTRA_INSTALL_REQUEST_ID = "install_request_id"
    }
}

private data class InstallerStep(
    @StringRes val title: Int,
    @StringRes val detail: Int,
    val icon: ImageVector,
)

private val installerSteps = listOf(
    InstallerStep(R.string.step_device_title, R.string.step_device_detail, Icons.Rounded.Security),
    InstallerStep(R.string.step_stage_title, R.string.step_stage_detail, Icons.Rounded.Inventory2),
    InstallerStep(R.string.step_exploit_title, R.string.step_exploit_detail, Icons.Rounded.Memory),
    InstallerStep(R.string.step_ksu_title, R.string.step_ksu_detail, Icons.Rounded.VerifiedUser),
)

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
private fun InstallScreen(
    installState: InstallUiState,
    uptimeMillis: Long,
    waitingForSafeUptime: Boolean,
    onStartNow: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val logScrollState = rememberScrollState()
    val view = LocalView.current
    val remainingSec = ((SAFE_UPTIME_MILLIS - uptimeMillis).coerceAtLeast(0L) / 1000L).toInt()

    LaunchedEffect(installState.log) {
        delay(40)
        logScrollState.scrollTo(logScrollState.maxValue)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.install_title),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = formatUptime(uptimeMillis, context),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = if (waitingForSafeUptime) {
                        stringResource(R.string.install_waiting_uptime_subtitle, remainingSec)
                    } else if (installState.busy) {
                        stringResource(R.string.install_keep_open)
                    } else {
                        installState.message
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            InstallerStatusCard(installState, waitingForSafeUptime, uptimeMillis)
            InstallerSteps(installState.phase)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.install_live_progress), style = MaterialTheme.typography.titleMedium)
                    val logText = when {
                        waitingForSafeUptime -> stringResource(
                            R.string.install_waiting_uptime_log,
                            remainingSec,
                            formatUptime(uptimeMillis, context),
                        )
                        installState.log.isNotBlank() -> installState.log
                        else -> stringResource(R.string.install_preparing)
                    }
                    Text(
                        text = logText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(logScrollState),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (waitingForSafeUptime) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            clickHaptic(view)
                            onClose()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            clickHaptic(view)
                            onStartNow()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.install_skip_wait))
                    }
                }
            } else if (!installState.busy) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (installState.phase == InstallPhase.Failed) {
                        FilledTonalButton(
                            onClick = {
                                clickHaptic(view)
                                onClose()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_close))
                        }
                        Button(
                            onClick = {
                                clickHaptic(view)
                                onRetry()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_retry))
                        }
                    } else {
                        Button(
                            onClick = {
                                clickHaptic(view)
                                onClose()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.action_done))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallerStatusCard(
    installState: InstallUiState,
    waitingForSafeUptime: Boolean,
    uptimeMillis: Long,
) {
    val remainingSec = ((SAFE_UPTIME_MILLIS - uptimeMillis).coerceAtLeast(0L) / 1000L).toInt()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = when {
                waitingForSafeUptime -> MaterialTheme.colorScheme.tertiaryContainer
                installState.phase == InstallPhase.Failed -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = when {
                waitingForSafeUptime -> MaterialTheme.colorScheme.onTertiaryContainer
                installState.phase == InstallPhase.Failed -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (waitingForSafeUptime) {
                    Icon(
                        Icons.Rounded.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                    )
                } else {
                    AnimatedContent(targetState = installState.phase, label = "install-status-icon") { phase ->
                        when {
                            installState.busy -> LoadingIndicator(
                                modifier = Modifier.size(44.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            phase == InstallPhase.Installed -> Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                            )
                            else -> Icon(
                                Icons.Rounded.Error,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (waitingForSafeUptime) {
                            stringResource(R.string.install_waiting_uptime_title)
                        } else {
                            installState.message
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = if (waitingForSafeUptime) {
                            stringResource(R.string.install_waiting_uptime_subtitle, remainingSec)
                        } else {
                            stringResource(
                                when (installState.phase) {
                                    InstallPhase.Checking -> R.string.phase_checking
                                    InstallPhase.Ready -> R.string.phase_ready
                                    InstallPhase.Staging -> R.string.phase_staging
                                    InstallPhase.Exploiting -> R.string.phase_exploiting
                                    InstallPhase.LoadingKernelSu -> R.string.phase_loading_ksu
                                    InstallPhase.Installed -> R.string.phase_installed
                                    InstallPhase.Failed -> R.string.phase_failed
                                },
                            )
                        },
                        color = LocalContentColor.current.copy(alpha = 0.78f),
                    )
                }
            }
            LinearProgressIndicator(
                progress = {
                    if (waitingForSafeUptime) {
                        (uptimeMillis.toFloat() / SAFE_UPTIME_MILLIS.toFloat()).coerceIn(0f, 1f)
                    } else {
                        when (installState.phase) {
                            InstallPhase.Checking -> 0.1f
                            InstallPhase.Ready -> 0f
                            InstallPhase.Staging -> 0.2f
                            InstallPhase.Exploiting -> 0.6f
                            InstallPhase.LoadingKernelSu -> 0.85f
                            InstallPhase.Installed -> 1f
                            InstallPhase.Failed -> 0f
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = LocalContentColor.current,
                trackColor = LocalContentColor.current.copy(alpha = 0.2f),
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun InstallerSteps(phase: InstallPhase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            installerSteps.forEachIndexed { index, step ->
                val stepState = stepState(phase, index)
                val circleColor by animateColorAsState(
                    targetValue = when (stepState) {
                        2 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    label = "step-circle-$index",
                )
                val contentColor by animateColorAsState(
                    targetValue = when (stepState) {
                        2 -> MaterialTheme.colorScheme.onPrimary
                        1 -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    label = "step-content-$index",
                )
                val border = if (stepState == 0) {
                    BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                } else null

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = circleColor,
                        contentColor = contentColor,
                        border = border,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = stepState,
                                label = "step-icon-$index",
                            ) { state ->
                                when (state) {
                                    2 -> Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    1 -> Icon(
                                        imageVector = step.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    else -> Icon(
                                        imageVector = step.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(step.title),
                            style = MaterialTheme.typography.titleSmall,
                            color = when (stepState) {
                                2 -> MaterialTheme.colorScheme.onSurface
                                1 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            },
                        )
                        Text(
                            text = stringResource(step.detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = when (stepState) {
                                2 -> MaterialTheme.colorScheme.onSurfaceVariant
                                1 -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        )
                    }
                    if (stepState == 1 && phase !in setOf(InstallPhase.Failed, InstallPhase.Ready)) {
                        LoadingIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private fun stepState(phase: InstallPhase, stepIndex: Int): Int {
    if (phase == InstallPhase.Installed) return 2
    val activeIndex = when (phase) {
        InstallPhase.Checking, InstallPhase.Ready, InstallPhase.Failed -> 0
        InstallPhase.Staging -> 1
        InstallPhase.Exploiting -> 2
        InstallPhase.LoadingKernelSu -> 3
        InstallPhase.Installed -> 4
    }
    return when {
        stepIndex < activeIndex -> 2
        stepIndex == activeIndex -> 1
        else -> 0
    }
}
