package com.yukigasai.trinkaus.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.AddHydrationButtons
import com.yukigasai.trinkaus.presentation.CurrentHydrationDisplay
import com.yukigasai.trinkaus.presentation.HistoryMonthSelector
import com.yukigasai.trinkaus.presentation.HydrationCalendar
import com.yukigasai.trinkaus.presentation.HydrationGraph
import com.yukigasai.trinkaus.presentation.SettingsPopup
import com.yukigasai.trinkaus.presentation.StreakDisplay
import com.yukigasai.trinkaus.util.StreakResult
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainUI(
    stateHolder: TrinkAusStateHolder,
    modifier: Modifier = Modifier,
) {
    val hydrationLevel = stateHolder.hydrationLevel.collectAsState(0.0)
    val hydrationGoal = stateHolder.hydrationGoal.collectAsState(0.1)
    val largestStreak = stateHolder.largestStreak.collectAsState(StreakResult())
    val currentStreak = stateHolder.currentStreak.collectAsState(StreakResult())
    val useGraphHistory = stateHolder.useGraphHistory.collectAsState(false)
    val showConfetti = remember { mutableStateOf(false) }
    val showSettingsModal = remember { mutableStateOf(false) }
    val isLoading = remember { stateHolder.isLoading }
    val isHideKonfettiEnabled = stateHolder.isHideKonfettiEnabled.collectAsState(false)
    val spacing = 22.dp

    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
    ) { padding ->
        val scrollState = rememberScrollState()

        PullToRefreshBox(
            isRefreshing = isLoading.value,
            onRefresh = {
                stateHolder.refreshDataFromSource()
            },
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            ) {
                Spacer(modifier = Modifier.height(spacing))

                CurrentHydrationDisplay(
                    hydrationLevel = hydrationLevel.value,
                    hydrationGoal = hydrationGoal.value,
                )

                if (hydrationGoal.value > 0) {
                    Text(
                        text = "${((hydrationLevel.value / hydrationGoal.value) * 100).toInt()}% ${stringResource(R.string.of_goal)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                AddHydrationButtons {
                    scope.launch {
                        showConfetti.value = true
                        stateHolder.addHydration(it)
                    }
                }

                HorizontalDivider()

                StreakDisplay(
                    largestStreak = largestStreak.value,
                    currentStreak = currentStreak.value,
                )

                HistoryMonthSelector(stateHolder)

                if (useGraphHistory.value) {
                    HydrationGraph(stateHolder)
                } else {
                    HydrationCalendar(stateHolder)
                }
            }

            IconButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = { showSettingsModal.value = true },
            ) {
                Icon(
                    imageVector = Lucide.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        // ... Konfetti and SettingsPopup logic remains the same ...
        if (showConfetti.value && !isHideKonfettiEnabled.value) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties =
                    listOf(
                        Party(
                            speed = 10f,
                            maxSpeed = 30f,
                            damping = 0.9f,
                            spread = 180,
                            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                            emitter =
                                Emitter(
                                    duration = 100,
                                    TimeUnit.MILLISECONDS,
                                ).perSecond(2000),
                            position = Position.Relative(0.5, 1.0),
                            angle = -90,
                        ),
                    ),
            )
        }

        if (showSettingsModal.value) {
            SettingsPopup(
                stateHolder = stateHolder,
                updateShowSettingsModal = { showSettingsModal.value = it },
            )
        }
    }
}
