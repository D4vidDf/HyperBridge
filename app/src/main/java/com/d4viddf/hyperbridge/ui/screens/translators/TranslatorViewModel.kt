package com.d4viddf.hyperbridge.ui.screens.translators

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
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
    ALL, ACTIVE, INACTIVE, GLOBAL, APPS, NOTIF_TYPES
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

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _installedThemes = MutableStateFlow<List<com.d4viddf.hyperbridge.models.theme.HyperTheme>>(emptyList())
    val installedThemes: StateFlow<List<com.d4viddf.hyperbridge.models.theme.HyperTheme>> = _installedThemes.asStateFlow()

    init {
        loadInstalledApps()
        loadInstalledThemes()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
                .map { AppItem(it.packageName, it.loadLabel(pm).toString()) }
                .sortedBy { it.label }
            _installedApps.value = apps
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

    private val _selectedFilter = MutableStateFlow(TranslatorFilterScope.ALL)
    val selectedFilter: StateFlow<TranslatorFilterScope> = _selectedFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: TranslatorFilterScope) {
        _selectedFilter.value = filter
    }

    fun getTranslatorsForApp(packageName: String): StateFlow<List<CustomTranslator>> {
        return allTranslators.map { list ->
            list.filter { translator ->
                translator.targetScope == TargetScope.GLOBAL ||
                (translator.targetScope == TargetScope.SPECIFIC_APPS && translator.targetPackages.contains(packageName))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    suspend fun getTranslatorById(id: String): CustomTranslator? = withContext(Dispatchers.IO) {
        translatorDao.getTranslatorById(id)?.toCustomTranslator()?.getOrNull()
    }

    fun toggleTranslator(id: String, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            translatorDao.setTranslatorEnabled(id, isEnabled)
        }
    }

    fun updatePriority(id: String, newPriority: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            translatorDao.updatePriority(id, newPriority)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                }
            } catch (_: Exception) {
                // Ignore permissions or missing package exceptions
            }
        }
        result.sortedWith(compareBy({ it.packageName ?: "" }, { it.name }))
    }
}
