package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.rules.*;
import local.luke.power.world.ExplosionSource;
import net.minecraft.class_114;
import net.minecraft.class_60;
import net.minecraft.entity.Entity;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_60.class)
public class ExplosionRulesMixin {
  @Shadow public Entity field_1395;
  @Unique private boolean power$fireball;

  @Inject(method = "<init>", at = @At("RETURN"))
  private void power$origin(World world, Entity source, double x, double y, double z, float power, CallbackInfo ci) {
    power$fireball = ExplosionSource.fireball();
  }

  // Only the destruction phase changes. Keep native entity damage, fire and particles.
  @WrapOperation(method = "method_1196", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/World;getBlockId(III)I"))
  private int power$blocks(World world, int x, int y, int z, Operation<Integer> original) {
    int id = original.call(world, x, y, z);
    if (!world.isRemote) {
      RuleSet rules = WorldRules.of(world.method_262());
      if (field_1395 instanceof class_114 && !rules.enabled(GameRule.CREEPER_GRIEFING)
          || power$fireball && !rules.enabled(GameRule.GHAST_GRIEFING)) return 0;
      if (id == Block.field_1887.id && !rules.enabled(GameRule.TNT_EXPLODES)) return 0;
    }
    return id;
  }
}
