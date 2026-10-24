package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.mining.*;
import local.luke.power.gameplay.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Real native mining draws against native block geometry, with color and depth evidence. */
final class MiningOverlay448Checks {
  private static final int SIZE = 256;
  private static final Path OUTPUT = Path.of("power-beta-data/reports/mining-overlay-448");
  private record Face(String name, int dx, int dy, int dz, float pitch, float yaw, int side) {}
  private static final List<Face> FACES = List.of(
      new Face("bottom", 0, -1, 0, -90, 0, 0), new Face("top", 0, 1, 0, 90, 0, 1),
      new Face("north", 0, 0, -1, 0, 180, 2), new Face("south", 0, 0, 1, 0, 0, 3),
      new Face("west", -1, 0, 0, 0, 90, 4), new Face("east", 1, 0, 0, 0, -90, 5));
  private record Pixels(byte[] rgba, float[] depth) {}
  private record Scenario(String prefix, int block, int tool, int obsidianSpeed, String feature, boolean full) {}
  private static final List<Scenario> SCENARIOS = List.of(
      new Scenario("", 16, 278, 0, "none", true),
      new Scenario("obsidian-0-", 49, 278, 0, "obsidian", true),
      new Scenario("obsidian-60-", 49, 278, 60, "obsidian", true),
      new Scenario("obsidian-100-", 49, 278, 100, "obsidian", true),
      new Scenario("sand-torch-shovel-", 12, 50, 0, "torch", false),
      new Scenario("gravel-torch-shovel-", 13, 50, 0, "torch", false),
      new Scenario("leaves-axe-", 18, 279, 0, "axe", false),
      new Scenario("stone-", 1, 278, 0, "none", false));

  private static Field field(Class<?> owner, String name) throws Exception {
    for (Class<?> type = owner; type != null; type = type.getSuperclass())
      try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
      catch (NoSuchFieldException next) {}
    throw new NoSuchFieldException(name);
  }

