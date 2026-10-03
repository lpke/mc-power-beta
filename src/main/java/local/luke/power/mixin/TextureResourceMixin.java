package local.luke.power.mixin;

import java.util.Optional;
import local.luke.power.visual.TextureOverrides;
import net.modificationstation.stationapi.api.resource.Resource;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.Namespace;
import net.modificationstation.stationapi.impl.resource.NamespaceResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers both the stitched block atlas and ordinary weather textures. */
@Mixin(value = NamespaceResourceManager.class, remap = false)
public class TextureResourceMixin {
  @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
  private void power$textures(Identifier id, CallbackInfoReturnable<Optional<Resource>> cir) {
    if (id.namespace != Namespace.MINECRAFT || !(id.path.equals("/terrain.png")
        || id.path.equals("/environment/rain.png") || id.path.equals("/environment/snow.png"))) return;
    cir.setReturnValue(cir.getReturnValue().map(resource -> new Resource(resource.getPack(),
        () -> TextureOverrides.replace(resource.getPack(), id.path, resource.getInputStream()), resource::getMetadata)));
  }
}
