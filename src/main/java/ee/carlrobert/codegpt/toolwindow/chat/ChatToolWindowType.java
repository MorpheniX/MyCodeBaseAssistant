package ee.carlrobert.codegpt.toolwindow.chat;

public enum ChatToolWindowType {
  CODEGPT_CHAT("MCBA Chat"),
  CODEGPT_CHAT_WITHOUT_PERSONA("MCBA Chat without Persona"),
  CODEGPT_CHAT_WITH_PERSONA("MCBA Chat with Persona");

  private final String name;

  ChatToolWindowType(String name) {
    this.name = name;
  }
}
