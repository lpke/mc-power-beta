package local.luke.power.worldedit.core;

public interface WorldAccess {
  boolean loaded(Pos pos);

  BlockValue get(Pos pos);

  /** Resolve desired native properties before planning, equality checks and history accounting. */
  default BlockValue prepare(Pos pos, BlockValue desired, BlockValue existing) { return desired; }

  void set(Pos pos, BlockValue state);
}
