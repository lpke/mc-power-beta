package local.luke.power.audio;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.World;

/** Optional action sounds. Native world events keep distance and volume handling intact. */
public final class ExtraSounds {
  private static final Set<String> EVENTS = Set.of("item.crop.plant", "item.hoe.till",
      "item.bucket.fill", "item.bucket.fill_lava", "item.bucket.empty", "item.bucket.empty_lava",
      "entity.cow.milk", "item.bone_meal.use");

  private ExtraSounds() {}

  public static boolean contains(String id) {
    return id != null && id.startsWith("power_beta:") && EVENTS.contains(id.substring(11));
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
