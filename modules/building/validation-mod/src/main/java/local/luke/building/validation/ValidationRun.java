package local.luke.building.validation;

import java.nio.file.*;
import java.util.*;
import local.luke.building.validation.mixin.ClickInvoker;
import local.luke.power.fakesneak.FakeSneak;
import local.luke.power.fastplace.FastPlace;
import local.luke.power.flexible.*;
import local.luke.power.slabplacement.SlabPlacement;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Development-only integration tests. Install only in a disposable clone. */
public final class ValidationRun {
  private static final String WORLD =
      "Building Laboratory " + Long.toString(System.currentTimeMillis(), 36);
  private static final Path REPORT = Path.of("building-validation.log"),
      COMMAND = Path.of("building-validation.command");
  private static int stage = -1, failures, ticks;
  private static boolean done;
  private static int soakTicks, soakPlaced;
  private static boolean soak;
  private static long started;
  private static final String[] CASES = {
    "normal native placement",
    "offset on all six faces",
    "adjacent bridge from above",
    "center offset leaves a gap",
    "reverse all piston directions",
    "rotate pistons to selected edge",
    "into-face and reverse combination",
    "reverse horizontal blocks and repeaters",
    "reject impossible vertical furnace rotation",
    "camera unchanged by rotation",
    "native chest interaction without modifiers",
    "place against chest with modifier",
    "container placement setting",
    "collision rejection",
    "height and coordinate bounds",
    "reach and entity occlusion",
    "non-block tools unaffected",
    "menu focus and world interruptions",
    "singleplayer only",
    "Freecam suspension",
    "keyboard toggle edges",
    "native controls registration",
    "config screen cancel and save",
    "overlay GL state restoration",
    "adjacent slab completion every face and variant",
    "mismatched slab variants stay separate",
    "slab merge collision",
    "survival slab consumption",
    "creative slab preservation",
    "slab toggle off",
    "native top slab completion",
    "Fast Place slab side continuation",
    "Fast Place match-first suppresses held completion",
    "Fast Place continuous slab completion",
    "Fast Place tracks adjacent merge",
    "Fast Place tracks flexible destination",
    "Fast Place reverse orientation",
    "Fake Sneak full speed and no sneak state",
    "Fake Sneak stops cardinal edges",
    "Fake Sneak stops diagonal holes",
    "Fake Sneak disabled restores walking off",
    "Fake Sneak jumping and airborne motion",
    "Fake Sneak flight and riding",
    "Fake Sneak multiplayer and other entities",
    "Fake Sneak toggle input lifecycle",
    "all wool and slab variants",
    "unload clears placement controls",
    "Fast Place camera turn with flexible offset",
    "Fake Sneak slab and stair edges",
    "Fake Sneak death and noclip",
    "slab completion singleplayer only"
  };

  public static void beginTick() {}

