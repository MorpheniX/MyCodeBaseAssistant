package ee.carlrobert.codegpt.codecompletions

import com.intellij.codeInsight.inline.completion.*
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import ee.carlrobert.codegpt.CodeGPTKeys.REMAINING_EDITOR_COMPLETION
import ee.carlrobert.codegpt.predictions.PredictionService
import ee.carlrobert.codegpt.settings.GeneralSettings
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.codegpt.settings.service.codegpt.CodeGPTServiceSettings
import ee.carlrobert.codegpt.settings.service.custom.CustomServicesSettings
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings
import ee.carlrobert.codegpt.ui.OverlayUtil
import ee.carlrobert.codegpt.util.StringUtil.extractUntilNewline
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import okhttp3.sse.EventSource
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DebouncedCodeCompletionProvider : DebouncedInlineCompletionProvider() {

    companion object {
        private val logger = thisLogger()
    }

    private val currentCallRef = AtomicReference<EventSource?>(null)

    override val id: InlineCompletionProviderID
        get() = InlineCompletionProviderID("CodeGPTInlineCompletionProvider")

    override val suggestionUpdateManager: CodeCompletionSuggestionUpdateAdapter
        get() = CodeCompletionSuggestionUpdateAdapter()

    override val insertHandler: InlineCompletionInsertHandler
        get() = CodeCompletionInsertHandler()

    override val providerPresentation: InlineCompletionProviderPresentation
        get() = CodeCompletionProviderPresentation()

    override suspend fun getSuggestionDebounced(request: InlineCompletionRequest): InlineCompletionSuggestion {
        val codegptSettings = service<CodeGPTServiceSettings>().state
        if (GeneralSettings.getSelectedService() == ServiceType.CODEGPT && codegptSettings.nextEditsEnabled) {
            if (codegptSettings.codeCompletionSettings.codeCompletionsEnabled) {
                codegptSettings.codeCompletionSettings.codeCompletionsEnabled = false
                OverlayUtil.showNotification(
                    "Code completions and multi-line edits cannot be active simultaneously.",
                    NotificationType.WARNING
                )
            }

            predictNextEdit(request)
            return InlineCompletionSingleSuggestion.build(elements = emptyFlow())
        }

        return if (service<ConfigurationSettings>().state.codeCompletionSettings.multiLineEnabled) {
            getMultiLineSuggestionDebounced(request)
        } else {
            getSingleLineSuggestionDebounced(request)
        }
    }

    private fun predictNextEdit(request: InlineCompletionRequest) {
        val project = request.editor.project ?: return
        try {
            CompletionProgressNotifier.update(project, true)
            project.service<PredictionService>().displayInlineDiff(request.editor)
        } catch (ex: Exception) {
            logger.error("Error communicating with server: ${ex.message}")
        }
    }

    private fun getSingleLineSuggestionDebounced(request: InlineCompletionRequest): InlineCompletionSuggestion {
        val editor = request.editor
        val remainingCompletion = REMAINING_EDITOR_COMPLETION.get(editor) ?: ""
        if (request.event is InlineCompletionEvent.DirectCall && remainingCompletion.isNotEmpty()
        ) {
            return sendNextSuggestion(remainingCompletion.extractUntilNewline(), request)
        }

        return getSuggestionDebounced(
            request,
            CompletionType.SINGLE_LINE
        ) { project, infillRequest ->
            project.service<CodeCompletionService>()
                .getCodeCompletionAsync(
                    infillRequest,
                    CodeCompletionSingleLineEventListener(request.editor, infillRequest) {
                        trySend(it)
                    }
                )
        }
    }

    private fun getMultiLineSuggestionDebounced(request: InlineCompletionRequest): InlineCompletionSuggestion {
        return getSuggestionDebounced(
            request,
            CompletionType.MULTI_LINE
        ) { project, infillRequest ->
            project.service<CodeCompletionService>()
                .getCodeCompletionAsync(
                    infillRequest,
                    CodeCompletionMultiLineEventListener(request) {
                        if (LookupManager.getActiveLookup(request.editor) == null) {
                            trySend(InlineCompletionGrayTextElement(it))
                        }
                    }
                )
        }
    }

    private fun getSuggestionDebounced(
        request: InlineCompletionRequest,
        completionType: CompletionType,
        fetchCompletion: ProducerScope<InlineCompletionElement>.(Project, InfillRequest) -> EventSource
    ): InlineCompletionSuggestion {
        val project = request.editor.project
        if (project == null) {
            logger.error("Could not find project")
            return InlineCompletionSingleSuggestion.build(elements = emptyFlow())
        }

        if (LookupManager.getActiveLookup(request.editor) != null) {
            return InlineCompletionSingleSuggestion.build(elements = emptyFlow())
        }

        request.editor.project?.let {
            CompletionProgressNotifier.update(it, true)
        }

        return InlineCompletionSingleSuggestion.build(elements = channelFlow {
            val infillRequest = InfillRequestUtil.buildInfillRequest(request, completionType)
            currentCallRef.set(fetchCompletion(project, infillRequest))
            awaitClose { currentCallRef.getAndSet(null)?.cancel() }
        })
    }

    override suspend fun getDebounceDelay(request: InlineCompletionRequest): Duration {
        return 400.toDuration(DurationUnit.MILLISECONDS)
    }

    override fun isEnabled(event: InlineCompletionEvent): Boolean {
        if (LookupManager.getActiveLookup(event.toRequest()?.editor) != null) {
            return false
        }

        val selectedService = GeneralSettings.getSelectedService()
        val codeCompletionsEnabled = when (selectedService) {
            ServiceType.CODEGPT -> service<CodeGPTServiceSettings>().state.codeCompletionSettings.codeCompletionsEnabled
            ServiceType.OPENAI -> OpenAISettings.getCurrentState().isCodeCompletionsEnabled
            ServiceType.CUSTOM_OPENAI -> service<CustomServicesSettings>().state.active.codeCompletionSettings.codeCompletionsEnabled
            null -> false
        }
        val hasActiveCompletion =
            REMAINING_EDITOR_COMPLETION.get(event.toRequest()?.editor)?.isNotEmpty() ?: false

        if (!codeCompletionsEnabled) {
            return event is InlineCompletionEvent.DocumentChange
                    && selectedService == ServiceType.CODEGPT
                    && service<CodeGPTServiceSettings>().state.nextEditsEnabled
                    && !hasActiveCompletion
        }

        return event is InlineCompletionEvent.DocumentChange || hasActiveCompletion
    }

    private fun sendNextSuggestion(
        nextCompletion: String,
        request: InlineCompletionRequest
    ): InlineCompletionSingleSuggestion {
        return InlineCompletionSingleSuggestion.build(elements = channelFlow {
            launch {
                trySend(
                    CodeCompletionTextElement(
                        nextCompletion,
                        request.startOffset,
                        TextRange.from(request.startOffset, nextCompletion.length),
                    )
                )
            }
        })
    }
}
