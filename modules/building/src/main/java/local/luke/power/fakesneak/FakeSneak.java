package local.luke.power.fakesneak;

import java.io.IOException;
import local.luke.power.fakesneak.config.*;
import local.luke.power.fakesneak.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public final class FakeSneak {
  public static final Logger LOG = LogManager.getLogger("Beta Fake Sneak");

  private static boolean toggleDown;

  private FakeSneak() {}

  public static Settings settings() {
    return local.luke.power.building.config.Config.current().sneak.copy();
  }

  public static void apply(Settings value) throws IOException {
    local.luke.power.building.config.Config.update(s -> s.sneak = value.copy());
  }

  public static void tick(Minecraft mc) {
    beginTick(mc, local.luke.power.input.Bindings.down(Keys.ALL[0]), Display.isCreated() && Display.isActive());
  }

  public static void beginTick(Minecraft mc, boolean down, boolean focused) {
    boolean playable =
        focused
            && mc.world != null
            && !mc.world.isRemote
            && mc.player != null
            && mc.player.health > 0
            && !mc.player.dead
            && mc.currentScreen == null
            && !mc.paused
            && mc.field_2778
            && mc.field_2807 == mc.player
            && !Compatibility.freecam();
    if (down && !toggleDown && playable) {
      Settings next = local.luke.power.building.config.Config.current().sneak.copy();
      next.enabled = !next.enabled;
      try {
        apply(next);
      } catch (IOException e) {
        LOG.error("Could not save Fake Sneak settings", e);
        mc.inGameHud.addChatMessage("\u00a7cFake sneak settings could not be saved.\u00a7r");
      }
    }
    toggleDown = down;
  }

  public static boolean protects(Entity entity, double dy) {
    Minecraft mc = MinecraftAccessor.instance();
    return local.luke.power.building.config.Config.current().sneak.enabled
        && mc != null
        && entity == mc.player
        && entity.world != null
        && entity.world == mc.world
        && Double.isFinite(entity.x)
        && Double.isFinite(entity.y)
        && Double.isFinite(entity.z)
        && !entity.world.isRemote
        && mc.player.health > 0
        && !entity.dead
        && entity.field_1623
        && !entity.field_1642
        && entity.field_1595 == null
        && dy <= 0
        && Double.isFinite(dy)
        && !Compatibility.freecam()
        && !Compatibility.flying(entity);
  }

  public static EdgeGuard.Motion clip(Entity e, double x, double z) {
    return EdgeGuard.clip(
        x, z, (dx, dz) -> !e.world.method_190(e, e.boundingBox.move(dx, -1, dz)).isEmpty());
  }
}
