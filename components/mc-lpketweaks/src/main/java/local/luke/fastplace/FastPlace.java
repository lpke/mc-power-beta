package local.luke.fastplace;

import java.io.IOException;
import local.luke.fastplace.config.Settings;
import net.minecraft.class_212;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public final class FastPlace {
  public static final Logger LOG = LogManager.getLogger("Beta Fast Place");
  public static final KeyBinding KEY = new KeyBinding("Fast place (toggle)", Keyboard.KEY_NONE);

  private static PlacementSession session;
  private static boolean toggleDown, releaseRequired, useDown;
  private static int placedThisTick;

  private FastPlace() {}

  public static Settings settings() {
    return local.luke.tweaks.config.Config.current().placement.copy();
  }

  public static void apply(Settings value) throws IOException {
    local.luke.tweaks.config.Config.update(s -> s.placement = value.copy());
    interrupt();
  }

  public static boolean keyDown(int key) {
    if (key < 0) {
      int button = key + 100;
      return Mouse.isCreated()
          && button >= 0
          && button < Mouse.getButtonCount()
          && Mouse.isButtonDown(button);
    }
    return Keyboard.isCreated()
        && key > 0
        && key < Keyboard.KEYBOARD_SIZE
        && Keyboard.isKeyDown(key);
  }

  public static void tickInput(Minecraft mc) {
    beginTick(
        mc,
        Mouse.isCreated()
            && Mouse.isButtonDown(1)
            && !local.luke.tweaks.hotbar.Hotbars.consumesMouse(mc, 1),
        keyDown(KEY.code),
        Display.isCreated() && Display.isActive());
  }

  public static void beginTick(Minecraft mc, boolean use, boolean toggle, boolean focused) {
    placedThisTick = 0;
    useDown = use;
    if (!use) {
      session = null;
      releaseRequired = false;
    }
    boolean playable = focused && canOperate(mc);
    if (!playable) interrupt();
    if (toggle && !toggleDown && playable) {
      Settings next = local.luke.tweaks.config.Config.current().placement.copy();
      next.setEnabled(!next.enabled);
      try {
        apply(next);
        mc.inGameHud.addChatMessage(
            "Fast place: "
                + (local.luke.tweaks.config.Config.current().placement.enabled ? "ON" : "OFF"));
      } catch (IOException e) {
        LOG.error("Could not save Fast Place settings", e);
        mc.inGameHud.addChatMessage("Fast Place settings could not be saved.");
      }
    }
    toggleDown = toggle;
    if (session != null
        && !sameItem(mc.player.inventory.getSelectedItem(), mc.player.inventory.selectedSlot))
      interrupt();
  }

  public static void release() {
    useDown = false;
    session = null;
    releaseRequired = false;
  }

  public static void interrupt() {
    session = null;
    releaseRequired = useDown;
  }

  public static boolean canOperate(Minecraft mc) {
    return mc.world != null
        && !mc.world.isRemote
        && mc.player != null
        && !mc.player.dead
        && !mc.player.field_1642
        && mc.player.health > 0
        && mc.currentScreen == null
        && !mc.paused
        && mc.field_2778
        && mc.interactionManager != null
        && Double.isFinite(mc.player.x)
        && Double.isFinite(mc.player.y)
        && Double.isFinite(mc.player.z)
        && Float.isFinite(mc.player.yaw)
        && Float.isFinite(mc.player.pitch)
        && mc.field_2807 == mc.player
        && !Compatibility.freecamActive();
  }

  public static boolean suppressVanilla(Minecraft mc, int button) {
    return button == 1
        && local.luke.tweaks.config.Config.current().placement.enabled
        && canOperate(mc)
        && (session != null || releaseRequired);
  }

  public static boolean eligible(Minecraft mc, ItemStack stack) {
    return local.luke.tweaks.config.Config.current().placement.enabled
        && !releaseRequired
        && canOperate(mc)
        && PlacementTarget.placedBlock(stack) >= 0
        && local.luke.tweaks.config.Config.current()
            .placement
            .permits(stack.itemId, stack.getDamage());
  }

  public static void firstPlacement(
      Minecraft mc,
      ItemStack stack,
      int slot,
      int face,
      float yaw,
      PlacementTarget target,
      boolean success) {
    if (success && target != null && eligible(mc, stack) && target.changedAsExpected(mc.world)) {
      session =
          new PlacementSession(
              target.position(),
              face,
              yaw,
              stack.itemId,
              stack.getDamage(),
              slot,
              target.expectedBlock() == 43 && target.previousId() == 44);
      placedThisTick++;
    }
  }

  private static boolean sameItem(ItemStack stack, int slot) {
    return stack != null
        && stack.count > 0
        && stack.itemId == session.itemId
        && stack.getDamage() == session.damage
        && slot == session.slot;
  }

  public static void placeBatch(Minecraft mc) {
    if (session == null
        || !useDown
        || !local.luke.tweaks.config.Config.current().placement.enabled
        || !canOperate(mc)) return;
    while (placedThisTick < local.luke.tweaks.config.Config.current().placement.attemptsPerTick) {
      ItemStack stack = mc.player.inventory.getSelectedItem();
      if (!sameItem(stack, mc.player.inventory.selectedSlot) || !eligible(mc, stack)) {
        interrupt();
        break;
      }
      mc.field_2818.method_1838(1.0f); // Includes entity occlusion and installed raycast hooks.
      class_27 hit = mc.field_2823;
      if (hit == null || hit.field_1983 != class_212.TILE) break;
      PlacementTarget target =
          PlacementTarget.at(
              mc.world, stack, hit.field_1984, hit.field_1985, hit.field_1986, hit.field_1987);
      if (target == null) break;
      boolean slab = stack.itemId == 44;
      boolean merge = slab && target.expectedBlock() == 43 && target.previousId() == 44;
      SlabMode mode = local.luke.tweaks.config.Config.current().placement.slabMode;
      if (slab
          && !SlabPolicy.allows(
              mode, session.firstWasMerge, merge, session.first.y(), target.position().y())) break;
      int restrictedFace =
          slab
                  && local.luke.tweaks.config.Config.current().placement.restrictionMode
                      == RestrictionMode.FACE
              ? SlabPolicy.restrictionFace(
                  mode, session.face, session.first.y(), target.position().y(), hit.field_1987)
              : hit.field_1987;
      boolean repeatMerge = slab && merge && mode != SlabMode.MATCH_FIRST;
      if (!session.allows(
          target.position(),
          restrictedFace,
          local.luke.tweaks.config.Config.current().placement.restrictionEnabled,
          local.luke.tweaks.config.Config.current().placement.restrictionMode,
          repeatMerge)) break;
      var player = mc.player;
      float yaw = player.yaw;
      int count = stack.count;
      boolean success;
      try {
        if (local.luke.tweaks.config.Config.current().placement.rememberOrientation
            && !Compatibility.flexibleModifiersActive()) player.yaw = session.yaw;
        success =
            mc.interactionManager.method_1713(
                player,
                mc.world,
                stack,
                hit.field_1984,
                hit.field_1985,
                hit.field_1986,
                hit.field_1987);
      } finally {
        player.yaw = yaw;
      }
      if (stack.count <= 0 && player.inventory.getSelectedItem() == stack)
        player.inventory.main[player.inventory.selectedSlot] = null;
      else if (stack.count != count) mc.field_2818.field_2342.method_1863();
      // Opening a screen can clear session during the call.
      if (session == null || !canOperate(mc)) break;
      if (!success || !target.changedAsExpected(mc.world)) break;
      session.placed(target.position());
      placedThisTick++;
      mc.player.method_500();
    }
  }
}
