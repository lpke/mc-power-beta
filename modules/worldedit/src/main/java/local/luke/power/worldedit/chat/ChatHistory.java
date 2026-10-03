package local.luke.power.worldedit.chat;

import java.util.*;

public final class ChatHistory {
  private static final List<String> entries = new ArrayList<>();
  private int position;
  private String draft = "";

  public static void add(String text) {
    if (entries.isEmpty() || !entries.get(entries.size() - 1).equals(text)) entries.add(text);
    if (entries.size() > 100) entries.remove(0);
  }

  public String move(String current, boolean up) {
    if (position == 0) draft = current;
    position = Math.max(0, Math.min(entries.size(), position + (up ? 1 : -1)));
    return position == 0 ? draft : entries.get(entries.size() - position);
  }
}
