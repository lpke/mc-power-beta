package local.luke.power.creative.inventory.mixin.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
	@Shadow public float walkedDistance;
	@Shadow private int stepsCount;
	
	@Inject(method = "move", at = @At(
		value = "FIELD",
		target = "Lnet/minecraft/entity/Entity;walkedDistance:F",
		shift = Shift.AFTER,
		ordinal = 1
	))
	private void creative_fixFallSound(double e, double f, double par3, CallbackInfo info) {
		Entity entity = Entity.class.cast(this);
		if (!(entity instanceof PlayerEntity player)) return;
		if (!local.luke.power.creative.Modes.flying(player)) return;
		this.stepsCount = (int) Math.floor(this.walkedDistance) + 1;
	}
    @Inject(method = "move", at = @At(value = "FIELD",
        target = "Lnet/minecraft/entity/Entity;stepsCount:I", opcode = org.objectweb.asm.Opcodes.PUTFIELD,
        shift = Shift.AFTER))
    private void power$discardStepBacklog(double x, double y, double z, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if (entity instanceof PlayerEntity player && !player.level.isRemote && player.vehicle == null)
            stepsCount = (int)Math.floor(walkedDistance) + 1;
    }
}
