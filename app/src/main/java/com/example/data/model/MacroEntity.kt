package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "Custom",
    val repeatCount: Int = 1, // -1 means infinite repeat
    val speedMultiplier: Float = 1.0f,
    val startDelaySeconds: Int = 3,
    val jitterRadiusPx: Int = 0, // Random touch variance for anti-detection
    val timingJitterMs: Int = 0, // Random delay variance
    val vibrateOnClick: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isInfiniteRepeat: Boolean
        get() = repeatCount <= 0
}
