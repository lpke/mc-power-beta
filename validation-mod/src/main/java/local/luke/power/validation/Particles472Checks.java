package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.render.BlockBreakingParticleControls;
import local.luke.power.status.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.class_75;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Native emitters and physical-key phases, confined to a muted disposable world. */
final class Particles472Checks {
  private static final String SETTING = "power_compat:config.PARTICLES_CONFIG.blockBreakingParticles";
  private static final String CONTROL = "keys.power_beta.blockBreakingParticles";
  private static final String HUD = "activeTweaks.blockBreakingParticles";
  private static Integer originalKey;
  private static boolean originalPreference, originalAll;
  private Particles472Checks() {}
  static void run(Minecraft mc, String phase) throws Exception {
    check(mc.world != null && mc.player != null && Display.isActive(), "focus a disposable world");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute disposable client");
    if (phase.equals("capture")) { capture(mc); return; }
    if (phase.equals("positive")) { positive(mc); return; }
    if (phase.equals("positive-save")) { positiveSave(mc); return; }
    var config = new ParticleConfig();
    if (phase.equals("arm")) {
      check(originalKey == null && BlockBreakingParticleControls.TOGGLE.code == 0, "control must start unassigned");
      originalKey = BlockBreakingParticleControls.TOGGLE.code; originalPreference = config.preference(); originalAll = config.allDisabled();
      config.preference(true); config.allDisabled(false); BlockBreakingParticles.reset();
      BlockBreakingParticleControls.TOGGLE.code = Keyboard.KEY_F10; Bindings.register(mc.options.allKeys);
      mc.setScreen(null); log("PARTICLES472 armed F10"); return;
    }
    if (phase.equals("off") || phase.equals("on")) {
      boolean enabled = phase.equals("on");
      check(BlockBreakingParticles.enabled() == enabled && BlockBreakingParticles.inverted() != enabled, "physical F10 state");
      check(config.preference(), "hotkey changed saved preference");
      check(Objects.equals(TweakIndicators.label(TweakIndicators.Tweak.BLOCK_BREAKING_PARTICLES), enabled ? null : "Block breaking particles disabled"), "physical toggle HUD state");
      log("PARTICLES472 physical " + phase + " PASS"); return;
    }
    if (phase.equals("finish")) {
      check(originalKey != null, "no physical fixture to restore");
      BlockBreakingParticleControls.TOGGLE.code = originalKey; originalKey = null;
      config.preference(originalPreference); config.allDisabled(originalAll);
      BlockBreakingParticles.reset(); Bindings.register(mc.options.allKeys); return;
    }
    check(phase.equals("native"), "expected capture, native, arm, off, on or finish");
    check(mc.currentScreen == null && !mc.paused, "close menus first");
    failures = 0; PowerConfig.flushPending(); byte[] disk = Files.readAllBytes(PowerConfig.path());
    boolean preference = config.preference(), all = config.allDisabled(), inverted = BlockBreakingParticles.inverted();
    int x = (int)Math.floor(mc.player.x) + 3, y = Math.max(1, Math.min(124, (int)Math.floor(mc.player.y) + 3)), z = (int)Math.floor(mc.player.z);
    check(mc.world.getBlockId(x, y, z) == 0, "fixture needs nearby air");
    try {
      mc.world.method_154(x, y, z, 4, 0);
      for (boolean saved : new boolean[]{false, true}) for (boolean temporary : new boolean[]{false, true}) {
        test("PARTICLES472 native hit/break saved=" + saved + " temporary=" + temporary, () -> {
          config.preference(saved); config.allDisabled(false);
          BlockBreakingParticles.reset(); if (temporary) BlockBreakingParticles.toggle();
          boolean enabled = saved != temporary;
          var particles = new class_75(mc.world, mc.textureManager);
          particles.method_321(x, y, z, 1);
          check(count(particles) == (enabled ? 1 : 0), "native hit particle gate");
          particles.method_323(mc.world); particles.method_322(x, y, z, 4, 0);
          check(enabled ? count(particles) > 0 : count(particles) == 0, "native break particle gate");
          int before = count(particles); particles.method_325(new Particle(mc.world, x, y, z, 0, 0, 0));
          check(count(particles) == before + 1, "unrelated particle was suppressed");
          check(config.preference() == saved, "toggle rewrote saved preference");
          String label = TweakIndicators.label(TweakIndicators.Tweak.BLOCK_BREAKING_PARTICLES);
          check(Objects.equals(label, temporary ? "Block breaking particles " + (enabled ? "enabled" : "disabled") : null), "HUD does not report effective override state");
          check(new StatusSettings().includes(TweakIndicators.Tweak.BLOCK_BREAKING_PARTICLES), "HUD inclusion defaults off");
        });
      }
      test("PARTICLES472 disable all wins over either saved preference and temporary inversion", () -> {
        config.allDisabled(true);
        for (boolean saved : new boolean[]{false, true}) for (boolean temporary : new boolean[]{false, true}) {
          config.preference(saved); BlockBreakingParticles.reset(); if (temporary) BlockBreakingParticles.toggle();
          var particles = new class_75(mc.world, mc.textureManager); particles.method_321(x, y, z, 1); particles.method_322(x, y, z, 4, 0);
          check(count(particles) == 0 && !BlockBreakingParticles.enabled(), "global particle suppression bypassed");
        }
      });
      test("PARTICLES472 menu input cannot activate toggle", () -> {
        int key = BlockBreakingParticleControls.TOGGLE.code; var previous = mc.currentScreen;
        try {
          BlockBreakingParticles.reset(); BlockBreakingParticleControls.TOGGLE.code = Keyboard.KEY_F10;
          mc.setScreen(new net.minecraft.class_525()); BlockBreakingParticleControls.press(Keyboard.KEY_F10);
          check(!BlockBreakingParticles.inverted(), "menu accepted gameplay toggle");
        } finally { BlockBreakingParticleControls.TOGGLE.code = key; mc.setScreen(previous); }
      });
      check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "temporary particle checks wrote settings");
    } finally {
      mc.world.method_154(x, y, z, 0, 0);
      config.preference(preference); config.allDisabled(all);
      BlockBreakingParticles.reset(); if (inverted) BlockBreakingParticles.toggle();
    }
    log("PARTICLES472 failures=" + failures); check(failures == 0, "particle regression failures=" + failures);
  }
  private static final class ParticleConfig {
    private final Object value;
    ParticleConfig() throws Exception {
      Object owner = Class.forName("local.luke.power.compat.Config").getField("config").get(null);
      value = owner.getClass().getField("PARTICLES_CONFIG").get(owner);
    }
    boolean preference() throws Exception { return (Boolean)value.getClass().getField("blockBreakingParticles").get(value); }
    void preference(boolean enabled) throws Exception { value.getClass().getField("blockBreakingParticles").set(value, enabled); }
    boolean allDisabled() throws Exception { return (Boolean)value.getClass().getField("disableAllParticles").get(value); }
    void allDisabled(boolean disabled) throws Exception { value.getClass().getField("disableAllParticles").set(value, disabled); }
  }
  private static int count(class_75 particles) { return Integer.parseInt(particles.method_326()); }
  private static void positive(Minecraft mc) throws Exception {
    String prefix = "power_compat:config.PARTICLES_CONFIG.";
    Object config = Class.forName("local.luke.power.compat.Config").getField("config").get(null);
    Object particles = config.getClass().getField("PARTICLES_CONFIG").get(config);
    var session = SettingsRegistry.open(mc);
    var rows = session.settings().stream().filter(s -> s.id.startsWith(prefix)).toList();
    check(rows.size() == 17, "missing particle controls");
    PowerConfig.flushPending(); byte[] before = Files.readAllBytes(PowerConfig.path());
    var original = new LinkedHashMap<String, Boolean>();
    try {
      for (Setting row : rows) {
        String field = row.id.substring(prefix.length());
        boolean raw = (Boolean)particles.getClass().getField(field).get(particles); original.put(field, raw);
        boolean inverted = field.startsWith("disable");
        check(row.value.getAsBoolean() == (inverted ? !raw : raw), "incorrect positive value for " + row.id);
        check(!row.label.startsWith("Disable") && row.defaultValue.getAsBoolean(), "negative particle label/default");
        for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, row.id, row.powerBetaDefault).getAsBoolean(), "particle default not On");
        row.value = new com.google.gson.JsonPrimitive(!row.value.getAsBoolean());
      }
      session.preview(true);
      for (Setting row : rows) {
        String field = row.id.substring(prefix.length()); boolean raw = (Boolean)particles.getClass().getField(field).get(particles);
        check(raw == (field.startsWith("disable") ? !row.value.getAsBoolean() : row.value.getAsBoolean()), "positive preview did not change live effect");
      }
      check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "particle preview saved preferences");
    } finally { session.discard(); }
    for (var entry : original.entrySet()) check(entry.getValue().equals(particles.getClass().getField(entry.getKey()).get(particles)), "Cancel did not restore effect");
    check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "particle Cancel changed preferences");
    log("PARTICLES472 all17 positive labels/defaults/current values, native previews and Cancel PASS");
  }
  private static void positiveSave(Minecraft mc) throws Exception {
    String prefix = "power_compat:config.PARTICLES_CONFIG.";
    PowerConfig.flushPending(); var original = PowerConfig.document();
    var session = SettingsRegistry.open(mc); var saved = new LinkedHashMap<String, com.google.gson.JsonElement>();
    session.settings().stream().filter(s -> s.id.startsWith(prefix)).forEach(s -> saved.put(s.id, s.value.deepCopy()));
    var changed = Set.of(prefix + "disableAllParticles", prefix + "disableFireSmokeParticle", prefix + "blockBreakingParticles");
    try {
      session.settings().stream().filter(s -> changed.contains(s.id)).forEach(s -> s.value = new com.google.gson.JsonPrimitive(false));
      session.save(Path.of(".").toAbsolutePath());
      var raw = PowerConfig.section("power_compat:config").getAsJsonObject("PARTICLES_CONFIG");
      check(raw.get("disableAllParticles").getAsBoolean() && raw.get("disableFireSmokeParticle").getAsBoolean()
          && !raw.get("blockBreakingParticles").getAsBoolean(), "native save inverted particle flags incorrectly");
      var reopened = SettingsRegistry.open(mc);
      for (Setting row : reopened.settings()) if (saved.containsKey(row.id))
        check(row.value.equals(changed.contains(row.id) ? new com.google.gson.JsonPrimitive(false) : saved.get(row.id)), "reopened particle value changed");
    } finally {
      var restore = SettingsRegistry.open(mc);
      restore.settings().stream().filter(s -> saved.containsKey(s.id)).forEach(s -> s.value = saved.get(s.id).deepCopy());
      restore.preview(true); PowerConfig.write(original);
    }
    check(original.equals(PowerConfig.document()), "particle save fixture did not restore settings");
    log("PARTICLES472 native Apply/reopen preserves stored IDs and positive values PASS");
  }
  private static void capture(Minecraft mc) throws Exception {
    Path report = Path.of("power-beta-data/reports/particles-472"); Files.createDirectories(report);
    var previous = mc.currentScreen;
    int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE); GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      for (String id : List.of(CONTROL, SETTING)) for (int[] size : id.equals(CONTROL) ? new int[][]{{854,480},{320,240}} : new int[][]{{854,480}}) {
        var screen = new PowerOptionsScreen(previous); screen.init(mc, size[0], size[1]);
        Setting setting = screen.session().settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
        field(screen, "page", setting.page); ((Set<?>)field(screen, "collapsed")).clear();
        field(screen, "scroll", 0d); call(screen, "layout");
        for (Object row : (List<?>)field(screen, "rows")) if (call(row, "setting") == setting) {
          field(screen, "scroll", Math.max(0, ((Number)call(row, "y")).doubleValue() - (id.equals(SETTING) ? 80 : 28))); break;
        }
        call(screen, "layout"); SettingsSnapshot.render(mc, screen, size[0], size[1], report.resolve((id.equals(CONTROL) ? "control-" : "setting-") + size[0] + ".png"));
        if (id.equals(CONTROL)) check(setting.label.equals("Block breaking particles (toggle)") && setting.value.getAsInt() == 0, "control label/default");
        if (id.equals(SETTING)) for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, id, setting.powerBetaDefault).getAsBoolean(), "native particle default must be on");
      }
      var session = SettingsRegistry.open(mc);
      check(session.settings().stream().filter(s -> s.id.equals(HUD)).findFirst().orElseThrow().value.getAsBoolean(), "new HUD inclusion missing/default off");
      log("PARTICLES472 UI defaults/captures PASS");
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glPopAttrib(); GL11.glMatrixMode(mode); mc.setScreen(previous);
    }
  }
}
