package local.luke.power.commands.util;

import local.luke.power.commands.ClientCommands;
import net.glasslauncher.mods.networking.GlassPacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;

import java.util.List;

public class NetworkingUtil implements GlassPacketListener {
    @Override
    public void registerGlassPackets() {
        registerGlassPacket("power_commands:op", ((glassPacket, networkHandler) -> {
            ClientCommands.mp_op = glassPacket.getNbt().getBoolean("op");
            ClientCommands.mp_rc = true;
        }), true, false);

        registerGlassPacket("power_commands:players", ((glassPacket, networkHandler) -> {
            ClientCommands.player_names = glassPacket.getNbt().getString("players").split(",");
        }), true, false);

        registerGlassPacket("power_commands:disabled", ((glassPacket, networkHandler) -> {
            ClientCommands.disabled_commands = List.of(glassPacket.getNbt().getString("disabled").split(","));
        }), true, false);
    }
}
