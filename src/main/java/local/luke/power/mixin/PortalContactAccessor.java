package local.luke.power.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface PortalContactAccessor {
  @Accessor("field_512") void power$portalContact(boolean touching);
}
