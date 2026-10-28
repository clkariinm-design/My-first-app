package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MacroEntity
import com.example.data.model.MacroStepEntity
import com.example.data.model.StepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MacroFlow Pro", appName)
  }

  @Test
  fun `macro entity creation and step mapping`() {
    val macro = MacroEntity(
      name = "Auto Clicker Test",
      category = "Gaming",
      repeatCount = 10,
      speedMultiplier = 1.5f
    )
    val step = MacroStepEntity(
      macroId = 1L,
      stepOrder = 0,
      type = StepType.TAP.name,
      x = 500f,
      y = 1000f,
      durationMs = 50L,
      delayAfterMs = 200L
    )
    assertEquals("Auto Clicker Test", macro.name)
    assertEquals(StepType.TAP, step.stepType)
    assertEquals(500f, step.x)
    assertEquals(1000f, step.y)
  }
}
