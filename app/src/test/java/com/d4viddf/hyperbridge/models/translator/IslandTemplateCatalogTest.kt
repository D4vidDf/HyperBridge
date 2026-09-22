package com.d4viddf.hyperbridge.models.translator

import com.d4viddf.hyperbridge.models.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandTemplateCatalogTest {

    @Test
    fun galleryOffersTheTenOfficialTemplates() {
        assertEquals(10, IslandTemplateCatalog.gallery.size)
    }

    @Test
    fun everyTemplateIdIsUnique() {
        val ids = IslandTemplateCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun theIdsDynamicTranslatorAlreadyRecognisesStillResolve() {
        // DynamicTranslator keys its call and media rendering off these two ids.
        assertNotNull(IslandTemplateCatalog.find("tpl_call_kit"))
        assertNotNull(IslandTemplateCatalog.find("tpl_media_compact"))
    }

    @Test
    fun compactMediaIsResolvableButIsNotOneOfTheTen() {
        assertTrue(IslandTemplateCatalog.gallery.none { it.id == "tpl_media_compact" })
    }

    @Test
    fun everyTemplateBindsItsTextSlots() {
        IslandTemplateCatalog.all.forEach { template ->
            assertTrue(template.id, template.presentation.textSlot.titleTemplate.isNotBlank())
            assertTrue(template.id, template.presentation.textSlot.subtitleTemplate.isNotBlank())
        }
    }

    @Test
    fun unknownTemplateIdLeavesThePresentationAlone() {
        val config = PresentationConfig(mode = PresentationMode.TEMPLATE, templateId = "tpl_not_a_template")
        assertEquals(config, IslandTemplateCatalog.effectivePresentation(config))
    }

    @Test
    fun standardPresentationIsNeverTouched() {
        val config = PresentationConfig(mode = PresentationMode.STANDARD, templateId = "tpl_call_kit")
        assertEquals(config, IslandTemplateCatalog.effectivePresentation(config))
    }

    @Test
    fun aBareTemplateIdIsFilledFromThePreset() {
        val template = IslandTemplateCatalog.find("tpl_file_transfer")!!
        val resolved = IslandTemplateCatalog.effectivePresentation(
            PresentationConfig(mode = PresentationMode.TEMPLATE, templateId = template.id)
        )

        assertEquals(ProgressSlotType.PROGRESS_BAR, resolved.progressSlot.type)
        assertEquals(template.presentation.textSlot, resolved.textSlot)
        assertEquals(template.presentation.pill, resolved.pill)
    }

    @Test
    fun anEditedTemplateIsRenderedExactlyAsSaved() {
        val edited = TextSlotConfig(titleTemplate = "{notif.sender}", subtitleTemplate = "{notif.conversation}")
        val config = PresentationConfig(
            mode = PresentationMode.TEMPLATE,
            templateId = "tpl_file_transfer",
            textSlot = edited
        )

        assertEquals(config, IslandTemplateCatalog.effectivePresentation(config))
    }

    @Test
    fun editingOneTextFieldKeepsThePresetsOtherFields() {
        // Pick a template in the editor, then change only the subtitle.
        val picked = IslandTemplateCatalog.applyTemplate(PresentationConfig(), "tpl_payment_wallet")
        val edited = picked.copy(textSlot = picked.textSlot.copy(subtitleTemplate = "{notif.subtext}"))

        val resolved = IslandTemplateCatalog.effectivePresentation(edited)

        val preset = IslandTemplateCatalog.find("tpl_payment_wallet")!!.presentation
        assertNotNull(preset.textSlot.highlightTextTemplate)
        assertEquals(preset.textSlot.highlightTextTemplate, resolved.textSlot.highlightTextTemplate)
        assertEquals("{notif.subtext}", resolved.textSlot.subtitleTemplate)
    }

    @Test
    fun deletingThePresetsActionsSticks() {
        val picked = IslandTemplateCatalog.applyTemplate(PresentationConfig(), "tpl_boarding_pass")
        assertTrue(picked.actionSlots.isNotEmpty())

        val resolved = IslandTemplateCatalog.effectivePresentation(picked.copy(actionSlots = emptyList()))

        assertTrue(resolved.actionSlots.isEmpty())
    }

    @Test
    fun applyingATemplateStartsFromItsPreset() {
        val template = IslandTemplateCatalog.find("tpl_courier_tracking")!!
        val applied = IslandTemplateCatalog.applyTemplate(
            PresentationConfig(textSlot = TextSlotConfig(titleTemplate = "old")),
            template.id
        )

        assertEquals(PresentationMode.TEMPLATE, applied.mode)
        assertEquals(template.id, applied.templateId)
        assertEquals(template.presentation.textSlot, applied.textSlot)
        assertEquals(template.presentation.actionSlots, applied.actionSlots)
    }

    @Test
    fun resolvingLeavesANonTemplateTranslatorUntouched() {
        val translator = CustomTranslator(id = "t", meta = TranslatorMetadata(name = "t"))
        assertSame(translator, translator.withResolvedTemplate())
    }

    @Test
    fun aNewDesignTargetsOnlyTheChosenNotificationType() {
        val template = IslandTemplateCatalog.find("tpl_courier_tracking")!!
        val design = IslandTemplateCatalog.newDesign(template, NotificationType.PROGRESS, "Courier")

        assertEquals(TargetScope.NOTIFICATION_TYPE, design.targetScope)
        assertEquals(listOf("PROGRESS"), design.targetNotificationTypes)
        assertTrue(design.targetPackages.isEmpty())
        assertEquals(PresentationMode.TEMPLATE, design.presentation.mode)
        assertEquals(template.id, design.presentation.templateId)
        assertEquals(template.iconName, design.meta.iconName)
        // No conditions are attached when a design is created; the editor adds those later.
        assertEquals(TranslatorConditions(), design.conditions)
    }

    @Test
    fun aNewDesignSurvivesTheHtransRoundTrip() {
        val template = IslandTemplateCatalog.find("tpl_boarding_pass")!!
        val design = IslandTemplateCatalog.newDesign(template, NotificationType.STANDARD, "Boarding")

        val restored = CustomTranslator.fromJson(CustomTranslator.toJson(design)).getOrNull()
        assertNotNull(restored)
        assertEquals(design.presentation.templateId, restored!!.presentation.templateId)
        assertEquals(PresentationMode.TEMPLATE, restored.presentation.mode)
        assertNull(restored.presentation.rawParamV2)
    }
}