  /** capture writes evidence and metrics; run additionally accepts or rejects the pixel/depth results. */
  static void run(Minecraft mc, String action) throws Exception {
    check(action.equals("capture") || action.equals("run"), "use mining-overlay-448 capture or run");
    boolean accept = action.equals("run");
    check(mc.world != null && mc.player != null && !mc.world.isRemote && mc.currentScreen == null,
        "open a disposable singleplayer world with no screen");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0 && Display.isActive(),
        "mute and focus the disposable client");
    check(!local.luke.power.input.MovementOwnership.cameraControlsMovement(), "leave freecam before mining checks");
    check(mc.interactionManager instanceof net.minecraft.SingleplayerInteractionManager, "use native singleplayer mining");
    failures = 0; Files.createDirectories(OUTPUT);
    var owner = mc.player; var camera = mc.field_2807;
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    Object controls = Class.forName("local.luke.power.controls.ControlFeatures").getField("BUGFIXES_CONFIG").get(null);
    Field fix = controls.getClass().getField("breakingAnimationFix"); fix.setAccessible(true); Object oldFix = fix.get(controls);
    Field config = field(VeinConfig.class, "current"); Object oldConfig = config.get(null);
    Field gameplayConfig = field(GameplayConfig.class, "current"); Object oldGameplay = gameplayConfig.get(null);
    Field buildingConfig = field(Class.forName("local.luke.power.building.config.Config"), "current");
    Object oldBuilding = buildingConfig.get(null);
    Object building = oldBuilding.getClass().getMethod("copy").invoke(oldBuilding);
    Field obsidianSpeed = field(building.getClass(), "obsidianBreakingSpeed");
    Map<Field, Object> mining = new LinkedHashMap<>();
    for (String name : List.of("candidate", "job", "executing", "failedWorld")) {
      Field f = field(VeinMining.class, name); mining.put(f, f.get(null));
    }
    var probe = new ClientPlayerEntity(mc, mc.world, mc.session, owner.dimensionId);
    probe.field_161 = new class_41(); probe.inventory.main[0] = new net.minecraft.item.ItemStack(278, 1, 0);
    probe.inventory.selectedSlot = 0;
    int x = ((int)Math.floor(owner.x) >> 4) * 16 + 8, y = 108, z = ((int)Math.floor(owner.z) >> 4) * 16 + 8;
    check(mc.world.method_155(x - 6, y - 6, z - 6, x + 6, y + 6, z + 6), "scene must be loaded");
    Map<String, int[]> blocks = new LinkedHashMap<>();
    for (Face face : FACES) for (int n = 0; n <= 5; n++)
      remember(mc, blocks, x - face.dx * n, y - face.dy * n, z - face.dz * n);
    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++)
      remember(mc, blocks, x + dx, y + dy, z + dz);
    for (int[] b : blocks.values()) check(mc.world.method_1777(b[0], b[1], b[2]) == null, "scene intersects a block entity");
    float oldProgress = mc.worldRenderer.field_1803;
    Map<Field, Double> offsets = new LinkedHashMap<>();
    for (Field f : Tessellator.class.getDeclaredFields()) if (f.getType() == double.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
      f.setAccessible(true); offsets.put(f, f.getDouble(Tessellator.INSTANCE));
    }
    var report = new JsonArray();
    try (var frame = new Framebuffer()) {
      for (Field f : mining.keySet()) f.set(null, null);
      mc.player = probe; mc.field_2807 = probe;
      var settings = VeinConfig.copy(); settings.enabled = true; settings.crouchingOnly = false;
      settings.timePercent = 100; config.set(null, settings); buildingConfig.set(null, building);
      var nativeBlocks = new class_13(mc.world);
      for (Scenario scenario : SCENARIOS) {
        probe.inventory.main[0] = new net.minecraft.item.ItemStack(scenario.tool, 1, 0);
        probe.inventory.main[1] = scenario.feature.equals("torch") ? new net.minecraft.item.ItemStack(277, 1, 0) : null;
        settings.tools.put(Integer.toString(scenario.block), List.of(scenario.feature.equals("torch") ? 277 : scenario.tool));
        for (Face face : FACES) {
          if (!scenario.full && !List.of("bottom", "top", "south").contains(face.name)) continue;
          for (int[] b : blocks.values()) mc.world.method_154(b[0], b[1], b[2], 0, 0);
          for (int n = 0; n <= 5; n++) mc.world.method_154(x - face.dx * n, y - face.dy * n, z - face.dz * n, scenario.block, 0);
          for (boolean crouch : scenario.full ? new boolean[]{false, true} : new boolean[]{true})
              for (double distance : scenario.full ? new double[]{2.5, 5} : new double[]{2.5})
                  for (int stereo : scenario.full ? new int[]{-1, 0, 1} : new int[]{0}) {
            String name = scenario.prefix + face.name + (crouch ? "-crouch" : "-stand") + "-d" + distance + "-eye" + stereo;
            test("MINING448 " + action + " " + name, () -> {
              probe.field_161.field_2536 = crouch;
              double cx = x + .5 + face.dx * distance, cy = y + .5 + face.dy * distance, cz = z + .5 + face.dz * distance;
              probe.method_1340(cx, cy, cz);
              probe.field_1637 = cx; probe.field_1638 = cy; probe.field_1639 = cz;
              var hit = new class_27(x, y, z, face.side, Vec3d.createCached(x + .5, y + .5, z + .5));
              VeinMining.reset();
              feature(scenario, false, obsidianSpeed, building);
              float ordinaryDelta = nativeDelta(mc, probe, x, y, z);
              fix.set(controls, false); frame.setup(face, crouch, stereo);
              Pixels ordinaryOverlay = overlay(mc, probe, hit, false);
              feature(scenario, true, obsidianSpeed, building);
              float featureDelta = nativeDelta(mc, probe, x, y, z);
              check(featureDelta > 0 && Float.isFinite(featureDelta), "native mining produced no finite progress");
              if (!scenario.feature.equals("none") && !(scenario.feature.equals("obsidian") && scenario.obsidianSpeed == 0))
                check(featureDelta > ordinaryDelta, "configured mining feature did not influence native progress");
              if (scenario.feature.equals("torch"))
                check(TorchShovel.slot(probe, Block.BLOCKS[scenario.block]) == 1, "torch shovel did not use the owned shovel");
              frame.setup(face, crouch, stereo);
              Pixels terrain = terrain(nativeBlocks, scenario.block, x, y, z, cx, cy, cz);
              Pixels[] normal = new Pixels[2], depth = new Pixels[2], vein = new Pixels[2];
              for (int enabled = 0; enabled < 2; enabled++) {
                fix.set(controls, enabled == 1);
                frame.setup(face, crouch, stereo); normal[enabled] = overlay(mc, probe, hit, false);
                frame.setup(face, crouch, stereo); terrain(nativeBlocks, scenario.block, x, y, z, cx, cy, cz);
                depth[enabled] = overlay(mc, probe, hit, true);
              }
              VeinMining.prepare(mc, x, y, z);
              for (int tick = 0; tick < 100 && VeinMining.progress(.1f, probe, x, y, z) == 0; tick++) VeinMining.tick(mc);
              float adjusted = VeinMining.progress(.1f, probe, x, y, z);
              check(adjusted > 0 && adjusted < .1f, "fixture did not establish an actual multi-block vein candidate");
              for (int enabled = 0; enabled < 2; enabled++) {
                fix.set(controls, enabled == 1);
                frame.setup(face, crouch, stereo); vein[enabled] = overlay(mc, probe, hit, false);
              }
              float nativeVeinDelta = nativeDelta(mc, probe, x, y, z);
              check(nativeVeinDelta > 0 && nativeVeinDelta < featureDelta, "native block progress did not compose with the prepared vein");
              var row = metrics(name, terrain, normal, depth, vein); row.addProperty("veinDelta", adjusted);
              row.addProperty("block", scenario.block); row.addProperty("heldTool", scenario.tool);
              row.addProperty("ordinaryDelta", ordinaryDelta); row.addProperty("featureDelta", featureDelta);
              row.addProperty("nativeVeinDelta", nativeVeinDelta); row.addProperty("obsidianSpeed", scenario.obsidianSpeed);
              row.addProperty("featureGeometryMismatch", difference(ordinaryOverlay, normal[0]));
              report.add(row);
              // Capture representative frames before any pixel acceptance can fail.
              if (distance == 2.5 && stereo == 0) {
                save(name + "-terrain", terrain);
                if (!scenario.feature.equals("none")) save(name + "-feature-off", ordinaryOverlay);
                for (int i = 0; i < 2; i++) {
                  save(name + "-normal-" + (i == 1 ? "on" : "off"), normal[i]);
                  save(name + "-depth-" + (i == 1 ? "on" : "off"), depth[i]);
                  save(name + "-vein-" + (i == 1 ? "on" : "off"), vein[i]);
                }
              }
              log("MINING448 METRICS " + row);
              if (accept) {
                check(row.get("terrainPixels").getAsInt() > 100, "native block plate did not render");
                check(row.get("geometryMismatch").getAsInt() == 0, "setting changed projected crack geometry");
                check(row.get("featureGeometryMismatch").getAsInt() == 0, "mining speed feature changed projected crack geometry");
                check(row.get("veinMismatch").getAsInt() == 0, "vein candidate changed projected crack geometry");
                check(row.get("visibleOn").getAsInt() > 30, "enabled native cracks failed depth test on " + face.name);
                check(row.get("visibleOn").getAsInt() >= row.get("visibleOff").getAsInt(), "enabled offset lost crack coverage");
                check(row.get("nearerOn").getAsInt() > 30, "enabled depth bias did not move fragments toward the camera");
                check(row.get("outsidePlate").getAsInt() <= 4, "cracks escaped the native block silhouette");
              }
            });
          }
        }
      }
    } finally {
      fix.set(controls, oldFix); config.set(null, oldConfig);
      gameplayConfig.set(null, oldGameplay); buildingConfig.set(null, oldBuilding);
      for (var e : mining.entrySet()) e.getKey().set(null, e.getValue());
      mc.player = owner; mc.field_2807 = camera; mc.worldRenderer.field_1803 = oldProgress;
      for (int[] b : blocks.values()) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      for (var e : offsets.entrySet()) e.getKey().setDouble(Tessellator.INSTANCE, e.getValue());
      Files.writeString(OUTPUT.resolve(action + "-metrics.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
    check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "mining renderer fixture wrote preferences");
    log("MINING OVERLAY 448 " + action + " FAILURES " + failures);
    check(failures == 0, "mining overlay failures=" + failures);
  }

  private static void remember(Minecraft mc, Map<String, int[]> blocks, int x, int y, int z) {
    blocks.putIfAbsent(x + "," + y + "," + z, new int[]{x, y, z, mc.world.getBlockId(x, y, z), mc.world.method_1778(x, y, z)});
  }

  private static void feature(Scenario scenario, boolean enabled, Field obsidianSpeed, Object building) throws Exception {
    var gameplay = GameplayConfig.copy();
    gameplay.torchShovel = enabled && scenario.feature.equals("torch");
    gameplay.axesSlashLeaves = enabled && scenario.feature.equals("axe");
    GameplayConfig.preview(gameplay);
    obsidianSpeed.setInt(building, enabled && scenario.feature.equals("obsidian") ? scenario.obsidianSpeed : 0);
  }

  private static float nativeDelta(Minecraft mc, ClientPlayerEntity player, int x, int y, int z) throws Exception {
    var getState = mc.world.getClass().getMethod("getBlockState", int.class, int.class, int.class); getState.setAccessible(true);
    Object state = getState.invoke(mc.world, x, y, z);
    var delta = state.getClass().getMethod("calcBlockBreakingDelta", net.minecraft.entity.player.PlayerEntity.class,
        net.minecraft.world.BlockView.class, class_339.class); delta.setAccessible(true);
    return ((Number)delta.invoke(state, player, mc.world, new class_339(x, y, z))).floatValue();
  }

  private static Pixels terrain(class_13 renderer, int block, int x, int y, int z, double cx, double cy, double cz) {
    GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_ALPHA_TEST);
    GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glColor4f(1, 1, 1, 1);
    Tessellator t = Tessellator.INSTANCE; t.startQuads(); t.translate(-cx, -cy, -cz); t.disableColor();
    renderer.method_51(Block.BLOCKS[block], x, y, z, 1); t.draw(); t.translate(0, 0, 0);
    return read();
  }

  private static Pixels overlay(Minecraft mc, ClientPlayerEntity probe, class_27 hit, boolean depth) {
    if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
    GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDepthMask(true);
    float[] matrix = matrix();
    mc.worldRenderer.field_1803 = .55f;
    mc.worldRenderer.method_1547(probe, hit, 0, null, 1);
    check(Arrays.equals(matrix, matrix()), "native mining draw leaked a model-view transform");
    check(!GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL) && GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR) == 0
        && GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS) == 0 && GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
        "native mining draw leaked polygon/depth state");
    check(GL11.glGetError() == GL11.GL_NO_ERROR, "native mining draw produced a GL error");
    return read();
  }

  private static float[] matrix() {
    FloatBuffer buffer = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
    float[] result = new float[16]; buffer.get(result); return result;
  }

  private static Pixels read() {
    GL11.glFinish(); ByteBuffer bytes = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
    FloatBuffer floats = BufferUtils.createFloatBuffer(SIZE * SIZE);
    GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
    GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, floats);
    byte[] rgba = new byte[bytes.capacity()]; float[] depth = new float[floats.capacity()];
    bytes.get(rgba); floats.get(depth); return new Pixels(rgba, depth);
  }

  private static int difference(Pixels a, Pixels b) {
    int count = 0;
    for (int i = 0; i < SIZE * SIZE; i++) for (int c = 0; c < 3; c++)
      if (a.rgba[i * 4 + c] != b.rgba[i * 4 + c]) { count++; break; }
    return count;
  }

  private static boolean dark(Pixels pixels, int i) {
    return (pixels.rgba[i * 4] & 255) < 240 || (pixels.rgba[i * 4 + 1] & 255) < 240 || (pixels.rgba[i * 4 + 2] & 255) < 240;
  }

  private static JsonObject metrics(String name, Pixels terrain, Pixels[] normal, Pixels[] depth, Pixels[] vein) {
    int plate = 0, off = 0, on = 0, nearer = 0, outside = 0;
    for (int i = 0; i < SIZE * SIZE; i++) {
      if (terrain.depth[i] < 1) {
        plate++;
        if (dark(depth[0], i)) off++;
        if (dark(depth[1], i)) on++;
        if (depth[1].depth[i] < depth[0].depth[i]) nearer++;
      } else if (dark(normal[1], i)) outside++;
    }
    var row = new JsonObject(); row.addProperty("case", name); row.addProperty("terrainPixels", plate);
    row.addProperty("geometryMismatch", difference(normal[0], normal[1]));
    row.addProperty("veinMismatch", difference(normal[0], vein[0]) + difference(normal[1], vein[1]));
    row.addProperty("visibleOff", off); row.addProperty("visibleOn", on); row.addProperty("nearerOn", nearer);
    row.addProperty("outsidePlate", outside); return row;
  }

  private static void save(String name, Pixels pixels) throws Exception {
    var image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
      int i = ((SIZE - 1 - y) * SIZE + x) * 4;
      image.setRGB(x, y, (pixels.rgba[i] & 255) << 16 | (pixels.rgba[i + 1] & 255) << 8 | pixels.rgba[i + 2] & 255);
    }
    ImageIO.write(image, "PNG", OUTPUT.resolve(name + ".png").toFile());
  }

  private static final class Framebuffer implements AutoCloseable {
    private final int oldFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
    private final int oldRenderbuffer = GL11.glGetInteger(EXTFramebufferObject.GL_RENDERBUFFER_BINDING_EXT);
    private final int oldMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE), oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int oldUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int color = GL11.glGenTextures(), framebuffer = EXTFramebufferObject.glGenFramebuffersEXT(), depth = EXTFramebufferObject.glGenRenderbuffersEXT();
    Framebuffer() {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT | GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
      GL20.glUseProgram(0); GL13.glActiveTexture(GL13.GL_TEXTURE0);
      GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPushMatrix(); GL11.glLoadIdentity();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, color);
      GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, SIZE, SIZE, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
      EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, color, 0);
      EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, depth);
      EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, GL14.GL_DEPTH_COMPONENT24, SIZE, SIZE);
      EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, depth);
      check(EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT) == EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT,
          "native mining framebuffer incomplete");
      GL11.glDrawBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT); GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
    }
    void setup(Face face, boolean crouch, int stereo) {
      GL11.glViewport(0, 0, SIZE, SIZE); GL11.glDisable(GL11.GL_SCISSOR_TEST); GL11.glDisable(GL11.GL_FOG);
      GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glEnable(GL11.GL_CULL_FACE); GL11.glCullFace(GL11.GL_BACK); GL11.glFrontFace(GL11.GL_CCW);
      GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL); GL11.glPolygonOffset(0, 0);
      GL11.glColorMask(true, true, true, true); GL11.glDepthMask(true); GL11.glDepthFunc(GL11.GL_LEQUAL); GL11.glDepthRange(0, 1);
      GL11.glClearColor(1, 1, 1, 1); GL11.glClearDepth(1); GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
      GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1); GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
      GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0); GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glFrustum(-.08, .08, -.08, .08, .1, 64);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
      // Native first-person/stereo translations precede view rotation and camera-relative vertices.
      GL11.glTranslatef(stereo * .03f, crouch ? .12f : 0, -.1f);
      GL11.glRotatef(face.pitch, 1, 0, 0); GL11.glRotatef(face.yaw, 0, 1, 0);
      Tessellator.INSTANCE.translate(0, 0, 0);
    }
    @Override public void close() {
      EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFramebuffer);
      EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, oldRenderbuffer);
      GL11.glDeleteTextures(color); EXTFramebufferObject.glDeleteRenderbuffersEXT(depth); EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPopMatrix(); GL11.glPopClientAttrib(); GL11.glPopAttrib();
      GL20.glUseProgram(oldProgram); GL13.glActiveTexture(oldUnit); GL11.glMatrixMode(oldMode);
    }
  }
}
