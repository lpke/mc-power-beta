package local.luke.power.controls.mixin.tweaks.resourceurl;

import local.luke.power.controls.ControlFeatures;
import net.minecraft.client.resource.ResourceDownloadThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ResourceDownloadThread.class, priority = 1500)
public class ResourceDownloadThreadMixin {
    @ModifyConstant(method = "run", constant = @Constant(stringValue = "http://s3.amazonaws.com/MinecraftResources/"), remap = false)
    private String getResourcesUrl(String original) {
        if (ControlFeatures.GENERAL_CONFIG.resourceDownloadUrl.isEmpty()) {
            return original;
        }
        
        return ControlFeatures.GENERAL_CONFIG.resourceDownloadUrl;
    }
}
