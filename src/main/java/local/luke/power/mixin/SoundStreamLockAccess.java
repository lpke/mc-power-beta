package local.luke.power.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import paulscode.sound.Source;
import paulscode.sound.StreamThread;

@Mixin(value = StreamThread.class, remap = false)
public interface SoundStreamLockAccess {
  @Accessor("listLock") Object power$streamLock();
  @Accessor("streamingSources") List<Source> power$streamSources();
}
