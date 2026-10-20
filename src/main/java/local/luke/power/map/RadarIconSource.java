package local.luke.power.map;

import java.util.Set;
import net.minecraft.class_613;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Immutable tick-owned appearance data. Icon generation never reads a live entity. */
public record RadarIconSource(String type, String texture, String skinUrl, int woolColor, boolean wool) {
  static final Set<String> MOBS = Set.of("Creeper", "Skeleton", "Spider", "Giant", "Zombie",
      "Slime", "Ghast", "PigZombie", "Pig", "Sheep", "Cow", "Chicken", "Squid", "Wolf");

  static RadarIconSource capture(Entity entity, String nativeType) {
    if (!(entity instanceof LivingEntity living)) return null;
    String type = nativeType;
    if (entity instanceof PlayerEntity) {
      // Native player implementations are not registered in the entity-type table.
      Class<?> playerClass = entity.getClass();
      if (playerClass != PlayerEntity.class && playerClass != net.minecraft.class_69.class
          && playerClass != net.minecraft.entity.player.ClientPlayerEntity.class
          && playerClass != net.minecraft.client.network.OtherPlayerEntity.class
          && playerClass != net.minecraft.client.network.MultiplayerClientPlayerEntity.class) return null;
      type = "Player";
    } else if (type == null || !nativeMob(entity, type)) return null;
    int color = entity instanceof class_613 sheep ? sheep.method_2047() : 0;
    boolean wool = entity instanceof class_613 sheep && !sheep.method_2048();
    return new RadarIconSource(type, living.method_1314(), entity.skinUrl, color, wool);
  }

  private static boolean nativeMob(Entity entity, String type) {
    Class<?> nativeClass = switch (type) {
      case "Creeper" -> net.minecraft.class_114.class;
      case "Skeleton" -> net.minecraft.class_90.class;
      case "Spider" -> net.minecraft.class_558.class;
      case "Giant" -> net.minecraft.class_311.class;
      case "Zombie" -> net.minecraft.class_480.class;
      case "Slime" -> net.minecraft.class_451.class;
      case "Ghast" -> net.minecraft.class_364.class;
      case "PigZombie" -> net.minecraft.class_535.class;
      case "Pig" -> net.minecraft.class_443.class;
      case "Sheep" -> class_613.class;
      case "Cow" -> net.minecraft.class_466.class;
      case "Chicken" -> net.minecraft.class_313.class;
      case "Squid" -> net.minecraft.class_151.class;
      case "Wolf" -> net.minecraft.class_121.class;
      default -> null;
    };
    return entity.getClass() == nativeClass;
  }
}
