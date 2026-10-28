package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class CanvasMode {
    VIEW,
    PICK_POINT,
    DRAW_SWIPE,
    SIMULATE
}

@Composable
fun VisualCanvasPicker(
    steps: List<MacroStepEntity>,
    mode: CanvasMode = CanvasMode.VIEW,
    onPointPicked: ((Float, Float) -> Unit)? = null,
    onSwipeDrawn: ((Float, Float, Float, Float) -> Unit)? = null,
    virtualScreenWidth: Float = 1080f,
    virtualScreenHeight: Float = 1920f,
    modifier: Modifier = Modifier
) {
    var pickedPoint by remember { mutableStateOf<Offset?>(null) }
    var swipeStart by remember { mutableStateOf<Offset?>(null) }
    var swipeEnd by remember { mutableStateOf<Offset?>(null) }

    var isSimulating by remember { mutableStateOf(false) }
    var activeSimStepIndex by remember { mutableIntStateOf(-1) }
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkObsidian)
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .testTag("visual_canvas_picker")
    ) {
        val containerWidth = maxWidth
        val density = LocalDensity.current

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            // Header Info & Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column {
                    Text(
                        text = when (mode) {
                            CanvasMode.VIEW -> "Visual Step Map (${steps.size} steps)"
                            CanvasMode.PICK_POINT -> "Tap Screen to Place Target"
                            CanvasMode.DRAW_SWIPE -> "Drag on Canvas to Draw Swipe"
                            CanvasMode.SIMULATE -> "Simulation Playback"
                        },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Virtual resolution: ${virtualScreenWidth.toInt()} x ${virtualScreenHeight.toInt()} px",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                if (mode == CanvasMode.VIEW && steps.isNotEmpty()) {
                    Button(
                        onClick = {
                            if (!isSimulating) {
                                isSimulating = true
                                coroutineScope.launch {
                                    for ((idx, step) in steps.withIndex()) {
                                        activeSimStepIndex = idx
                                        rippleRadius.snapTo(0f)
                                        rippleAlpha.snapTo(1f)
                                        launch {
                                            rippleRadius.animateTo(40f, tween(300, easing = LinearEasing))
                                        }
                                        launch {
                                            rippleAlpha.animateTo(0f, tween(300, easing = LinearEasing))
                                        }
                                        delay(400L)
                                    }
                                    activeSimStepIndex = -1
                                    isSimulating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = DarkObsidian
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Simulate",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSimulating) "Playing..." else "Test Run",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Screen Canvas Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF070A0E))
                    .border(2.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                    .pointerInput(mode) {
                        when (mode) {
                            CanvasMode.PICK_POINT -> {
                                detectTapGestures { offset ->
                                    val canvasW = size.width
                                    val canvasH = size.height
                                    val virtualX = (offset.x / canvasW) * virtualScreenWidth
                                    val virtualY = (offset.y / canvasH) * virtualScreenHeight
                                    pickedPoint = offset
                                    onPointPicked?.invoke(virtualX, virtualY)
                                }
                            }
                            CanvasMode.DRAW_SWIPE -> {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        swipeStart = offset
                                        swipeEnd = offset
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        swipeEnd = (swipeEnd ?: change.position) + dragAmount
                                    },
                                    onDragEnd = {
                                        val s = swipeStart
                                        val e = swipeEnd
                                        if (s != null && e != null) {
                                            val canvasW = size.width
                                            val canvasH = size.height
                                            val vx1 = (s.x / canvasW) * virtualScreenWidth
                                            val vy1 = (s.y / canvasH) * virtualScreenHeight
                                            val vx2 = (e.x / canvasW) * virtualScreenWidth
                                            val vy2 = (e.y / canvasH) * virtualScreenHeight
                                            onSwipeDrawn?.invoke(vx1, vy1, vx2, vy2)
                                        }
                                    }
                                )
                            }
                            else -> {}
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val scaleX = w / virtualScreenWidth
                    val scaleY = h / virtualScreenHeight

                    // 1. Draw subtle grid lines
                    val gridLines = 4
                    for (i in 1..gridLines) {
                        val gx = (w / (gridLines + 1)) * i
                        val gy = (h / (gridLines + 1)) * i
                        drawLine(
                            color = Color(0xFF1E2630),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFF1E2630),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                    }

                    // 2. Draw existing steps
                    steps.forEachIndexed { idx, step ->
                        val sx = step.x * scaleX
                        val sy = step.y * scaleY
                        val isStepActive = idx == activeSimStepIndex

                        when (step.stepType) {
                            StepType.SWIPE -> {
                                val ex = step.endX * scaleX
                                val ey = step.endY * scaleY

                                // Draw Swipe Arrow line
                                drawLine(
                                    color = if (isStepActive) NeonCyan else LaserPurple,
                                    start = Offset(sx, sy),
                                    end = Offset(ex, ey),
                                    strokeWidth = 4f,
                                    cap = StrokeCap.Round
                                )

                                // Arrow head
                                val angle = atan2(ey - sy, ex - sx)
                                val headLen = 14f
                                val headAngle = Math.PI / 6
                                val p1 = Offset(
                                    ex - headLen * cos(angle - headAngle).toFloat(),
                                    ey - headLen * sin(angle - headAngle).toFloat()
                                )
                                val p2 = Offset(
                                    ex - headLen * cos(angle + headAngle).toFloat(),
                                    ey - headLen * sin(angle + headAngle).toFloat()
                                )
                                val path = Path().apply {
                                    moveTo(ex, ey)
                                    lineTo(p1.x, p1.y)
                                    lineTo(p2.x, p2.y)
                                    close()
                                }
                                drawPath(path, if (isStepActive) NeonCyan else LaserPurple)

                                // Start dot
                                drawCircle(
                                    color = LaserPurple,
                                    radius = 7f,
                                    center = Offset(sx, sy)
                                )
                            }
                            StepType.WAIT -> {
                                // Non-visual delay
                            }
                            else -> {
                                // Tap, Double Tap, Long Press
                                val color = when (step.stepType) {
                                    StepType.LONG_PRESS -> ElectricAmber
                                    else -> NeonCyan
                                }

                                // Outer glow circle
                                drawCircle(
                                    color = color.copy(alpha = if (isStepActive) 0.5f else 0.2f),
                                    radius = if (isStepActive) 24f else 16f,
                                    center = Offset(sx, sy)
                                )

                                // Solid Center
                                drawCircle(
                                    color = if (isStepActive) Color.White else color,
                                    radius = 8f,
                                    center = Offset(sx, sy)
                                )

                                // Stroke outline
                                drawCircle(
                                    color = color,
                                    radius = 16f,
                                    center = Offset(sx, sy),
                                    style = Stroke(width = 2f)
                                )
                            }
                        }

                        // Active Simulation Ripple
                        if (isStepActive) {
                            drawCircle(
                                color = NeonCyan.copy(alpha = rippleAlpha.value),
                                radius = rippleRadius.value,
                                center = Offset(sx, sy),
                                style = Stroke(width = 3f)
                            )
                        }
                    }

                    // 3. Draw active interaction previews (picked point or live drawn swipe)
                    pickedPoint?.let { pt ->
                        drawCircle(
                            color = NeonCyan,
                            radius = 12f,
                            center = pt
                        )
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.3f),
                            radius = 26f,
                            center = pt
                        )
                    }

                    val s = swipeStart
                    val e = swipeEnd
                    if (s != null && e != null && mode == CanvasMode.DRAW_SWIPE) {
                        drawLine(
                            color = ElectricAmber,
                            start = s,
                            end = e,
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = ElectricAmber, radius = 6f, center = s)
                        drawCircle(color = Color.White, radius = 8f, center = e)
                    }
                }
            }

            // Footer Coordinate helper info
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cyan = Tap",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LaserPurple)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Purple = Swipe",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ElectricAmber)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Amber = Long Press",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
