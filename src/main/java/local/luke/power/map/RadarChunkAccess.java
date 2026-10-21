package local.luke.power.map;

import java.util.List;
import net.minecraft.entity.Entity;

/** Read-only reuse of the existing index, which excludes only exact native item and TNT classes. */
public interface RadarChunkAccess {
  List<Entity>[] power$radarEntities();
  default int power$radarBottom() { return 0; }
}
