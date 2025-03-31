package testsupport.mixin

import com.intellij.openapi.components.service
import com.intellij.testFramework.PlatformTestUtil
import ee.carlrobert.codegpt.credentials.CredentialsStore.CredentialKey.*
import ee.carlrobert.codegpt.credentials.CredentialsStore.setCredential
import ee.carlrobert.codegpt.settings.GeneralSettings
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.codegpt.settings.service.codegpt.CodeGPTServiceSettings
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings
import java.util.function.BooleanSupplier

interface ShortcutsTestMixin {

  fun useCodeGPTService() {
    service<GeneralSettings>().state.selectedService = ServiceType.CODEGPT
    setCredential(CodeGptApiKey, "TEST_API_KEY")
    service<CodeGPTServiceSettings>().state.run {
      chatCompletionSettings.model = "TEST_MODEL"
      codeCompletionSettings.model = "TEST_CODE_MODEL"
      codeCompletionSettings.codeCompletionsEnabled = true
    }
  }

  fun useOpenAIService(chatModel: String? = "gpt-4") {
    service<GeneralSettings>().state.selectedService = ServiceType.OPENAI
    setCredential(OpenaiApiKey, "TEST_API_KEY")
    service<OpenAISettings>().state.run {
      model = chatModel
      isCodeCompletionsEnabled = true
    }
  }

  fun waitExpecting(condition: BooleanSupplier?) {
    PlatformTestUtil.waitWithEventsDispatching(
      "Waiting for message response timed out or did not meet expected conditions",
      condition!!,
      5
    )
  }
}
