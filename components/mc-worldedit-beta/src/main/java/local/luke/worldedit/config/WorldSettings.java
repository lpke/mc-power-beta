package local.luke.worldedit.config;

/** Stored with level.dat, so renamed worlds and dimensions retain their setting. */
public interface WorldSettings {
  WorldOverride worldedit$override();

  void worldedit$override(WorldOverride value);
}
