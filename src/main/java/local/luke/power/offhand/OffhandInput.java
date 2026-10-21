package local.luke.power.offhand;

import local.luke.power.input.*;
import local.luke.power.inventory.tools.SortingSlots;
import local.luke.power.mixin.DeathSlotAccessor;
import local.luke.power.recipes.RecipeBooks;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Admission shared by runtime binding availability, keyboard swaps and native mouse actions. */
public final class OffhandInput {
  private OffhandInput() {}
  public static boolean available(Minecraft mc) {
    if (!Offhand.playable(mc) || !Display.isActive() || mc.player.inventory.getCursorStack() != null) return false;
    if (mc.currentScreen == null) return mc.field_2778 && !mc.paused && mc.player.container == mc.player.playerContainer;
    return hovered(mc) != null;
  }
  public static Slot hovered(Minecraft mc) {
    if (mc == null || !(mc.currentScreen instanceof OffhandSlotPicker picker)) return null;
    Slot slot = pick(mc, picker.power$offhandPointerX(), picker.power$offhandPointerY());
    return swapEligible(mc, slot) ? slot : null;
  }
  public static boolean container(Minecraft mc) {
    if (mc == null || mc.world == null || mc.player == null || mc.world.isRemote || !WorldCompatibility.ready(mc.world)
        || mc.player.world != mc.world || mc.player.health <= 0 || mc.player.dead || mc.field_2807 != mc.player
        || DetachedCamera.isActive() || InteractionState.carryingContainer || !Display.isActive() || Mouse.isGrabbed()
        || mc.player instanceof PlayerMode mode && mode.power$spectator()
        || !(mc.currentScreen instanceof ContainerScreen screen) || !(screen instanceof OffhandSlotPicker)
        || InventoryTextInput.active(screen) || InventoryPanels.modal(screen) || mc.player.inventory.getCursorStack() != null
        || screen.container == null || screen.container != mc.player.container || mc.player.inventory.main.length != 36 || mc.player.inventory.armor.length != 4) return false;
    Class<?> type = screen.getClass(), handler = screen.container.getClass();
    boolean nativeHandler = type == class_585.class && handler == class_277.class && screen.container == mc.player.playerContainer
        || type == class_264.class && handler == class_506.class || type == class_235.class && handler == class_407.class
        || type == class_516.class && handler == class_196.class || type == class_524.class && handler == class_337.class;
    int slots = screen.container.slots.size();
    if (!nativeHandler || slots < 36 || slots > 128 || type == class_585.class && slots != 45 && slots != 46
        || type == class_516.class && slots != 46 || type == class_524.class && slots != 39) return false;
    try { if (!screen.container.method_2094(mc.player)) return false; } catch (RuntimeException unavailable) { return false; }
    try { var book = RecipeBooks.book(screen); return book == null || !book.replacesNative(); }
    catch (RuntimeException unsupported) { return false; }
  }
  /** Normal native slots only. Header, recipe, modal and off-panel pointers never reach a slot. */
  public static Slot pick(Minecraft mc, int x, int y) {
    if (!container(mc)) return null;
    var screen = (ContainerScreen)mc.currentScreen; var picker = (OffhandSlotPicker)screen;
    if (x < 0 || y < 0 || x >= screen.width || y >= screen.height || InventoryPanels.contains(screen, x, y)
        || picker.power$offhandWidgetAt(x, y) || RecipeBooks.owns(screen, x, y)) return null;
    try {
      Slot slot = picker.power$offhandSlotAt(x, y);
      if (slot == null || slot.id < 0 || slot.id >= screen.container.slots.size() || screen.container.method_2084(slot.id) != slot) return null;
      var access = (DeathSlotAccessor)(Object)slot; var owner = access.power$deathInventory(); int index = access.power$deathIndex();
      if (owner == null || index < 0 || index >= owner.size() || owner instanceof OffhandSlotInventory) return null;
      boolean catalog = screen instanceof InventoryGui gui && gui.power$usesCatalogWheel();
      if (catalog) return owner == mc.player.inventory && index < 9 && slot.id == 36 + index && slot.getClass() == Slot.class ? slot : null;
      if (screen.getClass() == class_264.class || screen.getClass() == class_235.class) {
        SortingSlots.read(mc);
        if (slot.getClass() != Slot.class) return null;
      } else if (owner == mc.player.inventory) {
        if (index < 36) { if (slot.getClass() != Slot.class) return null; }
        else if (screen.getClass() != class_585.class || index > 39 || !slot.getClass().isAnonymousClass()
            || slot.getClass().getEnclosingClass() != class_277.class || slot.id != 44 - index) return null;
      } else if (screen.getClass() == class_585.class) {
        if (owner != ((class_277)screen.container).field_1124 || owner.getClass() != class_159.class
            || owner.size() != 4 || index >= 4 || slot.id != index + 1 || slot.getClass() != Slot.class) return null;
      } else if (screen.getClass() == class_516.class) {
        if (owner != ((class_196)screen.container).field_708 || owner.getClass() != class_159.class
            || owner.size() != 9 || index >= 9 || slot.id != index + 1 || slot.getClass() != Slot.class) return null;
      } else if (screen.getClass() == class_524.class) {
        if (owner.getClass() != class_138.class || index > 1 || slot.id != index || slot.getClass() != Slot.class) return null;
      } else return null;
      ItemStack stack = slot.getStack(); return stack == null || slot.canInsert(stack) ? slot : null;
    } catch (java.io.IOException | RuntimeException unsupported) { return null; }
  }
  public static boolean swapEligible(Minecraft mc, Slot slot) {
    if (!Offhand.playable(mc) || !container(mc) || slot == null || slot.id < 0
        || slot.id >= mc.player.container.slots.size() || mc.player.container.method_2084(slot.id) != slot) return false;
    try {
      ItemStack source = slot.getStack(), offhand = Offhand.get(mc.player.inventory);
      return (source != null || offhand != null) && (source == null || slot.canInsert(source))
          && (offhand == null || slot.canInsert(offhand) && offhand.count <= Math.min(offhand.getMaxCount(), slot.getMaxItemCount()));
    } catch (RuntimeException unavailable) { return false; }
  }
}
