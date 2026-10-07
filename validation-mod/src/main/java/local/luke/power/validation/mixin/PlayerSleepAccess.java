package local.luke.power.validation.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface PlayerSleepAccess {
  @Accessor("sleeping") void power$sleeping(boolean sleeping);
  @Accessor("field_515") void power$sleepTimer(int ticks);
}
