package local.luke.power.death;

import java.util.function.BooleanSupplier;
import net.minecraft.entity.Entity;

/** Scope environmental damage to its victim; nested callbacks cannot change another death. */
public final class DeathCause {
  private record Context(Entity victim, String cause) {}
  private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
  private DeathCause() {}
  public static boolean damage(Entity victim, String cause, BooleanSupplier action) {
    if (!(victim instanceof net.minecraft.entity.player.PlayerEntity)
        || !DeathConfig.current().enabled && !local.luke.power.map.MapConfig.current().deathpoints)
      return action.getAsBoolean();
    Context previous = CURRENT.get(); CURRENT.set(new Context(victim, cause));
    try { return action.getAsBoolean(); }
    finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
  }
  public static String describe(Entity victim, Entity attacker) {
    String command = local.luke.power.world.DeathSources.cause(victim);
    if (!command.isEmpty()) return command;
    Context value = CURRENT.get();
    if (value != null && value.victim == victim) return value.cause;
    if (attacker != null) {
      return "Killed by " + name(attacker);
    }
    return "Unknown cause";
  }
  private static String name(Entity attacker) {
    if (attacker instanceof net.minecraft.entity.player.PlayerEntity player) return player.name;
    String id = net.minecraft.class_206.method_734(attacker);
    return id == null ? attacker.getClass().getSimpleName().replace("Entity", "") : id;
  }
}
