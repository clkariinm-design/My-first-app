package com.example.data.model

enum class StepType(val displayName: String, val description: String) {
    TAP("Tap", "Single tap on target coordinate"),
    DOUBLE_TAP("Double Tap", "Rapid two taps at coordinate"),
    LONG_PRESS("Long Press", "Continuous touch hold at coordinate"),
    SWIPE("Swipe Gesture", "Drag from start point to end point"),
    WAIT("Wait / Delay", "Pause execution for specified duration")
}
