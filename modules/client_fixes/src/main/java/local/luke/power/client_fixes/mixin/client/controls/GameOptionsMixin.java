package local.luke.power.client_fixes.mixin.client.controls;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.Config;
import local.luke.power.client_fixes.client.ClientFixesClientMod;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Shadow
    public KeyBinding[] allKeys;

    @Inject(method = {"<init>()V", "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V"}, at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        ArrayList<KeyBinding> newKeys = new ArrayList<>(Arrays.asList(allKeys));


        if (Config.config.enableDebugGraphChanges) {
            newKeys.add(ClientFixesClientMod.DEBUG_GRAPH_KEYBIND);
        }

        allKeys = newKeys.toArray(new KeyBinding[0]);
    }
}
