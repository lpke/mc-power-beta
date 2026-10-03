package local.luke.worldedit;

import java.io.*;
import local.luke.worldedit.core.*;
import local.luke.worldedit.mixin.BlockEntityFactory;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.class_395;
import net.minecraft.nbt.*;
import net.minecraft.world.World;

/** Native chunks/light/save data, with scoped callbacks to prevent inventory drops. */
public final class MinecraftWorld implements WorldAccess {
  private final World world;

  public MinecraftWorld(World world) {
    this.world = world;
  }

  public boolean loaded(Pos p) {
    return p.valid() && world.method_239(p.x(), p.y(), p.z());
  }

  public BlockValue get(Pos p) {
    int id = world.getBlockId(p.x(), p.y(), p.z()), meta = world.method_1778(p.x(), p.y(), p.z());
    if (id == 36)
      throw new IllegalArgumentException("Wait for moving pistons to finish before editing.");
    BlockEntity entity = world.method_1777(p.x(), p.y(), p.z());
    byte[] bytes = null;
    if (entity != null) {
      try {
        NbtCompound tag = new NbtCompound();
        entity.writeNbt(tag);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.write(tag, new DataOutputStream(out));
        bytes = out.toByteArray();
        if (bytes.length > 65536)
          throw new IllegalArgumentException("Block entity exceeds 64 KiB at " + p);
      } catch (Exception e) {
        throw new IllegalArgumentException("Could not save block entity at " + p, e);
      }
    }
    return new BlockValue(id, meta, bytes);
  }

  public void set(Pos p, BlockValue state) {
    if (!p.valid()
        || state.id < 0
        || state.id >= Block.BLOCKS.length
        || state.id != 0 && Block.BLOCKS[state.id] == null)
      throw new IllegalArgumentException("Invalid block at " + p);
    BlockEntity replacement = null;
    byte[] bytes = state.nbt();
    if (bytes != null) {
      try {
        NbtCompound tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)));
        tag.putInt("x", p.x());
        tag.putInt("y", p.y());
        tag.putInt("z", p.z());
        replacement = BlockEntity.method_1068(tag);
        if (replacement == null) throw new IllegalArgumentException("Unknown block entity at " + p);
      } catch (Exception e) {
        throw new IllegalArgumentException("Could not restore block entity", e);
      }
    } else if (Block.BLOCKS[state.id] instanceof class_395 block)
      replacement = ((BlockEntityFactory) block).worldedit$create();
    final BlockEntity entity = replacement;
    EditScope.run(
        world,
        () -> {
          world.method_260(p.x(), p.y(), p.z());
          world.method_154(p.x(), p.y(), p.z(), state.id, state.meta);
          if (world.getBlockId(p.x(), p.y(), p.z()) != state.id
              || world.method_1778(p.x(), p.y(), p.z()) != state.meta)
            throw new IllegalStateException("Block write failed at " + p);
          if (entity != null) {
            entity.world = world;
            entity.x = p.x();
            entity.y = p.y();
            entity.z = p.z();
            world.method_157(p.x(), p.y(), p.z(), entity);
            entity.markDirty();
          }
          world.method_243(p.x(), p.y(), p.z());
        });
  }
}
