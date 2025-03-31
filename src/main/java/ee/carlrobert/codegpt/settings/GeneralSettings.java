package ee.carlrobert.codegpt.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import ee.carlrobert.codegpt.CodeGPTKeys;
import ee.carlrobert.codegpt.conversations.Conversation;
import ee.carlrobert.codegpt.settings.service.ProviderChangeNotifier;
import ee.carlrobert.codegpt.settings.service.ServiceType;
import ee.carlrobert.codegpt.settings.service.codegpt.CodeGPTService;
import ee.carlrobert.codegpt.settings.service.codegpt.CodeGPTServiceSettings;
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings;
import ee.carlrobert.codegpt.util.ApplicationUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "CodeGPT_GeneralSettings_270", storages = @Storage("CodeGPT_GeneralSettings_270.xml"))
public class GeneralSettings implements PersistentStateComponent<GeneralSettingsState> {

  private GeneralSettingsState state = new GeneralSettingsState();

  @Override
  @NotNull
  public GeneralSettingsState getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull GeneralSettingsState state) {
    this.state = state;
  }

  public static GeneralSettingsState getCurrentState() {
    return getInstance().getState();
  }

  public static GeneralSettings getInstance() {
    return ApplicationManager.getApplication().getService(GeneralSettings.class);
  }

  public static ServiceType getSelectedService() {
    return getCurrentState().getSelectedService();
  }

  public static boolean isSelected(ServiceType serviceType) {
    return getSelectedService() == serviceType;
  }

  public void sync(Conversation conversation) {
    var project = ApplicationUtil.findCurrentProject();
    var provider = ServiceType.fromClientCode(conversation.getClientCode());
    switch (provider) {
      case OPENAI:
        OpenAISettings.getCurrentState().setModel(conversation.getModel());
        break;
      case CODEGPT:
        ApplicationManager.getApplication().getService(CodeGPTServiceSettings.class).getState()
            .getChatCompletionSettings().setModel(conversation.getModel());

        var existingUserDetails = CodeGPTKeys.CODEGPT_USER_DETAILS.get(project);
        if (project != null && existingUserDetails == null) {
          project.getService(CodeGPTService.class).syncUserDetailsAsync();
        }
        break;
      default:
        break;
    }
    state.setSelectedService(provider);
    if (project != null) {
      project.getMessageBus()
          .syncPublisher(ProviderChangeNotifier.getTOPIC())
          .providerChanged(provider);
    }
  }

  public String getModel() {
    switch (state.getSelectedService()) {
      case CODEGPT:
        return ApplicationManager.getApplication().getService(CodeGPTServiceSettings.class)
            .getState()
            .getCodeCompletionSettings()
            .getModel();
      case OPENAI:
        return OpenAISettings.getCurrentState().getModel();
      default:
        return "Unknown";
    }
  }
}
