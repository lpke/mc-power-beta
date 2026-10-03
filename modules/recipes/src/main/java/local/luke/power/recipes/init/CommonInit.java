package local.luke.power.recipes.init;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import local.luke.power.recipes.api.ModPluginProvider;
import local.luke.power.recipes.api.event.ActionButtonRegisterEvent;
import local.luke.power.recipes.network.c2s.ActionButtonPacket;
import local.luke.power.recipes.network.c2s.GiveItemPacket;
import local.luke.power.recipes.network.c2s.RecipeTransferPacket;
import local.luke.power.recipes.network.s2c.RecipeSyncPacket;
import local.luke.power.recipes.registry.AMIItemRegistry;
import local.luke.power.recipes.registry.RecipeRegistry;
import local.luke.power.recipes.util.AMIHelpers;
import local.luke.power.recipes.util.RecipeBrowser;
import local.luke.power.recipes.util.ModRegistry;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.StationAPI;
import net.modificationstation.stationapi.api.event.init.InitFinishedEvent;
import net.modificationstation.stationapi.api.event.network.packet.PacketRegisterEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.Entrypoint;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import net.modificationstation.stationapi.api.mod.entrypoint.EventBusPolicy;
import net.modificationstation.stationapi.api.registry.PacketTypeRegistry;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Entrypoint(eventBus = @EventBusPolicy(registerInstance = false))
public class CommonInit {
    @Getter
    private static ImmutableMap<Identifier, ModPluginProvider> plugins;
    @Getter
    private static ModRegistry modRegistry;

    @EventListener
    public static void init(InitFinishedEvent event) {
        RecipeBrowser.setInitializing(true);
        RecipeBrowser.setHelpers(new AMIHelpers());
        initPlugins();
        FabricLoader.getInstance().getEntrypointContainers("power_recipes:action", Object.class).forEach(EntrypointManager::setup);
        StationAPI.EVENT_BUS.post(new ActionButtonRegisterEvent());
        initAMI();
        RecipeBrowser.setInitializing(false);
    }

    public static void initPlugins() {
        LinkedHashMap<Identifier, ModPluginProvider> pluginsMap = new LinkedHashMap<>();
        FabricLoader.getInstance().getEntrypointContainers("power_recipes:plugin", ModPluginProvider.class).stream().map(EntrypointContainer::getEntrypoint).forEach(iModPlugin -> pluginsMap.put(iModPlugin.getId(), iModPlugin));

        LinkedHashMap<Identifier, ModPluginProvider> oldPlugins = new LinkedHashMap<>(pluginsMap);
        pluginsMap.clear();

        pluginsMap.put(RecipeBrowser.NAMESPACE.id("vanilla"), oldPlugins.remove(RecipeBrowser.NAMESPACE.id("vanilla")));
        pluginsMap.putAll(oldPlugins);

        pluginsMap.values().forEach(iModPlugin -> {
            try {
                iModPlugin.onAMIHelpersAvailable(RecipeBrowser.getHelpers());
            } catch (RuntimeException e) {
                RecipeBrowser.LOGGER.error("Mod plugin failed: {}/{}", iModPlugin.getId(), iModPlugin.getClass(), e);
                pluginsMap.remove(iModPlugin.getId());
            }
        });
        plugins = ImmutableMap.copyOf(pluginsMap);
        RecipeBrowser.reloadBlacklist();
    }

    @EventListener
    public static void registerPackets(PacketRegisterEvent event){
        Registry.register(PacketTypeRegistry.INSTANCE, RecipeBrowser.NAMESPACE.id("action_button"), ActionButtonPacket.TYPE);
        Registry.register(PacketTypeRegistry.INSTANCE, RecipeBrowser.NAMESPACE.id("give_item"), GiveItemPacket.TYPE);
        Registry.register(PacketTypeRegistry.INSTANCE, RecipeBrowser.NAMESPACE.id("transfer"), RecipeTransferPacket.TYPE);
        Registry.register(PacketTypeRegistry.INSTANCE, RecipeBrowser.NAMESPACE.id("sync"), RecipeSyncPacket.TYPE);
    }

    public static void initAMI() {
        RecipeBrowser.setStarted(true);
        AMIItemRegistry itemRegistry = new AMIItemRegistry();
        RecipeBrowser.setItemRegistry(itemRegistry);
        HashMap<Identifier, ModPluginProvider> plugins = (HashMap<Identifier, ModPluginProvider>) CommonInit.getPlugins().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ArrayList<Identifier> badPlugins = new ArrayList<>();

        plugins.values().forEach(iModPlugin -> {
            try {
                iModPlugin.onItemRegistryAvailable(itemRegistry);
            } catch (RuntimeException e) {
                RecipeBrowser.LOGGER.error("Mod plugin failed: {}/{}", iModPlugin.getId(), iModPlugin.getClass(), e);
                badPlugins.add(iModPlugin.getId());
            }
        });

        badPlugins.forEach(plugins::remove);
        modRegistry = new ModRegistry();

        plugins.values().forEach(iModPlugin -> {
            try {
                iModPlugin.register(modRegistry);
                RecipeBrowser.LOGGER.info("Registered plugin: {}/{}", iModPlugin.getId(), iModPlugin.getClass().getName());
            } catch (RuntimeException e) {
                RecipeBrowser.LOGGER.error("Mod plugin failed: {}/{}", iModPlugin.getId(), iModPlugin.getClass(), e);
                badPlugins.add(iModPlugin.getId());
            }
        });

        badPlugins.forEach(plugins::remove);
        RecipeRegistry recipeRegistry = modRegistry.createRecipeRegistry();
        RecipeBrowser.setRecipeRegistry(recipeRegistry);

        plugins.values().forEach(iModPlugin -> {
            RecipeBrowser.LOGGER.info("Initializing plugin {}", iModPlugin.getName());
            try {
                iModPlugin.onRecipeRegistryAvailable(recipeRegistry);
            } catch (RuntimeException e) {
                RecipeBrowser.LOGGER.error("Mod plugin failed: {}/{}", iModPlugin.getId(), iModPlugin.getClass(), e);
                badPlugins.add(iModPlugin.getId());
            }
        });
        badPlugins.forEach(plugins::remove);
        CommonInit.plugins = ImmutableMap.copyOf(plugins);
        if (!badPlugins.isEmpty()) {
            RecipeBrowser.LOGGER.error("List of failed plugins: {}", badPlugins.stream().map(Object::toString).collect(Collectors.joining(", ")));

        }
    }
}
