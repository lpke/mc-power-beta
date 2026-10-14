package local.luke.power.render;

import local.luke.power.input.*;
import local.luke.power.visual.*;
import local.luke.power.commands.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.*;
import org.lwjgl.opengl.Display;

public final class RenderControls {
  public static final KeyBinding ENTITIES = new KeyBinding("power_beta.entities", 0);
  public static final KeyBinding FULL_BRIGHT = new KeyBinding("power_beta.fullBright", 0);
  private static final FullBrightState brightness = new FullBrightState();
  private static boolean initialized, active;
  private static int renderDepth;
  private static Thread renderThread;
  private RenderControls() {}
  public static void register(GameOptions options) {
    if (java.util.Arrays.asList(options.allKeys).contains(ENTITIES)) return;
    var keys = java.util.Arrays.copyOf(options.allKeys, options.allKeys.length + 2);
    keys[keys.length - 2] = ENTITIES; keys[keys.length - 1] = FULL_BRIGHT;
    options.allKeys = keys;
    if (!initialized) {
      initialized = true;
      Bindings.onMousePress("power_beta.render", RenderControls::press);
      TweakIndicators.register(TweakIndicators.Tweak.HIDDEN_ENTITIES,
          () -> VisualConfig.current().hideEntities ? "" : null,
          () -> VisualConfig.current().entityRenderingMessages);
      TweakIndicators.register(TweakIndicators.Tweak.FULL_BRIGHT,
          () -> active ? "" : null, () -> VisualConfig.current().fullBrightMessages);
    }
  }
  private static Minecraft client() {
    return (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
  }
  public static void press(int key) {
    Minecraft mc = client();
    if (mc.world == null || mc.player == null || mc.currentScreen != null || mc.paused
        || mc.player.dead || mc.player.health <= 0 || !Display.isActive()) return;
    if (Bindings.matches(ENTITIES, key)) {
      VisualSettings settings = VisualConfig.copy(); settings.hideEntities = !settings.hideEntities;
      VisualConfig.queue(settings);
    }
    if (Bindings.matches(FULL_BRIGHT, key)) {
      tick(mc); brightness.toggle(); refresh(mc);
    }
  }
  public static void tick(Minecraft mc) {
    brightness.update(mc.world, CommandContext.current(mc).cheats(),
        mc.player instanceof PlayerMode mode && mode.power$spectator(), VisualConfig.current().spectatorFullBright);
    refresh(mc);
  }
  private static void refresh(Minecraft mc) {
    boolean next = mc.world != null && brightness.active();
    if (active == next) return;
    active = next;
    if (mc.world != null && mc.worldRenderer != null) mc.worldRenderer.method_1537();
  }
  public static boolean fullBright() { return active; }
  public static void reset(Minecraft mc) {
    brightness.update(null, false, false, false);
    refresh(mc);
    if (VisualConfig.current().hideEntities) {
      var settings = VisualConfig.copy(); settings.hideEntities = false; VisualConfig.queue(settings);
    }
  }
  public static boolean renderingFullBright() { return active && renderDepth > 0 && Thread.currentThread() == renderThread; }
  public static void beginRender() { if (renderDepth == 0) renderThread = Thread.currentThread(); renderDepth++; }
  public static void endRender() { renderDepth--; }
}
