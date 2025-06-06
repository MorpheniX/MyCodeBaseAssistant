package ee.carlrobert.codegpt.completions

import com.intellij.openapi.components.service
import ee.carlrobert.codegpt.completions.factory.CodeGPTRequestFactory
import ee.carlrobert.codegpt.completions.factory.CustomOpenAIRequestFactory
import ee.carlrobert.codegpt.completions.factory.OpenAIRequestFactory
import ee.carlrobert.codegpt.psistructure.ClassStructureSerializer
import ee.carlrobert.codegpt.settings.prompts.CoreActionsState
import ee.carlrobert.codegpt.settings.prompts.PromptsSettings
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.llm.completion.CompletionRequest

interface CompletionRequestFactory {
    fun createChatRequest(params: ChatCompletionParameters): CompletionRequest
    fun createEditCodeRequest(params: EditCodeCompletionParameters): CompletionRequest
    fun createCommitMessageRequest(params: CommitMessageCompletionParameters): CompletionRequest
    fun createLookupRequest(params: LookupCompletionParameters): CompletionRequest

    companion object {
        @JvmStatic
        fun getFactory(serviceType: ServiceType): CompletionRequestFactory {
            return when (serviceType) {
                ServiceType.CODEGPT -> CodeGPTRequestFactory(ClassStructureSerializer)
                ServiceType.OPENAI -> OpenAIRequestFactory()
                ServiceType.CUSTOM_OPENAI -> CustomOpenAIRequestFactory()
            }
        }
    }
}

abstract class BaseRequestFactory : CompletionRequestFactory {
    override fun createEditCodeRequest(params: EditCodeCompletionParameters): CompletionRequest {
        val prompt = "Code to modify:\n${params.selectedText}\n\nInstructions: ${params.prompt}"
        return createBasicCompletionRequest(
            service<PromptsSettings>().state.coreActions.editCode.instructions
                ?: CoreActionsState.DEFAULT_EDIT_CODE_PROMPT, prompt, 8192, true
        )
    }

    override fun createCommitMessageRequest(params: CommitMessageCompletionParameters): CompletionRequest {
        return createBasicCompletionRequest(params.systemPrompt, params.gitDiff, 512, true)
    }

    override fun createLookupRequest(params: LookupCompletionParameters): CompletionRequest {
        return createBasicCompletionRequest(
            service<PromptsSettings>().state.coreActions.generateNameLookups.instructions
                ?: CoreActionsState.DEFAULT_GENERATE_NAME_LOOKUPS_PROMPT,
            params.prompt,
            512
        )
    }

    abstract fun createBasicCompletionRequest(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 4096,
        stream: Boolean = false
    ): CompletionRequest

    protected fun getPromptWithFilesContext(callParameters: ChatCompletionParameters): String {
        return callParameters.referencedFiles?.let {
            if (it.isEmpty()) {
                callParameters.message.prompt
            } else {
                CompletionRequestUtil.getPromptWithContext(
                    it,
                    callParameters.message.prompt,
                    callParameters.psiStructure,
                )
            }
        } ?: return callParameters.message.prompt
    }
}
