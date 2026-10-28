package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.local.MacroDatabase
import com.example.data.repository.MacroRepository
import com.example.engine.ExecutionStatus
import com.example.engine.MacroExecutionEngine
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MacroFloatingOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private lateinit var engine: MacroExecutionEngine
    private lateinit var repository: MacroRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        engine = MacroExecutionEngine.getInstance(this)
        repository = MacroRepository(MacroDatabase.getDatabase(this).macroDao())

        startForegroundNotification()

        if (Settings.canDrawOverlays(this)) {
            setupFloatingWindow()
        }
        _isOverlayRunning.value = true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val channelId = "macroflow_overlay_channel"
        val channelName = "MacroFlow Overlay Controller"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MacroFlow Pro Controller")
            .setContentText("Floating overlay is active and ready")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 200
        }
        windowParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MacroFloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@MacroFloatingOverlayService)
            setContent {
                FloatingOverlayUi(
                    engine = engine,
                    onDragWindow = { dx, dy ->
                        params.x += dx.roundToInt()
                        params.y += dy.roundToInt()
                        windowManager?.updateViewLayout(this, params)
                    },
                    onClose = {
                        stopSelf()
                    },
                    onOpenApp = {
                        val intent = Intent(this@MacroFloatingOverlayService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                    }
                )
            }
        }

        overlayView = composeView
        windowManager?.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        _isOverlayRunning.value = false
    }

    companion object {
        private val _isOverlayRunning = MutableStateFlow(false)
        val isOverlayRunning = _isOverlayRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, MacroFloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MacroFloatingOverlayService::class.java)
            context.stopService(intent)
        }
    }
}

@Composable
fun FloatingOverlayUi(
    engine: MacroExecutionEngine,
    onDragWindow: (Float, Float) -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showTargetCrosshair by remember { mutableStateOf(false) }
    var crosshairOffset by remember { mutableStateOf(IntOffset(0, 0)) }

    val status by engine.status.collectAsState()
    val currentMacro by engine.currentMacro.collectAsState()

    Box {
        // Floating Control Panel
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSlate.copy(alpha = 0.94f))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Drag handle and minimize button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragWindow(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    // Glowing Status Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (status) {
                                    is ExecutionStatus.Running -> NeonCyan
                                    is ExecutionStatus.Countdown -> ElectricAmber
                                    is ExecutionStatus.Paused -> ElectricAmber
                                    else -> TextSecondary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "MacroFlow",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Toggle expand / collapse
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(DarkObsidian)
                            .clickable { isExpanded = !isExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isExpanded) "−" else "+",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Close overlay
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(NeonRed.copy(alpha = 0.2f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Overlay",
                            tint = NeonRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Current Macro & Step Information
                        val statusText = when (val s = status) {
                            is ExecutionStatus.Countdown -> "Starting in ${s.secondsRemaining}s..."
                            is ExecutionStatus.Running -> "Loop ${s.currentLoop} • Step ${s.currentStepIndex}/${s.totalSteps}"
                            is ExecutionStatus.Paused -> "Paused (Step ${s.currentStepIndex}/${s.totalSteps})"
                            is ExecutionStatus.Completed -> "Finished All Steps"
                            is ExecutionStatus.Stopped -> "Stopped"
                            is ExecutionStatus.Idle -> currentMacro?.macro?.name ?: "No Macro Selected"
                            is ExecutionStatus.Error -> "Error: ${s.message}"
                        }

                        Text(
                            text = statusText,
                            color = when (status) {
                                is ExecutionStatus.Running -> NeonCyan
                                is ExecutionStatus.Countdown -> ElectricAmber
                                else -> TextSecondary
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Controls Row: Play/Pause, Stop, Target Picker, Open App
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play / Pause Button
                            val isRunning = status is ExecutionStatus.Running
                            val isPaused = status is ExecutionStatus.Paused

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) ElectricAmber else NeonCyan)
                                    .clickable {
                                        if (isRunning) {
                                            engine.pause()
                                        } else if (isPaused) {
                                            engine.resume()
                                        } else {
                                            currentMacro?.let { engine.start(it) } ?: onOpenApp()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isRunning) "Pause" else "Play",
                                    tint = DarkObsidian,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Stop Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonRed.copy(alpha = 0.25f))
                                    .clickable { engine.stop() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = NeonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Target Coordinate Crosshair Toggle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (showTargetCrosshair) NeonCyan.copy(alpha = 0.3f) else DarkObsidian)
                                    .clickable { showTargetCrosshair = !showTargetCrosshair },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = "Target Pointer",
                                    tint = if (showTargetCrosshair) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Open App Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkObsidian)
                                    .clickable { onOpenApp() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Open App",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Draggable Crosshair Target Marker
        if (showTargetCrosshair) {
            Box(
                modifier = Modifier
                    .offset { crosshairOffset }
                    .size(60.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            crosshairOffset = IntOffset(
                                (crosshairOffset.x + dragAmount.x).roundToInt(),
                                (crosshairOffset.y + dragAmount.y).roundToInt()
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Crosshair circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, NeonCyan, CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                )
                // Center point
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
            }
        }
    }
}
