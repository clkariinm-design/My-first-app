package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MacroWithSteps(
    @Embedded
    val macro: MacroEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "macroId"
    )
    val steps: List<MacroStepEntity> = emptyList()
) {
    val sortedSteps: List<MacroStepEntity>
        get() = steps.sortedBy { it.stepOrder }

    val totalDurationMs: Long
        get() = steps.sumOf { it.durationMs + it.delayAfterMs }
}
