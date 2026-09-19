package com.d4viddf.hyperbridge.data.translator

import android.content.Context
import android.content.ContextWrapper
import com.d4viddf.hyperbridge.data.db.TranslatorDao
import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import com.d4viddf.hyperbridge.models.translator.ActionDisplayMode
import com.d4viddf.hyperbridge.models.translator.ActionMatchBy
import com.d4viddf.hyperbridge.models.translator.ActionMatcher
import com.d4viddf.hyperbridge.models.translator.ActionSlotConfig
import com.d4viddf.hyperbridge.models.translator.ActionSource
import com.d4viddf.hyperbridge.models.translator.BehaviorOverride
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.EngineMode
import com.d4viddf.hyperbridge.models.translator.MediaConditions
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.models.translator.SmartActionType
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TranslatorConditions
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.models.translator.TypeSpecificConditions
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TranslatorRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeContext: FakeTestContext
    private lateinit var fakeDao: FakeTranslatorDao
    private lateinit var repository: TranslatorRepository
    private lateinit var filesDir: File
    private lateinit var cacheDir: File

    class FakeTestContext(
        private val files: File,
        private val cache: File
    ) : ContextWrapper(null) {
        override fun getFilesDir(): File = files
        override fun getCacheDir(): File = cache
        override fun getPackageName(): String = "com.d4viddf.hyperbridge"
        override fun getApplicationContext(): Context = this
    }

    class FakeTranslatorDao : TranslatorDao {
        val insertedTranslators = mutableListOf<TranslatorEntity>()

        override fun getAllTranslatorsFlow() = emptyFlow<List<TranslatorEntity>>()
        override fun getActiveTranslatorsFlow() = emptyFlow<List<TranslatorEntity>>()
        override suspend fun getActiveTranslators() = insertedTranslators.filter { it.isEnabled }
        override suspend fun getTranslatorById(id: String) = insertedTranslators.find { it.id == id }
        override fun getTranslatorByIdFlow(id: String) = emptyFlow<TranslatorEntity?>()

        override suspend fun insertTranslator(translator: TranslatorEntity) {
            insertedTranslators.removeAll { it.id == translator.id }
            insertedTranslators.add(translator)
        }

        override suspend fun insertAll(translators: List<TranslatorEntity>) {
            translators.forEach { insertTranslator(it) }
        }

        override suspend fun updateTranslator(translator: TranslatorEntity) {
            insertTranslator(translator)
        }

        override suspend fun setTranslatorEnabled(id: String, isEnabled: Boolean, timestamp: Long) {
            val existing = insertedTranslators.find { it.id == id }
            if (existing != null) {
                insertTranslator(existing.copy(isEnabled = isEnabled, updatedAt = timestamp))
            }
        }

        override suspend fun updatePriority(id: String, priority: Int, timestamp: Long) {
            val existing = insertedTranslators.find { it.id == id }
            if (existing != null) {
                insertTranslator(existing.copy(priority = priority, updatedAt = timestamp))
            }
        }

        override suspend fun deleteTranslatorById(id: String) {
            insertedTranslators.removeAll { it.id == id }
        }

        override suspend fun deleteAll() {
            insertedTranslators.clear()
        }
    }

    @Before
    fun setup() {
        filesDir = tempFolder.newFolder("files")
        cacheDir = tempFolder.newFolder("cache")

        fakeContext = FakeTestContext(filesDir, cacheDir)
        fakeDao = FakeTranslatorDao()

        repository = TranslatorRepository(fakeContext, fakeDao)
    }

    @Test
    fun testExportTranslatorToZipAndImportRoundtrip() = runBlocking {
        val originalTranslator = CustomTranslator(
            id = "com.spotify.enhanced_test",
            meta = TranslatorMetadata(
                name = "Spotify Enhanced Test",
                author = "CommunityTester",
                version = 2,
                description = "Enhanced Spotify Island with lyrics and quick skip."
            ),
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("com.spotify.music", "com.spotify.lite"),
            targetNotificationTypes = listOf("MEDIA"),
            priority = 250,
            isEnabled = true,
            conditions = TranslatorConditions(
                titleRegex = ".*",
                textRegex = ".*",
                category = "transport",
                typeSpecificConditions = TypeSpecificConditions(
                    media = MediaConditions(
                        artistRegex = "(?i)Daft Punk",
                        isPlaying = true
                    )
                )
            ),
            engineMode = EngineMode.CUSTOM_ISLAND,
            behaviorOverride = BehaviorOverride(
                engineMode = EngineMode.CUSTOM_ISLAND,
                isFloat = true,
                floatTimeoutSeconds = 6
            ),
            presentation = PresentationConfig(
                progressSlot = ProgressSlotConfig(
                    type = ProgressSlotType.PROGRESS_BAR,
                    showPercentage = true
                ),
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        isVisible = true,
                        source = ActionSource.NOTIFICATION_ACTION,
                        actionMatcher = ActionMatcher(matchBy = ActionMatchBy.INDEX, actionIndex = 0),
                        displayMode = ActionDisplayMode.ICON_ONLY
                    ),
                    ActionSlotConfig(
                        slotPosition = 1,
                        isVisible = true,
                        source = ActionSource.SMART_ACTION,
                        smartActionType = SmartActionType.OTP_COPY,
                        displayMode = ActionDisplayMode.ICON_AND_TEXT
                    )
                )
            )
        )

        // 1. Export to Zip OutputStream
        val zipBaos = ByteArrayOutputStream()
        val exportResult = repository.exportTranslatorToZip(originalTranslator, zipBaos)
        assertTrue("Export should succeed", exportResult.isSuccess)
        val zipBytes = zipBaos.toByteArray()
        assertTrue("Zip bytes should not be empty", zipBytes.isNotEmpty())

        // 2. Import from Zip Stream
        val importStream = ByteArrayInputStream(zipBytes)
        val importResult = repository.importTranslatorFromStream(importStream)
        assertTrue("Import should succeed", importResult.isSuccess)

        val imported = importResult.getOrThrow()
        assertEquals(originalTranslator.id, imported.id)
        assertEquals(originalTranslator.meta.name, imported.meta.name)
        assertEquals(originalTranslator.meta.author, imported.meta.author)
        assertEquals(originalTranslator.targetScope, imported.targetScope)
        assertEquals(originalTranslator.targetPackages, imported.targetPackages)
        assertEquals(originalTranslator.priority, imported.priority)
        assertEquals(originalTranslator.presentation.actionSlots.size, imported.presentation.actionSlots.size)
        assertEquals(SmartActionType.OTP_COPY, imported.presentation.actionSlots[1].smartActionType)

        // 3. Verify entity inserted to DAO
        assertEquals(1, fakeDao.insertedTranslators.size)
        val inserted = fakeDao.insertedTranslators[0]
        assertEquals(originalTranslator.id, inserted.id)
        assertEquals(originalTranslator.meta.name, inserted.name)
    }

    @Test
    fun testZipSlipSecurityRejection() = runBlocking {
        // Construct malicious zip with directory traversal entry
        val maliciousBaos = ByteArrayOutputStream()
        ZipOutputStream(maliciousBaos).use { zos ->
            zos.putNextEntry(ZipEntry("../../evil_file.txt"))
            zos.write("malicious payload".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val stream = ByteArrayInputStream(maliciousBaos.toByteArray())
        val result = repository.importTranslatorFromStream(stream)
        assertTrue("ZipSlip should be rejected and fail", result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue("Exception should indicate security failure", ex is SecurityException || ex?.message?.contains("Invalid zip") == true)
    }

    @Test
    fun testFallbackPlainJsonStreamImport() = runBlocking {
        val translator = CustomTranslator(
            id = "simple.json.rule",
            meta = TranslatorMetadata(name = "Direct JSON Rule"),
            targetScope = TargetScope.GLOBAL,
            priority = 100
        )
        val jsonString = CustomTranslator.toJson(translator)
        val stream = ByteArrayInputStream(jsonString.toByteArray(Charsets.UTF_8))

        val result = repository.importTranslatorFromStream(stream)
        assertTrue("Importing plain JSON should succeed", result.isSuccess)
        assertEquals("simple.json.rule", result.getOrThrow().id)
        assertEquals(1, fakeDao.insertedTranslators.size)
        assertEquals("simple.json.rule", fakeDao.insertedTranslators[0].id)
    }

    @Test
    fun testExportTranslatorToFile() = runBlocking {
        val translator = CustomTranslator(
            id = "file.export.rule",
            meta = TranslatorMetadata(name = "File Export Test"),
            targetScope = TargetScope.GLOBAL
        )

        val result = repository.exportTranslatorToFile(translator)
        assertTrue("File export should succeed", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue("Exported file should exist", file.exists())
        assertEquals("File_Export_Test.htrans", file.name)
        assertTrue("File size should be greater than 0", file.length() > 0)
    }

    @Test
    fun testExportAndImportWithIcons() = runBlocking {
        val translatorId = "icon.translator.test"
        val translator = CustomTranslator(
            id = translatorId,
            meta = TranslatorMetadata(name = "Icon Test Translator"),
            targetScope = TargetScope.GLOBAL
        )

        // 1. Create a dummy icon file in translator storage directory
        val iconStorageDir = File(filesDir, "translators/$translatorId/icons")
        iconStorageDir.mkdirs()
        val customIcon = File(iconStorageDir, "action_icon.png")
        customIcon.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        // 2. Export to zip
        val zipBaos = ByteArrayOutputStream()
        val exportResult = repository.exportTranslatorToZip(translator, zipBaos)
        assertTrue(exportResult.isSuccess)

        // 3. Clear storage to verify re-extraction
        iconStorageDir.deleteRecursively()

        // 4. Import from zip
        val importStream = ByteArrayInputStream(zipBaos.toByteArray())
        val importResult = repository.importTranslatorFromStream(importStream)
        assertTrue(importResult.isSuccess)

        // 5. Verify icon file was extracted back to storage
        val reExtractedIcon = File(filesDir, "translators/$translatorId/icons/action_icon.png")
        assertTrue("Icon file should be re-extracted", reExtractedIcon.exists())
        assertEquals(5, reExtractedIcon.length())
    }

    @Test
    fun testThemeBundledTranslatorsExtraction() = runBlocking {
        val bundledTranslator1 = CustomTranslator(
            id = "bundled.rule.1",
            meta = TranslatorMetadata(name = "Bundled Rule 1"),
            targetScope = TargetScope.GLOBAL
        )
        val bundledTranslator2 = CustomTranslator(
            id = "bundled.rule.2",
            meta = TranslatorMetadata(name = "Bundled Rule 2"),
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("com.whatsapp")
        )

        // Simulate a theme directory with a translators/ folder
        val themeTempDir = tempFolder.newFolder("theme_temp")
        val translatorsDir = File(themeTempDir, "translators")
        translatorsDir.mkdirs()

        // Export bundledTranslator1 as .htrans
        val trans1File = File(translatorsDir, "trans1.htrans")
        trans1File.outputStream().use {
            repository.exportTranslatorToZip(bundledTranslator1, it)
        }

        // Write bundledTranslator2 as plain .json
        val trans2File = File(translatorsDir, "trans2.json")
        trans2File.writeText(CustomTranslator.toJson(bundledTranslator2))

        // Run extraction loop
        translatorsDir.walkTopDown().filter { it.isFile }.forEach { file ->
            if (file.extension.equals("htrans", ignoreCase = true) || file.extension.equals("json", ignoreCase = true)) {
                file.inputStream().use { stream ->
                    val result = repository.importTranslatorFromStream(stream)
                    assertTrue("Bundled translator ${file.name} should import", result.isSuccess)
                }
            }
        }

        assertEquals(2, fakeDao.insertedTranslators.size)
        val ids = fakeDao.insertedTranslators.map { it.id }.toSet()
        assertTrue(ids.contains("bundled.rule.1"))
        assertTrue(ids.contains("bundled.rule.2"))
    }
}
