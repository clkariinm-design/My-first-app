package com.example.engine

sealed interface ExecutionStatus {
    object Idle : ExecutionStatus
    data class Countdown(val secondsRemaining: Int) : ExecutionStatus
    data class Running(
        val currentLoop: Int,
        val totalLoops: Int, // -1 for infinite
        val currentStepIndex: Int,
        val totalSteps: Int,
        val stepDescription: String,
        val targetX: Float,
        val targetY: Float
    ) : ExecutionStatus
    data class Paused(
        val currentLoop: Int,
        val totalLoops: Int,
        val currentStepIndex: Int,
        val totalSteps: Int
    ) : ExecutionStatus
    object Completed : ExecutionStatus
    object Stopped : ExecutionStatus
    data class Error(val message: String) : ExecutionStatus
}

data class ExecutionLog(
    val timestamp: Long = System.currentTimeMillis(),
    val stepIndex: Int,
    val loopNumber: Int,
    val actionText: String,
    val isSuccess: Boolean = true
)
