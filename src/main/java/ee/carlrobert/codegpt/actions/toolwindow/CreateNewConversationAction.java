package ee.carlrobert.codegpt.actions.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import ee.carlrobert.codegpt.actions.ActionType;
import ee.carlrobert.codegpt.actions.editor.EditorActionsUtil;
import org.jetbrains.annotations.NotNull;

public class CreateNewConversationAction extends AnAction {

  private final Runnable onCreate;

  public CreateNewConversationAction(Runnable onCreate) {
    super("Create New Chat", "Create new chat", AllIcons.General.Add);
    this.onCreate = onCreate;
    EditorActionsUtil.registerAction(this);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
      var project = event.getProject();
      if (project != null) {
        onCreate.run();
      }
  }
}
