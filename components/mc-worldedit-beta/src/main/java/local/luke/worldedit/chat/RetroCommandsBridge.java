package local.luke.worldedit.chat;

import java.lang.reflect.*;
import java.util.*;
import local.luke.worldedit.WorldEditBeta;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Uses RetroCommands' public API without making it a runtime dependency. */
public final class RetroCommandsBridge implements ModInitializer {
  @Override
  public void onInitialize() {
    if (!FabricLoader.getInstance().isModLoaded("retrocommands")) return;
    try {
      Class<?> api = Class.forName("com.matthewperiut.retrocommands.api.Command");
      Method add =
          Class.forName("com.matthewperiut.retrocommands.api.CommandRegistry")
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
                        WorldEditBeta.command(
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
      WorldEditBeta.LOG.info("Registered WorldEdit Beta commands with RetroCommands");
    } catch (ReflectiveOperationException | LinkageError e) {
      WorldEditBeta.LOG.warn(
          "RetroCommands integration unavailable; WorldEdit commands still work", e);
    }
  }
}
