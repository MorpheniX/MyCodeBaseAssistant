package ee.carlrobert.codegpt.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import ee.carlrobert.codegpt.codecompletions.CodeCompletionService
import ee.carlrobert.codegpt.settings.GeneralSettings
import ee.carlrobert.codegpt.settings.service.ServiceType.*
import ee.carlrobert.codegpt.settings.service.codegpt.CodeGPTServiceSettings
import ee.carlrobert.codegpt.settings.service.custom.CustomServicesSettings
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings

abstract class CodeCompletionFeatureToggleActions(
    private val enableFeatureAction: Boolean
) : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) = when (GeneralSettings.getSelectedService()) {
        CODEGPT -> service<CodeGPTServiceSettings>().state.codeCompletionSettings::codeCompletionsEnabled::set

        OPENAI -> OpenAISettings.getCurrentState()::setCodeCompletionsEnabled

        CUSTOM_OPENAI -> service<CustomServicesSettings>().state.active.codeCompletionSettings::codeCompletionsEnabled::set

        null -> { _: Boolean -> Unit } // no-op for these services
    }(enableFeatureAction)

    override fun update(e: AnActionEvent) {
        val selectedService = GeneralSettings.getSelectedService()
        val codeCompletionEnabled =
            e.project?.service<CodeCompletionService>()?.isCodeCompletionsEnabled(selectedService)
                ?: false
        e.presentation.isVisible = codeCompletionEnabled != enableFeatureAction
        e.presentation.isEnabled = when (selectedService) {
            CODEGPT,
            OPENAI,
            CUSTOM_OPENAI -> true

            null -> false
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

class EnableCompletionsAction : CodeCompletionFeatureToggleActions(true)

class DisableCompletionsAction : CodeCompletionFeatureToggleActions(false)