package com.d4viddf.hyperbridge.data.composer

import com.d4viddf.hyperbridge.models.composer.ComposerActionButton
import com.d4viddf.hyperbridge.models.composer.ComposerTemplate
import com.d4viddf.hyperbridge.models.composer.ComposerTemplateType
import com.d4viddf.hyperbridge.models.composer.FieldBinding
import com.d4viddf.hyperbridge.models.composer.GraphicSource
import com.d4viddf.hyperbridge.models.composer.IslandTemplateDefinition
import com.d4viddf.hyperbridge.models.composer.LeftGraphicSlot
import com.d4viddf.hyperbridge.models.composer.NotificationField
import com.d4viddf.hyperbridge.models.composer.ProgressKind
import com.d4viddf.hyperbridge.models.composer.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.composer.TemplateRule
import com.d4viddf.hyperbridge.models.composer.TextSlotConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ComposerTemplateRepositoryTest {

    private class FakeComposerTemplateDao : ComposerTemplateDao {
        val store = mutableMapOf<String, ComposerTemplateEntity>()
        val flow = MutableStateFlow<List<ComposerTemplateEntity>>(emptyList())

        private fun publish() {
            flow.value = store.values.sortedByDescending { it.priority }
        }

        override fun getAllFlow(): Flow<List<ComposerTemplateEntity>> = flow

        override suspend fun getEnabledSync(): List<ComposerTemplateEntity> =
            store.values.filter { it.enabled }.sortedByDescending { it.priority }

        override suspend fun getById(id: String): ComposerTemplateEntity? = store[id]

        override suspend fun upsert(entity: ComposerTemplateEntity) {
            store[entity.id] = entity
            publish()
        }

        override suspend fun delete(id: String) {
            store.remove(id)
            publish()
        }
    }

    private lateinit var fakeDao: FakeComposerTemplateDao
    private lateinit var repository: ComposerTemplateRepository

    @Before
    fun setUp() {
        fakeDao = FakeComposerTemplateDao()
        repository = ComposerTemplateRepository(fakeDao)
    }

    private fun fullDefinition() = IslandTemplateDefinition(
        templateType = ComposerTemplateType.T10_COURIER,
        leftGraphic = LeftGraphicSlot(source = GraphicSource.LARGE_ICON, shapeId = "square", paddingPercent = 20),
        text = TextSlotConfig(
            title = FieldBinding(NotificationField.TITLE),
            content = FieldBinding(NotificationField.STATIC, "On the way"),
            subContent = FieldBinding(NotificationField.SUBTEXT),
            titleColor = "#FFFFFF",
            contentColor = "#000000",
            showBadge = true,
            badgeText = FieldBinding(NotificationField.STATIC, "New")
        ),
        progress = ProgressSlotConfig(
            kind = ProgressKind.CIRCULAR,
            valueBinding = FieldBinding(NotificationField.PROGRESS),
            activeColor = "#00FF00",
            finishedColor = "#0000FF"
        ),
        buttons = listOf(
            ComposerActionButton(label = "Track", useNotificationAction = true, notificationActionIndex = 0, bgColor = "#FF0000"),
            ComposerActionButton(label = "Call", useNotificationAction = false)
        ),
        highlightColor = "#123456"
    )

    @Test
    fun saveRoundTripsEveryDefinitionField() = runBlocking {
        val definition = fullDefinition()
        val template = ComposerTemplate(
            id = "tmpl1",
            name = "Courier",
            definition = definition,
            rule = TemplateRule(packageNameRegex = "com\\.courier", titleRegex = null, textRegex = "arriving", priority = 42)
        )

        repository.save(template)

        val loaded = repository.getById("tmpl1")
        assertEquals(template.copy(createdAt = loaded!!.createdAt, updatedAt = loaded.updatedAt), loaded)
        assertEquals(definition, loaded.definition)
        assertTrue(loaded.createdAt > 0)
        assertTrue(loaded.updatedAt > 0)
    }

    @Test
    fun deleteRemovesTheRow() = runBlocking {
        repository.save(ComposerTemplate(id = "tmpl1", name = "A", definition = IslandTemplateDefinition(), rule = TemplateRule()))
        assertTrue(repository.getById("tmpl1") != null)

        repository.delete("tmpl1")
        assertNull(repository.getById("tmpl1"))
    }

    @Test
    fun getEnabledSyncFiltersDisabledTemplates() = runBlocking {
        repository.save(ComposerTemplate(id = "on", name = "On", definition = IslandTemplateDefinition(), rule = TemplateRule(), enabled = true))
        repository.save(ComposerTemplate(id = "off", name = "Off", definition = IslandTemplateDefinition(), rule = TemplateRule(), enabled = false))

        val enabled = repository.getEnabledSync()
        assertEquals(1, enabled.size)
        assertEquals("on", enabled.first().id)
    }
}
