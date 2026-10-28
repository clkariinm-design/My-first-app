package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MacroWithSteps
import com.example.engine.ExecutionStatus
import com.example.ui.components.CreateMacroDialog
import com.example.ui.components.JsonImportExportDialog
import com.example.ui.components.PermissionBanner
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
fun DashboardScreen(
    viewModel: MacroViewModel,
    onNavigateToBuilder: (Long) -> Unit,
    onNavigateToRecorder: () -> Unit,
    onNavigateToExecution: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions(context)
    }

    val macros by viewModel.filteredMacros.collectAsState()
    val allMacrosList by viewModel.allMacros.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabledInSettings.collectAsState()
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()
    val isOverlayRunning by viewModel.isOverlayServiceRunning.collectAsState()
    val engineStatus by viewModel.engineStatus.collectAsState()
    val executingMacro by viewModel.currentExecutingMacro.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var jsonExportString by remember { mutableStateOf<String?>(null) }
    var showJsonDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkObsidian,
        contentWindowInsets = WindowInsets.statusBars,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Record Touch Sequence FAB
                OutlinedButton(
                    onClick = onNavigateToRecorder,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkSlate,
                        contentColor = NeonRed
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NeonRed)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("record_macro_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = "Record",
                        tint = NeonRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Touch Recorder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Create New Macro FAB
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = NeonCyan,
                    contentColor = DarkObsidian,
                    shape = CircleShape,
                    modifier = Modifier.testTag("create_macro_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Macro", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top App Bar Area
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MacroFlow",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Auto Clicker & Automation Engine",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Floating Overlay Quick Toggle Button
                        IconButton(
                            onClick = {
                                viewModel.toggleFloatingOverlay(context)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isOverlayRunning) NeonCyan else DarkSlate)
                                .border(1.dp, if (isOverlayRunning) NeonCyan else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                                .size(40.dp)
                                .testTag("toggle_floating_overlay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Floating Overlay",
                                tint = if (isOverlayRunning) DarkObsidian else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // JSON Import / Export Button
                        IconButton(
                            onClick = {
                                jsonExportString = null
                                showJsonDialog = true
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSlate)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                                .size(40.dp)
                                .testTag("json_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Import / Export JSON",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Permissions Status Banner
            item {
                PermissionBanner(
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isOverlayPermissionGranted = isOverlayPermissionGranted,
                    onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings(context) },
                    onOpenOverlaySettings = { viewModel.toggleFloatingOverlay(context) }
                )
            }

            // Active Execution Quick Bar (if running or paused)
            if (engineStatus !is ExecutionStatus.Idle && engineStatus !is ExecutionStatus.Completed && engineStatus !is ExecutionStatus.Stopped) {
                item {
                    ActiveExecutionBanner(
                        status = engineStatus,
                        macro = executingMacro,
                        onPause = { viewModel.pauseMacro() },
                        onResume = { viewModel.resumeMacro() },
                        onStop = { viewModel.stopMacro() },
                        onOpenDetails = onNavigateToExecution
                    )
                }
            }

            // Search Bar & Category Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search macros...", color = TextSecondary) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedContainerColor = DarkSlate,
                            unfocusedContainerColor = DarkSlate
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_macros_input")
                    )

                    // Categories Row
                    val categories = listOf("All", "Gaming", "Social", "Productivity", "Custom")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan else DarkSlate)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else DarkSurfaceBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setSelectedCategory(cat) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) DarkObsidian else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Macros Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SAVED MACROS (${macros.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Empty State
            if (macros.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSlate)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(DarkObsidian),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No macros found",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the + button to build a custom macro sequence or record touches with Touch Recorder.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Macro Cards List
                items(macros, key = { it.macro.id }) { item ->
                    MacroCard(
                        macroWithSteps = item,
                        isRunning = executingMacro?.macro?.id == item.macro.id && (engineStatus is ExecutionStatus.Running || engineStatus is ExecutionStatus.Countdown),
                        onRun = {
                            viewModel.startMacro(item)
                            onNavigateToExecution()
                        },
                        onEdit = { onNavigateToBuilder(item.macro.id) },
                        onDuplicate = { viewModel.duplicateMacro(item.macro.id) },
                        onDelete = { viewModel.deleteMacro(item.macro.id) },
                        onExport = {
                            jsonExportString = viewModel.exportToJson(item)
                            showJsonDialog = true
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateMacroDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, category, description ->
                viewModel.createMacro(name, category, description) { newId ->
                    onNavigateToBuilder(newId)
                }
            }
        )
    }

    if (showJsonDialog) {
        JsonImportExportDialog(
            initialExportJson = jsonExportString,
            onDismiss = { showJsonDialog = false },
            onImport = { json ->
                viewModel.importFromJson(json) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun MacroCard(
    macroWithSteps: MacroWithSteps,
    isRunning: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    val macro = macroWithSteps.macro
    val steps = macroWithSteps.sortedSteps
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSlate)
            .border(
                1.dp,
                if (isRunning) NeonCyan else DarkSurfaceBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable { onEdit() }
            .padding(16.dp)
            .testTag("macro_item_${macro.id}")
    ) {
        Column {
            // Header Row: Name & Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = macro.name,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Category Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkObsidian)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = macro.category,
                                color = ElectricAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (macro.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = macro.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkSlate)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Steps", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export JSON", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null, tint = ElectricAmber) },
                            onClick = {
                                showMenu = false
                                onExport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = NeonRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Configuration Badges: Steps Count, Repeat Loop, Speed Multiplier
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steps badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkObsidian)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${steps.size} Steps",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Repeat badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkObsidian)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (macro.isInfiniteRepeat) "Loop: ∞ Infinite" else "Loop: ${macro.repeatCount}x",
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                }

                // Speed badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkObsidian)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${macro.speedMultiplier}x Speed",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Edit & Run Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sequence", fontSize = 12.sp)
                }

                Button(
                    onClick = onRun,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) ElectricAmber else NeonCyan,
                        contentColor = DarkObsidian
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("run_macro_button_${macro.id}")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "Running..." else "Execute",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveExecutionBanner(
    status: ExecutionStatus,
    macro: MacroWithSteps?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val isRunning = status is ExecutionStatus.Running
    val isCountdown = status is ExecutionStatus.Countdown
    val isPaused = status is ExecutionStatus.Paused

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSlate)
            .border(1.dp, NeonCyan, RoundedCornerShape(14.dp))
            .clickable { onOpenDetails() }
            .padding(14.dp)
            .testTag("active_execution_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) CyberGreen else ElectricAmber)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = macro?.macro?.name ?: "Automation Active",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (status) {
                        is ExecutionStatus.Countdown -> "Starting countdown: ${status.secondsRemaining}s"
                        is ExecutionStatus.Running -> "Running Loop ${status.currentLoop} • Step ${status.currentStepIndex}/${status.totalSteps}"
                        is ExecutionStatus.Paused -> "Paused at Step ${status.currentStepIndex}/${status.totalSteps}"
                        else -> "Executing..."
                    },
                    color = NeonCyan,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isRunning) {
                    IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = ElectricAmber)
                    }
                } else if (isPaused) {
                    IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = CyberGreen)
                    }
                }

                IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = NeonRed)
                }
            }
        }
    }
}
