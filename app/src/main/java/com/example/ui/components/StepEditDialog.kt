package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MacroStepEntity
import com.example.data.model.StepType
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StepEditDialog(
    initialStep: MacroStepEntity,
    stepIndex: Int,
    onDismiss: () -> Unit,
    onSave: (MacroStepEntity) -> Unit
) {
    var stepType by remember { mutableStateOf(initialStep.stepType) }
    var x by remember { mutableFloatStateOf(initialStep.x) }
    var y by remember { mutableFloatStateOf(initialStep.y) }
    var endX by remember { mutableFloatStateOf(initialStep.endX) }
    var endY by remember { mutableFloatStateOf(initialStep.endY) }
    var durationMs by remember { mutableLongStateOf(initialStep.durationMs) }
    var delayAfterMs by remember { mutableLongStateOf(initialStep.delayAfterMs) }
    var label by remember { mutableStateOf(initialStep.label) }

    var isInteractivePicking by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSlate,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("step_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Edit Step #${stepIndex + 1}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step Type Selector
                Text(
                    text = "ACTION TYPE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StepType.values().forEach { type ->
                        val isSelected = stepType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else DarkSurfaceVariant)
                                .clickable {
                                    stepType = type
                                    // Adjust default durations if switching to swipe or long press
                                    if (type == StepType.SWIPE && durationMs < 200L) {
                                        durationMs = 300L
                                    } else if (type == StepType.LONG_PRESS && durationMs < 500L) {
                                        durationMs = 1000L
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName.split(" ").first(),
                                color = if (isSelected) DarkObsidian else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Pick on Canvas Option
                if (stepType != StepType.WAIT) {
                    if (isInteractivePicking) {
                        VisualCanvasPicker(
                            steps = listOf(
                                MacroStepEntity(
                                    macroId = 0,
                                    stepOrder = 0,
                                    type = stepType.name,
                                    x = x,
                                    y = y,
                                    endX = endX,
                                    endY = endY
                                )
                            ),
                            mode = if (stepType == StepType.SWIPE) CanvasMode.DRAW_SWIPE else CanvasMode.PICK_POINT,
                            onPointPicked = { px, py ->
                                x = px
                                y = py
                                isInteractivePicking = false
                            },
                            onSwipeDrawn = { sx, sy, ex, ey ->
                                x = sx
                                y = sy
                                endX = ex
                                endY = ey
                                isInteractivePicking = false
                            }
                        )
                    } else {
                        OutlinedButton(
                            onClick = { isInteractivePicking = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Pick on Screen",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (stepType == StepType.SWIPE) "Draw Swipe on Canvas" else "Pick (X, Y) on Visual Canvas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Coordinate Numeric Inputs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = x.toInt().toString(),
                            onValueChange = { x = it.toFloatOrNull() ?: x },
                            label = { Text("X (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                focusedLabelColor = NeonCyan
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = y.toInt().toString(),
                            onValueChange = { y = it.toFloatOrNull() ?: y },
                            label = { Text("Y (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                focusedLabelColor = NeonCyan
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (stepType == StepType.SWIPE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = endX.toInt().toString(),
                                onValueChange = { endX = it.toFloatOrNull() ?: endX },
                                label = { Text("End X (px)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    focusedLabelColor = NeonCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endY.toInt().toString(),
                                onValueChange = { endY = it.toFloatOrNull() ?: endY },
                                label = { Text("End Y (px)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    focusedLabelColor = NeonCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration settings (if applicable)
                if (stepType != StepType.WAIT) {
                    Text(
                        text = "TOUCH DURATION: ${durationMs}ms",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = durationMs.toFloat(),
                        onValueChange = { durationMs = it.toLong() },
                        valueRange = 20f..3000f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Delay after step settings
                Text(
                    text = "DELAY AFTER ACTION: ${delayAfterMs}ms (${String.format("%.1f", delayAfterMs / 1000f)}s)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = delayAfterMs.toFloat(),
                    onValueChange = { delayAfterMs = it.toLong() },
                    valueRange = 50f..10000f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricAmber,
                        activeTrackColor = ElectricAmber,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Step label / note
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Step Label (Optional)") },
                    placeholder = { Text("e.g., Click Confirm Button") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Cancel and Save
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val updated = initialStep.copy(
                                type = stepType.name,
                                x = x,
                                y = y,
                                endX = endX,
                                endY = endY,
                                durationMs = durationMs,
                                delayAfterMs = delayAfterMs,
                                label = label.trim()
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = DarkObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_step_button")
                    ) {
                        Text("Save Step", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
