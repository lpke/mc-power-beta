package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.ConfigSession;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.tools.*;
import local.luke.power.offhand.*;
import local.luke.power.recipes.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import local.luke.power.world.WorldSnapshots;
import net.minecraft.class_277;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native input and fixed logical captures in an already muted disposable world. */
final class Polish400Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/polish-400");
  private Polish400Checks() {}
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable loaded singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute this disposable clone before checks");
    check(!NativeInventoryPlan.busy() && !local.luke.power.inventory.InventoryActions.busy() && !DetachedCamera.isActive(), "finish inventory work and exit freecam");
    check(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
        && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "release Shift and mouse buttons");
    check(Display.getWidth() >= 1280 && Display.getHeight() >= 720, "resize the disposable client to at least 1280x720");
    failures = 0; Files.createDirectories(REPORT); PowerConfig.flushPending();
    var recipeDocument = PowerConfig.section("recipeBook");
    var recipeSettings = RecipeBookConfig.copy(); var offhandSettings = OffhandConfig.copy();
    var toolSettings = InventoryToolsConfig.copy(); var rules = AccessRules.copy(mc);
    var previousScreen = mc.currentScreen; var previousContainer = mc.player.container;
    var inventory = mc.player.inventory; var main = inventory.main; var armor = inventory.armor;
    var cursor = inventory.getCursorStack(); var offhand = Offhand.get(inventory); int selected = inventory.selectedSlot;
    boolean grabbed = Mouse.isGrabbed(), focused = mc.field_2778, paused = mc.paused;
    boolean textFocused = InventoryTextInput.active(previousScreen);
    var view = mc.field_2807;
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    Class<?> creativePlayer = Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer");
    boolean flying = (Boolean)creativePlayer.getMethod("creative_isFlying").invoke(mc.player);
    Field creativeCurrent = declared(Class.forName("local.luke.power.creative.config.Config"), "settings");
    Object creativeSettings = creativeCurrent.get(null);
    Map<Field,Object> modeFields = new LinkedHashMap<>();
    for (String name : new String[]{"power$previous", "power$flightOverride", "power$spectatorSpeed", "power$restoringFlight"}) {
      Field saved = playerField(mc, name); modeFields.put(saved, saved.get(mc.player));
    }
    double vx = mc.player.velocityX, vy = mc.player.velocityY, vz = mc.player.velocityZ;
    int fire = mc.player.fire, air = mc.player.air;
    Field fall = declared(Entity.class, "field_1636"); float fallDistance = fall.getFloat(mc.player);
    boolean grounded = mc.player.field_1623;
    var screens = new ArrayList<Screen>(); var manifest = new ArrayList<String>();
    Set<Object> existingEntities = Collections.newSetFromMap(new IdentityHashMap<>()); existingEntities.addAll(mc.world.field_198);
    int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      Object creative = creativeSettings.getClass().getMethod("copy").invoke(creativeSettings);
      creative.getClass().getField("flight").setBoolean(creative, true);
      creative.getClass().getField("destroySlot").setBoolean(creative, true); creativeCurrent.set(null, creative);
      AccessRules.set(mc, local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE, true);
      var enabledHands = OffhandConfig.copy(); enabledHands.enabled = true; OffhandConfig.preview(enabledHands);
      var tools = InventoryToolsConfig.copy(); tools.sorting = tools.creativeHotbars = true; InventoryToolsConfig.preview(tools);
      inventory.main = new ItemStack[36]; inventory.armor = new ItemStack[4]; inventory.selectedSlot = 0;
      inventory.main[0] = DeathChecks.tagged(278, 1, 37, "polish-hotbar"); inventory.main[9] = new ItemStack(5, 32, 0);
      inventory.setCursorStack(null); Offhand.set(inventory, DeathChecks.tagged(35, 17, 5, "polish-offhand"));
      Mouse.setGrabbed(false); mc.field_2778 = true; mc.paused = false; mc.field_2807 = mc.player;
      for (boolean creativeMode : new boolean[]{false, true}) {
        CreativeVehicleChecks.mode(mc, creativeMode ? "CREATIVE" : "SURVIVAL");
        for (boolean survivalView : creativeMode ? new boolean[]{false, true} : new boolean[]{true})
          for (int[] size : new int[][]{{320,240},{640,360}}) {
            var recipe = RecipeBookConfig.copy(); recipe.enabled = recipe.inventory = recipe.pauseMenu = true;
            recipe.craftingOpen = false; recipe.craftingFilter = false; RecipeBookConfig.preview(recipe);
            var screen = new class_585(mc.player); screen.container = new class_277(inventory);
            mc.currentScreen = screen; mc.player.container = screen.container; screen.init(mc, size[0], size[1]); screens.add(screen);
            if (creativeMode) field(screen, "creative_normalGUI", survivalView);
            String name = (!creativeMode ? "survival" : survivalView ? "creative-survival" : "creative-catalogue") + "-" + size[0];
            capture(mc, screen, name, manifest);
            test("POLISH400 native offhand pickup/putback " + name, () -> {
              var slot = screen.container.method_2084(45);
              check(slot instanceof OffhandNativeSlot && slot.x == -17 && slot.y == 142, "offhand native slot is not beside the hotbar");
              if (creativeMode) check((int)call(screen, "power$destroyY") == 120, "Creative destroy slot is not above offhand");
              int x = (screen.width - 176) / 2 + InventoryPanels.offset(screen) + slot.x + 8;
              int y = (screen.height - 166) / 2 + slot.y + 8;
              String before = DeathItems.encode(Offhand.get(inventory));
              try (var scope=OffhandSlotClick.begin(mc,screen,slot,x,y,0)) {
                check(OffhandSlotClick.nativeSlot(mc,-999)==slot.id,"native outside click did not admit the side slot");
                check(OffhandSlotClick.nativeSlot(mc,36)==36,"side-slot scope redirected another slot");
              }
              check(OffhandSlotClick.nativeSlot(mc,-999)==-999,"side-slot scope leaked after press");
              var input = (ScreenInput)(Object)screen;
              input.power$click(x,y,0); input.power$release(x,y,0);
              check(Offhand.get(inventory) == null && inventory.getCursorStack() != null
                  && before.equals(DeathItems.encode(inventory.getCursorStack())), "offhand click dropped or changed native item data");
              input.power$click(x,y,0); input.power$release(x,y,0);
              check(inventory.getCursorStack() == null && before.equals(DeathItems.encode(Offhand.get(inventory))), "offhand putback changed count/data or cursor");
              try {
                InventoryWheel.event(screen,true,120,false); input.power$release(x,y,-1);
                check(inventory.getCursorStack()!=null && inventory.getCursorStack().count==1
                    && Offhand.get(inventory).count==16,"wheel did not take one item from offhand");
                InventoryWheel.event(screen,true,-120,false); input.power$release(x,y,-1);
                check(inventory.getCursorStack()==null && before.equals(DeathItems.encode(Offhand.get(inventory))),
                    "wheel putback changed native data or counts");
                check(InventoryWheel.take(screen,true)==0,"offhand wheel leaked to the Creative catalogue");
              } finally {InventoryWheel.clear(); inventory.setCursorStack(null); Offhand.set(inventory,DeathItems.decode(before));}

            });
            if (!creativeMode) test("POLISH400 native recipe toggle " + size[0], () -> {
              var book = RecipeBooks.book(screen); check(book != null && book.enabled() && !book.layout().open(), "recipe book did not start closed");
              var toggle = book.layout().toggle(false, 2); var input = (ScreenInput)(Object)screen;
              input.power$click(toggle.x()+8,toggle.y()+8,0); input.power$release(toggle.x()+8,toggle.y()+8,0);
              check(book.layout().open(), "native recipe button did not open its panel"); warm(book);
              if (size[0] == 640) capture(mc, screen, "survival-book-open-640", manifest);
            });
            var book = RecipeBooks.book(screen); if (book != null) book.removed(); InventoryPanels.remove(screen);
          }
      }
      CreativeVehicleChecks.mode(mc, "SURVIVAL"); mc.player.container = previousContainer;
      for (int[] size : new int[][]{{320,240},{640,360}}) {
        var viewer = new RecipeBookViewerScreen(previousScreen); mc.currentScreen = viewer;
        viewer.init(mc,size[0],size[1]); screens.add(viewer); var book = RecipeBooks.book(viewer); warm(book);
        @SuppressWarnings("unchecked") var recipes = (List<RecipeDefinition>)field(field(book,"model"),"recipes");
        var detail = recipes.stream().filter(r -> r.kind == RecipeDefinition.Kind.CRAFTING).findFirst().orElseThrow();
        field(book,"detail",detail); book.tick();
        check(book.layout().detail().x() < book.layout().background().x() + book.layout().background().width(), "viewer panes are detached");
        capture(mc,viewer,"pause-viewer-detail-"+size[0],manifest); book.removed(); InventoryPanels.remove(viewer);
        var session = new ConfigSession(); InventoryToolsBackend.register(session);
        var editor = new SortingPreferencesScreen(previousScreen,session,() -> {}); mc.currentScreen = editor;
        editor.init(mc,size[0],size[1]); screens.add(editor); capture(mc,editor,"sorting-layout-"+size[0],manifest);
        check(session.changes() == 0, "sorting layout rendering published a draft");
      }
      for (var action : new WorldSnapshots.Action[]{WorldSnapshots.Action.BACKUP,WorldSnapshots.Action.DUPLICATE}) {
        var operation = new WorldOperationScreen(previousScreen,"polish-fixture","Polish fixture",action);
        mc.currentScreen = operation; operation.init(mc,320,240); screens.add(operation);
        capture(mc,operation,"world-"+action.name().toLowerCase(Locale.ROOT)+"-320",manifest);
        check(field(operation,"task") == null, "rendering started a world operation");
      }
      check(!NativeInventoryPlan.busy(), "visual/input fixture queued an inventory plan");
    } finally {
      try {
      for (Screen screen : screens) { var book = RecipeBooks.book(screen); if (book != null) book.removed(); InventoryPanels.remove(screen); }
      InventoryWheel.clear(); InventoryTextInput.clear();
      mc.currentScreen = previousScreen; mc.player.container = previousContainer;
      inventory.main = main; inventory.armor = armor; inventory.selectedSlot = selected;
      inventory.setCursorStack(cursor); Offhand.set(inventory,offhand);
      RecipeBookConfig.preview(recipeSettings); OffhandConfig.preview(offhandSettings); InventoryToolsConfig.preview(toolSettings);
      PowerConfig.saveDeferred("recipeBook",recipeDocument); PowerConfig.flushPending();
      } finally {
      try {
        CreativeVehicleChecks.mode(mc,mode);
        playerField(mc,"power$restoringFlight").setBoolean(mc.player,true);
        creativePlayer.getMethod("creative_setFlying",boolean.class).invoke(mc.player,flying);
      } finally {
        creativeCurrent.set(null,creativeSettings); for (var saved : modeFields.entrySet()) saved.getKey().set(mc.player,saved.getValue());
        AccessRules.restore(mc,rules); mc.player.velocityX=vx; mc.player.velocityY=vy; mc.player.velocityZ=vz;
        mc.player.fire=fire; mc.player.air=air; fall.setFloat(mc.player,fallDistance); mc.player.field_1623=grounded;
        mc.field_2807=view; mc.field_2778=focused; mc.paused=paused; Mouse.setGrabbed(grabbed);
        if(textFocused)InventoryTextInput.focus(previousScreen,true);
        for (Object entity : new ArrayList<>(mc.world.field_198))
          if (entity instanceof net.minecraft.class_142 item && !existingEntities.contains(entity)) item.markDead();
        Keyboard.enableRepeatEvents(false);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
        GL11.glPopAttrib(); GL11.glMatrixMode(matrixMode);
      }
      }
    }
    Files.write(REPORT.resolve("captures.txt"),manifest);
    check(inventory.main==main && inventory.armor==armor && inventory.getCursorStack()==cursor && Offhand.get(inventory)==offhand, "fixture failed to restore original inventory owners");
    check(PowerConfig.section("recipeBook").equals(recipeDocument),"fixture failed to restore recipe settings");
    log("POLISH400 failures="+failures+" captures="+manifest.size()+" directory="+REPORT);
  }
  private static void warm(RecipeBookOverlay book) throws Exception {
    for(int tick=0;tick<720;tick++) { book.tick(); var model=field(book,"model");
      if(model!=null && (int)field(model,"cursor")>=((List<?>)field(model,"recipes")).size())break;
    }
    field(book,"ticks",0);
  }
  private static void capture(Minecraft mc,Screen screen,String name,List<String> manifest)throws Exception {
    SettingsSnapshot.render(mc,screen,screen.width,screen.height,REPORT.resolve(name+".png"));
    manifest.add(name+".png logical="+screen.width+"x"+screen.height);
  }
  private static Field declared(Class<?> owner,String name)throws Exception {var result=owner.getDeclaredField(name);result.setAccessible(true);return result;}
  private static Field playerField(Minecraft mc,String name)throws Exception {
    for(Class<?> type=mc.player.getClass();type!=null;type=type.getSuperclass())try{return declared(type,name);}catch(NoSuchFieldException absent){}
    throw new NoSuchFieldException(name);
  }
}
