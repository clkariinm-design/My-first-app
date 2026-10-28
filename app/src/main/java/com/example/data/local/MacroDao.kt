package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.MacroEntity
import com.example.data.model.MacroStepEntity
import com.example.data.model.MacroWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {

    @Transaction
    @Query("SELECT * FROM macros ORDER BY updatedAt DESC")
    fun getAllMacros(): Flow<List<MacroWithSteps>>

    @Transaction
    @Query("SELECT * FROM macros WHERE id = :id")
    fun getMacroById(id: Long): Flow<MacroWithSteps?>

    @Transaction
    @Query("SELECT * FROM macros WHERE id = :id")
    suspend fun getMacroByIdOnce(id: Long): MacroWithSteps?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity): Long

    @Update
    suspend fun updateMacro(macro: MacroEntity)

    @Delete
    suspend fun deleteMacro(macro: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacroById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: MacroStepEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<MacroStepEntity>): List<Long>

    @Update
    suspend fun updateStep(step: MacroStepEntity)

    @Delete
    suspend fun deleteStep(step: MacroStepEntity)

    @Query("DELETE FROM macro_steps WHERE macroId = :macroId")
    suspend fun deleteStepsForMacro(macroId: Long)

    @Transaction
    suspend fun replaceStepsForMacro(macroId: Long, newSteps: List<MacroStepEntity>) {
        deleteStepsForMacro(macroId)
        val orderedSteps = newSteps.mapIndexed { index, step ->
            step.copy(id = 0, macroId = macroId, stepOrder = index)
        }
        insertSteps(orderedSteps)
        val macro = getMacroByIdOnce(macroId)?.macro
        if (macro != null) {
            updateMacro(macro.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    @Transaction
    suspend fun insertMacroWithSteps(macro: MacroEntity, steps: List<MacroStepEntity>): Long {
        val macroId = insertMacro(macro)
        val orderedSteps = steps.mapIndexed { index, step ->
            step.copy(id = 0, macroId = macroId, stepOrder = index)
        }
        insertSteps(orderedSteps)
        return macroId
    }

    @Transaction
    suspend fun duplicateMacro(macroId: Long): Long? {
        val original = getMacroByIdOnce(macroId) ?: return null
        val newMacro = original.macro.copy(
            id = 0,
            name = "${original.macro.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = insertMacro(newMacro)
        val newSteps = original.steps.map { it.copy(id = 0, macroId = newId) }
        insertSteps(newSteps)
        return newId
    }
}
