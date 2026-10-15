package local.luke.power.validation;

import local.luke.power.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;

/** Muted startup skips native audio initialization; playback fixtures still need its state. */
final class SilentAudioChecks {
  static void ready(Minecraft mc) {
    String sink=System.getenv("PULSE_SINK");
    Validation.check(sink!=null&&sink.startsWith("power_beta_")&&"pulse".equals(System.getenv("ALSOFT_DRIVERS")),
        "Route this disposable clone to its verified silent Pulse sink before audio checks");
    if(SoundManagerAccessor.power$started())return;
    float music=mc.options.musicVolume,sound=mc.options.soundVolume;
    try {mc.options.musicVolume=.01f;mc.options.soundVolume=0;mc.soundManager.method_2008();}
    finally {mc.options.musicVolume=music;mc.options.soundVolume=sound;}
    Validation.check(SoundManagerAccessor.power$started(),"native audio did not initialize");
  }
}
