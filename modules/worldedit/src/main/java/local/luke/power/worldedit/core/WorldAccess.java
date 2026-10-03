package local.luke.power.worldedit.core;

public interface WorldAccess {
  boolean loaded(Pos pos);

  BlockValue get(Pos pos);

  void set(Pos pos, BlockValue state);
}
