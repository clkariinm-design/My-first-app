package com.example.ui.screens

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MacroEntity
import com.example.data.model.MacroStepEntity
import com.example.data.model.StepType
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.LaserPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MacroViewModel
import kotlinx.coroutines.delay

@Composable
fun MacroRecorderScreen(
    viewModel: MacroViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var isRecording by remember { mutableStateOf(false) }
    var recordingName by remember { mutableStateOf("Recorded Macro ${System.currentTimeMillis() % 1000}") }
    val recordedSteps = remember { mutableStateListOf<MacroStepEntity>() }

    var lastActionTimestamp by remember { mutableLongStateOf(0L) }
    var touchFeedbackPosition by remember { mutableStateOf<Offset?>(null) }
    var dragStartPos by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentPos by remember { mutableStateOf<Offset?>(null) }

    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    val virtualWidth = 1080f
    val virtualHeight = 1920f

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val start = SystemClock.elapsedRealtime()
            while (isRecording) {
                elapsedTimeMs = SystemClock.elapsedRealtime() - start
                delay(100L)
            }
        }
    }

    Scaffold(
        containerColor = DarkObsidian,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSlate)
                    .border(1.dp, DarkSurfaceBorder)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            ) {
                if (!isRecording) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                recordedSteps.clear()
                                lastActionTimestamp = SystemClock.elapsedRealtime()
                                isRecording = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("start_recording_button")
                        ) {
                            Icon(imageVector = Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Recording", fontWeight = FontWeight.Bold)
                        }

                        if (recordedSteps.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val macro = MacroEntity(
                                        name = recordingName,
                                        description = "Recorded touch sequence with ${recordedSteps.size} actions",
                                        category = "Custom",
                                        repeatCount = 1,
                                        speedMultiplier = 1.0f,
                                        startDelaySeconds = 3,
                                        vibrateOnClick = true
                                    )
                                    viewModel.saveMacroAndSteps(macro, recordedSteps.toList())
                                    Toast.makeText(context, "Recorded macro saved!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberGreen,
                                    contentColor = DarkObsidian
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("save_recorded_macro_button")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save (${recordedSteps.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            isRecording = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricAmber,
                            contentColor = DarkObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("stop_recording_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop Recording (${recordedSteps.size} steps)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
                        text = "Touch Macro Recorder",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonRed.copy(alpha = 0.2f))
                            .border(1.dp, NeonRed, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REC ${String.format("%.1f", elapsedTimeMs / 1000f)}s",
                            color = NeonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Macro Name Field (when not recording)
            if (!isRecording) {
                OutlinedTextField(
                    value = recordingName,
                    onValueChange = { recordingName = it },
                    label = { Text("Macro Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Interactive Recording Screen Pad
            Text(
                text = if (isRecording) "TAP OR SWIPE ON SCREEN PAD TO CAPTURE" else "PRESS 'START RECORDING' THEN TAP ON SCREEN",
                color = if (isRecording) NeonCyan else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.9f)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF070A0E))
                    .border(
                        2.dp,
                        if (isRecording) NeonRed else DarkSurfaceBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .pointerInput(isRecording) {
                        if (isRecording) {
                            detectTapGestures { offset ->
                                val now = SystemClock.elapsedRealtime()
                                val delayFromPrev = if (lastActionTimestamp > 0) (now - lastActionTimestamp).coerceAtLeast(100L) else 500L
                                lastActionTimestamp = now

                                val cw = size.width
                                val ch = size.height
                                val vx = (offset.x / cw) * virtualWidth
                                val vy = (offset.y / ch) * virtualHeight

                                touchFeedbackPosition = offset
                                recordedSteps.add(
                                    MacroStepEntity(
                                        macroId = 0,
                                        stepOrder = recordedSteps.size,
                                        type = StepType.TAP.name,
                                        x = vx,
                                        y = vy,
                                        durationMs = 50L,
                                        delayAfterMs = delayFromPrev,
                                        label = "Tap #${recordedSteps.size + 1}"
                                    )
                                )
                            }
                        }
                    }
                    .pointerInput(isRecording) {
                        if (isRecording) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStartPos = offset
                                    dragCurrentPos = offset
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragCurrentPos = (dragCurrentPos ?: change.position) + dragAmount
                                },
                                onDragEnd = {
                                    val s = dragStartPos
                                    val e = dragCurrentPos
                                    if (s != null && e != null) {
                                        val now = SystemClock.elapsedRealtime()
                                        val delayFromPrev = if (lastActionTimestamp > 0) (now - lastActionTimestamp).coerceAtLeast(100L) else 500L
                                        lastActionTimestamp = now

                                        val cw = size.width
                                        val ch = size.height
                                        val vx1 = (s.x / cw) * virtualWidth
                                        val vy1 = (s.y / ch) * virtualHeight
                                        val vx2 = (e.x / cw) * virtualWidth
                                        val vy2 = (e.y / ch) * virtualHeight

                                        recordedSteps.add(
                                            MacroStepEntity(
                                                macroId = 0,
                                                stepOrder = recordedSteps.size,
                                                type = StepType.SWIPE.name,
                                                x = vx1,
                                                y = vy1,
                                                endX = vx2,
                                                endY = vy2,
                                                durationMs = 300L,
                                                delayAfterMs = delayFromPrev,
                                                label = "Swipe #${recordedSteps.size + 1}"
                                            )
                                        )
                                    }
                                    dragStartPos = null
                                    dragCurrentPos = null
                                }
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cw = size.width
                    val ch = size.height
                    val sx = cw / virtualWidth
                    val sy = ch / virtualHeight

                    // Draw already recorded steps
                    recordedSteps.forEachIndexed { idx, step ->
                        val px = step.x * sx
                        val py = step.y * sy
                        if (step.stepType == StepType.SWIPE) {
                            val ex = step.endX * sx
                            val ey = step.endY * sy
                            drawLine(
                                color = LaserPurple,
                                start = Offset(px, py),
                                end = Offset(ex, ey),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = LaserPurple, radius = 6f, center = Offset(px, py))
                            drawCircle(color = Color.White, radius = 6f, center = Offset(ex, ey))
                        } else {
                            drawCircle(
                                color = NeonCyan.copy(alpha = 0.4f),
                                radius = 16f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = NeonCyan,
                                radius = 6f,
                                center = Offset(px, py)
                            )
                        }
                    }

                    // Live Drag Preview
                    val ds = dragStartPos
                    val de = dragCurrentPos
                    if (ds != null && de != null) {
                        drawLine(
                            color = ElectricAmber,
                            start = ds,
                            end = de,
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Live Tap Feedback
                    touchFeedbackPosition?.let { pt ->
                        drawCircle(
                            color = NeonRed,
                            radius = 20f,
                            center = pt,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Recorded ${recordedSteps.size} actions",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
