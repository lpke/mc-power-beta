package local.luke.power.audio;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.World;

/** Optional action sounds. Native world events keep distance and volume handling intact. */
public final class ExtraSounds {
  private static final Set<String> EVENTS = Set.of("item.crop.plant", "item.hoe.till",
      "item.bucket.fill", "item.bucket.fill_lava", "item.bucket.empty", "item.bucket.empty_lava",
      "entity.cow.milk", "item.bone_meal.use", "block.lava.pop", "block.lava.ambient", "entity.generic.splash", "entity.hostile.splash", "entity.player.swim",
      "entity.player.splash", "entity.player.splash.high_speed", "ambient.underwater.enter", "ambient.underwater.exit",
      "ambient.underwater.loop", "ambient.underwater.loop.additions", "ambient.underwater.loop.additions.rare",
      "ambient.underwater.loop.additions.ultra_rare");

  private ExtraSounds() {}

  public static boolean contains(String id) {
    return id != null && id.startsWith("power_beta:") && EVENTS.contains(id.substring(11));
  }

  /** Preview uses the same source and category gain as the individual-sound controls. */
  public static String previewId(String settingId) {
    return switch (settingId) {
      case "power_controls:features.eatingSounds" -> "power_controls:random.eat";
      case "power_controls:features.shearingSounds" -> "power_controls:entity.sheep.shear";
      case "power_controls:features.toolBreakSounds", "power_controls:features.armorBreakSounds" -> "power_controls:random.break";
      case "power_controls:features.moreSoundsChests" -> "power_controls:random.chestopen";
      case "audio.redstonePlacementSound" -> "step.stone";
      case "audio.seedPlantingSounds" -> "power_beta:item.crop.plant";
      case "audio.hoeTillingSounds" -> "power_beta:item.hoe.till";
      case "audio.bucketSounds" -> "power_beta:item.bucket.fill";
      case "audio.lavaBucketSounds" -> "power_beta:item.bucket.fill_lava";
      case "audio.boneMealSounds" -> "power_beta:item.bone_meal.use";
      case "audio.lavaPopSounds" -> "power_beta:block.lava.pop";
      case "audio.lavaAmbientSounds" -> "power_beta:block.lava.ambient";
      case "audio.waterSplashSounds" -> "power_beta:entity.player.splash";
      case "audio.swimmingSounds" -> "power_beta:entity.player.swim";
      case "audio.underwaterEnterSounds" -> "power_beta:ambient.underwater.enter";
      case "audio.underwaterExitSounds" -> "power_beta:ambient.underwater.exit";
      case "audio.underwaterAmbience" -> "power_beta:ambient.underwater.loop";
      case "audio.underwaterAdditions" -> "power_beta:ambient.underwater.loop.additions";
      case "audio.underwaterRareAdditions" -> "power_beta:ambient.underwater.loop.additions.rare";
      case "audio.underwaterUltraRareAdditions" -> "power_beta:ambient.underwater.loop.additions.ultra_rare";
      default -> null;
    };
  }

  public static void play(World world, double x, double y, double z, String event) {
    // Remote actions and their sounds remain server-owned.
    if (world.isRemote) return;
    if (!EVENTS.contains(event)) throw new IllegalArgumentException("Unknown extra sound: " + event);
    float volume = event.equals("item.crop.plant") ? .45f : 1;
    var random = ThreadLocalRandom.current();
    float pitch = event.equals("item.crop.plant") && random.nextBoolean() ? 1.2f
        : event.equals("item.bone_meal.use") ? .9f + .1f * random.nextInt(3) : 1;
    world.method_150(x, y, z, "power_beta:" + event, volume, pitch);
  }
}
