package ee.carlrobert.codegpt.settings.state

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ee.carlrobert.codegpt.conversations.Conversation
import ee.carlrobert.codegpt.settings.GeneralSettings
import ee.carlrobert.codegpt.settings.service.ServiceType
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings
import org.assertj.core.api.Assertions.assertThat

class GeneralSettingsTest : BasePlatformTestCase() {

  fun testCustomOpenAISettingsSync() {
    val conversation = Conversation()
    conversation.clientCode = "custom.openai.chat.completion"
    val settings = GeneralSettings.getInstance()
    settings.state.selectedService = ServiceType.OPENAI

    settings.sync(conversation)

    assertThat(settings.state.selectedService).isEqualTo(ServiceType.CUSTOM_OPENAI)
  }
}
