package local.luke.power.visual;

import java.io.IOException;
import javax.imageio.ImageIO;
import local.luke.power.PowerBeta;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.client.event.resource.AssetsResourceReloaderRegisterEvent;
import net.modificationstation.stationapi.api.resource.ResourceManager;
import net.modificationstation.stationapi.api.resource.SynchronousResourceReloader;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.mixin.render.client.TextureManagerAccessor;

/** Native texture refresh runs before StationAPI changes the active resource packs. */
public final class ModTextureReload implements SynchronousResourceReloader {
  @Override
  public String getName() { return "PowerBetaModTextures"; }

  @EventListener
  public void register(AssetsResourceReloaderRegisterEvent event) {
    event.resourceManager.registerReloader(this);
  }

  @Override
  public void reload(ResourceManager resources) {
    var mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    var textures = mc.textureManager;
    if (textures == null) return;
    // Refresh existing IDs on the application thread. Do not start another pack
    // reload, allocate textures, or touch native atlases and dynamic images.
    var cached = ((TextureManagerAccessor) textures).getTextures();
    for (var entry : cached.entrySet()) {
      String path = entry.getKey();
      if (!path.startsWith("powerbeta:") && !path.startsWith("/assets/")) continue;
      var resource = resources.getResource(Identifier.of(path));
      if (resource.isEmpty()) continue;
      try (var stream = resource.get().getInputStream()) {
        var image = ImageIO.read(stream);
        if (image == null) throw new IOException("Unsupported image");
        textures.method_1089(image, entry.getValue());
      } catch (IOException e) {
        PowerBeta.LOG.warn("Could not refresh texture {}", path, e);
      }
    }
  }
}
