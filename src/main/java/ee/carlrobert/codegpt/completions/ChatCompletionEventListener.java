package ee.carlrobert.codegpt.completions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import ee.carlrobert.codegpt.codecompletions.CompletionProgressNotifier;
import ee.carlrobert.codegpt.events.CodeGPTEvent;
import ee.carlrobert.llm.client.openai.completion.ErrorDetails;
import ee.carlrobert.llm.completion.CompletionEventListener;
import okhttp3.sse.EventSource;

public class ChatCompletionEventListener implements CompletionEventListener<String> {

  private final Project project;
  private final ChatCompletionParameters callParameters;
  private final CompletionResponseEventListener eventListener;
  private final StringBuilder messageBuilder = new StringBuilder();

  public ChatCompletionEventListener(
      Project project,
      ChatCompletionParameters callParameters,
      CompletionResponseEventListener eventListener) {
    this.project = project;
    this.callParameters = callParameters;
    this.eventListener = eventListener;
  }

  @Override
  public void onOpen() {
    eventListener.handleRequestOpen();
  }

  @Override
  public void onEvent(String data) {
    try {
      var event = new ObjectMapper().readValue(data, CodeGPTEvent.class);
      eventListener.handleCodeGPTEvent(event);
    } catch (JsonProcessingException e) {
      // ignore
    }
  }

  @Override
  public void onMessage(String message, EventSource eventSource) {
    messageBuilder.append(message);
    callParameters.getMessage().setResponse(messageBuilder.toString());
    eventListener.handleMessage(message);
  }

  @Override
  public void onComplete(StringBuilder messageBuilder) {
    handleCompleted(messageBuilder);
  }

  @Override
  public void onCancelled(StringBuilder messageBuilder) {
    handleCompleted(messageBuilder);
  }

  @Override
  public void onError(ErrorDetails error, Throwable ex) {
      callParameters.getConversation().addMessage(callParameters.getMessage());
      eventListener.handleError(error, ex);
  }

  private void handleCompleted(StringBuilder messageBuilder) {
    CompletionProgressNotifier.Companion.update(project, false);
    eventListener.handleCompleted(messageBuilder.toString(), callParameters);
  }
}
