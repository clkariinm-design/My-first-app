package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MacroEntity
import com.example.data.model.MacroStepEntity
import com.example.data.model.MacroWithSteps
import com.example.data.model.StepType
import com.example.ui.components.CanvasMode
import com.example.ui.components.StepCard
import com.example.ui.components.StepEditDialog
import com.example.ui.components.VisualCanvasPicker
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MacroViewModel

@Composable
fun MacroBuilderScreen(
    macroId: Long,
    viewModel: MacroViewModel,
    onBack: () -> Unit,
    onExecute: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val allMacros by viewModel.allMacros.collectAsState()
    val currentMacroWithSteps = allMacros.find { it.macro.id == macroId }

    var macroName by remember { mutableStateOf(currentMacroWithSteps?.macro?.name ?: "Macro") }
    var macroCategory by remember { mutableStateOf(currentMacroWithSteps?.macro?.category ?: "Custom") }
    var repeatCount by remember { mutableIntStateOf(currentMacroWithSteps?.macro?.repeatCount ?: 1) }
    var isInfiniteRepeat by remember { mutableStateOf(currentMacroWithSteps?.macro?.isInfiniteRepeat ?: false) }
    var speedMultiplier by remember { mutableFloatStateOf(currentMacroWithSteps?.macro?.speedMultiplier ?: 1.0f) }
    var startDelaySeconds by remember { mutableIntStateOf(currentMacroWithSteps?.macro?.startDelaySeconds ?: 3) }
    var jitterRadiusPx by remember { mutableIntStateOf(currentMacroWithSteps?.macro?.jitterRadiusPx ?: 0) }
    var timingJitterMs by remember { mutableIntStateOf(currentMacroWithSteps?.macro?.timingJitterMs ?: 0) }
    var vibrateOnClick by remember { mutableStateOf(currentMacroWithSteps?.macro?.vibrateOnClick ?: true) }

    val stepsList = remember { mutableStateListOf<MacroStepEntity>() }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(currentMacroWithSteps) {
        if (!isLoaded && currentMacroWithSteps != null) {
            stepsList.clear()
            stepsList.addAll(currentMacroWithSteps.sortedSteps)
            macroName = currentMacroWithSteps.macro.name
            macroCategory = currentMacroWithSteps.macro.category
            repeatCount = currentMacroWithSteps.macro.repeatCount
            isInfiniteRepeat = currentMacroWithSteps.macro.isInfiniteRepeat
            speedMultiplier = currentMacroWithSteps.macro.speedMultiplier
            startDelaySeconds = currentMacroWithSteps.macro.startDelaySeconds
            jitterRadiusPx = currentMacroWithSteps.macro.jitterRadiusPx
            timingJitterMs = currentMacroWithSteps.macro.timingJitterMs
            vibrateOnClick = currentMacroWithSteps.macro.vibrateOnClick
            isLoaded = true
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Steps, 1: Canvas Map
    var showSettingsExpander by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }

    fun saveMacro() {
        val entity = MacroEntity(
            id = macroId,
            name = macroName.ifBlank { "Untitled Macro" },
            category = macroCategory,
            repeatCount = if (isInfiniteRepeat) -1 else repeatCount.coerceAtLeast(1),
            speedMultiplier = speedMultiplier,
            startDelaySeconds = startDelaySeconds,
            jitterRadiusPx = jitterRadiusPx,
            timingJitterMs = timingJitterMs,
            vibrateOnClick = vibrateOnClick
        )
        viewModel.saveMacroAndSteps(entity, stepsList.toList())
        Toast.makeText(context, "Macro saved", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = DarkObsidian,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            // Sticky Bottom Action Bar: Add Step & Run Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSlate)
                    .border(1.dp, DarkSurfaceBorder)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            val nextOrder = stepsList.size
                            val newStep = MacroStepEntity(
                                macroId = macroId,
                                stepOrder = nextOrder,
                                type = StepType.TAP.name,
                                x = 540f,
                                y = 960f,
                                durationMs = 50L,
                                delayAfterMs = 1000L,
                                label = "Step ${nextOrder + 1}"
                            )
                            stepsList.add(newStep)
                            editingStepIndex = stepsList.size - 1
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(NeonCyan)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("add_step_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Step", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            saveMacro()
                            val entity = MacroEntity(
                                id = macroId,
                                name = macroName,
                                category = macroCategory,
                                repeatCount = if (isInfiniteRepeat) -1 else repeatCount,
                                speedMultiplier = speedMultiplier,
                                startDelaySeconds = startDelaySeconds,
                                jitterRadiusPx = jitterRadiusPx,
                                timingJitterMs = timingJitterMs,
                                vibrateOnClick = vibrateOnClick
                            )
                            viewModel.startMacro(MacroWithSteps(entity, stepsList.toList()))
                            onExecute()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = DarkObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("run_macro_builder_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Execute Macro", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Top Bar
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Macro Builder",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { saveMacro() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSlate,
                            contentColor = CyberGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_macro_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Macro Name & Settings Expandable Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSlate)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        OutlinedTextField(
                            value = macroName,
                            onValueChange = { macroName = it },
                            label = { Text("Macro Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                focusedLabelColor = NeonCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Toggle settings expander
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSettingsExpander = !showSettingsExpander }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Execution Engine & Loop Settings",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = if (showSettingsExpander) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = showSettingsExpander) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                // Repeat mode toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("Loop Infinitely (∞)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("Run without stopping until manual stop", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = isInfiniteRepeat,
                                        onCheckedChange = { isInfiniteRepeat = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = NeonCyan,
                                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                                        )
                                    )
                                }

                                if (!isInfiniteRepeat) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("REPEAT LOOPS: $repeatCount", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = repeatCount.toFloat(),
                                        onValueChange = { repeatCount = it.toInt() },
                                        valueRange = 1f..100f,
                                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Speed multiplier
                                Text("EXECUTION SPEED: ${String.format("%.1f", speedMultiplier)}x", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = speedMultiplier,
                                    onValueChange = { speedMultiplier = it },
                                    valueRange = 0.5f..3.0f,
                                    steps = 4,
                                    colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Start Delay Countdown
                                Text("START COUNTDOWN DELAY: ${startDelaySeconds}s", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = startDelaySeconds.toFloat(),
                                    onValueChange = { startDelaySeconds = it.toInt() },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Anti-detection Random Touch Jitter
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("Human Touch Jitter (Anti-Detection)", color = TextPrimary, fontSize = 13.sp)
                                        Text("Adds ±${jitterRadiusPx}px random offset to clicks", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = jitterRadiusPx > 0,
                                        onCheckedChange = { enabled ->
                                            jitterRadiusPx = if (enabled) 6 else 0
                                            timingJitterMs = if (enabled) 15 else 0
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyberGreen,
                                            checkedTrackColor = CyberGreen.copy(alpha = 0.3f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Haptic Click Vibration
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Tactile Vibration on Click", color = TextPrimary, fontSize = 13.sp)
                                    Switch(
                                        checked = vibrateOnClick,
                                        onCheckedChange = { vibrateOnClick = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = NeonCyan,
                                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // View Tabs: Steps Sequence vs Screen Canvas Map
            item {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = DarkSlate,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = NeonCyan
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Step Sequence (${stepsList.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Screen Canvas", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTabIndex == 1) {
                // Screen Canvas Map View & Interactive Simulation
                item {
                    VisualCanvasPicker(
                        steps = stepsList,
                        mode = CanvasMode.VIEW,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Steps Sequence List
                if (stepsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSlate)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No steps added yet. Tap \"Add Step\" below to add your first tap or gesture.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    itemsIndexed(stepsList) { index, step ->
                        StepCard(
                            step = step,
                            stepIndex = index,
                            totalSteps = stepsList.size,
                            onMoveUp = {
                                if (index > 0) {
                                    val temp = stepsList[index]
                                    stepsList[index] = stepsList[index - 1]
                                    stepsList[index - 1] = temp
                                }
                            },
                            onMoveDown = {
                                if (index < stepsList.size - 1) {
                                    val temp = stepsList[index]
                                    stepsList[index] = stepsList[index + 1]
                                    stepsList[index + 1] = temp
                                }
                            },
                            onDuplicate = {
                                val dup = step.copy(id = 0, stepOrder = stepsList.size, label = "${step.label} (Copy)")
                                stepsList.add(index + 1, dup)
                            },
                            onEdit = {
                                editingStepIndex = index
                            },
                            onDelete = {
                                stepsList.removeAt(index)
                            }
                        )
                    }
                }
            }
        }
    }

    // Edit Step Dialog
    editingStepIndex?.let { idx ->
        if (idx in stepsList.indices) {
            StepEditDialog(
                initialStep = stepsList[idx],
                stepIndex = idx,
                onDismiss = { editingStepIndex = null },
                onSave = { updated ->
                    stepsList[idx] = updated
                    editingStepIndex = null
                }
            )
        }
    }
}
