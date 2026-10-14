package local.luke.power.mining;

import java.util.*;
import java.util.function.IntPredicate;

public final class VeinSettings {
  public boolean enabled;
  public boolean crouchingOnly = true, durabilityPerBlock = true;
  public int maxBlocks = 64, maxChecks = 4096, checksPerTick = 512, blocksPerTick = 8, radius = 16;
  public int timePercent = 25;
  public Map<String, List<Integer>> tools = defaults();

  private static Map<String, List<Integer>> defaults() {
    var rules = new LinkedHashMap<String, List<Integer>>();
    rules.put("16", List.of(270, 274, 257, 278, 285));
    rules.put("15", List.of(274, 257, 278));
    rules.put("14", List.of(257, 278));
    rules.put("21", List.of(274, 257, 278));
    rules.put("56", List.of(257, 278));
    rules.put("73", List.of(257, 278));
    return rules;
  }
  public void validate() {
    if (maxBlocks < 1 || maxBlocks > 512 || maxChecks < 1 || maxChecks > 16384
        || checksPerTick < 1 || checksPerTick > 2048 || blocksPerTick < 1 || blocksPerTick > 32
        || radius < 1 || radius > 32 || timePercent < 1 || timePercent > 200)
      throw new IllegalArgumentException("Invalid vein mining limit");
    validateTools(id -> id > 0, id -> id > 0);
  }
  public void validateTools(IntPredicate blockExists, IntPredicate itemExists) {
    if (tools == null || tools.size() > 256) throw new IllegalArgumentException("Use up to 256 block rules");
    for (var entry : tools.entrySet()) {
      int block;
      try { block = Integer.parseInt(entry.getKey()); }
      catch (RuntimeException e) { throw new IllegalArgumentException("Use block IDs as rule keys, for example \"16\" for coal ore"); }
      if (!Integer.toString(block).equals(entry.getKey()) || !blockExists.test(block))
        throw new IllegalArgumentException("Unknown block ID: " + entry.getKey());
      if (block == 74) throw new IllegalArgumentException("Use block 73 for redstone ore; it includes lit ore 74");
      List<Integer> items = entry.getValue();
      if (items == null || items.size() > 64) throw new IllegalArgumentException("Use a list of up to 64 tool IDs for block " + block);
      for (Integer item : items) if (item == null || !itemExists.test(item))
        throw new IllegalArgumentException("Unknown tool ID for block " + block + ": " + item);
    }
  }
  public boolean accepts(int block, int item) {
    var allowed = tools.get(Integer.toString(block == 74 ? 73 : block));
    return allowed != null && (allowed.isEmpty() || allowed.contains(item));
  }
  public double timeFactor(int count) { return Math.max(1, Math.max(1, count) * timePercent / 100.0); }
}
