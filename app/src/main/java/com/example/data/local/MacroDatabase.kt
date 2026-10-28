package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.MacroEntity
import com.example.data.model.MacroStepEntity
import com.example.data.model.StepType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MacroEntity::class, MacroStepEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MacroDatabase : RoomDatabase() {

    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: MacroDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): MacroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MacroDatabase::class.java,
                    "macroflow_pro.db"
                )
                    .addCallback(MacroDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class MacroDatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialMacros(database.macroDao())
                    }
                }
            }

            private suspend fun populateInitialMacros(dao: MacroDao) {
                // 1. Fast Clicker (CPS Burst)
                val m1 = MacroEntity(
                    name = "Fast Clicker (CPS Burst)",
                    description = "Rapid multi-point auto-tapper at center screen with jitter anti-detection.",
                    category = "Gaming",
                    repeatCount = 10,
                    speedMultiplier = 1.0f,
                    startDelaySeconds = 3,
                    jitterRadiusPx = 6,
                    timingJitterMs = 15,
                    vibrateOnClick = true
                )
                val m1Steps = listOf(
                    MacroStepEntity(macroId = 0, stepOrder = 0, type = StepType.TAP.name, x = 540f, y = 960f, durationMs = 40L, delayAfterMs = 100L, label = "Center Tap 1"),
                    MacroStepEntity(macroId = 0, stepOrder = 1, type = StepType.TAP.name, x = 540f, y = 960f, durationMs = 40L, delayAfterMs = 100L, label = "Center Tap 2"),
                    MacroStepEntity(macroId = 0, stepOrder = 2, type = StepType.TAP.name, x = 540f, y = 960f, durationMs = 40L, delayAfterMs = 100L, label = "Center Tap 3"),
                    MacroStepEntity(macroId = 0, stepOrder = 3, type = StepType.TAP.name, x = 540f, y = 960f, durationMs = 40L, delayAfterMs = 100L, label = "Center Tap 4")
                )
                dao.insertMacroWithSteps(m1, m1Steps)

                // 2. Social Media Reel Auto-Scroller
                val m2 = MacroEntity(
                    name = "Social Reel Auto-Scroller",
                    description = "Automatically swipes up to next video every 6 seconds. Infinite loop.",
                    category = "Social",
                    repeatCount = -1, // Infinite
                    speedMultiplier = 1.0f,
                    startDelaySeconds = 5,
                    jitterRadiusPx = 8,
                    timingJitterMs = 200,
                    vibrateOnClick = false
                )
                val m2Steps = listOf(
                    MacroStepEntity(
                        macroId = 0,
                        stepOrder = 0,
                        type = StepType.SWIPE.name,
                        x = 540f,
                        y = 1500f,
                        endX = 540f,
                        endY = 400f,
                        durationMs = 280L,
                        delayAfterMs = 6000L,
                        label = "Swipe Up Video"
                    )
                )
                dao.insertMacroWithSteps(m2, m2Steps)

                // 3. Dual-Target Alternating Tapper
                val m3 = MacroEntity(
                    name = "Dual Alternating Tap",
                    description = "Alternates clicking two separate on-screen coordinates.",
                    category = "Gaming",
                    repeatCount = 20,
                    speedMultiplier = 1.2f,
                    startDelaySeconds = 3,
                    jitterRadiusPx = 4,
                    timingJitterMs = 20,
                    vibrateOnClick = true
                )
                val m3Steps = listOf(
                    MacroStepEntity(macroId = 0, stepOrder = 0, type = StepType.TAP.name, x = 320f, y = 1100f, durationMs = 50L, delayAfterMs = 500L, label = "Left Target A"),
                    MacroStepEntity(macroId = 0, stepOrder = 1, type = StepType.TAP.name, x = 760f, y = 1100f, durationMs = 50L, delayAfterMs = 500L, label = "Right Target B")
                )
                dao.insertMacroWithSteps(m3, m3Steps)

                // 4. Daily Reward Claim & Dismiss
                val m4 = MacroEntity(
                    name = "Reward Claim & Dismiss",
                    description = "Three-step sequence to click claim button, wait for animation, and close popup.",
                    category = "Productivity",
                    repeatCount = 1,
                    speedMultiplier = 1.0f,
                    startDelaySeconds = 3,
                    jitterRadiusPx = 0,
                    timingJitterMs = 0,
                    vibrateOnClick = true
                )
                val m4Steps = listOf(
                    MacroStepEntity(macroId = 0, stepOrder = 0, type = StepType.TAP.name, x = 540f, y = 1350f, durationMs = 60L, delayAfterMs = 1500L, label = "Claim Button"),
                    MacroStepEntity(macroId = 0, stepOrder = 1, type = StepType.WAIT.name, x = 0f, y = 0f, durationMs = 0L, delayAfterMs = 2000L, label = "Wait for Animation"),
                    MacroStepEntity(macroId = 0, stepOrder = 2, type = StepType.TAP.name, x = 950f, y = 250f, durationMs = 60L, delayAfterMs = 800L, label = "Close Modal (X)")
                )
                dao.insertMacroWithSteps(m4, m4Steps)
            }
        }
    }
}
