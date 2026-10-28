package com.example.data.repository

import com.example.data.local.MacroDao
import com.example.data.model.MacroEntity
import com.example.data.model.MacroStepEntity
import com.example.data.model.MacroWithSteps
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class MacroRepository(private val macroDao: MacroDao) {

    val allMacros: Flow<List<MacroWithSteps>> = macroDao.getAllMacros()

    fun getMacroById(id: Long): Flow<MacroWithSteps?> = macroDao.getMacroById(id)

    suspend fun getMacroByIdOnce(id: Long): MacroWithSteps? = macroDao.getMacroByIdOnce(id)

    suspend fun saveMacroWithSteps(macro: MacroEntity, steps: List<MacroStepEntity>): Long {
        return if (macro.id > 0) {
            macroDao.updateMacro(macro.copy(updatedAt = System.currentTimeMillis()))
            macroDao.replaceStepsForMacro(macro.id, steps)
            macro.id
        } else {
            macroDao.insertMacroWithSteps(macro, steps)
        }
    }

    suspend fun updateMacro(macro: MacroEntity) {
        macroDao.updateMacro(macro.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun replaceSteps(macroId: Long, steps: List<MacroStepEntity>) {
        macroDao.replaceStepsForMacro(macroId, steps)
    }

    suspend fun deleteMacroById(id: Long) {
        macroDao.deleteMacroById(id)
    }

    suspend fun duplicateMacro(id: Long): Long? {
        return macroDao.duplicateMacro(id)
    }

    fun exportToJson(macroWithSteps: MacroWithSteps): String {
        val root = JSONObject()
        val m = macroWithSteps.macro
        root.put("format", "MacroFlowPro_v1")
        root.put("name", m.name)
        root.put("description", m.description)
        root.put("category", m.category)
        root.put("repeatCount", m.repeatCount)
        root.put("speedMultiplier", m.speedMultiplier.toDouble())
        root.put("startDelaySeconds", m.startDelaySeconds)
        root.put("jitterRadiusPx", m.jitterRadiusPx)
        root.put("timingJitterMs", m.timingJitterMs)
        root.put("vibrateOnClick", m.vibrateOnClick)

        val stepsArray = JSONArray()
        macroWithSteps.sortedSteps.forEach { s ->
            val stepObj = JSONObject()
            stepObj.put("type", s.type)
            stepObj.put("x", s.x.toDouble())
            stepObj.put("y", s.y.toDouble())
            stepObj.put("endX", s.endX.toDouble())
            stepObj.put("endY", s.endY.toDouble())
            stepObj.put("durationMs", s.durationMs)
            stepObj.put("delayAfterMs", s.delayAfterMs)
            stepObj.put("label", s.label)
            stepsArray.put(stepObj)
        }
        root.put("steps", stepsArray)
        return root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Long {
        val root = JSONObject(jsonString)
        val name = root.optString("name", "Imported Macro")
        val description = root.optString("description", "Imported via MacroFlow JSON")
        val category = root.optString("category", "Custom")
        val repeatCount = root.optInt("repeatCount", 1)
        val speedMultiplier = root.optDouble("speedMultiplier", 1.0).toFloat()
        val startDelaySeconds = root.optInt("startDelaySeconds", 3)
        val jitterRadiusPx = root.optInt("jitterRadiusPx", 0)
        val timingJitterMs = root.optInt("timingJitterMs", 0)
        val vibrateOnClick = root.optBoolean("vibrateOnClick", true)

        val macro = MacroEntity(
            name = "$name (Imported)",
            description = description,
            category = category,
            repeatCount = repeatCount,
            speedMultiplier = speedMultiplier,
            startDelaySeconds = startDelaySeconds,
            jitterRadiusPx = jitterRadiusPx,
            timingJitterMs = timingJitterMs,
            vibrateOnClick = vibrateOnClick
        )

        val stepsArray = root.optJSONArray("steps") ?: JSONArray()
        val steps = mutableListOf<MacroStepEntity>()
        for (i in 0 until stepsArray.length()) {
            val sObj = stepsArray.getJSONObject(i)
            steps.add(
                MacroStepEntity(
                    macroId = 0,
                    stepOrder = i,
                    type = sObj.optString("type", "TAP"),
                    x = sObj.optDouble("x", 540.0).toFloat(),
                    y = sObj.optDouble("y", 960.0).toFloat(),
                    endX = sObj.optDouble("endX", 540.0).toFloat(),
                    endY = sObj.optDouble("endY", 480.0).toFloat(),
                    durationMs = sObj.optLong("durationMs", 50L),
                    delayAfterMs = sObj.optLong("delayAfterMs", 1000L),
                    label = sObj.optString("label", "")
                )
            )
        }
        return saveMacroWithSteps(macro, steps)
    }
}
