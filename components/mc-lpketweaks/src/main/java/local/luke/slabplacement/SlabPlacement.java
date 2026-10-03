package local.luke.slabplacement;

import java.io.IOException;
import local.luke.slabplacement.config.*;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public final class SlabPlacement {
  public static final Logger LOG = LogManager.getLogger("Beta Slab Placement");

  private static boolean toggleDown;

  private SlabPlacement() {}

  public static Settings settings() {
    return local.luke.tweaks.config.Config.current().slabs.copy();
  }

  public static void apply(Settings value) throws IOException {
    local.luke.tweaks.config.Config.update(s -> s.slabs = value.copy());
  }

  public static void tick(Minecraft mc) {
    beginTick(mc, Input.down(Keys.ALL[0].code), Display.isCreated() && Display.isActive());
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
      Settings next = local.luke.tweaks.config.Config.current().slabs.copy();
      next.enabled = !next.enabled;
      try {
        apply(next);
        if (local.luke.tweaks.config.Config.current().slabs.announceToggle)
          mc.inGameHud.addChatMessage(
              "Slab completion: "
                  + (local.luke.tweaks.config.Config.current().slabs.enabled ? "ON" : "OFF"));
      } catch (IOException e) {
        LOG.error("Could not save Slab Placement settings", e);
        mc.inGameHud.addChatMessage("Slab Placement settings could not be saved.");
      }
    }
    toggleDown = down;
  }

  public static boolean enabled() {
    return local.luke.tweaks.config.Config.current().slabs.enabled;
  }
}
