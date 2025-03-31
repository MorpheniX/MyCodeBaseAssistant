package ee.carlrobert.codegpt.toolwindow.chat.ui;

import static javax.swing.event.HyperlinkEvent.EventType.ACTIVATED;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.roots.ui.componentsList.layout.VerticalStackLayout;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import ee.carlrobert.codegpt.credentials.CredentialsStore;
import ee.carlrobert.codegpt.credentials.CredentialsStore.CredentialKey;
import ee.carlrobert.codegpt.settings.GeneralSettings;
import ee.carlrobert.codegpt.settings.service.ServiceType;
import ee.carlrobert.codegpt.settings.service.codegpt.CodeGPTServiceConfigurable;
import ee.carlrobert.codegpt.toolwindow.ui.ResponseMessagePanel;
import ee.carlrobert.codegpt.ui.UIUtil;
import ee.carlrobert.codegpt.util.ApplicationUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class ChatToolWindowScrollablePanel extends ScrollablePanel {

  private final Map<UUID, JPanel> visibleMessagePanels = new HashMap<>();

  public ChatToolWindowScrollablePanel() {
    super(new VerticalStackLayout());
  }

  public void displayLandingView(JComponent landingView) {
    clearAll();
    add(landingView);
  }

  public ResponseMessagePanel getResponseMessagePanel(UUID messageId) {
    return (ResponseMessagePanel) Arrays.stream(visibleMessagePanels.get(messageId).getComponents())
        .filter(ResponseMessagePanel.class::isInstance)
        .findFirst().orElseThrow();
  }

  public JPanel addMessage(UUID messageId) {
    var messageWrapper = new JPanel();
    messageWrapper.setLayout(new BoxLayout(messageWrapper, BoxLayout.PAGE_AXIS));
    add(messageWrapper);
    visibleMessagePanels.put(messageId, messageWrapper);
    return messageWrapper;
  }

  public void removeMessage(UUID messageId) {
    remove(visibleMessagePanels.get(messageId));
    update();
    visibleMessagePanels.remove(messageId);
  }

  public void clearAll() {
    visibleMessagePanels.clear();
    removeAll();
    update();
  }

  public void update() {
    repaint();
    revalidate();
  }
}
