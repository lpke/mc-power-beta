package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.commands.CommandContext;
import local.luke.power.config.*;
import local.luke.power.permissions.*;
import local.luke.power.worldedit.WorldEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;

/** Exercises permissions through chat and the settings transaction in a disposable world. */
public final class CheatsChecks {
  public static void creation(Minecraft mc) throws Exception {
    failures = 0;
    mc.setWorld(null);
    for (boolean enabled : new boolean[] {false, true}) {
      final String name = "Cheats creation " + (enabled ? "on" : "off");
      test(
          "world creation, save and world-list metadata: " + enabled,
          () -> {
            var screen = new net.minecraft.class_180(null);
            mc.setScreen(screen);
            var input = (local.luke.power.validation.mixin.ScreenInput) screen;
            var toggle =
                input.power$buttons().stream().filter(b -> b.id == 2).findFirst().orElseThrow();
            check(toggle.text.equals("Cheats: Off"), "new-world default differs");
            if (enabled) {
              input.power$click(toggle.x + 5, toggle.y + 5, 0);
              check(toggle.text.equals("Cheats: On"), "creation toggle failed");
            }
            ((net.minecraft.client.gui.widget.TextFieldWidget) UiChecks.field(screen, "field_632"))
                .setText(name);
            ((net.minecraft.client.gui.widget.TextFieldWidget) UiChecks.field(screen, "field_633"))
                .setText("17320261003");
            input.power$key('\0', org.lwjgl.input.Keyboard.KEY_RIGHT);
            String folder = (String) UiChecks.field(screen, "field_634");
            var create =
                input.power$buttons().stream().filter(b -> b.id == 0).findFirst().orElseThrow();
            input.power$click(create.x + 5, create.y + 5, 0);
            check(
                mc.world != null
                    && ((CheatWorld) mc.world.method_262()).power$cheatsEnabled() == enabled,
                "new world lost chosen cheats flag");
            check(
                Class.forName("local.luke.power.creative.api.ModePlayer")
                    .getMethod("power_mode")
                    .invoke(mc.player)
                    .toString()
                    .equals("SURVIVAL"),
                "cheats forced creative mode");
            mc.world.method_195(true, null);
            mc.setWorld(null);
            var metadata =
                mc.method_2127().method_1002().stream()
                    .filter(value -> ((net.minecraft.class_591) value).method_1956().equals(folder))
                    .findFirst()
                    .orElseThrow();
            check(
                ((CheatWorld) metadata).power$cheatsEnabled() == enabled,
                "world-list suffix metadata incorrect");
          });
    }
    mc.setScreen(new net.minecraft.client.gui.screen.world.SelectWorldScreen(null));
    log("CHEATS CREATION FAILURES " + failures);
  }

