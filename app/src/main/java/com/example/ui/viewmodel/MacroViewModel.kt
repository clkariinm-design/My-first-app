package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MacroDatabase
import com.example.data.model.MacroEntity
import com.example.data.model.MacroStepEntity
import com.example.data.model.MacroWithSteps
import com.example.data.model.StepType
import com.example.data.repository.MacroRepository
import com.example.engine.ExecutionLog
import com.example.engine.ExecutionStatus
import com.example.engine.MacroExecutionEngine
import com.example.service.MacroAccessibilityService
import com.example.service.MacroFloatingOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MacroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MacroRepository
    private val engine: MacroExecutionEngine

    init {
        val db = MacroDatabase.getDatabase(application, viewModelScope)
        repository = MacroRepository(db.macroDao())
        engine = MacroExecutionEngine.getInstance(application)
    }

    val allMacros: StateFlow<List<MacroWithSteps>> = repository.allMacros
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val engineStatus: StateFlow<ExecutionStatus> = engine.status
    val engineLogs: StateFlow<List<ExecutionLog>> = engine.logs
    val currentExecutingMacro: StateFlow<MacroWithSteps?> = engine.currentMacro

    val isAccessibilityBound: StateFlow<Boolean> = MacroAccessibilityService.isServiceBound
    val isOverlayServiceRunning: StateFlow<Boolean> = MacroFloatingOverlayService.isOverlayRunning

    private val _isAccessibilityEnabledInSettings = MutableStateFlow(false)
    val isAccessibilityEnabledInSettings: StateFlow<Boolean> = _isAccessibilityEnabledInSettings.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredMacros: StateFlow<List<MacroWithSteps>> = combine(
        allMacros,
        _searchQuery,
        _selectedCategory
    ) { macros, query, cat ->
        macros.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.macro.name.contains(query, ignoreCase = true) ||
                    item.macro.description.contains(query, ignoreCase = true)
            val matchesCategory = cat == "All" || item.macro.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun refreshPermissions(context: Context) {
        _isAccessibilityEnabledInSettings.value =
            MacroAccessibilityService.isAccessibilitySettingsEnabled(context) ||
                    MacroAccessibilityService.instance != null
        _isOverlayPermissionGranted.value = Settings.canDrawOverlays(context)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun createMacro(
        name: String,
        category: String,
        description: String = "",
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val entity = MacroEntity(
                name = name.ifBlank { "New Macro" },
                category = category,
                description = description,
                repeatCount = 1,
                speedMultiplier = 1.0f,
                startDelaySeconds = 3,
                jitterRadiusPx = 0,
                timingJitterMs = 0,
                vibrateOnClick = true
            )
            // Default with 1 tap step
            val defaultStep = MacroStepEntity(
                macroId = 0,
                stepOrder = 0,
                type = StepType.TAP.name,
                x = 540f,
                y = 960f,
                durationMs = 50L,
                delayAfterMs = 1000L,
                label = "First Tap"
            )
            val id = repository.saveMacroWithSteps(entity, listOf(defaultStep))
            onCreated(id)
        }
    }

    fun updateMacro(macro: MacroEntity) {
        viewModelScope.launch {
            repository.updateMacro(macro)
        }
    }

    fun saveMacroAndSteps(macro: MacroEntity, steps: List<MacroStepEntity>) {
        viewModelScope.launch {
            repository.saveMacroWithSteps(macro, steps)
        }
    }

    fun duplicateMacro(id: Long) {
        viewModelScope.launch {
            repository.duplicateMacro(id)
        }
    }

    fun deleteMacro(id: Long) {
        viewModelScope.launch {
            if (currentExecutingMacro.value?.macro?.id == id) {
                stopMacro()
            }
            repository.deleteMacroById(id)
        }
    }

    fun startMacro(macroWithSteps: MacroWithSteps) {
        engine.start(macroWithSteps)
    }

    fun pauseMacro() {
        engine.pause()
    }

    fun resumeMacro() {
        engine.resume()
    }

    fun stopMacro() {
        engine.stop()
    }

    fun exportToJson(macroWithSteps: MacroWithSteps): String {
        return repository.exportToJson(macroWithSteps)
    }

    fun importFromJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val newId = repository.importFromJson(jsonString)
                onComplete(true, "Macro imported successfully (ID: $newId)")
            } catch (e: Exception) {
                onComplete(false, "Failed to import: ${e.localizedMessage}")
            }
        }
    }

    fun toggleFloatingOverlay(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }

        if (isOverlayServiceRunning.value) {
            MacroFloatingOverlayService.stopService(context)
        } else {
            MacroFloatingOverlayService.startService(context)
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
