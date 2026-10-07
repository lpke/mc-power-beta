package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.commands.CommandContext;
import local.luke.power.config.*;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.permissions.CommandPermissions.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;

public final class CommandsBackend implements Backend {
  private final Minecraft mc;
  private final String world;

  private CommandsBackend(Minecraft mc) {
    this.mc = mc;
    world = CommandContext.world(mc);
  }

  public static void register(ConfigSession session, Minecraft mc) {
    CommandsBackend backend = new CommandsBackend(mc);
    Settings config = CommandPermissions.current();
    List<Setting> entries = new ArrayList<>();
    entries.add(
        new Setting(
            "commands.enabled",
            backend.id(),
            "Commands",
            "Access",
            "Cheat commands",
            "Allow cheat commands when this world's Cheats enabled setting is on. Individual rules"
                + " can further restrict access. Saved data is kept when access is disabled.",
            Setting.Kind.BOOLEAN,
            new JsonPrimitive(config.enabled),
            new JsonPrimitive(true),
            0,
            1,
            1,
            List.of(),
            false));
    for (Command command : CommandPermissions.COMMANDS) {
      entries.add(
          new Setting(
              "commands.rule." + command.name(),
              backend.id(),
              "Commands",
              command.cheat() ? "Cheat commands" : "Non-cheat commands",
              "/" + (command.name().equals("toggledownfall") ? "weather" : command.name()),
              command.description() + " Global access rule; this world's override takes priority.",
              Setting.Kind.CHOICE,
              new JsonPrimitive(config.rule(command).ordinal()),
              new JsonPrimitive(command.defaultRule().ordinal()),
              0,
              2,
              1,
              List.of("Enabled", "Disabled"),
              false));
      if (!backend.world.isEmpty())
        entries.add(
            new Setting(
                "commands.world." + command.name(),
                backend.id(),
                "Commands",
                command.cheat()
                    ? "Cheat commands in this world"
                    : "Non-cheat commands in this world",
                "/" + (command.name().equals("toggledownfall") ? "weather" : command.name()),
                command.description()
                    + " Use global follows its rule above. Allow bypasses that rule; Block prevents"
                    + " use here. " + (command.cheat() ? "Cheats and Cheat commands must both be enabled."
                        : "Cheats and Cheat commands do not restrict this command."),
                Setting.Kind.CHOICE,
                new JsonPrimitive(config.override(backend.world, command.name()).ordinal()),
                new JsonPrimitive(0),
                0,
                2,
                1,
                List.of("Use global", "Allow", "Block"),
                false));
    }
    for (String mode : List.of("creative", "spectator")) {
      String label = "Enter " + mode;
      String description =
          "Allow entering "
              + mode
              + " through commands or the mode switcher. Cheats enabled and /gamemode access also"
              + " apply. Returning to survival is always allowed.";
      entries.add(
          new Setting(
              "commands.mode." + mode,
              backend.id(),
              "Commands",
              "Game mode access",
              label,
              description,
              Setting.Kind.BOOLEAN,
              new JsonPrimitive(config.modeAccess.getOrDefault(mode, true)),
              new JsonPrimitive(true),
              0,
              1,
              1,
              List.of(),
              false));
      if (!backend.world.isEmpty())
        entries.add(
            new Setting(
                "commands.worldMode." + mode,
                backend.id(),
                "Commands",
                "Game mode access in this world",
                label,
                description
                    + " Allow overrides the global mode setting; Block prevents entering it here.",
                Setting.Kind.CHOICE,
                new JsonPrimitive(
                    config
                        .worldModeAccess
                        .getOrDefault(backend.world, Map.of())
                        .getOrDefault(mode, CommandPermissions.Override.INHERIT)
                        .ordinal()),
                new JsonPrimitive(0),
                0,
                2,
                1,
                List.of("Use global", "Allow", "Block"),
                false));
    }
    session.add(backend, entries);
  }

  public String id() {
    return "commandAccess";
  }

  public List<Path> files() {
    return List.of(PowerConfig.path());
  }

  public void validate(Map<String, JsonElement> values) {
    edited(values).validate();
  }

  private Settings edited(Map<String, JsonElement> values) {
    Settings next = CommandPermissions.copy();
    values.forEach(
        (key, value) -> {
          if (key.equals("commands.enabled")) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
              throw new IllegalArgumentException("Choose On or Off");
            next.enabled = value.getAsBoolean();
            return;
          }
          if (key.startsWith("commands.mode.") || key.startsWith("commands.worldMode.")) {
            boolean worldMode = key.startsWith("commands.worldMode.");
            String mode =
                key.substring((worldMode ? "commands.worldMode." : "commands.mode.").length());
            if (!List.of("creative", "spectator").contains(mode))
              throw new IllegalArgumentException("Unknown game mode");
            if (worldMode) {
              if (world.isEmpty() || !world.equals(CommandContext.world(mc)))
                throw new IllegalArgumentException("The open world changed. Reopen Options.");
              int ordinal = value.getAsBigDecimal().intValueExact();
              if (ordinal < 0 || ordinal > 2)
                throw new IllegalArgumentException("Choose a listed rule");
              var modes = next.worldModeAccess.computeIfAbsent(world, k -> new LinkedHashMap<>());
              if (ordinal == 0) modes.remove(mode);
              else modes.put(mode, CommandPermissions.Override.values()[ordinal]);
              if (modes.isEmpty()) next.worldModeAccess.remove(world);
            } else {
              if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("Choose On or Off");
              next.modeAccess.put(mode, value.getAsBoolean());
            }
            return;
          }
          boolean local = key.startsWith("commands.world.");
          String prefix = local ? "commands.world." : "commands.rule.";
          if (!key.startsWith(prefix))
            throw new IllegalArgumentException("Unknown command setting");
          String name = key.substring(prefix.length());
          if (CommandPermissions.COMMANDS.stream().noneMatch(c -> c.name().equals(name)))
            throw new IllegalArgumentException("Unknown command");
          int ordinal = value.getAsBigDecimal().intValueExact();
          if (ordinal < 0 || ordinal > (local ? 2 : 1))
            throw new IllegalArgumentException("Choose a listed rule");
          if (local) {
            if (world.isEmpty() || !world.equals(CommandContext.world(mc)))
              throw new IllegalArgumentException("The open world changed. Reopen Options.");
            Map<String, CommandPermissions.Override> overrides =
                next.worlds.computeIfAbsent(world, k -> new LinkedHashMap<>());
            if (ordinal == 0) overrides.remove(name);
            else overrides.put(name, CommandPermissions.Override.values()[ordinal]);
            if (overrides.isEmpty()) next.worlds.remove(world);
          } else next.rules.put(name, Rule.values()[ordinal]);
        });
    return next;
  }

  public boolean previewsAutomatically(Setting setting) {
    return true;
  }

  public void preview(Map<String, JsonElement> values) {
    CommandPermissions.preview(edited(values));
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    CommandPermissions.save(edited(values));
  }
}