  public static void run(Minecraft mc) throws Exception {
    failures = 0;
    CheatWorld world = (CheatWorld) mc.world.method_262();
    boolean before = world.power$cheatsEnabled();
    var permissions = CommandPermissions.copy();
    var editor = WorldEditor.settings().copy();
    Object mode =
        Class.forName("local.luke.power.creative.api.ModePlayer")
            .getMethod("power_mode")
            .invoke(mc.player);
    try {
      CommandPermissions.preview(new CommandPermissions.Settings());
      var allowedEditor = editor.copy();
      allowedEditor.enabled = true;
      WorldEditor.preview(allowedEditor);
      world.power$cheatsEnabled(true);
      ChatChecks.submit(mc, "/gamemode survival");
      test(
          "cheats default off and persist through world property copies and NBT",
          () -> {
            NbtCompound tag = new NbtCompound();
            var fresh = new WorldProperties(tag);
            check(!((CheatWorld) fresh).power$cheatsEnabled(), "new world starts with cheats");
            tag.putBoolean(CheatWorld.TAG, true);
            var loaded = new WorldProperties(tag);
            check(
                ((CheatWorld) new WorldProperties(loaded)).power$cheatsEnabled(),
                "world copy lost cheats");
            check(
                new WorldProperties(loaded.asNbt()).asNbt().getBoolean(CheatWorld.TAG),
                "saved NBT lost cheats");
            tag.putBoolean(CheatWorld.TAG, false);
            tag.putBoolean("Creative", true);
            check(
                !((CheatWorld) new WorldProperties(tag)).power$cheatsEnabled(),
                "explicit off ignored for legacy creative world");
            NbtCompound legacy = new NbtCompound();
            legacy.putBoolean("Creative", true);
            check(
                ((CheatWorld) new WorldProperties(legacy)).power$cheatsEnabled(),
                "legacy creative access lost");
            NbtCompound oldPlayer = new NbtCompound();
            oldPlayer.putString(local.luke.power.storage.LegacyKeys.original("PowerBetaGameMode"), "SPECTATOR");
            NbtCompound oldSpectator = new NbtCompound();
            oldSpectator.put("Player", oldPlayer);
            check(((CheatWorld) new WorldProperties(oldSpectator)).power$cheatsEnabled(), "legacy spectator access lost");
          });
      test(
          "cheats enable commands and editing while still in survival",
          () -> {
            check(CommandPermissions.allowed("give"), "survival give restricted");
            check(WorldEditor.permitted(mc), "survival editor restricted");
            check(
                ChatChecks.submit(mc, "/tp @s ~ ~ ~").stream().noneMatch(s -> s.contains("§c")),
                "survival tp restricted");
          });
      test(
          "applying cheats off returns to survival and blocks all entry paths",
          () -> {
            ChatChecks.submit(mc, "/gamemode creative");
            ConfigSession session = SettingsRegistry.open(mc);
            find(session, "world.cheats").value = new JsonPrimitive(false);
            check(
                !SettingAccess.reason(session, find(session, "creative.flight")).isEmpty(),
                "draft lock missing");
            session.save(Path.of(".").toAbsolutePath());
            check(!world.power$cheatsEnabled(), "cheats flag unchanged");
            Object current =
                Class.forName("local.luke.power.creative.api.ModePlayer")
                    .getMethod("power_mode")
                    .invoke(mc.player);
            check(current.toString().equals("SURVIVAL"), "player remains creative");
            for (String command :
                List.of(
                    "/gamemode creative",
                    "/gm spectator",
                    "/give @s 1",
                    "/tp @s ~ ~ ~",
                    "//wand",
                    "//set stone")) {
              var messages = ChatChecks.submit(mc, command);
              check(
                  messages.size() == 1 && messages.get(0).contains("Cheats"),
                  "cheat bypass or duplicate output: " + command + messages);
            }
            check(!WorldEditor.permitted(mc), "wand still permitted");
            check(!CommandPermissions.modeDenial("spectator").isEmpty(), "switcher bypass");
            check(CommandPermissions.modeDenial("survival").isEmpty(), "survival locked");
            check(
                ChatChecks.submit(mc, "/seed").stream().noneMatch(s -> s.contains("§c")),
                "information command locked");
            check(
                ChatChecks.submit(mc, "//help").stream().noneMatch(s -> s.contains("§c")),
                "editor help locked");
          });
      test(
          "locked options stay visible and non-cheat controls remain editable",
          () -> {
            ConfigSession session = SettingsRegistry.open(mc);
            for (String id :
                List.of(
                    "commands.rule.tp",
                    "commands.world.tp",
                    "worldedit.enabled",
                    "creative.modePicker"))
              check(
                  !SettingAccess.reason(session, find(session, id)).isEmpty(),
                  "missing lock: " + id);
            for (String id :
                List.of(
                    "commands.rule.seed",
                    "world.cheats",
                    "tweaks.placement.enabled",
                    "power_camera:config.enabled",
                    "creative.sprintToggle",
                    "creative.sprintMultiplier"))
              check(
                  SettingAccess.reason(session, find(session, id)).isEmpty(),
                  "QoL incorrectly locked: " + id);
            check(
                session.settings().stream().noneMatch(s -> s.id.equals("worldedit.creativeOnly")),
                "obsolete creative restriction remains");
          });
      test(
          "toggle keeps per-world restrictions and saved warps",
          () -> {
            var custom = CommandPermissions.copy();
            custom.worlds.put(
                CommandContext.world(mc), Map.of("give", CommandPermissions.Override.BLOCK));
            custom.worldModeAccess.put(
                CommandContext.world(mc), Map.of("spectator", CommandPermissions.Override.BLOCK));
            CommandPermissions.preview(custom);
            var warps = ((local.luke.power.commands.api.PlayerWarps) mc.player).spc$getWarpString();
            ConfigSession session = SettingsRegistry.open(mc);
            find(session, "world.cheats").value = new JsonPrimitive(true);
            session.save(Path.of(".").toAbsolutePath());
            check(
                world.power$cheatsEnabled() && !CommandPermissions.allowed("give"),
                "world restrictions erased");
            check(
                !CommandPermissions.modeDenial("spectator").isEmpty()
                    && CommandPermissions.modeDenial("creative").isEmpty(),
                "mode restrictions incorrect");
            check(
                warps.equals(
                    ((local.luke.power.commands.api.PlayerWarps) mc.player).spc$getWarpString()),
                "saved warps changed");
          });
    } finally {
      CommandPermissions.preview(permissions);
      WorldEditor.preview(editor);
      world.power$cheatsEnabled(true);
      ChatChecks.submit(mc, "/gamemode " + mode.toString().toLowerCase(Locale.ROOT));
      world.power$cheatsEnabled(before);
      mc.setScreen(null);
    }
    log("CHEATS FAILURES " + failures);
  }
}
