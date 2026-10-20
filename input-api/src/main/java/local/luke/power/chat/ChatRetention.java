package local.luke.power.chat;

import java.util.*;

/** Separate limits count submitted messages, retaining every wrapped line of each kept message. */
public final class ChatRetention<T> {
  private record Tag(long group, boolean own) {}
  private final Map<T, Tag> tags = new IdentityHashMap<>();
  private long sequence;
  public long group() { return ++sequence; }
  public Long groupOf(T line) { Tag tag = tags.get(line); return tag == null ? null : tag.group; }
  public void add(T line, long group, boolean own) { tags.put(line, new Tag(group, own)); }
  public void clear() { tags.clear(); }
  public void removeOwn(List<T> lines) {
    lines.removeIf(line -> { Tag tag = tags.get(line); return tag != null && tag.own; });
    tags.entrySet().removeIf(entry -> entry.getValue().own);
  }
  public void trim(List<T> newestFirst, int ownLimit, int otherLimit) {
    Set<Long> own = new HashSet<>(), other = new HashSet<>();
    for (var iterator = newestFirst.iterator(); iterator.hasNext();) {
      T line = iterator.next();
      Tag tag = tags.computeIfAbsent(line, ignored -> new Tag(group(), false));
      Set<Long> seen = tag.own ? own : other; seen.add(tag.group);
      if (seen.size() > (tag.own ? ownLimit : otherLimit)) iterator.remove();
    }
    Set<T> retained = Collections.newSetFromMap(new IdentityHashMap<>()); retained.addAll(newestFirst);
    tags.keySet().retainAll(retained);
  }
}
