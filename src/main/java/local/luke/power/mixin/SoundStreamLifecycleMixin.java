package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.audio.MusicStream;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import paulscode.sound.*;

/** Detach a stream under the worker's own lock before freeing its decoder. */
@Mixin(value = Library.class, remap = false)
public class SoundStreamLifecycleMixin {
  @Shadow protected StreamThread streamThread;

  @WrapOperation(method = "pause", at = @At(
      value = "INVOKE", target = "Lpaulscode/sound/Source;pause()V"))
  private void power$pauseStartedMusic(Source source, Operation<Void> original) {
    if (MusicStream.canPause(source)) original.call(source);
  }

  @WrapOperation(method = {"removeSource", "removeTemporarySources", "cleanup"}, at = @At(
      value = "INVOKE", target = "Lpaulscode/sound/Source;cleanup()V"))
  private void power$dispose(Source source, Operation<Void> original) {
    if (streamThread == null) { original.call(source); return; }
    var streams = (SoundStreamLockAccess)streamThread;
    synchronized (streams.power$streamLock()) {
      streams.power$streamSources().remove(source);
      original.call(source);
    }
  }
}
