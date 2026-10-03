package local.luke.power.flexible;

import java.io.IOException;
import local.luke.power.flexible.config.*;
import local.luke.power.flexible.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public final class FlexiblePlacement {
  public static final Logger LOG = LogManager.getLogger("Beta Flexible Placement");

  private static Modes modes = Modes.NONE;
  private static boolean toggleDown, releaseRequired;

  private FlexiblePlacement() {}

  public static Settings settings() {
    return local.luke.power.building.config.Config.current().flexible.copy();
  }

  public static void apply(Settings value) throws IOException {
    local.luke.power.building.config.Config.update(s -> s.flexible = value.copy());
    interrupt();
  }

  public static boolean modifiersActive() {
    return local.luke.power.building.config.Config.current().flexible.enabled && modes.active();
  }

  public static Modes modes() {
    return modes;
  }

  public static void interrupt() {
    modes = Modes.NONE;
    releaseRequired = true;
  }

  public static boolean canOperate(Minecraft mc) {
    return mc != null
        && mc.world != null
        && !mc.world.isRemote
        && mc.player != null
        && mc.player.health > 0
        && !mc.player.dead
        && !mc.player.field_1642
        && mc.currentScreen == null
        && !mc.paused
        && mc.field_2778
        && mc.interactionManager != null
        && mc.field_2807 == mc.player
        && !Compatibility.freecam()
        && Double.isFinite(mc.player.x)
        && Double.isFinite(mc.player.y)
        && Double.isFinite(mc.player.z)
        && Float.isFinite(mc.player.yaw)
        && Float.isFinite(mc.player.pitch);
  }

  public static void tick(Minecraft mc) {
    beginTick(
        mc,
        local.luke.power.input.Bindings.down(Keys.ALL[0]),
        new Modes(
            local.luke.power.input.Bindings.down(Keys.ALL[1]),
            local.luke.power.input.Bindings.down(Keys.ALL[2]),
            local.luke.power.input.Bindings.down(Keys.ALL[3]),
            local.luke.power.input.Bindings.down(Keys.ALL[4]),
            local.luke.power.input.Bindings.down(Keys.ALL[5])),
        Display.isCreated() && Display.isActive());
  }

  public static void beginTick(Minecraft mc, boolean toggle, Modes held, boolean focused) {
    boolean playable = focused && canOperate(mc);
    if (!playable) interrupt();
    if (!held.active()) releaseRequired = false;
    if (toggle && !toggleDown && playable) {
      Settings next = local.luke.power.building.config.Config.current().flexible.copy();
      next.enabled = !next.enabled;
      try {
        apply(next);
        mc.inGameHud.addChatMessage(
            "Flexible placement: "
                + (local.luke.power.building.config.Config.current().flexible.enabled ? "\u00a7aON\u00a7r" : "\u00a7cOFF\u00a7r"));
      } catch (IOException e) {
        LOG.error("Could not save Flexible Placement settings", e);
        mc.inGameHud.addChatMessage("\u00a7cFlexible placement settings could not be saved.\u00a7r");
      }
    }
    toggleDown = toggle;
    modes =
        playable && local.luke.power.building.config.Config.current().flexible.enabled && !releaseRequired
            ? held
            : Modes.NONE;
  }

  public static boolean handles(Minecraft mc, ItemStack stack) {
    return local.luke.power.building.config.Config.current().flexible.enabled
        && canOperate(mc)
        && NativePlacement.block(stack) >= 0
        && (modes.active()
            || local.luke.power.building.config.Config.current().flexible.placeAgainstContainers);
  }

  public static PlacementPlan plan(Minecraft mc, ItemStack s, int x, int y, int z, int face) {
    return handles(mc, s) ? PlacementPlan.create(mc, s, x, y, z, face, modes) : null;
  }

  /** Optional Fast Place integration. Null means native; an empty array rejects the attempt. */
  public static int[] resolveClick(World world, ItemStack stack, int x, int y, int z, int face) {
    Minecraft mc = MinecraftAccessor.instance();
    if (mc == null || mc.world != world || !handles(mc, stack)) return null;
    PlacementPlan p = plan(mc, stack, x, y, z, face);
    if (p == null || !p.valid()) return new int[0];
    return new int[] {p.clicked().x(), p.clicked().y(), p.clicked().z(), p.face().ordinal()};
  }
}
