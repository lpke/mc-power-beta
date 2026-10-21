package local.luke.power.mixin;

import local.luke.power.map.RadarChunkAccess;
import org.spongepowered.asm.mixin.*;

/** Flattened sections start at the chunk's native bottom, which can differ from zero. */
@Mixin(targets = "net.modificationstation.stationapi.impl.world.chunk.FlattenedChunk", remap = false)
public abstract class FlattenedRadarMixin implements RadarChunkAccess {
  @Shadow @Final public short firstBlock;
  @Override public int power$radarBottom() { return firstBlock; }
}