  public static void tick(Minecraft mc) {
    if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("power_creative_inventory")) {
      BareValidation.tick(mc);
      return;
    }
    try {
      if (stage == -1) {
        stage = 0;
        Files.deleteIfExists(REPORT);
        mc.options.difficulty = 0;
        mc.interactionManager = new SingleplayerInteractionManager(mc);
        mc.method_2120(WORLD, WORLD, 17320261002L);
        mc.setScreen(null);
        log("START full enabled mod set");
        return;
      }
      if (mc.world == null || mc.player == null) return;
      if (done) {
        if (soak) soakTick(mc);
        FinalChecks.tick(mc);
        if (++ticks % 5 == 0) commands(mc);
        return;
      }
      if (stage < CASES.length) {
        int i = stage++;
        try {
          reset(mc);
          run(mc, i);
          log("PASS " + CASES[i]);
        } catch (Throwable e) {
          failures++;
          log("FAIL " + CASES[i] + " " + e);
          e.printStackTrace();
        } finally {
          FastPlace.release();
          mc.setScreen(null);
        }
        return;
      }
      if (stage == CASES.length) {
        set(mc.world, 8, 101, 8, 43, 2);
        set(mc.world, 7, 101, 8, 33, 5);
        mc.player.inventory.main[0] = new ItemStack(44, 23, 2);
        mc.world.method_195(true, null);
        mc.setWorld(null);
        mc.method_2120(WORLD, WORLD, 17320261002L);
        mc.setScreen(null);
        stage++;
        return;
      }
      check(id(mc, 8, 101, 8) == 43 && meta(mc, 8, 101, 8) == 2, "saved double slab variant");
      check(id(mc, 7, 101, 8) == 33 && meta(mc, 7, 101, 8) == 5, "saved piston direction");
      check(count(mc) == 23 && mc.player.inventory.main[0].getDamage() == 2, "saved inventory");
      log("PASS disk save/unload/reload");
      done = true;
      ExtendedValidation.run(mc);
      showcase(mc);
      log("COMPLETE failures=" + failures);
    } catch (Throwable e) {
      done = true;
      failures++;
      log("FATAL " + e);
      e.printStackTrace();
    }
  }

  static void reset(Minecraft mc) throws Exception {
    var worldeditSettings = local.luke.power.worldedit.WorldEditor.settings();
    worldeditSettings.enabled = true;
    worldeditSettings.creativeOnly = false;
    local.luke.power.worldedit.WorldEditor.apply(worldeditSettings);
    local.luke.power.worldedit.WorldEditor.worldOverride(
        mc, local.luke.power.worldedit.config.WorldOverride.INHERIT);
    creative(mc, false);
    flight(mc, false);
    FastPlace.release();
    FastPlace.apply(new local.luke.power.fastplace.config.Settings());
    FastPlace.release();
    FlexiblePlacement.apply(new local.luke.power.flexible.config.Settings());
    FakeSneak.apply(new local.luke.power.fakesneak.config.Settings());
    SlabPlacement.apply(new local.luke.power.slabplacement.config.Settings());
    mc.setScreen(null);
    mc.paused = false;
    mc.field_2778 = true;
    mc.world.isRemote = false;
    mc.player.health = 20;
    mc.player.dead = false;
    mc.player.field_1642 = false;
    mc.player.field_1595 = null;
    mc.field_2807 = mc.player;
    mc.player.inventory.selectedSlot = 0;
    Arrays.fill(mc.player.inventory.main, null);
    stack(mc, 1, 0);
    for (int x = -6; x <= 12; x++)
      for (int z = -6; z <= 12; z++) {
        set(mc.world, x, 100, z, 1, 0);
        for (int y = 101; y <= 109; y++) set(mc.world, x, y, z, 0, 0);
      }
    aim(mc, .5, 103, 3.5, .5, 101, .5);
    FlexiblePlacement.beginTick(mc, false, Modes.NONE, true);
    FakeSneak.beginTick(mc, false, true);
  }

  private static void modes(Minecraft mc, Modes m) {
    FlexiblePlacement.beginTick(mc, false, Modes.NONE, true);
    FlexiblePlacement.beginTick(mc, false, m, true);
  }

  static void stack(Minecraft mc, int id, int meta) {
    mc.player.inventory.main[0] = new ItemStack(id, 64, meta);
  }

  private static int count(Minecraft mc) {
    var s = mc.player.inventory.getSelectedItem();
    return s == null ? 0 : s.count;
  }

  private static int id(Minecraft mc, int x, int y, int z) {
    return mc.world.getBlockId(x, y, z);
  }

  private static int meta(Minecraft mc, int x, int y, int z) {
    return mc.world.method_1778(x, y, z);
  }

  static void set(World w, int x, int y, int z, int id, int meta) {
    w.method_201(x, y, z, id, meta);
  }

  static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  private static void hit(Minecraft mc, int x, int y, int z, int face, double u, double v) {
    Direction d = Direction.values()[face], f = Direction.horizontal(mc.player.yaw);
    double plane = (d.x + d.y + d.z) > 0 ? 1 : 0;
    double[] p = FaceGrid.point(d, f, u, v, plane);
    mc.field_2823 = new class_27(x, y, z, face, Vec3d.createCached(x + p[0], y + p[1], z + p[2]));
  }

  private static boolean place(Minecraft mc, int x, int y, int z, int face, double u, double v) {
    hit(mc, x, y, z, face, u, v);
    return mc.interactionManager.method_1713(
        mc.player, mc.world, mc.player.inventory.getSelectedItem(), x, y, z, face);
  }

  static void click(Minecraft mc, int x, int y, int z, int face) {
    hit(mc, x, y, z, face, .5, .5);
    ((ClickInvoker) mc).validate$click(1);
  }

  static void aim(Minecraft mc, double px, double py, double pz, double x, double y, double z) {
    mc.player.method_1340(px, py, pz);
    mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
    double dx = x - px, dy = y - py, dz = z - pz;
    mc.player.yaw = mc.player.prevYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
    mc.player.pitch =
        mc.player.prevPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
  }

  static void creative(Minecraft mc, boolean enabled) throws Exception {
    Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer")
        .getMethod("creative_setCreative", boolean.class)
        .invoke(mc.player, enabled);
  }

  static void flight(Minecraft mc, boolean enabled) throws Exception {
    Class.forName("local.luke.power.creative.inventory.interfaces.CreativePlayer")
        .getMethod("creative_setFlying", boolean.class)
        .invoke(mc.player, enabled);
  }

  private static void fast(Minecraft mc, boolean keepSlab) throws Exception {
    var s = FastPlace.settings();
    s.setEnabled(true);
    s.slabMode =
        keepSlab
            ? local.luke.power.fastplace.SlabMode.MATCH_FIRST
            : local.luke.power.fastplace.SlabMode.CONTINUOUS;
    FastPlace.apply(s);
    FastPlace.release();
    FastPlace.beginTick(mc, true, false, true);
  }

  private static void fake(boolean value) throws Exception {
    var s = FakeSneak.settings();
    s.enabled = value;
    FakeSneak.apply(s);
  }

  private static void edge(Minecraft mc, boolean lShape) {
    for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) set(mc.world, x, 100, z, 0, 0);
    set(mc.world, 0, 100, 0, 1, 0);
    if (lShape) {
      set(mc.world, 1, 100, 0, 1, 0);
      set(mc.world, 0, 100, 1, 1, 0);
    }
    mc.player.method_1340(.5, 101 + mc.player.field_1631, .5);
    mc.player.field_1623 = true;
  }

  private static void run(Minecraft mc, int test) throws Exception {
    switch (test) {
      case 0 -> {
        click(mc, 0, 100, 0, 1);
        check(id(mc, 0, 101, 0) == 1 && count(mc) == 63, "native block and consumption");
      }
      case 1 -> {
        for (Direction face : Direction.values()) {
          set(mc.world, 0, 104, 0, 1, 0);
          stack(mc, 1, 0);
          Direction forward = Direction.NORTH;
          aim(mc, .5 + face.x * 3, 104.5 + face.y * 3, .5 + face.z * 3, .5, 104.5, .5);
          mc.player.yaw = 180;
          modes(mc, new Modes(true, false, false, false, false));
          hit(mc, 0, 104, 0, face.ordinal(), .5, .9);
          var p =
              FlexiblePlacement.plan(
                  mc, mc.player.inventory.getSelectedItem(), 0, 104, 0, face.ordinal());
          check(p != null && p.valid(), "valid plan " + face + " " + p);
          check(place(mc, 0, 104, 0, face.ordinal(), .5, .9), "placed " + face);
          check(
              id(mc, p.destination().x(), p.destination().y(), p.destination().z()) == 1,
              "offset result " + face);
          set(mc.world, p.destination().x(), p.destination().y(), p.destination().z(), 0, 0);
        }
      }
      case 2 -> {
        for (int z = -2; z <= 2; z++)
          for (int x = -2; x <= 2; x++) if (x != 0 || z != 0) set(mc.world, x, 100, z, 0, 0);
        aim(mc, .5, 103, 1.5, .5, 101, .5);
        modes(mc, new Modes(false, true, false, false, false));
        hit(mc, 0, 100, 0, 1, .5, .9);
        var p = FlexiblePlacement.plan(mc, mc.player.inventory.getSelectedItem(), 0, 100, 0, 1);
        check(p != null && p.valid() && p.destination().y() == 100, "same height bridge");
        place(mc, 0, 100, 0, 1, .5, .9);
        check(id(mc, p.destination().x(), 100, p.destination().z()) == 1, "bridge placed");
      }
      case 3 -> {
        modes(mc, new Modes(true, false, false, false, false));
        place(mc, 0, 100, 0, 1, .5, .5);
        check(id(mc, 0, 101, 0) == 0 && id(mc, 0, 102, 0) == 1, "one block gap");
      }
      case 4 -> {
        stack(mc, 33, 0);
        double[][] poses = {
          {.5, 104.3, .5},
          {.5, 99, .5},
          {.5, 102, 3.5},
          {-2.5, 102, .5},
          {.5, 102, -2.5},
          {3.5, 102, .5}
        };
        Set<Integer> seen = new HashSet<>();
        for (double[] p : poses) {
          set(mc.world, 0, 101, 0, 0, 0);
          aim(mc, p[0], p[1], p[2], .5, 101, .5);
          modes(mc, Modes.NONE);
          place(mc, 0, 100, 0, 1, .5, .5);
          int normal = meta(mc, 0, 101, 0);
          seen.add(normal);
          set(mc.world, 0, 101, 0, 0, 0);
          modes(mc, new Modes(false, false, false, true, false));
          check(place(mc, 0, 100, 0, 1, .5, .5), "reverse placed");
          check(
              meta(mc, 0, 101, 0) == (normal ^ 1),
              "reverse " + normal + " -> " + meta(mc, 0, 101, 0));
        }
        check(seen.size() == 6, "all six piston defaults covered " + seen);
      }
      case 5 -> {
        stack(mc, 33, 0);
        modes(mc, new Modes(false, false, true, false, false));
        place(mc, 0, 100, 0, 1, .5, .9);
        check(meta(mc, 0, 101, 0) == 2, "piston points north");
      }
      case 6 -> {
        stack(mc, 29, 0);
        modes(mc, new Modes(false, false, false, false, true));
        place(mc, 0, 100, 0, 1, .5, .5);
        check(meta(mc, 0, 101, 0) == 0, "piston down");
        set(mc.world, 0, 101, 0, 0, 0);
        modes(mc, new Modes(false, false, false, true, true));
        place(mc, 0, 100, 0, 1, .5, .5);
        check(meta(mc, 0, 101, 0) == 1, "into plus reverse points up");
      }
      case 7 -> {
        for (int item : new int[] {23, 53, 61, 67, 86, 91, 356}) {
          stack(mc, item, 0);
          set(mc.world, 0, 101, 0, 0, 0);
          modes(mc, Modes.NONE);
          place(mc, 0, 100, 0, 1, .5, .5);
          int normal = meta(mc, 0, 101, 0);
          set(mc.world, 0, 101, 0, 0, 0);
          modes(mc, new Modes(false, false, false, true, false));
          place(mc, 0, 100, 0, 1, .5, .5);
          int flipped = meta(mc, 0, 101, 0);
          check(flipped != normal, "rotated item " + item);
          check(id(mc, 0, 101, 0) == (item == 356 ? 93 : item), "native item " + item);
        }
      }
      case 8 -> {
        stack(mc, 61, 0);
        modes(mc, new Modes(false, false, false, false, true));
        check(!place(mc, 0, 100, 0, 1, .5, .5) && count(mc) == 64, "vertical furnace rejected");
      }
      case 9 -> {
        stack(mc, 33, 0);
        float yaw = mc.player.yaw, pitch = mc.player.pitch;
        double x = mc.player.x, y = mc.player.y, z = mc.player.z;
        modes(mc, new Modes(false, false, true, true, false));
        place(mc, 0, 100, 0, 1, .5, .9);
        check(
            mc.player.yaw == yaw
                && mc.player.pitch == pitch
                && mc.player.x == x
                && mc.player.y == y
                && mc.player.z == z,
            "no player transform");
      }
      case 10 -> {
        set(mc.world, 0, 101, 0, 54, 0);
        place(mc, 0, 101, 0, 1, .5, .5);
        check(mc.currentScreen != null && count(mc) == 64, "chest opened");
      }
      case 11 -> {
        set(mc.world, 0, 101, 0, 54, 0);
        modes(mc, new Modes(false, false, false, true, false));
        place(mc, 0, 101, 0, 1, .5, .5);
        check(
            mc.currentScreen == null && id(mc, 0, 102, 0) == 1,
            "modifier places instead of opening");
      }
      case 12 -> {
        var s = FlexiblePlacement.settings();
        s.placeAgainstContainers = true;
        FlexiblePlacement.apply(s);
        modes(mc, Modes.NONE);
        set(mc.world, 0, 101, 0, 54, 0);
        place(mc, 0, 101, 0, 1, .5, .5);
        check(mc.currentScreen == null && id(mc, 0, 102, 0) == 1, "optional container placement");
      }
      case 13 -> {
        mc.player.method_1340(.5, 102 + mc.player.field_1631, .5);
        modes(mc, new Modes(true, false, false, false, false));
        check(!place(mc, 0, 100, 0, 1, .5, .5) && count(mc) == 64, "player collision stops offset");
      }
      case 14 -> {
        modes(mc, new Modes(true, false, false, false, false));
        for (int f : new int[] {-1, 6, Integer.MAX_VALUE})
          check(
              FlexiblePlacement.plan(mc, mc.player.inventory.getSelectedItem(), 0, 100, 0, f)
                  == null,
              "invalid face");
        check(
            FlexiblePlacement.plan(
                    mc, mc.player.inventory.getSelectedItem(), Integer.MAX_VALUE, 100, 0, 1)
                == null,
            "coordinate bounds");
        set(mc.world, 0, 127, 0, 1, 0);
        aim(mc, .5, 126, 3.5, .5, 127, .5);
        check(!place(mc, 0, 127, 0, 1, .5, .5), "height limit");
      }
      case 15 -> {
        modes(mc, new Modes(true, false, false, false, false));
        aim(mc, 20, 110, 20, .5, 100, .5);
        check(!place(mc, 0, 100, 0, 1, .5, .5), "outside reach");
        aim(mc, .5, 103, 3.5, .5, 101, .5);
        mc.field_2823 = new class_27(mc.player);
        check(
            FlexiblePlacement.plan(mc, mc.player.inventory.getSelectedItem(), 0, 100, 0, 1) == null,
            "entity occlusion");
      }
      case 16 -> {
        modes(mc, new Modes(true, true, true, true, true));
        for (int item : new int[] {280, 259, 326, 328, 333, 261, 324, 355}) {
          stack(mc, item, 0);
          check(
              !FlexiblePlacement.handles(mc, mc.player.inventory.getSelectedItem()),
              "excluded item " + item);
        }
      }
      case 17 -> {
        Modes held = new Modes(true, false, false, false, false);
        modes(mc, held);
        check(FlexiblePlacement.modes().offset(), "held initially");
        FlexiblePlacement.beginTick(mc, false, held, false);
        check(!FlexiblePlacement.modes().active(), "focus loss");
        FlexiblePlacement.beginTick(mc, false, held, true);
        check(!FlexiblePlacement.modes().active(), "release required");
        modes(mc, held);
        mc.setScreen(new local.luke.power.building.config.SettingsScreen(null));
        check(!FlexiblePlacement.modes().active(), "menu clears held state");
      }
      case 18 -> {
        modes(mc, new Modes(true, false, false, false, false));
        mc.world.isRemote = true;
        check(
            !FlexiblePlacement.handles(mc, mc.player.inventory.getSelectedItem()),
            "remote blocked");
        mc.world.isRemote = false;
      }
      case 19 -> {
        var c = Class.forName("local.luke.power.camera.Freecam");
        var f = c.getField("freecamController");
        Object controller = f.get(null);
        var active = controller.getClass().getDeclaredField("active");
        active.setAccessible(true);
        active.setBoolean(controller, true);
        try {
          check(!FlexiblePlacement.canOperate(mc), "freecam placement blocked");
          fake(true);
          mc.player.field_1623 = true;
          check(!FakeSneak.protects(mc.player, 0), "freecam edge clip blocked");
        } finally {
          active.setBoolean(controller, false);
        }
      }
      case 20 -> {
        FlexiblePlacement.beginTick(mc, true, Modes.NONE, true);
        check(!FlexiblePlacement.settings().enabled, "toggle off");
        FlexiblePlacement.beginTick(mc, true, Modes.NONE, true);
        check(!FlexiblePlacement.settings().enabled, "held key only once");
        FlexiblePlacement.beginTick(mc, false, Modes.NONE, true);
        FlexiblePlacement.beginTick(mc, true, Modes.NONE, true);
        check(FlexiblePlacement.settings().enabled, "second press on");
      }
      case 21 -> {
        List<?> all = Arrays.asList(mc.options.allKeys);
        for (var k : Keys.ALL) check(all.contains(k), "flexible native key");
        check(all.contains(local.luke.power.fakesneak.Keys.ALL[0]), "fake native key");
        check(all.contains(local.luke.power.slabplacement.Keys.ALL[0]), "slab native key");
      }
      case 22 -> {
        Screen screen = new local.luke.power.building.config.SettingsScreen(null);
        mc.setScreen(screen);
        button(screen, 0);
        button(screen, 103);
        check(!FastPlace.settings().enabled, "cancel discards draft");
        screen = new local.luke.power.building.config.SettingsScreen(null);
        mc.setScreen(screen);
        button(screen, 0);
        button(screen, 102);
        check(FastPlace.settings().enabled, "done saves root config");
        check(FlexiblePlacement.settings().enabled, "unrelated settings preserved");
      }
      case 23 -> {
        modes(mc, new Modes(true, false, false, false, false));
        hit(mc, 0, 100, 0, 1, .5, .9);
        boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        PlacementOverlay.render(mc.player, mc.field_2823, 1);
        check(
            depth == org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST),
            "GL depth restored");
        check(org.lwjgl.opengl.GL11.glGetError() == 0, "no GL errors");
      }
      case 24 -> {
        for (int variant = 0; variant < 4; variant++)
          for (Direction face : Direction.values()) {
            stack(mc, 44, variant);
            set(mc.world, 0, 104, 0, 44, variant);
            int x = -face.x, y = 104 - face.y, z = -face.z;
            set(mc.world, x, y, z, 1, 0);
            place(mc, x, y, z, face.ordinal(), .5, .5);
            check(
                id(mc, 0, 104, 0) == 43 && meta(mc, 0, 104, 0) == variant,
                "merge " + variant + " " + face);
            set(mc.world, x, y, z, 0, 0);
          }
      }
      case 25 -> {
        stack(mc, 44, 1);
        set(mc.world, 0, 101, 0, 44, 2);
        set(mc.world, -1, 101, 0, 1, 0);
        place(mc, -1, 101, 0, 5, .5, .5);
        check(
            id(mc, 0, 101, 0) == 44 && meta(mc, 0, 101, 0) == 2 && count(mc) == 64,
            "different variants not merged");
      }
      case 26 -> {
        stack(mc, 44, 0);
        set(mc.world, 0, 101, 0, 44, 0);
        set(mc.world, -1, 101, 0, 1, 0);
        mc.player.method_1340(.5, 101.5 + mc.player.field_1631, .5);
        check(
            !place(mc, -1, 101, 0, 5, .5, .5) && count(mc) == 64 && id(mc, 0, 101, 0) == 44,
            "no suffocation merge");
      }
      case 27 -> {
        stack(mc, 44, 3);
        set(mc.world, 0, 101, 0, 44, 3);
        set(mc.world, -1, 101, 0, 1, 0);
        place(mc, -1, 101, 0, 5, .5, .5);
        check(count(mc) == 63 && meta(mc, 0, 101, 0) == 3, "consumed one");
      }
      case 28 -> {
        creative(mc, true);
        stack(mc, 44, 2);
        set(mc.world, 0, 101, 0, 44, 2);
        set(mc.world, -1, 101, 0, 1, 0);
        place(mc, -1, 101, 0, 5, .5, .5);
        check(count(mc) == 64 && id(mc, 0, 101, 0) == 43, "creative preserves inventory");
      }
      case 29 -> {
        var s = SlabPlacement.settings();
        s.enabled = false;
        SlabPlacement.apply(s);
        stack(mc, 44, 0);
        set(mc.world, 0, 101, 0, 44, 0);
        set(mc.world, -1, 101, 0, 1, 0);
        place(mc, -1, 101, 0, 5, .5, .5);
        check(
            id(mc, 0, 101, 0) == 44 && count(mc) == 64,
            "disabled restores native adjacent behaviour");
      }
      case 30 -> {
        stack(mc, 44, 1);
        set(mc.world, 0, 101, 0, 44, 1);
        place(mc, 0, 101, 0, 1, .5, .5);
        check(id(mc, 0, 101, 0) == 43 && meta(mc, 0, 101, 0) == 1, "top completion");
      }
      case 31 -> {
        stack(mc, 44, 0);
        fast(mc, true);
        click(mc, 0, 100, 0, 1);
        aim(mc, 2.5, 102, 3.5, .99, 101.25, .5);
        FastPlace.placeBatch(mc);
        check(id(mc, 1, 101, 0) == 44, "side continuation same layer");
        check(id(mc, 0, 101, 0) == 44, "first remains half");
      }
      case 32 -> {
        stack(mc, 44, 0);
        fast(mc, true);
        click(mc, 0, 100, 0, 1);
        aim(mc, .5, 103, 3.5, .5, 101.49, .5);
        FastPlace.placeBatch(mc);
        check(id(mc, 0, 101, 0) == 44 && count(mc) == 63, "held merge suppressed");
      }
      case 33 -> {
        stack(mc, 44, 0);
        fast(mc, false);
        click(mc, 0, 100, 0, 1);
        aim(mc, .5, 103, 1.5, .5, 101.49, .5);
        FastPlace.placeBatch(mc);
        check(id(mc, 0, 101, 0) == 43 && count(mc) == 62, "held merge allowed once");
      }
      case 34 -> {
        stack(mc, 44, 0);
        set(mc.world, 1, 101, 0, 44, 0);
        set(mc.world, 0, 101, 0, 1, 0);
        var t =
            local.luke.power.fastplace.PlacementTarget.at(
                mc.world, mc.player.inventory.getSelectedItem(), 0, 101, 0, 5);
        check(
            t != null && t.expectedBlock() == 43 && t.position().x() == 1,
            "adjacent merge prediction");
      }
      case 35 -> {
        fast(mc, true);
        modes(mc, new Modes(true, false, false, false, false));
        click(mc, 0, 100, 0, 1);
        check(id(mc, 0, 102, 0) == 1, "first offset");
        aim(mc, 2.5, 103, 3.5, 2.5, 100.99, .5);
        FastPlace.beginTick(mc, true, false, true);
        FastPlace.placeBatch(mc);
        check(id(mc, 2, 102, 0) == 1, "Fast Place continues at offset destination");
      }
      case 36 -> {
        stack(mc, 33, 0);
        fast(mc, true);
        modes(mc, new Modes(false, false, false, true, false));
        click(mc, 0, 100, 0, 1);
        int first = meta(mc, 0, 101, 0);
        aim(mc, 2.5, 103, 3.5, 2.5, 100.99, .5);
        FastPlace.beginTick(mc, true, false, true);
        FastPlace.placeBatch(mc);
        check(
            id(mc, 2, 101, 0) == 33 && meta(mc, 2, 101, 0) == first,
            "reverse remains during repeat");
      }
      case 37 -> {
        fake(true);
        mc.player.method_1340(.5, 101 + mc.player.field_1631, .5);
        mc.player.field_1623 = true;
        double x = mc.player.x;
        mc.player.move(.2, 0, 0);
        check(Math.abs(mc.player.x - x - .2) < 1e-6, "full movement retained");
        check(!mc.player.method_1373(), "not actually sneaking");
      }
      case 38 -> {
        fake(true);
        for (Direction d :
            new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
          edge(mc, false);
          mc.player.move(d.x, 0, d.z);
          check(
              !mc.world.method_190(mc.player, mc.player.boundingBox.move(0, -1, 0)).isEmpty(),
              "supported at " + d);
        }
      }
      case 39 -> {
        fake(true);
        edge(mc, true);
        mc.player.move(1, 0, 1);
        check(
            !mc.world.method_190(mc.player, mc.player.boundingBox.move(0, -1, 0)).isEmpty(),
            "diagonal supported");
      }
      case 40 -> {
        edge(mc, false);
        fake(false);
        mc.player.move(1, 0, 0);
        check(mc.player.x > 1.4, "walks off when disabled");
      }
      case 41 -> {
        fake(true);
        edge(mc, false);
        mc.player.move(1, .42, 0);
        check(mc.player.x > 1.4, "jump can cross edge");
        edge(mc, false);
        mc.player.field_1623 = false;
        mc.player.move(1, -.1, 0);
        check(mc.player.x > 1.4, "airborne movement unrestricted");
      }
      case 42 -> {
        creative(mc, true);
        fake(true);
        edge(mc, false);
        flight(mc, true);
        check(!FakeSneak.protects(mc.player, 0), "flight unrestricted");
        flight(mc, false);
        mc.player.field_1595 = mc.player;
        check(!FakeSneak.protects(mc.player, 0), "riding unrestricted");
        mc.player.field_1595 = null;
      }
      case 43 -> {
        fake(true);
        edge(mc, false);
        mc.world.isRemote = true;
        check(!FakeSneak.protects(mc.player, 0), "remote unrestricted");
        mc.world.isRemote = false;
        var other = mc.interactionManager.method_1717(mc.world);
        other.field_1623 = true;
        check(!FakeSneak.protects(other, 0), "other player unaffected");
      }
      case 44 -> {
        FakeSneak.beginTick(mc, true, true);
        check(FakeSneak.settings().enabled, "toggle on");
        FakeSneak.beginTick(mc, true, true);
        check(FakeSneak.settings().enabled, "held toggle once");
        FakeSneak.beginTick(mc, false, true);
        FakeSneak.beginTick(mc, true, false);
        check(FakeSneak.settings().enabled, "unfocused toggle ignored");
        FakeSneak.beginTick(mc, true, true);
        check(FakeSneak.settings().enabled, "refocus does not retrigger");
        FakeSneak.beginTick(mc, false, true);
        FakeSneak.beginTick(mc, true, true);
        check(!FakeSneak.settings().enabled, "second press off");
      }
      case 45 -> {
        for (int item : new int[] {35, 44})
          for (int variant = 0; variant < (item == 35 ? 16 : 4); variant++) {
            set(mc.world, 0, 101, 0, 0, 0);
            stack(mc, item, variant);
            modes(mc, new Modes(false, false, false, true, false));
            place(mc, 0, 100, 0, 1, .5, .5);
            check(
                id(mc, 0, 101, 0) == item && meta(mc, 0, 101, 0) == variant,
                "variant retained " + item + ":" + variant);
          }
      }
      case 47 -> {
        fast(mc, true);
        modes(mc, new Modes(true, false, false, false, false));
        click(mc, 0, 100, 0, 1);
        // Turn ninety degrees; the top grid must follow the current camera.
        aim(mc, 3.5, 103, 2.5, .08, 100.99, 2.5);
        mc.field_2818.method_1838(1);
        var hit = mc.field_2823;
        var predicted =
            FlexiblePlacement.plan(
                mc,
                mc.player.inventory.getSelectedItem(),
                hit.field_1984,
                hit.field_1985,
                hit.field_1986,
                hit.field_1987);
        check(
            predicted != null && predicted.valid() && predicted.part() != FaceGrid.Part.CENTER,
            "peripheral target after turn");
        FastPlace.beginTick(mc, true, false, true);
        FastPlace.placeBatch(mc);
        var at = predicted.destination();
        check(id(mc, at.x(), at.y(), at.z()) == 1, "placement matches current-camera preview");
      }
      case 48 -> {
        fake(true);
        for (int block : new int[] {44, 53, 67}) {
          edge(mc, false);
          set(mc.world, 0, 100, 0, block, 0);
          mc.player.method_1340(.5, 100.5 + mc.player.field_1631, .5);
          mc.player.field_1623 = true;
          mc.player.move(1, 0, 0);
          check(
              !mc.world.method_190(mc.player, mc.player.boundingBox.move(0, -1, 0)).isEmpty(),
              "partial block support " + block);
        }
      }
      case 49 -> {
        fake(true);
        edge(mc, false);
        mc.player.dead = true;
        check(!FakeSneak.protects(mc.player, 0), "dead player");
        mc.player.dead = false;
        mc.player.field_1642 = true;
        check(!FakeSneak.protects(mc.player, 0), "noclip");
        mc.player.field_1642 = false;
      }
      case 50 -> {
        stack(mc, 44, 0);
        set(mc.world, 0, 101, 0, 44, 0);
        set(mc.world, -1, 101, 0, 1, 0);
        mc.world.isRemote = true;
        check(
            local.luke.power.slabplacement.SlabMerge.target(
                    mc.world, mc.player.inventory.getSelectedItem(), -1, 101, 0, 5)
                == null,
            "remote merge disabled");
        mc.world.isRemote = false;
      }
      case 46 -> {
        modes(mc, new Modes(true, false, false, false, false));
        FlexiblePlacement.interrupt();
        check(!FlexiblePlacement.modes().active(), "no held modes retained");
      }
    }
  }

  private static void button(Screen s, int id) throws Exception {
    var method =
        Arrays.stream(s.getClass().getDeclaredMethods())
            .filter(m -> Arrays.equals(m.getParameterTypes(), new Class<?>[] {ButtonWidget.class}))
            .findFirst()
            .orElseThrow();
    method.setAccessible(true);
    method.invoke(s, new ButtonWidget(id, 0, 0, "test"));
  }

  private static void showcase(Minecraft mc) throws Exception {
    reset(mc);
    mc.player.method_1340(.5, 103, 3.5);
    mc.player.yaw = mc.player.prevYaw = 180;
    mc.player.pitch = mc.player.prevPitch = 35;
    int[] items = {1, 44, 33, 29, 53, 61, 356, 35, 54};
    for (int i = 0; i < items.length; i++)
      mc.player.inventory.main[i] = new ItemStack(items[i], 64, 0);
    for (var k : Keys.ALL) k.code = 0;
    Keys.ALL[1].code = org.lwjgl.input.Keyboard.KEY_LCONTROL;
    Keys.ALL[2].code = org.lwjgl.input.Keyboard.KEY_LMENU;
    Keys.ALL[3].code = org.lwjgl.input.Keyboard.KEY_R;
    Keys.ALL[4].code = org.lwjgl.input.Keyboard.KEY_V;
    Keys.ALL[5].code = org.lwjgl.input.Keyboard.KEY_B;
    local.luke.power.fakesneak.Keys.ALL[0].code = org.lwjgl.input.Keyboard.KEY_N;
    mc.options.save();
    mc.world.method_195(true, null);
    log("LAB READY " + WORLD);
  }

  private static void soakTick(Minecraft mc) throws Exception {
    int x = soakTicks % 2 + 1;
    stack(mc, 33, 0);
    set(mc.world, x, 101, 0, 0, 0);
    aim(mc, x + .5, 103, 3.5, x + .5, 100.99, .5);
    modes(mc, new Modes(false, false, false, true, false));
    place(mc, x, 100, 0, 1, .5, .5);
    check(id(mc, x, 101, 0) == 33 && count(mc) == 64, "soak native placement and creative count");
    mc.player.field_1623 = true;
    double before = mc.player.x;
    mc.player.move(.1, 0, 0);
    check(Math.abs(mc.player.x - before - .1) < 1e-6, "soak full-speed movement");
    soakTicks++;
    soakPlaced++;
    if (soakTicks % 600 == 0) log("SOAK ticks=" + soakTicks + " placements=" + soakPlaced);
    if (soakTicks >= 2400) {
      soak = false;
      log(
          "SOAK COMPLETE ticks="
              + soakTicks
              + " placements="
              + soakPlaced
              + " elapsedMs="
              + (System.currentTimeMillis() - started));
      showcase(mc);
    }
  }

  private static void commands(Minecraft mc) throws Exception {
    if (!Files.exists(COMMAND)) return;
    String s = Files.readString(COMMAND).trim();
    Files.delete(COMMAND);
    if (s.equals("config-flex")) mc.setScreen(new local.luke.power.building.config.SettingsScreen(null));
    if (s.equals("config-fake")) mc.setScreen(new local.luke.power.building.config.SettingsScreen(null));
    if (s.equals("config-slab")) mc.setScreen(new local.luke.power.building.config.SettingsScreen(null));
    if (FeatureValidation.command(mc, s)) return;
    if (s.equals("config-we")) mc.setScreen(new local.luke.power.worldedit.config.SettingsScreen(null));
    if (s.startsWith("we ")) local.luke.power.worldedit.WorldEditor.command(mc, s.substring(3));
    if (s.equals("extended")) ExtendedValidation.run(mc);
    if (s.equals("large-edit")) FinalChecks.largeEdit(mc);
    if (s.startsWith("slab-input ")) FinalChecks.prepareSlabs(mc, s.substring(11));
    if (s.equals("slab-result")) FinalChecks.slabResult(mc);
    if (s.equals("showcase")) showcase(mc);
    if (s.equals("soak")) {
      reset(mc);
      creative(mc, true);
      fake(true);
      soakTicks = soakPlaced = 0;
      started = System.currentTimeMillis();
      soak = true;
      log("SOAK START");
    }
    if (s.equals("status"))
      log(
          "STATUS fake="
              + FakeSneak.settings().enabled
              + " modes="
              + FlexiblePlacement.modes()
              + " count="
              + count(mc)
              + " key="
              + local.luke.power.fakesneak.Keys.ALL[0].code
              + " xyz="
              + mc.player.x
              + ","
              + mc.player.y
              + ","
              + mc.player.z);
    if (s.equals("revision")) RevisionValidation.run(mc);
    if (s.startsWith("camera ")) RevisionValidation.camera(mc, s.substring(7));
    if (s.equals("revision-state")) RevisionValidation.state(mc);
    if (s.equals("quit")) {
      mc.world.method_195(true, null);
      mc.scheduleStop();
    }
  }

  static void log(String s) {
    try {
      Files.writeString(REPORT, s + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
