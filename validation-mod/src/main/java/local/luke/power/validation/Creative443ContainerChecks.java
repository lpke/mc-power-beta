package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.mixin.DeathSlotAccessor;
import local.luke.power.offhand.*;
import local.luke.power.validation.mixin.ContainerInput;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

/** Actual native-container tooltip rendering, with physical Shift and isolated player items. */
final class Creative443ContainerChecks {
  private static final Path REPORT = Path.of("power-beta-data/reports/creative-443-containers");
  private static Inventory442Checks.State staged;
  private static TestInventory stagedChest;
  private static String stagedExternal;
  private static local.luke.power.building.config.Settings stagedBuilding;
  private record Host(String name, ContainerScreen screen) {}
  private Creative443ContainerChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("stop")) {
      if (staged != null) { staged.owner(mc); closeStaged(); }
      log("CREATIVE443 CONTAINERS stopped; original settings/inventory/mode restored"); return;
    }
    if (action.equals("prepare")) { prepare(mc); return; }
    if (action.equals("check")) {
      check(staged != null, "prepare the native chest clear check first"); staged.owner(mc);
      check(!Mouse.isButtonDown(0) && !NativeInventoryPlan.busy() && !NativeInventoryPlan.failed(), "release the pointer and wait for durable publication");
      check(mc.currentScreen == staged.screen, "keep the prepared chest open");
      check(Arrays.stream(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)).allMatch(Objects::isNull)
          && mc.player.playerContainer.method_2084(0).getStack() == null, "native Shift-click did not clear every owned player slot");
      check(stagedExternal.equals(inventory(stagedChest)), "native Shift-click changed external chest items");
      log("PASS CREATIVE443 CONTAINERS physical native Shift-click cleared player items only; chest and full native item data retained"); return;
    }
    check(action.isEmpty() || action.equals("run"), "use creative-443-containers run|prepare|check|stop");
    check(staged == null, "stop the prepared physical check before rendering tests"); guard(mc);
    check(shift(), "hold physical Left Shift or Right Shift during creative-443-containers run");
    failures = 0; Files.createDirectories(REPORT);
    byte[] disk = Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
    boolean capture = Release173Checks.capture;
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try (var state = new Inventory442Checks.State(mc)) {
      state.reset(true, false); fillPlayer(mc);
      var chest = new TestInventory(27); var second = new TestInventory(27);
      chest.setStack(0, DeathChecks.tagged(35, 11, 5, "tooltip-external-first"));
      second.setStack(26, DeathChecks.tagged(267, 1, 71, "tooltip-external-second"));
      int[] dispenser = state.block(23), furnace = state.block(61), table = state.block(58);
      List<Host> hosts = List.of(new Host("chest", new class_264(mc.player.inventory, chest)),
          new Host("double-chest", new class_264(mc.player.inventory, new class_320("Creative443 double", chest, second))),
          new Host("dispenser", new class_235(mc.player.inventory, (class_137)mc.world.method_1777(dispenser[0], dispenser[1], dispenser[2]))),
          new Host("furnace", new class_524(mc.player.inventory, (class_138)mc.world.method_1777(furnace[0], furnace[1], furnace[2]))),
          new Host("crafting-table", new class_516(mc.player.inventory, mc.world, table[0], table[1], table[2])));
      for (Host host : hosts) {
        state.open(host.screen, false); Slot external = seedExternal(mc, host.screen);
        try {
          for (boolean hand : new boolean[]{false, true}) for (boolean clear : new boolean[]{false, true}) for (int width : new int[]{320, 640}) {
            String name = host.name + "-" + width + "-hand-" + hand + "-clear-" + clear;
            test("CREATIVE443 CONTAINERS Shift hint " + name, () -> {
              check(shift(), "physical Shift released during native tooltip checks");
              var offhand = OffhandConfig.copy(); offhand.enabled = hand; OffhandConfig.preview(offhand);
              Offhand.set(mc.player.inventory, hand ? DeathChecks.tagged(35, 11, 5, "tooltip-offhand") : null);
              state.clear.setBoolean(state.creative, clear); state.destroy.setBoolean(state.creative, true);
              int height = width == 320 ? 240 : 360;
              host.screen.init(mc, width, height);
              int[] point = destroyPoint(mc, host.screen, hand);
              check(((CreativeDeletion.Host)host.screen).power$deleteSlotAt(point[0], point[1]), "actual delete hit region missing");
              String before = player(mc), slots = slots(host.screen), cursor = bytes(mc.player.inventory.getCursorStack());
              resetCapture();
              capture(mc, host.screen, width, height, REPORT.resolve(name + ".png"), point[0], point[1]);
              check(Release173Checks.lastItem > 0 && Release173Checks.tooltipCount == (clear ? 1 : 0), "wrong actual Clear inventory tooltip count " + Release173Checks.tooltipCount);
              if (clear) check(Release173Checks.tooltip > Release173Checks.lastItem && !Release173Checks.tooltipDepth && !Release173Checks.tooltipLighting,
                  "tooltip did not draw once after all native items with depth/lighting disabled");
              check(GL11.glIsEnabled(GL11.GL_DEPTH_TEST) && GL11.glIsEnabled(GL11.GL_LIGHTING), "tooltip changed native final depth/lighting state");
              check(before.equals(player(mc)) && slots.equals(slots(host.screen)) && Objects.equals(cursor, bytes(mc.player.inventory.getCursorStack())),
                  "tooltip rendering changed player, cursor, native inputs/output or external items");
              resetCapture();
              draw(mc, host.screen, width, height, point[0] - 30, point[1]);
              check(Release173Checks.tooltipCount == 0, "Clear inventory hint leaked outside the destroy slot");
              if (hand) {
                resetCapture(); draw(mc, host.screen, width, height, point[0], point[1] + 22);
                check(Release173Checks.tooltipCount == 0, "offhand hover acquired the destroy hint");
              }
              check(before.equals(player(mc)) && slots.equals(slots(host.screen)), "unrelated hover changed native item data");
            });
          }
        } finally { if (external != null) external.setStack(null); }
      }
    } finally {
      Release173Checks.capture = capture;
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(matrixMode);
    }
    check(Arrays.equals(disk, Files.readAllBytes(local.luke.power.storage.PowerConfig.path())), "native tooltip checks changed saved configuration");
    log("CREATIVE443 CONTAINERS failures=" + failures + " captures=" + REPORT);
    check(failures == 0, "native Creative tooltip failures=" + failures);
  }

  private static void prepare(Minecraft mc) throws Exception {
    guard(mc); check(staged == null, "stop the previous physical container check");
    staged = new Inventory442Checks.State(mc);
    stagedBuilding = local.luke.power.building.config.Config.current().copy();
    try {
      staged.reset(true, true); fillPlayer(mc);
      // This isolated inventory has no world position. Avoid reach-based closure
      // without placing a block or changing the player's physical simulation.
      var building = stagedBuilding.copy(); building.inventoryWhileMoving = false;
      local.luke.power.building.config.Config.preview(building);
      stagedChest = new TestInventory(27); stagedChest.setStack(0, DeathChecks.tagged(35, 11, 5, "physical-external"));
      staged.open(new class_264(mc.player.inventory, stagedChest), true); stagedExternal = inventory(stagedChest);
      int[] point = destroyPoint(mc, staged.screen, true);
      int px = (int)((point[0] + .5) * mc.displayWidth / staged.screen.width);
      int py = (int)((point[1] + .5) * mc.displayHeight / staged.screen.height);
      log("CREATIVE443 CONTAINERS ACTION hold physical Shift and click left at client=" + px + "," + py
          + " GUI=" + point[0] + "," + point[1] + "; release; wait for publication; check; stop");
    } catch (Exception | Error failure) {
      try { closeStaged(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
      throw failure;
    }
  }
  private static void closeStaged() throws Exception {
    try { staged.close(); }
    finally {
      try { if (stagedBuilding != null) local.luke.power.building.config.Config.preview(stagedBuilding); }
      finally { staged = null; stagedChest = null; stagedExternal = null; stagedBuilding = null; }
    }
  }
  private static void guard(Minecraft mc) {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone");
    check(!NativeInventoryPlan.busy() && !NativeInventoryPlan.failed() && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "finish inventory work and release mouse");
  }
  private static boolean shift() { return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT); }
  private static void fillPlayer(Minecraft mc) throws Exception {
    mc.player.inventory.main[9] = DeathChecks.tagged(35, 17, 4, "tooltip-player");
    mc.player.inventory.armor[0] = DeathChecks.tagged(298, 1, 17, "tooltip-equipment");
    mc.player.inventory.setCursorStack(DeathChecks.tagged(267, 1, 71, "tooltip-cursor"));
    Offhand.set(mc.player.inventory, DeathChecks.tagged(35, 11, 5, "tooltip-offhand"));
    mc.player.playerContainer.method_2084(1).setStack(new ItemStack(17, 3, 0));
    check(mc.player.playerContainer.method_2084(0).getStack() != null, "personal recipe output missing from ownership snapshot");
  }
  private static Slot seedExternal(Minecraft mc, ContainerScreen screen) throws Exception {
    if (screen.getClass() == class_264.class) return null;
    for (Object value : screen.container.slots) {
      Slot slot = (Slot)value; var access = (DeathSlotAccessor)(Object)slot;
      if (access.power$deathInventory() != mc.player.inventory && !(access.power$deathInventory() instanceof OffhandSlotInventory)
          && slot.canInsert(new ItemStack(17, 1, 0))) {
        slot.setStack(DeathChecks.tagged(17, 3, 0, "tooltip-native-input")); return slot;
      }
    }
    throw new AssertionError("native container has no external input slot");
  }
  private static int[] destroyPoint(Minecraft mc, ContainerScreen screen, boolean hand) {
    var bounds = (ContainerInput)(Object)screen;
    int x = (screen.width - bounds.power$width()) / 2 + InventoryPanels.offset(screen);
    int y = (screen.height - bounds.power$height()) / 2;
    return new int[]{x + InventorySideSlots.SLOT_X + 8,
        y + OffhandContainers.hotbarY(screen.container, mc.player.inventory) - (hand ? 22 : 0) + 8};
  }
  private static void resetCapture() {
    Release173Checks.capture = true;
    Release173Checks.event = Release173Checks.lastItem = Release173Checks.tooltip = Release173Checks.tooltipCount = 0;
    Release173Checks.tooltipDepth = Release173Checks.tooltipLighting = true;
  }
  private static void capture(Minecraft mc, ContainerScreen screen, int width, int height, Path file, int x, int y) throws Exception {
    SettingsSnapshot.render(mc, screen, width, height, file, x, y);
  }
  private static void draw(Minecraft mc, ContainerScreen screen, int width, int height, int x, int y) {
    GL11.glViewport(0, 0, width * 2, height * 2); screen.render(x, y, 0);
    check(GL11.glGetError() == GL11.GL_NO_ERROR, "native container hover OpenGL error");
  }
  private static String bytes(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
  private static String player(Minecraft mc) throws Exception { return DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer)) + ":" + bytes(mc.player.playerContainer.method_2084(0).getStack()); }
  private static String slots(ContainerScreen screen) throws Exception {
    var values = new ArrayList<String>(); for (Object value : screen.container.slots) values.add(bytes(((Slot)value).getStack())); return values.toString();
  }
  private static String inventory(Inventory inventory) throws Exception {
    var values = new ArrayList<String>(); for (int i = 0; i < inventory.size(); i++) values.add(bytes(inventory.getStack(i))); return values.toString();
  }
  private static final class TestInventory implements Inventory {
    final ItemStack[] items; TestInventory(int size) { items = new ItemStack[size]; }
    public int size() { return items.length; } public ItemStack getStack(int index) { return items[index]; }
    public ItemStack removeStack(int index, int count) { var item = items[index]; if (item == null) return null; if (item.count <= count) { items[index] = null; return item; } return item.split(count); }
    public void setStack(int index, ItemStack stack) { items[index] = stack; } public String getName() { return "Creative443"; }
    public int getMaxCountPerStack() { return 64; } public void markDirty() {} public boolean canPlayerUse(PlayerEntity player) { return true; }
  }
}
