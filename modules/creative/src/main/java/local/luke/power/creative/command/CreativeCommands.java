package local.luke.power.creative.command;

import java.lang.reflect.*;
import java.util.*;
import local.luke.power.creative.*;
import local.luke.power.creative.api.GameMode;
import local.luke.power.creative.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class CreativeCommands {
  private CreativeCommands() {}

  public static boolean execute(String input) {
    String[] args = input.trim().split("\\s+");
    if (args.length == 0 || !Set.of("/gamemode", "/gm", "/fly").contains(args[0].toLowerCase(Locale.ROOT)))
      return false;
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    if (mc.level != null && mc.level.isRemote) return false;
    if (!ClientRuntime.local(mc)) return true;
    try {
      if (args[0].equalsIgnoreCase("/fly")) {
        if (args.length == 2 && args[1].equalsIgnoreCase("help")) { flyHelp(mc); return true; }
        if (args.length != 1) throw new IllegalArgumentException("Usage: /fly");
        if (Modes.spectator(mc.player)) throw new IllegalArgumentException("Spectator always flies. Change game mode to use /fly.");
        String denied = local.luke.power.permissions.CommandPermissions.denial("fly");
        if (!denied.isEmpty()) throw new IllegalArgumentException(denied);
        var state = (local.luke.power.creative.api.ModePlayer) mc.player;
        boolean enabled = !Modes.flightAvailable(mc.player);
        if (mc.player.creative_isCreative()) {
          state.power_flightOverride(-1);
          Config.flight(enabled);
        } else state.power_flightOverride(enabled ? 1 : 0);
        FlightController.reset();
        mc.player.creative_setFlying(enabled && mc.player.vehicle == null);
        mc.overlay.addChatMessage("§7Flight " + (enabled ? "enabled" : "disabled") + ".§r");
        return true;
      }
      if (args.length == 2 && args[1].equalsIgnoreCase("help")) {
        mc.overlay.addChatMessage("§b/gamemode <survival|creative|spectator> [target]§r");
        return true;
      }
      if (args.length < 2 || args.length > 3)
        throw new IllegalArgumentException(
            "Usage: /gamemode <survival|creative|spectator> [target]");
      if (args.length == 3 && !Set.of("@s", "@p", "@a", "@r", mc.player.name).contains(args[2]))
        throw new IllegalArgumentException("Only the local player can be targeted.");
      if (ClientRuntime.freecam() || mc.viewEntity != mc.player)
        throw new IllegalArgumentException("Exit freecam before changing gamemode.");
      Modes.change(mc, GameMode.parse(args[1]));
    } catch (IllegalArgumentException e) {
      mc.overlay.addChatMessage("\u00a7c" + e.getMessage() + "\u00a7r");
    }
    return true;
  }

  private static void flyHelp(Minecraft mc) {
    mc.overlay.addChatMessage("§b/fly §7Toggle flight ability without changing game mode. Requires allow_flight outside Creative; unavailable in Spectator.§r");
  }

  public static void register() {
    local.luke.power.permissions.CommandPermissions.creativePlayer(() -> {
      Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
      return ClientRuntime.local(mc) && mc.player.creative_isCreative();
    });
    if (!FabricLoader.getInstance().isModLoaded("power_commands")) return;
    try {
      Class<?> api = Class.forName("local.luke.power.commands.api.Command");
      Method nameMethod = api.getMethod("name");
      List<Object> commands =
          (List<Object>)
              Class.forName("local.luke.power.commands.util.RetroChatUtil")
                  .getField("commands")
                  .get(null);
      // Replace the survival/creative-only command using the command registry.
      for (Iterator<Object> it = commands.iterator(); it.hasNext(); ) {
        String name = (String) nameMethod.invoke(it.next());
        if (name.equals("gamemode") || name.equals("gm") || name.equals("fly")) it.remove();
      }
      for (String name : List.of("gamemode", "gm", "fly"))
        commands.add(
            Proxy.newProxyInstance(
                api.getClassLoader(),
                new Class<?>[] {api},
                (proxy, method, args) ->
                    switch (method.getName()) {
                      case "name" -> name;
                      case "command" -> {
                        execute("/" + String.join(" ", (String[]) args[1]));
                        yield null;
                      }
                      case "manual" -> {
                        Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
                        if (name.equals("fly")) flyHelp(mc);
                        else mc.overlay.addChatMessage(
                            "\u00a7b/gamemode <survival|creative|spectator> [target]\u00a7r");
                        yield null;
                      }
                      case "suggestion" -> {
                        int index = (Integer) args[1];
                        String prefix = (String) args[2];
                        List<String> choices =
                            index == 1 && name.equals("fly") ? List.of("help") : index == 1
                                ? List.of("survival", "creative", "spectator")
                                : index == 2 ? List.of("@s", "@p", "@a") : List.of();
                        yield choices.stream()
                            .filter(v -> local.luke.power.chat.ChatCompletion.startsWith(v, prefix))
                            .map(v -> v.substring(prefix.length()))
                            .toArray(String[]::new);
                      }
                      case "available" -> !name.equals("fly") || local.luke.power.permissions.CommandPermissions.allowed("fly");
                      case "helpGroup" -> "";
                      case "disableInSingleplayer" -> false;
                      case "needsPermissions" -> true;
                      case "toString" -> "CreativeFeatures " + name;
                      case "hashCode" -> System.identityHashCode(proxy);
                      case "equals" -> proxy == args[0];
                      default -> throw new UnsupportedOperationException(method.getName());
                    }));
    } catch (ReflectiveOperationException | LinkageError e) {
      Config.LOG.warn("Command integration unavailable", e);
    }
  }
}
