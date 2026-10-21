package local.luke.power.offhand;

import net.minecraft.nbt.NbtCompound;

/** Retains supported document extensions when native respawn replaces the player entity. */
public interface OffhandDataOwner {
  NbtCompound power$offhandDocument();
  void power$offhandDocument(NbtCompound document);
}
