package local.luke.power.ui;

import java.util.function.*;
import local.luke.power.chat.ChatBuffer;

/** Rendering and pointer selection use the same visible character range. */
public final class TextPointer {
  private final ChatBuffer editor;
  private int left, width;
  private Object owner;
  private ToIntFunction<String> measure;
  private ChatBuffer.View view = new ChatBuffer.View(0, 0);
  public TextPointer(ChatBuffer editor) { this.editor = editor; }
  public ChatBuffer.View layout(Object owner, int left, int width, ToIntFunction<String> measure) {
    this.owner = owner; this.left = left; this.width = Math.max(2, width); this.measure = measure;
    return view = editor.view(this.width, measure);
  }
  public void click(int x, BooleanSupplier focused) {
    click(x, TextEditing.shift(), focused);
  }
  public void click(int x, boolean shift, BooleanSupplier focused) {
    if (measure == null) return;
    editor.position(view.hit(editor.text(), x - left, measure), shift);
    TextDrag.begin(owner, focused, this::drag);
  }
  private void drag(int x) {
    if (x < left) editor.position(Math.max(0, view.start() - 1), true);
    else if (x >= left + width) editor.position(Math.min(editor.text().length(), view.end() + 1), true);
    else editor.position(view.hit(editor.text(), x - left, measure), true);
    view = editor.view(width, measure);
  }
}
