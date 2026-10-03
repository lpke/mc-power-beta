package net.glasslauncher.mods.networking.test;

import net.glasslauncher.mods.networking.GlassPacketListener;

public class PacketRegister implements GlassPacketListener {
    @Override
    public void registerGlassPackets() {
        registerGlassPacket("glassnetworkingtest:message", (glassPacket, networkHandler) -> System.out.println("I got " + glassPacket.getNbt().getString("message")), true, true);
    }
}
