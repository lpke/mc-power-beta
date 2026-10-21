package local.luke.power.map;

import java.util.*;
import java.util.function.Consumer;

/** Fair, bounded visits to native chunk sections. Entity-count changes never restart a sweep. */
final class RadarChunkScan<C, T> {
  interface Access<C, T> {
    C loaded(int x, int z);
    List<T>[] sections(C chunk);
    int bottom(C chunk);
  }

  private static final int SECTION_QUOTA = 8, CHUNK_QUOTA = 32, MAX_CURSORS = 65 * 65;
  private static final class Cursor {
    final int[] offsets;
    int section;
    Cursor(int sections) { offsets = new int[sections]; }
  }
  // Cursors contain only offsets. The native provider remains the sole owner of loaded terrain.
  private final WeakHashMap<C, Cursor> cursors = new WeakHashMap<>();
  private int[][] offsets = {{0, 0}};
  private int radius = -1, nextChunk;

  int scan(Access<C, T> access, int centerX, int centerZ, int range, double bottom, double top,
      int chunkBudget, int entityBudget, Consumer<T> visit) {
    if (radius != range) configure(range);
    int visited = 0;
    for (int chunks = 0; chunks < Math.min(chunkBudget, offsets.length) && visited < entityBudget; chunks++) {
      int[] offset = offsets[nextChunk];
      nextChunk = (nextChunk + 1) % offsets.length;
      C chunk = access.loaded(centerX + offset[0], centerZ + offset[1]);
      if (chunk == null) continue;
      List<T>[] sections = access.sections(chunk);
      if (sections == null || sections.length == 0) continue;
      int origin = access.bottom(chunk);
      int first = Math.max(0, (int)Math.floor((bottom - origin) / 16));
      int last = Math.min(sections.length - 1, (int)Math.floor((top - origin) / 16));
      if (first > last) continue;
      Cursor cursor = cursors.get(chunk);
      if (cursor == null || cursor.offsets.length != sections.length) {
        cursor = new Cursor(sections.length);
        cursors.put(chunk, cursor);
        if (cursors.size() > MAX_CURSORS) cursors.remove(cursors.keySet().iterator().next());
      }
      int inChunk = 0;
      for (int n = 0; n <= last - first && inChunk < CHUNK_QUOTA && visited < entityBudget; n++) {
        if (cursor.section < first || cursor.section > last) cursor.section = first;
        int section = cursor.section;
        cursor.section = section == last ? first : section + 1;
        List<T> entities = sections[section];
        if (entities == null || entities.isEmpty()) { cursor.offsets[section] = 0; continue; }
        int count = Math.min(Math.min(SECTION_QUOTA, entities.size()),
            Math.min(CHUNK_QUOTA - inChunk, entityBudget - visited));
        for (int i = 0; i < count; i++) {
          if (cursor.offsets[section] >= entities.size()) cursor.offsets[section] = 0;
          visit.accept(entities.get(cursor.offsets[section]++));
          visited++; inChunk++;
        }
      }
    }
    return visited;
  }

  private void configure(int range) {
    radius = Math.max(0, Math.min(32, range));
    offsets = new int[(radius * 2 + 1) * (radius * 2 + 1)][2];
    int index = 1;
    for (int r = 1; r <= radius; r++) {
      for (int x = -r; x <= r; x++) offsets[index++] = new int[] {x, -r};
      for (int z = -r + 1; z <= r; z++) offsets[index++] = new int[] {r, z};
      for (int x = r - 1; x >= -r; x--) offsets[index++] = new int[] {x, r};
      for (int z = r - 1; z > -r; z--) offsets[index++] = new int[] {-r, z};
    }
    nextChunk = 0;
  }

  void clear() { cursors.clear(); nextChunk = 0; }
}
