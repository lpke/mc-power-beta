package local.luke.worldedit.chat;

import java.util.List;

public final class CompletionCycle {
  private List<String> choices = List.of();
  private String previous;
  private int index;

  public String next(String input, boolean backwards) {
    if (!input.equals(previous)) {
      choices = CommandCatalog.complete(input);
      index = backwards ? choices.size() : -1;
    }
    if (choices.isEmpty()) return input;
    index = Math.floorMod(index + (backwards ? -1 : 1), choices.size());
    previous = choices.get(index);
    return previous;
  }

  public void reset() {
    previous = null;
  }
}
