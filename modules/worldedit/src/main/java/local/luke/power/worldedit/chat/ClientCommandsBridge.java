package local.luke.power.worldedit.chat;

import java.lang.reflect.*;
import java.util.*;
import local.luke.power.worldedit.WorldEditor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Uses ClientCommands' public API without making it a runtime dependency. */
public final class ClientCommandsBridge implements ModInitializer {
  @Override
  public void onInitialize() {
    if (!FabricLoader.getInstance().isModLoaded("power_commands")) return;
    try {
      Class<?> api = Class.forName("local.luke.power.commands.api.Command");
      Method add =
          Class.forName("local.luke.power.commands.api.CommandRegistry")
              .getMethod("add", api);
      List<String> names = new ArrayList<>();
      CommandCatalog.COMMANDS.forEach(n -> names.add("/" + n));
      names.addAll(CommandCatalog.SINGLE);
      for (String name : names) {
        Object command =
            Proxy.newProxyInstance(
                api.getClassLoader(),
                new Class<?>[] {api},
                (proxy, method, args) ->
                    switch (method.getName()) {
                      case "name" -> name;
                      case "command" -> {
                        WorldEditor.command(
                            (Minecraft) FabricLoader.getInstance().getGameInstance(),
                            "/" + String.join(" ", (String[]) args[1]));
                        yield null;
                      }
                      case "manual" -> {
                        Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
                        mc.inGameHud.addChatMessage(
                            ChatFormat.info("Use //help [1-6] for world-editing commands."));
                        yield null;
                      }
                      case "suggestion" -> {
                        String input = (String) args[3];
                        yield CommandCatalog.complete(input).stream()
                            .filter(v -> v.startsWith(input))
                            .map(v -> v.substring(input.length()))
                            .toArray(String[]::new);
                      }
                      case "disableInSingleplayer" -> false;
                      case "needsPermissions" -> true;
                      case "toString" -> "WorldEdit Beta command " + name;
                      case "hashCode" -> System.identityHashCode(proxy);
                      case "equals" -> proxy == args[0];
                      default -> throw new UnsupportedOperationException(method.getName());
                    });
        add.invoke(null, command);
      }
      WorldEditor.LOG.info("Registered WorldEdit Beta commands with the command registry");
    } catch (ReflectiveOperationException | LinkageError e) {
      WorldEditor.LOG.warn(
          "Command integration unavailable; WorldEdit commands still work", e);
    }
  }
}
