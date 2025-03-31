package ee.carlrobert.codegpt.completions

import com.intellij.openapi.components.service
import ee.carlrobert.codegpt.completions.llama.PromptTemplate.LLAMA
import ee.carlrobert.codegpt.conversations.ConversationService
import ee.carlrobert.codegpt.conversations.message.Message
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings
import ee.carlrobert.codegpt.settings.persona.PersonaSettings
import ee.carlrobert.codegpt.settings.prompts.PromptsSettings
import ee.carlrobert.llm.client.http.RequestEntity
import ee.carlrobert.llm.client.http.exchange.NdJsonStreamHttpExchange
import ee.carlrobert.llm.client.http.exchange.StreamHttpExchange
import ee.carlrobert.llm.client.util.JSONUtil.*
import org.apache.http.HttpHeaders
import org.assertj.core.api.Assertions.assertThat
import testsupport.IntegrationTest

class DefaultToolwindowChatCompletionRequestHandlerTest : IntegrationTest() {

    fun testOpenAIChatCompletionCall() {
        useOpenAIService()
        service<PromptsSettings>().state.personas.selectedPersona.instructions = "TEST_SYSTEM_PROMPT"
        val message = Message("TEST_PROMPT")
        val conversation = ConversationService.getInstance().startConversation()
        expectOpenAI(StreamHttpExchange { request: RequestEntity ->
            assertThat(request.uri.path).isEqualTo("/v1/chat/completions")
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.headers[HttpHeaders.AUTHORIZATION]!![0]).isEqualTo("Bearer TEST_API_KEY")
            assertThat(request.body)
                .extracting(
                    "model",
                    "messages"
                )
                .containsExactly(
                    "gpt-4",
                    listOf(
                        mapOf("role" to "system", "content" to "TEST_SYSTEM_PROMPT"),
                        mapOf("role" to "user", "content" to "TEST_PROMPT")
                    )
                )
            listOf(
                jsonMapResponse(
                    "choices",
                    jsonArray(jsonMap("delta", jsonMap("role", "assistant")))
                ),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "Hel")))),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "lo")))),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "!"))))
            )
        })
        val requestHandler =
            ToolwindowChatCompletionRequestHandler(project, getRequestEventListener(message))

        requestHandler.call(ChatCompletionParameters.builder(conversation, message).build())

        waitExpecting { "Hello!" == message.response }
    }

    fun testCodeGPTServiceChatCompletionCall() {
        useCodeGPTService()
        service<PromptsSettings>().state.personas.selectedPersona.instructions = "TEST_SYSTEM_PROMPT"
        val message = Message("TEST_PROMPT")
        val conversation = ConversationService.getInstance().startConversation()
        expectCodeGPT(StreamHttpExchange { request: RequestEntity ->
            assertThat(request.uri.path).isEqualTo("/v1/chat/completions")
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.headers[HttpHeaders.AUTHORIZATION]!![0]).isEqualTo("Bearer TEST_API_KEY")
            assertThat(request.body)
                .extracting(
                    "model",
                    "messages"
                )
                .containsExactly(
                    "TEST_MODEL",
                    listOf(
                        mapOf("role" to "system", "content" to "TEST_SYSTEM_PROMPT"),
                        mapOf("role" to "user", "content" to "TEST_PROMPT")
                    )
                )
            listOf(
                jsonMapResponse(
                    "choices",
                    jsonArray(jsonMap("delta", jsonMap("role", "assistant")))
                ),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "Hel")))),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "lo")))),
                jsonMapResponse("choices", jsonArray(jsonMap("delta", jsonMap("content", "!"))))
            )
        })
        val requestHandler =
            ToolwindowChatCompletionRequestHandler(project, getRequestEventListener(message))

        requestHandler.call(ChatCompletionParameters.builder(conversation, message).build())

        waitExpecting { "Hello!" == message.response }
    }

    private fun getRequestEventListener(message: Message): CompletionResponseEventListener {
        return object : CompletionResponseEventListener {
            override fun handleCompleted(
                fullMessage: String,
                callParameters: ChatCompletionParameters
            ) {
                message.response = fullMessage
            }
        }
    }
}
