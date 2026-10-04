package local.luke.power.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** A local array draft. Invalid intermediate edits never replace the saved setting. */
public final class IntegerArrayDraft {
  public record Rules(int min, int max, int limit, boolean unique) {
    public Rules {
      if (min > max || limit < 1) throw new IllegalArgumentException("Invalid array bounds");
    }
  }

  private final Rules rules;
  private final List<Integer> values = new ArrayList<>();

  public IntegerArrayDraft(Rules rules, List<Integer> initial) {
    this.rules = rules;
    reset(initial);
  }

  public List<Integer> values() { return List.copyOf(values); }
  public int size() { return values.size(); }
  public int get(int index) { return values.get(index); }

  public void reset(List<Integer> next) {
    if (next.isEmpty() || next.size() > rules.limit())
      throw new IllegalArgumentException("Use 1 to " + rules.limit() + " entries");
    for (int value : next) checkRange(value);
    values.clear();
    values.addAll(next);
  }

  public void set(int index, int value) {
    checkRange(value);
    values.set(index, value);
  }

  public boolean add() {
    if (size() >= rules.limit()) return false;
    for (long candidate = rules.min(); candidate <= rules.max(); candidate++) {
      int value = (int) candidate;
      if (!rules.unique() || !values.contains(value)) {
        values.add(value);
        return true;
      }
    }
    return false;
  }

  public void remove(int index) {
    if (size() > 1 && index >= 0 && index < size()) values.remove(index);
  }

  public void move(int index, int direction) {
    if ((direction == -1 || direction == 1) && index >= 0 && index < size()
        && index + direction >= 0 && index + direction < size())
      Collections.swap(values, index, index + direction);
  }

  public String error() {
    return rules.unique() && new HashSet<>(values).size() != size()
        ? "Each distance must be different." : "";
  }

  private void checkRange(int value) {
    if (value < rules.min() || value > rules.max())
      throw new IllegalArgumentException("Use " + rules.min() + " to " + rules.max());
  }
}
