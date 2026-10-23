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
  private static Minecraft client;

  private FakeSneak() {}

  public static Settings settings() {
    return local.luke.power.building.config.Config.current().sneak.copy();
  }

  public static void apply(Settings value) throws IOException {
    local.luke.power.building.config.Config.update(s -> s.sneak = value.copy());
  }

  public static void tick(Minecraft mc) {
    if (client != mc) {
      client = mc;
      local.luke.power.input.Bindings.onKeyPress("building.fakeSneak", FakeSneak::press);
      local.luke.power.input.Bindings.onMousePress("building.fakeSneak", FakeSneak::press);
      local.luke.power.input.Bindings.onRelease("building.fakeSneak", key -> { if (key == Keys.ALL[0].code) toggleDown = false; });
    }
    boolean down = local.luke.power.input.Bindings.physical(Keys.ALL[0].code);
    if (!down) toggleDown = false;
    else if (!playable(mc, Display.isCreated() && Display.isActive())) toggleDown = true;
  }

  private static void press(int key) {
    if (key != Keys.ALL[0].code) return;
    if (!toggleDown && playable(client, Display.isActive())
        && local.luke.power.input.Bindings.matches(Keys.ALL[0], key)) toggle(client);
    toggleDown = true;
  }

  private static boolean playable(Minecraft mc, boolean focused) {
    return focused && mc != null && mc.world != null && !mc.world.isRemote
        && mc.player != null && mc.player.health > 0 && !mc.player.dead
        && mc.currentScreen == null && !mc.paused && mc.field_2778
        && mc.field_2807 == mc.player && !Compatibility.freecam();
  }

  private static void toggle(Minecraft mc) {
    Settings state = local.luke.power.building.config.Config.current().sneak;
    state.enabled = !state.enabled;
  }

  /** Explicit synthetic input adapter used by the disposable runtime fixtures. */
  public static void beginTick(Minecraft mc, boolean down, boolean focused) {
    if (down && !toggleDown && playable(mc, focused)) toggle(mc);
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
