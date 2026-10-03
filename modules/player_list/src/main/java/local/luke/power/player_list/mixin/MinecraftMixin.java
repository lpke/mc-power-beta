package local.luke.power.player_list.mixin;

import local.luke.power.player_list.PlayerListMod;
import local.luke.power.player_list.PlayerList;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow public abstract boolean isWorldRemote();

    @Inject(method = "tick", at = @At("TAIL"))
    public void PlayerList$tabKeybind(CallbackInfo ci) {
        if (isWorldRemote() && local.luke.power.input.Bindings.down(PlayerListMod.PLAYER_LIST_KEY)) {
            if (!PlayerList.isTabPressed()) PlayerList.onTabPressed();
        } else if (PlayerList.isTabPressed()) PlayerList.onTabReleased();
    }
}
