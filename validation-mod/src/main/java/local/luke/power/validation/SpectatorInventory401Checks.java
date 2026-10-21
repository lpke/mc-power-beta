package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.mixin.DeathCraftingAccessor;
import local.luke.power.offhand.*;
import local.luke.power.rules.GameRule;
import local.luke.power.validation.mixin.Client253Access;
import net.minecraft.class_277;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the existing native player container, only in an already muted disposable world. */
final class SpectatorInventory401Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/spectator-inventory-401");
  private SpectatorInventory401Checks() {}

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone before checks");
    check(mc.player.health > 0 && !mc.player.dead && mc.player.field_1595 == null, "use a living, unmounted player");
    check(!NativeInventoryPlan.busy() && !local.luke.power.inventory.InventoryActions.busy() && !DetachedCamera.isActive(), "finish inventory work and exit freecam");
    check(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
        && !Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "release modifiers and mouse buttons");
    check(Display.getWidth() >= 1280 && Display.getHeight() >= 720, "resize the disposable client to at least 1280x720");
    failures = 0; Files.createDirectories(REPORT);
    var player = mc.player; var inventory = player.inventory; var nativeContainer = player.playerContainer;
    check(nativeContainer.getClass() == class_277.class, "expected the native player container");
    var grid = ((DeathCraftingAccessor)(Object)((class_277)nativeContainer).field_1124).power$deathStacks();
    ItemStack[] main = inventory.main, armor = inventory.armor, inputs = grid.clone();
    ItemStack cursor = inventory.getCursorStack(), offhand = Offhand.get(inventory), output = nativeContainer.method_2084(0).getStack();
    int selected = inventory.selectedSlot, health = player.health, fire = player.fire, air = player.air;
    var previousScreen = mc.currentScreen; var previousContainer = player.container; var view = mc.field_2807; var target = mc.field_2823;
    boolean grabbed = Mouse.isGrabbed(), focused = mc.field_2778, paused = mc.paused, grounded = player.field_1623;
    boolean textFocused = InventoryTextInput.active(previousScreen), repeatEvents = Keyboard.areRepeatEventsEnabled();
    double vx = player.velocityX, vy = player.velocityY, vz = player.velocityZ;
    var fall = declared(net.minecraft.entity.Entity.class, "field_1636"); float fallDistance = fall.getFloat(player);
    String mode = mode(mc); var rules = AccessRules.copy(mc); var handSettings = OffhandConfig.copy();
    Class<?> creativePlayer = Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer");
    boolean flying = (Boolean)creativePlayer.getMethod("creative_isFlying").invoke(player);
    Field creativeCurrent = declared(Class.forName("local.luke.power.creative.config.Config"), "settings");
    Object creativeSettings = creativeCurrent.get(null);
    Map<Field,Object> modeFields = new LinkedHashMap<>();
    for (String name : new String[]{"power$previous", "power$flightOverride", "power$spectatorSpeed", "power$restoringFlight"}) {
      Field saved = member(player.getClass(), name); modeFields.put(saved, saved.get(player));
    }
    Set<Object> entities = Collections.newSetFromMap(new IdentityHashMap<>()); entities.addAll(mc.world.field_198);
    var screens = new ArrayList<class_585>();
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      Object creative = creativeSettings.getClass().getMethod("copy").invoke(creativeSettings);
      creative.getClass().getField("destroySlot").setBoolean(creative, true); creativeCurrent.set(null, creative);
      var hands = OffhandConfig.copy(); hands.enabled = true; OffhandConfig.preview(hands);
      inventory.main = new ItemStack[36]; inventory.armor = new ItemStack[4]; inventory.selectedSlot = 0;
      Arrays.fill(grid, null); nativeContainer.method_2084(0).setStack(null); inventory.setCursorStack(null); Offhand.set(inventory, null);
      mc.field_2807 = player; mc.field_2778 = true; mc.paused = false; Mouse.setGrabbed(false);
      AccessRules.set(mc, GameRule.ALLOW_SPECTATOR_MODE, true);
      CreativeVehicleChecks.mode(mc, "SPECTATOR");
      for (boolean allowed : new boolean[]{true, false}) {
        AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, allowed);
        var screen = new class_585(player); screen.container = nativeContainer;
        mc.currentScreen = screen; player.container = nativeContainer; screen.init(mc, 640, 360); screens.add(screen);
        field(screen, "creative_normalGUI", false);
        test("SPECTATOR401 own native inventory capability creative=" + allowed, () -> {
          var state = (PlayerMode)player;
          check(state.power$spectator() && state.power$inventoryEditable() && !state.power$creative(), "inventory access changed actual mode");
          check(state.power$creativeInventory() == allowed, "Creative inventory ignored its permission");
          check(((InventoryGui)(Object)screen).power$usesCatalogWheel() == allowed, "catalog visibility ignored its permission");
          var handler = player.container; player.container = null;
          try { check(!state.power$inventoryEditable() && !state.power$creativeInventory(), "mismatched native container admitted"); }
          finally { player.container = handler; }
        });
        test("SPECTATOR401 native stack movement preserves count damage NBT creative=" + allowed, () -> {
          clear(mc, grid); inventory.main[9] = DeathChecks.tagged(35, 17, 5, "spectator-wool");
          String bytes = bytes(inventory.main[9]); click(mc, 9); click(mc, 10);
          check(inventory.main[9] == null && bytes.equals(bytes(inventory.main[10])) && inventory.getCursorStack() == null, "native movement changed wool count/data");
          inventory.main[9] = DeathChecks.tagged(267, 1, 73, "spectator-tool"); bytes = bytes(inventory.main[9]);
          click(mc, 9); click(mc, 11);
          check(inventory.main[9] == null && bytes.equals(bytes(inventory.main[11])), "native movement changed tool damage/NBT");
        });
        test("SPECTATOR401 native offhand movement creative=" + allowed, () -> {
          clear(mc, grid); inventory.main[9] = DeathChecks.tagged(35, 13, 5, "spectator-hand");
          String bytes = bytes(inventory.main[9]); var slot = nativeContainer.method_2084(45);
          check(slot instanceof OffhandNativeSlot && slot.canInsert(inventory.main[9]), "own inventory offhand rejected insertion");
          click(mc, 9); click(mc, 45);
          check(inventory.main[9] == null && inventory.getCursorStack() == null && bytes.equals(bytes(Offhand.get(inventory))), "offhand insertion changed native data");
          click(mc, 45); click(mc, 10);
          check(Offhand.get(inventory) == null && bytes.equals(bytes(inventory.main[10])), "offhand extraction changed native data");
        });
        test("SPECTATOR401 native 2x2 crafting creative=" + allowed, () -> {
          clear(mc, grid); inventory.main[9] = new ItemStack(17, 1, 0); click(mc, 9); click(mc, 1);
          ItemStack result = nativeContainer.method_2084(0).getStack();
          check(result != null && result.itemId == 5 && result.count == 4, "native 2x2 recipe did not produce planks");
          click(mc, 0); click(mc, 9);
          check(nativeContainer.method_2084(1).getStack() == null && nativeContainer.method_2084(0).getStack() == null
              && inventory.main[9] != null && inventory.main[9].itemId == 5 && inventory.main[9].count == 4, "crafting consumed wrong inputs or output");
        });
        test("SPECTATOR401 open inventory native drops creative=" + allowed, () -> {
          clear(mc, grid); ItemStack carried = DeathChecks.tagged(35, 3, 5, "spectator-outside");
          String bytes = bytes(carried); inventory.setCursorStack(carried); Set<Object> before = entitySet(mc);
          click(mc, -999);
          check(inventory.getCursorStack() == null && newDrops(mc, before).size() == 1
              && bytes.equals(bytes(newDrops(mc, before).get(0).field_564)), "outside drop changed full native stack");
          inventory.main[0] = DeathChecks.tagged(35, 2, 5, "spectator-selected");
          ItemStack single = DeathItems.decode(bytes(inventory.main[0])); single.count = 1; before = entitySet(mc); player.dropSelectedItem();
          check(bytes(single).equals(bytes(inventory.main[0])) && newDrops(mc, before).size() == 1
              && bytes(single).equals(bytes(newDrops(mc, before).get(0).field_564)), "open selected-item drop changed native data/count");
        });
        test("SPECTATOR401 catalog and destroy permission creative=" + allowed, () -> {
          clear(mc, grid); Class<?> actions = Class.forName("local.luke.power.creative.InventoryActions");
          boolean supplied = (Boolean)actions.getMethod("catalogueStack", net.minecraft.entity.player.PlayerEntity.class, ItemStack.class)
              .invoke(null, player, new ItemStack(4, 1, 0));
          check(supplied == allowed, "catalog action ignored Creative permission");
          check(allowed ? inventory.main[0] != null && inventory.main[0].count == 64 : inventory.main[0] == null, "catalog count or denied action changed inventory");
          inventory.setCursorStack(DeathChecks.tagged(35, 7, 5, "spectator-destroy")); String before = bytes(inventory.getCursorStack());
          boolean destroyed = (Boolean)actions.getMethod("destroy", net.minecraft.entity.player.PlayerEntity.class, boolean.class).invoke(null, player, false);
          check(destroyed == allowed && (allowed ? inventory.getCursorStack() == null : before.equals(bytes(inventory.getCursorStack()))), "destroy ignored permission or changed denied cursor");
        });
        test("SPECTATOR401 world interactions and closed inventory drops creative=" + allowed, () -> {
          clear(mc, grid); inventory.main[0] = DeathChecks.tagged(260, 3, 0, "spectator-food"); player.health = 10;
          String before = bytes(inventory.main[0]); Set<Object> beforeEntities = entitySet(mc); mc.field_2823 = null;
          for (int button : new int[]{0, 1}) {
            CallbackInfo callback = new CallbackInfo("spectator fixture", true);
            hook(mc, "power$interact", int.class, CallbackInfo.class).invoke(mc, button, callback);
            check(callback.isCancelled(), "Spectator native interaction hook did not cancel button " + button);
            ((Client253Access)(Object)mc).power$use(button);
          }
          check(before.equals(bytes(inventory.main[0])) && player.health == 10, "world attack/use consumed an item or healed player");
          mc.currentScreen = null;
          try {
            check(!((PlayerMode)player).power$inventoryEditable() && !((PlayerMode)player).power$creativeInventory(), "closed inventory retained inventory capabilities");
            check(!nativeContainer.method_2084(45).canInsert(inventory.main[0]), "closed Spectator offhand admitted insertion");
            Method keys = hook(mc, "power$keys", int.class);
            check((int)keys.invoke(mc, mc.options.dropKey.code) == 0, "closed Spectator drop key escaped filter");
            check((int)keys.invoke(mc, mc.options.inventoryKey.code) == mc.options.inventoryKey.code,
                "Spectator inventory key was blocked");
            player.dropSelectedItem();
            check(before.equals(bytes(inventory.main[0])) && inventory.getCursorStack() == null && newDrops(mc, beforeEntities).isEmpty(), "closed native drop changed inventory or created entities");
          } finally { mc.currentScreen = screen; player.health = health; }
          check(mode(mc).equals("SPECTATOR"), "inventory actions changed actual game mode");
        });
        clear(mc, grid); inventory.main[0] = DeathChecks.tagged(267, 1, 73, "spectator-view"); Offhand.set(inventory, new ItemStack(35, 13, 5));
        test("SPECTATOR401 capture creative=" + allowed, () -> {
          field(screen, "creative_normalGUI", false); capture(mc, screen, allowed ? "creative-view" : "permission-disabled");
          if (allowed) { field(screen, "creative_normalGUI", true); capture(mc, screen, "survival-view"); }
        });
        test("SPECTATOR401 live Creative permission changes keep own inventory and Spectator", () -> {
          field(screen, "creative_normalGUI", false); String before = bytes(inventory.main[0]);
          for (boolean next : new boolean[]{true, false, true, allowed}) {
            AccessRules.set(mc, GameRule.ALLOW_CREATIVE_MODE, next);
            check(((PlayerMode)player).power$inventoryEditable() && ((PlayerMode)player).power$creativeInventory() == next
                && ((InventoryGui)(Object)screen).power$usesCatalogWheel() == next, "live permission change left stale inventory capabilities");
            check(mode(mc).equals("SPECTATOR") && before.equals(bytes(inventory.main[0])), "permission change altered mode or held inventory");
          }
        });
      }
    } finally {
      try {
        for (var screen : screens) { var book = local.luke.power.recipes.RecipeBooks.book(screen); if (book != null) book.removed(); InventoryPanels.remove(screen); }
        InventoryWheel.clear(); InventoryTextInput.clear(); inventory.main = main; inventory.armor = armor; inventory.selectedSlot = selected;
        System.arraycopy(inputs, 0, grid, 0, inputs.length); nativeContainer.method_2084(0).setStack(output);
        inventory.setCursorStack(cursor); Offhand.set(inventory, offhand); OffhandConfig.preview(handSettings);
        AccessRules.restore(mc, rules); CreativeVehicleChecks.mode(mc, mode);
        member(player.getClass(), "power$restoringFlight").setBoolean(player, true);
        creativePlayer.getMethod("creative_setFlying", boolean.class).invoke(player, flying);
      } finally {
        creativeCurrent.set(null, creativeSettings); for (var saved : modeFields.entrySet()) saved.getKey().set(player, saved.getValue());
        player.health = health; player.fire = fire; player.air = air; player.velocityX = vx; player.velocityY = vy; player.velocityZ = vz;
        player.field_1623 = grounded; fall.setFloat(player, fallDistance);
        mc.currentScreen = previousScreen; player.container = previousContainer; mc.field_2807 = view; mc.field_2823 = target;
        mc.field_2778 = focused; mc.paused = paused; Mouse.setGrabbed(grabbed); if (textFocused) InventoryTextInput.focus(previousScreen, true);
        Keyboard.enableRepeatEvents(repeatEvents);
        for (var drop : newDrops(mc, entities)) drop.markDead();
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glPopAttrib(); GL11.glMatrixMode(matrixMode);
      }
    }
    check(inventory.main == main && inventory.armor == armor && inventory.getCursorStack() == cursor && Offhand.get(inventory) == offhand
        && player.playerContainer == nativeContainer && mc.currentScreen == previousScreen && player.container == previousContainer, "fixture did not restore inventory/screen owners");
    log("SPECTATOR401 failures=" + failures + " captures=" + REPORT);
    check(failures == 0, "Spectator inventory regression failures=" + failures);
  }

  private static void clear(Minecraft mc, ItemStack[] grid) {
    Arrays.fill(mc.player.inventory.main, null); Arrays.fill(mc.player.inventory.armor, null); Arrays.fill(grid, null);
    mc.player.playerContainer.method_2084(0).setStack(null); mc.player.inventory.setCursorStack(null); Offhand.set(mc.player.inventory, null);
  }
  private static void click(Minecraft mc, int slot) { mc.interactionManager.clickSlot(mc.player.playerContainer.syncId, slot, 0, false, mc.player); }
  private static String bytes(ItemStack stack) throws Exception { return stack == null ? null : DeathItems.encode(stack); }
  private static String mode(Minecraft mc) throws Exception { return Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString(); }
  private static Set<Object> entitySet(Minecraft mc) { Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>()); result.addAll(mc.world.field_198); return result; }
  private static List<net.minecraft.class_142> newDrops(Minecraft mc, Set<Object> previous) {
    var result = new ArrayList<net.minecraft.class_142>();
    for (Object entity : mc.world.field_198) if (entity instanceof net.minecraft.class_142 drop && !previous.contains(entity) && !drop.dead) result.add(drop);
    return result;
  }
  private static void capture(Minecraft mc, class_585 screen, String name) throws Exception { SettingsSnapshot.render(mc, screen, 640, 360, REPORT.resolve(name + ".png")); }
  private static Field declared(Class<?> type, String name) throws Exception { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
  private static Method hook(Object owner, String name, Class<?>... parameters) throws Exception {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass())
      for (Method method : type.getDeclaredMethods())
        if (method.getName().contains(name) && Arrays.equals(method.getParameterTypes(), parameters)) { method.setAccessible(true); return method; }
    throw new NoSuchMethodException(name);
  }
  private static Field member(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try { return declared(type, name); } catch (NoSuchFieldException absent) {}
    throw new NoSuchFieldException(name);
  }
}
