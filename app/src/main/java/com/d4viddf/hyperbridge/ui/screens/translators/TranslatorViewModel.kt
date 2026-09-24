package com.d4viddf.hyperbridge.ui.screens.translators

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.service.NotificationReaderService
import com.d4viddf.hyperbridge.ui.screens.theme.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class TranslatorFilterScope {
    ALL, ACTIVE, INACTIVE, GLOBAL, APPS, SYSTEM_APPS, NOTIF_TYPES
}

data class TranslatorFilterState(
    val statusScope: TranslatorFilterScope = TranslatorFilterScope.ALL,
    val selectedNotificationTypes: Set<String> = emptySet(),
    val selectedPackages: Set<String> = emptySet(),
    val selectedAuthors: Set<String> = emptySet(),
    val selectedIcons: Set<String> = emptySet()
) {
    val isCustomFilterActive: Boolean
        get() = statusScope != TranslatorFilterScope.ALL ||
                selectedNotificationTypes.isNotEmpty() ||
                selectedPackages.isNotEmpty() ||
                selectedAuthors.isNotEmpty() ||
                selectedIcons.isNotEmpty()
}

data class NotificationChannelInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val packageName: String? = null
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val translatorDao = db.translatorDao()
    private val pm = application.packageManager
    private val themeRepo = com.d4viddf.hyperbridge.data.theme.ThemeRepository(application)
    private val translatorRepo = com.d4viddf.hyperbridge.data.translator.TranslatorRepository(application, translatorDao)

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _systemApps = MutableStateFlow<List<AppItem>>(emptyList())
    val systemApps: StateFlow<List<AppItem>> = _systemApps.asStateFlow()

    private val _installedThemes = MutableStateFlow<List<com.d4viddf.hyperbridge.models.theme.HyperTheme>>(emptyList())
    val installedThemes: StateFlow<List<com.d4viddf.hyperbridge.models.theme.HyperTheme>> = _installedThemes.asStateFlow()

    init {
        loadInstalledApps()
        loadSystemApps()
        loadInstalledThemes()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val currentPkg = getApplication<Application>().packageName

            val apps = resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val pkg = resolveInfo.activityInfo.packageName
                    if (pkg == currentPkg || pkg == "com.d4viddf.hyperbridge" || pkg == "com.d4viddf.hyperbridge.screenrecorder") return@mapNotNull null
                    val label = resolveInfo.loadLabel(pm).toString()
                    AppItem(pkg, label)
                } catch (_: Exception) { null }
            }.distinctBy { it.packageName }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

            _installedApps.value = apps
        }
    }

    private fun loadSystemApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPkg = getApplication<Application>().packageName
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val systemList = allApps.filter { appInfo ->
                appInfo.packageName != currentPkg &&
                appInfo.packageName != "com.d4viddf.hyperbridge" &&
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            }.map { appInfo ->
                val label = try {
                    appInfo.loadLabel(pm).toString().ifBlank { appInfo.packageName }
                } catch (_: Exception) {
                    appInfo.packageName
                }
                AppItem(packageName = appInfo.packageName, label = label)
            }.distinctBy { it.packageName }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

            _systemApps.value = systemList
        }
    }

    private fun loadInstalledThemes() {
        viewModelScope.launch(Dispatchers.IO) {
            val themes = themeRepo.getAvailableThemes()
            _installedThemes.value = themes
        }
    }

    val allTranslators: StateFlow<List<CustomTranslator>> = translatorDao.getAllTranslatorsFlow()
        .map { entities ->
            entities.mapNotNull { it.toCustomTranslator().getOrNull() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(TranslatorFilterState())
    val filterState: StateFlow<TranslatorFilterState> = _filterState.asStateFlow()

    // Backwards-compatible alias for single scope selection
    val selectedFilter: StateFlow<TranslatorFilterScope> = _filterState
        .map { it.statusScope }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TranslatorFilterScope.ALL)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: TranslatorFilterScope) {
        _filterState.value = _filterState.value.copy(statusScope = filter)
    }

    fun setFilterState(state: TranslatorFilterState) {
        _filterState.value = state
    }

    fun resetFilterState() {
        _filterState.value = TranslatorFilterState()
    }

    fun getTranslatorsForApp(packageName: String): StateFlow<List<CustomTranslator>> {
        return allTranslators.map { list ->
            list.filter { translator ->
                translator.targetScope == TargetScope.GLOBAL ||
                ((translator.targetScope == TargetScope.SPECIFIC_APPS || translator.targetScope == TargetScope.SYSTEM_APPS) && translator.targetPackages.contains(packageName))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getDesignsForApp(packageName: String): StateFlow<List<CustomTranslator>> {
        return allTranslators.map { list ->
            list.filter { translator ->
                (translator.presentation.mode == PresentationMode.TEMPLATE || translator.presentation.mode == PresentationMode.WIDGET) &&
                (translator.targetScope == TargetScope.GLOBAL ||
                ((translator.targetScope == TargetScope.SPECIFIC_APPS || translator.targetScope == TargetScope.SYSTEM_APPS) && translator.targetPackages.contains(packageName)))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun toggleDesignForApp(designId: String, packageName: String, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = translatorDao.getTranslatorById(designId) ?: return@launch
            val custom = existing.toCustomTranslator().getOrNull() ?: return@launch

            val updatedCustom = when (custom.targetScope) {
                TargetScope.GLOBAL -> {
                    val currentExcluded = custom.excludedPackages.toMutableList()
                    if (isEnabled) {
                        currentExcluded.removeAll { it.equals(packageName, ignoreCase = true) }
                    } else {
                        if (!currentExcluded.any { it.equals(packageName, ignoreCase = true) }) {
                            currentExcluded.add(packageName)
                        }
                    }
                    custom.copy(excludedPackages = currentExcluded)
                }
                TargetScope.SPECIFIC_APPS, TargetScope.SYSTEM_APPS -> {
                    val currentTargets = custom.targetPackages.toMutableList()
                    val currentExcluded = custom.excludedPackages.toMutableList()
                    if (isEnabled) {
                        currentExcluded.removeAll { it.equals(packageName, ignoreCase = true) }
                        if (!currentTargets.any { it.equals(packageName, ignoreCase = true) }) {
                            currentTargets.add(packageName)
                        }
                    } else {
                        if (!currentExcluded.any { it.equals(packageName, ignoreCase = true) }) {
                            currentExcluded.add(packageName)
                        }
                    }
                    custom.copy(
                        targetPackages = currentTargets,
                        excludedPackages = currentExcluded
                    )
                }
                TargetScope.NOTIFICATION_TYPE -> {
                    val currentExcluded = custom.excludedPackages.toMutableList()
                    if (isEnabled) {
                        currentExcluded.removeAll { it.equals(packageName, ignoreCase = true) }
                    } else {
                        if (!currentExcluded.any { it.equals(packageName, ignoreCase = true) }) {
                            currentExcluded.add(packageName)
                        }
                    }
                    custom.copy(excludedPackages = currentExcluded)
                }
            }

            val updatedJson = CustomTranslator.toJson(updatedCustom)
            val updatedEntity = existing.copy(
                targetPackages = updatedCustom.targetPackages.joinToString(","),
                jsonContent = updatedJson,
                updatedAt = System.currentTimeMillis()
            )
            translatorDao.updateTranslator(updatedEntity)
        }
    }

    suspend fun getTranslatorById(id: String): CustomTranslator? = withContext(Dispatchers.IO) {
        translatorDao.getTranslatorById(id)?.toCustomTranslator()?.getOrNull()
    }

    fun toggleTranslator(id: String, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = translatorDao.getTranslatorById(id)
            if (existing != null) {
                val updatedCustom = existing.toCustomTranslator().getOrNull()?.copy(isEnabled = isEnabled)
                val updatedJson = updatedCustom?.let { CustomTranslator.toJson(it) } ?: existing.jsonContent
                val updatedEntity = existing.copy(
                    isEnabled = isEnabled,
                    jsonContent = updatedJson,
                    updatedAt = System.currentTimeMillis()
                )
                translatorDao.updateTranslator(updatedEntity)
            } else {
                translatorDao.setTranslatorEnabled(id, isEnabled)
            }
        }
    }

    fun updatePriority(id: String, newPriority: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = translatorDao.getTranslatorById(id)
            if (existing != null) {
                val updatedCustom = existing.toCustomTranslator().getOrNull()?.copy(priority = newPriority)
                val updatedJson = updatedCustom?.let { CustomTranslator.toJson(it) } ?: existing.jsonContent
                val updatedEntity = existing.copy(
                    priority = newPriority,
                    jsonContent = updatedJson,
                    updatedAt = System.currentTimeMillis()
                )
                translatorDao.updateTranslator(updatedEntity)
            } else {
                translatorDao.updatePriority(id, newPriority)
            }
        }
    }

    fun saveTranslator(translator: CustomTranslator, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = TranslatorEntity.fromCustomTranslator(translator)
            translatorDao.insertTranslator(entity)
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun duplicateTranslator(source: CustomTranslator, onDone: () -> Unit = {}) {
        val newId = UUID.randomUUID().toString()
        val duplicated = source.copy(
            id = newId,
            meta = source.meta.copy(
                name = "${source.meta.name} (Copy)"
            ),
            priority = source.priority - 1
        )
        saveTranslator(duplicated, onDone)
    }

    fun deleteTranslator(id: String, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            translatorDao.deleteTranslatorById(id)
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun importTranslator(uri: Uri, onResult: (Result<CustomTranslator>) -> Unit) {
        viewModelScope.launch {
            val result = translatorRepo.importTranslatorFromUri(uri)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun exportTranslatorToUri(translator: CustomTranslator, uri: Uri, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    translatorRepo.exportTranslatorToZip(translator, outputStream).getOrThrow()
                } ?: throw IllegalArgumentException("Could not open output stream for URI: $uri")
            }
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun shareTranslator(context: Context, translator: CustomTranslator, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = translatorRepo.shareTranslator(context, translator)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    suspend fun getNotificationChannelsForPackages(targetPackages: List<String>): List<NotificationChannelInfo> = withContext(Dispatchers.IO) {
        val service = NotificationReaderService.instance ?: return@withContext emptyList()
        val userHandle = android.os.Process.myUserHandle()
        val result = mutableListOf<NotificationChannelInfo>()

        val packagesToQuery = if (targetPackages.isNotEmpty()) {
            targetPackages
        } else {
            installedApps.value.map { it.packageName }
        }

        for (pkg in packagesToQuery) {
            try {
                val channels = service.getNotificationChannels(pkg, userHandle) ?: emptyList()
                for (ch in channels) {
                    result.add(
                        NotificationChannelInfo(
                            id = ch.id,
                            name = ch.name?.toString() ?: ch.id,
                            description = ch.description,
                            packageName = pkg
                        )
                    )
                }
            } catch (_: Exception) {
                // Ignore permissions or missing package exceptions
            }
        }
        result.sortedWith(compareBy({ it.packageName ?: "" }, { it.name }))
    }
}
