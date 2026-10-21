package local.luke.power.inventory.tools;

import java.io.IOException;
import java.util.*;
import local.luke.power.input.*;
import local.luke.power.inventory.tools.mixin.SortingDoubleChestAccessor;
import local.luke.power.mixin.DeathSlotAccessor;
import local.luke.power.offhand.OffhandSlotInventory;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.*;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Exact native backing owners and indices, rather than transport slot-count arithmetic. */
public final class SortingSlots {
  public record Regions(List<Slot> player, List<Slot> storage) {
    public Regions { player = List.copyOf(player); storage = List.copyOf(storage); }
  }
  private SortingSlots() {}
  public static boolean screen(Screen screen) {
    return screen != null && (screen.getClass() == class_585.class || screen.getClass() == class_264.class || screen.getClass() == class_235.class);
  }
  public static boolean eligible(Minecraft mc) {
    return mc != null && mc.player != null && mc.world != null && !mc.world.isRemote && WorldCompatibility.ready(mc.world)
        && mc.player.world == mc.world && mc.player.health > 0 && !mc.player.dead && mc.field_2807 == mc.player
        && !DetachedCamera.isActive() && !InteractionState.carryingContainer && Display.isActive() && !Mouse.isGrabbed()
        && !(mc.player instanceof PlayerMode mode && !mode.power$inventoryEditable()) && screen(mc.currentScreen)
        && !(mc.currentScreen instanceof InventoryGui gui && gui.power$usesCatalogWheel())
        && !InventoryTextInput.active(mc.currentScreen) && !InventoryPanels.modal(mc.currentScreen) && mc.player.inventory.getCursorStack() == null;
  }
  public static Regions read(Minecraft mc) throws IOException {
    if (!eligible(mc)) throw new IOException("Open a native player inventory, chest or dispenser with an empty cursor.");
    Container handler = mc.player.container; Class<?> screen = mc.currentScreen.getClass();
    if (handler == null || ((ContainerScreen)mc.currentScreen).container != handler || !handler.method_2094(mc.player)
        || screen == class_585.class && (handler != mc.player.playerContainer || handler.getClass() != class_277.class)
        || screen == class_264.class && handler.getClass() != class_506.class
        || screen == class_235.class && handler.getClass() != class_407.class)
      throw new IOException("This container does not have a supported native layout.");
    if (handler.slots.size() < 36 || handler.slots.size() > 128 || mc.player.inventory.main.length != 36)
      throw new IOException("This inventory layout is unsupported.");
    Map<Integer, Slot> main = new TreeMap<>(), storage = new TreeMap<>(); Inventory storageOwner = null;
    Set<Integer> allMain = new HashSet<>();
    for (Object entry : handler.slots) {
      if (!(entry instanceof Slot slot)) throw new IOException("Unsupported native slot entry.");
      var access = (DeathSlotAccessor)(Object)slot;
      Inventory owner = access.power$deathInventory(); int index = access.power$deathIndex();
      if (slot.id < 0 || slot.id >= handler.slots.size() || handler.method_2084(slot.id) != slot) throw new IOException("Aliased native slot IDs.");
      if (owner == mc.player.inventory && index >= 0 && index < 36) {
        if (slot.getClass() != Slot.class || !allMain.add(index)) throw new IOException("Custom or aliased player storage slots.");
        if (index >= 9) main.put(index, slot);
      } else if (screen != class_585.class && owner != mc.player.inventory) {
        if (slot.getClass() != Slot.class || !owner(owner, screen) || index < 0 || index >= owner.size()
            || storage.put(index, slot) != null || storageOwner != null && storageOwner != owner)
          throw new IOException("Custom or aliased container storage slots.");
        storageOwner = owner;
      }
    }
    if (main.size() != 27 || allMain.size() != 36 || screen != class_585.class && (storageOwner == null || storage.size() != storageOwner.size()
        || handler.slots.size() != storage.size() + 36))
      throw new IOException("The native storage region is incomplete.");
    if (screen == class_585.class && (handler.slots.size() != 45 && handler.slots.size() != 46
        || handler.slots.size() == 46 && !(((DeathSlotAccessor)(Object)handler.method_2084(45)).power$deathInventory() instanceof OffhandSlotInventory)))
      throw new IOException("This player inventory has unsupported custom slots.");
    return new Regions(new ArrayList<>(main.values()), new ArrayList<>(storage.values()));
  }
  public static boolean owns(Minecraft mc, Regions expected) {
    try {
      Regions current = read(mc);
      return same(current.player, expected.player) && same(current.storage, expected.storage);
    } catch (IOException | RuntimeException unavailable) { return false; }
  }
  private static boolean same(List<Slot> first, List<Slot> second) {
    if (first.size() != second.size()) return false;
    for (int i = 0; i < first.size(); i++) if (first.get(i) != second.get(i)) return false;
    return true;
  }
  private static boolean owner(Inventory inventory, Class<?> screen) {
    if (inventory == null) return false;
    if (screen == class_235.class) return inventory.getClass() == class_137.class && inventory.size() == 9;
    if (inventory.getClass() == class_225.class) return inventory.size() == 27;
    if (inventory.getClass() != class_320.class || inventory.size() != 54) return false;
    var pair = (SortingDoubleChestAccessor)(Object)inventory;
    Inventory first = pair.power$sortingFirst(), second = pair.power$sortingSecond();
    return first != second && first != null && second != null && first.getClass() == class_225.class
        && second.getClass() == class_225.class && first.size() == 27 && second.size() == 27;
  }
}
