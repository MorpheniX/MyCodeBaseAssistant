package ee.carlrobert.codegpt.settings.state

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ee.carlrobert.codegpt.conversations.Conversation
import ee.carlrobert.codegpt.settings.GeneralSettings
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings
import org.assertj.core.api.Assertions.assertThat

class GeneralSettingsTest : BasePlatformTestCase() {

  fun testOpenAISettingsSync() {
    val openAISettings = OpenAISettings.getCurrentState()
    openAISettings.model = "gpt-3.5-turbo"
    val conversation = Conversation()
    conversation.model = "gpt-4"
    conversation.clientCode = "chat.completion"
    val settings = GeneralSettings.getInstance()

    settings.sync(conversation)

    assertThat(settings.state.selectedService).isEqualTo(ServiceType.OPENAI)
    assertThat(openAISettings.model).isEqualTo("gpt-4")
  }

  fun testCustomOpenAISettingsSync() {
    val conversation = Conversation()
    conversation.clientCode = "custom.openai.chat.completion"
    val settings = GeneralSettings.getInstance()
    settings.state.selectedService = ServiceType.OPENAI

    settings.sync(conversation)

    assertThat(settings.state.selectedService).isEqualTo(ServiceType.CUSTOM_OPENAI)
  }
}
