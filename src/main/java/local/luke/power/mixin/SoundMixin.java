package local.luke.power.mixin;
import local.luke.power.audio.AudioController;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.class_266;
import net.minecraft.class_267;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

@Mixin(SoundManager.class)
public class SoundMixin {
  @Inject(method="method_2017",at=@At("HEAD"),cancellable=true)
  private void power$musicGate(CallbackInfo ci){if(AudioController.blockBackground())ci.cancel();}
  @WrapOperation(method="method_2017",at=@At(value="INVOKE",target="Lnet/minecraft/class_266;method_957()Lnet/minecraft/class_267;"))
  private class_267 power$playlist(class_266 pool,Operation<class_267> original){synchronized(pool){return AudioController.choose(java.util.List.copyOf(((SoundPoolAccessor)pool).power$tracks()));}}
  @Inject(method="method_2017",at=@At(value="INVOKE",target="Lpaulscode/sound/SoundSystem;backgroundMusic(Ljava/lang/String;Ljava/net/URL;Ljava/lang/String;Z)V"))
  private void power$delay(CallbackInfo ci){AudioController.nextDelay((SoundManagerAccessor)this);}
  @WrapOperation(method={"method_2017","method_2008"},at=@At(value="INVOKE",target="Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V"))
  private void power$musicVolume(SoundSystem system,String channel,float volume,Operation<Void> original){original.call(system,channel,AudioController.backgroundVolume(volume));}
  @WrapOperation(method="method_2015",at=@At(value="INVOKE",target="Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V"))
  private void power$positional(SoundSystem system,String channel,float volume,Operation<Void> original,String id,float x,float y,float z,float input,float pitch){original.call(system,channel,AudioController.mix(channel,id,volume,false));}
  @WrapOperation(method="method_2009",at=@At(value="INVOKE",target="Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V"))
  private void power$interface(SoundSystem system,String channel,float volume,Operation<Void> original,String id,float input,float pitch){original.call(system,channel,AudioController.mix(channel,id,volume,true));}
  @WrapOperation(method="method_2010",at=@At(value="INVOKE",target="Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V"))
  private void power$record(SoundSystem system,String channel,float volume,Operation<Void> original,String id,float x,float y,float z,float input,float pitch){original.call(system,channel,AudioController.mix(channel,"records."+id,volume,false));}
}
