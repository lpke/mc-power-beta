package local.luke.power.status;

import local.luke.power.input.TweakToggleMessages;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

public final class TweakMessages {
  private static final TweakToggleMessages messages = new TweakToggleMessages();
  private static Object world, player;
  private static boolean wasPlayable;

  private TweakMessages() {}

  public static void tick(Minecraft mc) {
    if (world != mc.world || player != mc.player) {
      messages.reset();
      world = mc.world;
      player = mc.player;
      wasPlayable = false;
    }
    boolean playable = mc.world != null && mc.player != null && mc.player.health > 0
        && !mc.player.dead && mc.currentScreen == null && !mc.paused && Display.isActive();
    for (String message : messages.update(playable && wasPlayable)) mc.inGameHud.addChatMessage(message);
    wasPlayable = playable;
  }
}
