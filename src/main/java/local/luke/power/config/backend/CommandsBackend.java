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
            "Singleplayer commands",
            "Master switch for cheat commands. Takes priority over every command and world"
                + " override. Help and information remain available. World editing has separate"
                + " controls. Saved warps are never deleted.",
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
              "Global command rules",
              "/" + command.name(),
              command.description() + " Global access rule; this world's override takes priority.",
              Setting.Kind.CHOICE,
              new JsonPrimitive(config.rule(command).ordinal()),
              new JsonPrimitive(command.defaultRule().ordinal()),
              0,
              2,
              1,
              List.of("Any mode", "Creative only", "Disabled"),
              false));
      if (!backend.world.isEmpty())
        entries.add(
            new Setting(
                "commands.world." + command.name(),
                backend.id(),
                "Commands",
                "This world",
                "/" + command.name(),
                command.description()
                    + " Use global follows its rule above. Allow bypasses that rule; Block prevents"
                    + " use here. The master switch always wins.",
                Setting.Kind.CHOICE,
                new JsonPrimitive(config.override(backend.world, command.name()).ordinal()),
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
          boolean local = key.startsWith("commands.world.");
          String prefix = local ? "commands.world." : "commands.rule.";
          if (!key.startsWith(prefix))
            throw new IllegalArgumentException("Unknown command setting");
          String name = key.substring(prefix.length());
          if (CommandPermissions.COMMANDS.stream().noneMatch(c -> c.name().equals(name)))
            throw new IllegalArgumentException("Unknown command");
          int ordinal = value.getAsBigDecimal().intValueExact();
          if (ordinal < 0 || ordinal > 2)
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
