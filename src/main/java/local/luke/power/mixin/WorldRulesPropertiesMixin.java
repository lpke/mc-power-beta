package local.luke.power.mixin;

import local.luke.power.rules.*;
import local.luke.power.world.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldProperties.class)
public abstract class WorldRulesPropertiesMixin implements WorldRules, WorldInventory, WorldSpawning {
  @Unique private RuleSet power$rules = new RuleSet();
  @Unique private String power$rulesProblem = "";

  public RuleSet power$rules() {
    if (!power$rulesProblem.isEmpty()) throw new IllegalStateException(power$rulesProblem);
    return power$rules;
  }
  public void power$rules(RuleSet value) {
    if (!power$rulesProblem.isEmpty()) throw new IllegalStateException(power$rulesProblem);
    power$rules = value.copy();
  }
  public String power$rulesProblem() { return power$rulesProblem; }
  public boolean power$keepInventory() { return power$rules().enabled(GameRule.KEEP_INVENTORY); }
  public void power$keepInventory(boolean value) { power$rules().set(GameRule.KEEP_INVENTORY, value); }
  public boolean power$hostileSpawning() { return power$rules().enabled(GameRule.SPAWN_MONSTERS); }
  public void power$hostileSpawning(boolean value) { power$rules().set(GameRule.SPAWN_MONSTERS, value); }
  public boolean power$passiveSpawning() { return power$rules().enabled(GameRule.SPAWN_ANIMALS); }
  public void power$passiveSpawning(boolean value) { power$rules().set(GameRule.SPAWN_ANIMALS, value); }

  @Inject(method = "<init>(JLjava/lang/String;)V", at = @At("RETURN"))
  private void power$create(long seed, String name, CallbackInfo ci) { power$rules = WorldCreation.rules(); }

  @Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
  private void power$read(NbtCompound tag, CallbackInfo ci) {
    try { power$rules = WorldRuleData.read(tag); }
    catch (RuntimeException e) { power$rulesProblem = "World " + tag.getString("LevelName") + ": " + e.getMessage(); }
  }

  @Inject(method = "<init>(Lnet/minecraft/world/WorldProperties;)V", at = @At("RETURN"))
  private void power$copy(WorldProperties other, CallbackInfo ci) {
    WorldRules source = (WorldRules) other;
    power$rulesProblem = source.power$rulesProblem();
    if (power$rulesProblem.isEmpty()) power$rules = source.power$rules().copy();
  }

  @Inject(method = "updateProperties", at = @At("RETURN"))
  private void power$write(NbtCompound tag, NbtCompound player, CallbackInfo ci) { WorldRuleData.write(tag, power$rules()); }
}
