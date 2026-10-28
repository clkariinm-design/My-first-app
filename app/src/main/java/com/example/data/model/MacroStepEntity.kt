package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "macro_steps",
    foreignKeys = [
        ForeignKey(
            entity = MacroEntity::class,
            parentColumns = ["id"],
            childColumns = ["macroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["macroId"])]
)
data class MacroStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val macroId: Long,
    val stepOrder: Int,
    val type: String = StepType.TAP.name,
    val x: Float = 540f,
    val y: Float = 960f,
    val endX: Float = 540f,
    val endY: Float = 480f,
    val durationMs: Long = 50L,
    val delayAfterMs: Long = 1000L,
    val label: String = ""
) {
    val stepType: StepType
        get() = try {
            StepType.valueOf(type)
        } catch (e: Exception) {
            StepType.TAP
        }
}
