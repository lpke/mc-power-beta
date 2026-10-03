package net.glasslauncher.mods.networking.test.mixin;

import net.glasslauncher.mods.networking.GlassNetworkHandler;
import net.glasslauncher.mods.networking.GlassNetworking;
import net.glasslauncher.mods.networking.GlassPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.login.LoginHelloPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerLoginNetworkHandler.class)
public class NetworkingMixin {

    @Inject(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_166;method_556(Lnet/minecraft/entity/player/ServerPlayerEntity;Lnet/minecraft/world/ServerWorld;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void ping(LoginHelloPacket par1, CallbackInfo ci, ServerPlayerEntity var2) {
        if (((GlassNetworkHandler) this).glass_Networking$hasGlassNetworking()) {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("message", "This is a cool packet!");
            GlassNetworking.sendToPlayer(var2, new GlassPacket("glassnetworkingtest", "message", nbt));
        }
        else {
            System.out.println(var2.name + " has no networking!");
        }
    }
}
