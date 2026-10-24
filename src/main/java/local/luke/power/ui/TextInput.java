package local.luke.power.ui;

import local.luke.power.chat.ChatBuffer;
import java.util.function.ToIntFunction;

/** The same editor used by chat, with each input retaining its own selection. */
public final class TextInput {
  private final ChatBuffer editor;
  private final TextPointer pointer;
  public boolean focused;
  public TextInput(String text, int limit) {
    editor = new ChatBuffer(limit); pointer = new TextPointer(editor); setText(text);
  }
  public String text() { return editor.text(); }
  public void setText(String text) { editor.set(java.util.Objects.requireNonNull(text)); }
  public void selectAll() { editor.all(); }
  public int cursor() { return editor.cursor(); }
  public int start() { return editor.start(); }
  public int end() { return editor.end(); }
  public ChatBuffer.View layout(Object owner, int left, int width, ToIntFunction<String> measure) {
    return pointer.layout(owner, left, width, measure);
  }
  public void click(int x, int button) {
    if (!focused) return;
    if (button == 0) pointer.click(x, () -> focused);
    else TextEditing.clearOnRightClick(editor, button);
  }
  public void key(char c, int key) {
    if (focused) TextEditing.key(editor, c, key, value -> value >= 32 && value != 127);
  }
}
