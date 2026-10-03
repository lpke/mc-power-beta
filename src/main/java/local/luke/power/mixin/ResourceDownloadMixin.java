package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Set;
import local.luke.power.PowerBeta;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.ResourceDownloadThread;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceDownloadThread.class)
public class ResourceDownloadMixin {
  @Shadow public File resourcesDirectory;
  @Shadow private Minecraft minecraft;

  @Inject(method = "run", at = @At("HEAD"))
  private void power$cachedSounds(CallbackInfo ci) {
    Path base = resourcesDirectory.toPath();
    if (!Files.isDirectory(base)) return;
    try (var stream = Files.walk(base, 12)) {
      for (Path file :
          stream
              .filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
              .limit(20000)
              .toList()) {
        Path relative = base.relativize(file);
        if (relative.getNameCount() < 2
            || !Set.of("sound", "newsound", "streaming", "music", "newmusic")
                .contains(relative.getName(0).toString())) continue;
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!(name.endsWith(".ogg") || name.endsWith(".mus") || name.endsWith(".wav"))) continue;
        minecraft.loadResource(relative.toString().replace(File.separatorChar, '/'), file.toFile());
      }
    } catch (IOException | RuntimeException e) {
      PowerBeta.LOG.warn("Could not preload cached sounds", e);
    }
  }

  @WrapOperation(
      method = {"run", "downloadFile"},
      at =
          @At(
              value = "INVOKE",
              target = "Ljava/net/URL;openStream()Ljava/io/InputStream;",
              remap = false))
  private InputStream power$timeout(URL url, Operation<InputStream> original) throws IOException {
    URLConnection connection = url.openConnection();
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(10000);
    return connection.getInputStream();
  }
}
