package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import local.luke.power.config.Setting;
import org.junit.jupiter.api.Test;

class DraftCommitControlsTest {
  private Setting setting(Setting.Kind kind, String value) {
    return new Setting("test.value", "test", "Test", "", "Value", "", kind,
        JsonParser.parseString(value), JsonParser.parseString(value), 0, 100, 1, List.of(), false);
  }

  private static final class Editor extends DraftEditorScreen {
    Editor(Setting source) { super(null, new SettingsDraft(List.of(source), value -> true)); }
    @Override protected String title() { return "Test"; }
    @Override protected List<Setting> controls() { return draft.settings(); }
    @Override protected void renderToolbar(int x, int y) {}
    @Override protected void renderPreview(int x, int y, float delta) {}
  }

  private Object field(Object object, String name) throws ReflectiveOperationException {
    Field field = object.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(object);
  }

  private Object call(Object object, String name) throws ReflectiveOperationException {
    Method method = object.getClass().getDeclaredMethod(name);
    method.setAccessible(true);
    return method.invoke(object);
  }

  @Test void sharedDraftDoneDisablesAgainAfterUndoOrRestoringItsOriginalValue() {
    var source = setting(Setting.Kind.INTEGER, "4");
    var editor = new Editor(source);
    assertFalse(editor.doneEnabled());
    assertDoesNotThrow(editor::save);
    editor.draft.checkpoint();
    editor.draft.get(source.id).parse("5");
    assertTrue(editor.doneEnabled());
    assertEquals(4, source.value.getAsInt());
    editor.undo();
    assertFalse(editor.doneEnabled());
    editor.draft.get(source.id).parse("6");
    assertTrue(editor.doneEnabled());
    editor.draft.get(source.id).parse("4");
    assertFalse(editor.doneEnabled());
  }

  @Test void unchangedValueSaveDoesNotValidateOrInvokeItsAcceptanceCallback() throws Exception {
    var source = setting(Setting.Kind.INTEGER, "4");
    var validated = new AtomicInteger();
    var accepted = new AtomicInteger();
    source.validator(value -> validated.incrementAndGet());
    var editor = new ValueScreen(source, null, accepted::incrementAndGet);
    assertEquals(false, call(editor, "doneEnabled"));
    call(editor, "save");
    assertEquals(0, accepted.get());
    assertEquals(0, validated.get());
    assertEquals(4, source.value.getAsInt());
    var input = (TextInput)field(editor, "input");
    input.setText("5");
    assertEquals(true, call(editor, "doneEnabled"));
    assertEquals(4, source.value.getAsInt());
    input.setText("4");
    assertEquals(false, call(editor, "doneEnabled"));
  }

  @Test void invalidValueEntryRetainsTheOriginalSettingAndCallback() throws Exception {
    var source = setting(Setting.Kind.INTEGER, "4");
    var accepted = new AtomicInteger();
    var editor = new ValueScreen(source, null, accepted::incrementAndGet);
    ((TextInput)field(editor, "input")).setText("-1");
    call(editor, "save");
    assertEquals(4, source.value.getAsInt());
    assertEquals(0, accepted.get());
    assertFalse(((String)field(editor, "error")).isBlank());
  }

  @Test void orderedDistanceDoneRequiresAChangedValidDraft() throws Exception {
    var source = setting(Setting.Kind.LIST, "[2,4]");
    var editor = new IntegerArrayScreen(null, source, new IntegerArrayDraft.Rules(1, 10, 4, true), "Distance", "blocks");
    var draft = (IntegerArrayDraft)field(editor, "draft");
    assertEquals(false, call(editor, "doneEnabled"));
    call(editor, "save");
    draft.set(0, 4);
    assertEquals(false, call(editor, "doneEnabled"));
    draft.set(0, 3);
    assertEquals(true, call(editor, "doneEnabled"));
    assertEquals("[2,4]", source.value.toString());
    draft.reset(List.of(2, 4));
    assertEquals(false, call(editor, "doneEnabled"));
    draft.move(0, 1);
    assertEquals(true, call(editor, "doneEnabled"));
  }
}
