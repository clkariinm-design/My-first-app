package com.example.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.MacroWithSteps
import com.example.data.model.StepType
import com.example.service.MacroAccessibilityService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class MacroExecutionEngine private constructor(private val appContext: Context) {

    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var executionJob: Job? = null

    private val _status = MutableStateFlow<ExecutionStatus>(ExecutionStatus.Idle)
    val status: StateFlow<ExecutionStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<ExecutionLog>>(emptyList())
    val logs: StateFlow<List<ExecutionLog>> = _logs.asStateFlow()

    private val _currentMacro = MutableStateFlow<MacroWithSteps?>(null)
    val currentMacro: StateFlow<MacroWithSteps?> = _currentMacro.asStateFlow()

    private var isPaused = false
    private val pauseMutex = Mutex()

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun start(macroWithSteps: MacroWithSteps) {
        stop() // Ensure previous run is cleared
        _currentMacro.value = macroWithSteps
        _logs.value = emptyList()
        isPaused = false

        executionJob = engineScope.launch {
            try {
                val macro = macroWithSteps.macro
                val steps = macroWithSteps.sortedSteps

                if (steps.isEmpty()) {
                    _status.value = ExecutionStatus.Error("Macro has no steps to execute.")
                    return@launch
                }

                // 1. Initial Countdown
                val countdownSeconds = macro.startDelaySeconds
                for (sec in countdownSeconds downTo 1) {
                    _status.value = ExecutionStatus.Countdown(sec)
                    delay(1000L)
                }

                val speed = macro.speedMultiplier.coerceAtLeast(0.1f)
                val isInfinite = macro.isInfiniteRepeat
                val maxLoops = if (isInfinite) Int.MAX_VALUE else macro.repeatCount

                var currentLoop = 1

                while (currentLoop <= maxLoops) {
                    addLog(-1, currentLoop, "--- Starting Loop $currentLoop ${if (isInfinite) "(Infinite Mode)" else "of $maxLoops"} ---")

                    for ((stepIndex, step) in steps.withIndex()) {
                        // Check paused state
                        while (isPaused) {
                            _status.value = ExecutionStatus.Paused(
                                currentLoop = currentLoop,
                                totalLoops = if (isInfinite) -1 else maxLoops,
                                currentStepIndex = stepIndex + 1,
                                totalSteps = steps.size
                            )
                            delay(200L)
                        }

                        // Jitter calculations
                        val jitterX = if (macro.jitterRadiusPx > 0) Random.nextInt(-macro.jitterRadiusPx, macro.jitterRadiusPx + 1) else 0
                        val jitterY = if (macro.jitterRadiusPx > 0) Random.nextInt(-macro.jitterRadiusPx, macro.jitterRadiusPx + 1) else 0
                        val jitterDelay = if (macro.timingJitterMs > 0) Random.nextLong(0, macro.timingJitterMs.toLong() + 1) else 0L

                        val targetX = (step.x + jitterX).coerceAtLeast(0f)
                        val targetY = (step.y + jitterY).coerceAtLeast(0f)
                        val targetEndX = (step.endX + jitterX).coerceAtLeast(0f)
                        val targetEndY = (step.endY + jitterY).coerceAtLeast(0f)

                        val effectiveDuration = (step.durationMs / speed).toLong().coerceAtLeast(10L)
                        val effectiveDelay = ((step.delayAfterMs / speed) + jitterDelay).toLong().coerceAtLeast(10L)

                        val stepDesc = when (step.stepType) {
                            StepType.TAP -> "Tap at (${targetX.toInt()}, ${targetY.toInt()})"
                            StepType.DOUBLE_TAP -> "Double Tap at (${targetX.toInt()}, ${targetY.toInt()})"
                            StepType.LONG_PRESS -> "Long Press (${effectiveDuration}ms) at (${targetX.toInt()}, ${targetY.toInt()})"
                            StepType.SWIPE -> "Swipe (${targetX.toInt()}, ${targetY.toInt()}) -> (${targetEndX.toInt()}, ${targetEndY.toInt()})"
                            StepType.WAIT -> "Wait for ${effectiveDelay}ms"
                        }

                        _status.value = ExecutionStatus.Running(
                            currentLoop = currentLoop,
                            totalLoops = if (isInfinite) -1 else maxLoops,
                            currentStepIndex = stepIndex + 1,
                            totalSteps = steps.size,
                            stepDescription = stepDesc,
                            targetX = targetX,
                            targetY = targetY
                        )

                        // Execute Gesture via Accessibility Service or Simulation fallback
                        val service = MacroAccessibilityService.instance
                        val isNative = service != null

                        when (step.stepType) {
                            StepType.TAP -> {
                                if (isNative) {
                                    service?.dispatchTap(targetX, targetY, effectiveDuration)
                                }
                                triggerHaptic(macro.vibrateOnClick)
                                addLog(stepIndex + 1, currentLoop, "[Tap] (${targetX.toInt()}, ${targetY.toInt()}) [${if (isNative) "Native" else "Simulated"}]")
                            }
                            StepType.DOUBLE_TAP -> {
                                if (isNative) {
                                    service?.dispatchTap(targetX, targetY, 40L)
                                    delay(80L)
                                    service?.dispatchTap(targetX, targetY, 40L)
                                }
                                triggerHaptic(macro.vibrateOnClick)
                                addLog(stepIndex + 1, currentLoop, "[Double Tap] (${targetX.toInt()}, ${targetY.toInt()}) [${if (isNative) "Native" else "Simulated"}]")
                            }
                            StepType.LONG_PRESS -> {
                                if (isNative) {
                                    service?.dispatchTap(targetX, targetY, effectiveDuration)
                                }
                                triggerHaptic(macro.vibrateOnClick)
                                addLog(stepIndex + 1, currentLoop, "[Long Press] ${effectiveDuration}ms at (${targetX.toInt()}, ${targetY.toInt()}) [${if (isNative) "Native" else "Simulated"}]")
                            }
                            StepType.SWIPE -> {
                                if (isNative) {
                                    service?.dispatchSwipe(targetX, targetY, targetEndX, targetEndY, effectiveDuration)
                                }
                                triggerHaptic(macro.vibrateOnClick)
                                addLog(stepIndex + 1, currentLoop, "[Swipe] (${targetX.toInt()}, ${targetY.toInt()}) to (${targetEndX.toInt()}, ${targetEndY.toInt()}) [${if (isNative) "Native" else "Simulated"}]")
                            }
                            StepType.WAIT -> {
                                addLog(stepIndex + 1, currentLoop, "[Wait] Pausing ${effectiveDelay}ms")
                            }
                        }

                        // Delay after step
                        delay(effectiveDelay)
                    }

                    currentLoop++
                }

                addLog(-1, currentLoop - 1, "Macro sequence completed successfully.")
                _status.value = ExecutionStatus.Completed
            } catch (e: CancellationException) {
                addLog(-1, -1, "Execution stopped by user.")
                _status.value = ExecutionStatus.Stopped
            } catch (e: Exception) {
                addLog(-1, -1, "Execution error: ${e.localizedMessage}")
                _status.value = ExecutionStatus.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun pause() {
        if (_status.value is ExecutionStatus.Running) {
            isPaused = true
        }
    }

    fun resume() {
        if (isPaused) {
            isPaused = false
        }
    }

    fun stop() {
        executionJob?.cancel()
        executionJob = null
        isPaused = false
        if (_status.value !is ExecutionStatus.Idle && _status.value !is ExecutionStatus.Completed) {
            _status.value = ExecutionStatus.Stopped
        }
    }

    private fun addLog(stepIndex: Int, loop: Int, text: String, success: Boolean = true) {
        val entry = ExecutionLog(
            stepIndex = stepIndex,
            loopNumber = loop,
            actionText = text,
            isSuccess = success
        )
        _logs.value = (_logs.value + entry).takeLast(100)
    }

    private fun triggerHaptic(enabled: Boolean) {
        if (!enabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20L)
            }
        } catch (e: Exception) {
            // Ignored if vibrator not permitted or available
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MacroExecutionEngine? = null

        fun getInstance(context: Context): MacroExecutionEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = MacroExecutionEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
