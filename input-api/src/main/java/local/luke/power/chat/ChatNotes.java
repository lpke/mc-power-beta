package local.luke.power.chat;

import com.google.gson.*;
import java.util.*;
import java.util.function.*;
import local.luke.power.storage.PowerConfig;

/** Only locally submitted text is persisted. The normal transcript never reaches storage. */
public final class ChatNotes {
  public interface History { void power$removeSavedMessages(); }
  private static final List<String> messages = new ArrayList<>();
  private static boolean loaded, readOnly;
  private static IntSupplier ownLimit = () -> 100, otherLimit = () -> 100;
  private static Consumer<String> sent = message -> {};
  private static String error = "";
  private ChatNotes() {}
  public static void limits(IntSupplier other, IntSupplier own) { otherLimit = other; ownLimit = own; }
  public static int ownLimit() { return Math.max(1, Math.min(5000, ownLimit.getAsInt())); }
  public static int otherLimit() { return Math.max(1, Math.min(5000, otherLimit.getAsInt())); }
  public static void onSent(Consumer<String> handler) { sent = handler; }
  private static void load() {
    if (loaded) return;
    loaded = true;
    try {
      JsonObject data = PowerConfig.section("chatNotes");
      if (data.size() == 0) return;
      if (!data.has("version") || !data.getAsJsonPrimitive("version").isNumber()
          || data.get("version").getAsBigDecimal().intValueExact() != 1
          || !data.has("messages") || !data.get("messages").isJsonArray()) throw new IllegalArgumentException();
      var parsed = new ArrayList<String>();
      if (data.getAsJsonArray("messages").size() > 5000) throw new IllegalArgumentException();
      for (JsonElement value : data.getAsJsonArray("messages")) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().length() > 100
            || value.getAsString().chars().anyMatch(c -> c < 32 || c == 127)) throw new IllegalArgumentException();
        parsed.add(value.getAsString());
      }
      messages.addAll(parsed);
    } catch (RuntimeException failure) {
      readOnly = true; error = "Saved player messages could not be read. Original notes are preserved; restore a settings backup or use a compatible release.";
    }
  }
  public static List<String> history() { load(); return List.copyOf(messages); }
  public static String error() { return error; }
  public static void record(String text) {
    load();
    if (text == null || text.isBlank() || text.length() > 100 || text.chars().anyMatch(c -> c < 32 || c == 127)) return;
    if (messages.isEmpty() || !messages.get(messages.size() - 1).equals(text)) messages.add(text);
    trim(); save(); sent.accept(text);
  }
  public static void maintainLimit() {
    if (!loaded) return;
    if (trim()) save();
  }
  private static boolean trim() {
    int extra = messages.size() - ownLimit();
    if (extra <= 0) return false;
    messages.subList(0, extra).clear(); return true;
  }
  private static void save() {
    if (readOnly) return;
    JsonObject data = new JsonObject(); data.addProperty("version", 1);
    JsonArray values = new JsonArray(); messages.forEach(values::add); data.add("messages", values);
    PowerConfig.saveDeferred("chatNotes", data);
  }
}
