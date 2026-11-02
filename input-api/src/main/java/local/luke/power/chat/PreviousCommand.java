package local.luke.power.chat;

import java.util.function.Consumer;

/** One submitted command per world visit. Replaying it never appends chat history. */
public final class PreviousCommand {
  public enum Result { EXECUTED, EMPTY, BUSY }
  private static Object world;
  private static String command;
  private static boolean executing;
  private static int consumedKey;
  private PreviousCommand() {}

  public static void clear() { world = null; command = null; consumedKey = 0; }
  /** Only the native Chat binding defers to a repeat-command press. */
  public static void consumePress(int key) { consumedKey = key; }
  public static boolean ownsPress(int key) { return consumedKey != 0 && consumedKey == key; }
  public static boolean hasPress() { return consumedKey != 0; }
  public static void releasePress(int key) { if (consumedKey == key) consumedKey = 0; }

  private static void context(Object currentWorld) {
    if (world != currentWorld) { world = currentWorld; command = null; }
  }

  public static void record(Object currentWorld, String text) {
    context(currentWorld);
    if (currentWorld == null || executing || text == null) return;
    String submitted = text.trim();
    if (submitted.length() <= 1 || submitted.length() > 100 || submitted.charAt(0) != '/'
        || submitted.substring(1).isBlank() || submitted.chars().anyMatch(c -> c < 32 || c == 127)) return;
    command = submitted;
  }

  public static Result execute(Object currentWorld, Consumer<String> dispatch) {
    context(currentWorld);
    if (executing) return Result.BUSY;
    if (currentWorld == null || command == null) return Result.EMPTY;
    String submitted = command;
    executing = true;
    try { dispatch.accept(submitted); return Result.EXECUTED; }
    finally { executing = false; }
  }
}
