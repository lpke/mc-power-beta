package local.luke.power.worldedit;

import java.io.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.class_104;
import net.minecraft.nbt.*;

/** Resolve a spawner's requested mob into complete native NBT before the edit is published. */
public final class SpawnerState {
  private SpawnerState() {}
  public static BlockValue prepare(Pos pos,BlockValue desired,BlockValue existing) {
    if(desired.spawnerMob==null)return desired;
    try {
      NbtCompound tag;
      byte[] before=desired.nbt();
      if(before!=null) {
        tag=NbtIo.read(new DataInputStream(new ByteArrayInputStream(before)));
        // Complete clipboard/history payloads already own their mob and must remain byte-identical.
        if(tag.getString("EntityId").equals(desired.spawnerMob))return desired;
      } else {
        before=existing.id==52?existing.nbt():null;
        tag=before==null?new NbtCompound():NbtIo.read(new DataInputStream(new ByteArrayInputStream(before)));
        if(before==null)new class_104().writeNbt(tag);
      }
      String mob=SpawnerMobs.registryName(desired.spawnerMob);
      if(!tag.getString("id").equals("MobSpawner"))throw new IllegalArgumentException("Unsupported spawner data at "+pos);
      tag.putString("EntityId",mob);
      tag.putInt("x",pos.x());tag.putInt("y",pos.y());tag.putInt("z",pos.z());
      var out=new ByteArrayOutputStream();NbtIo.write(tag,new DataOutputStream(out));
      if(out.size()>65536)throw new IllegalArgumentException("Spawner data exceeds 64 KiB at "+pos);
      return new BlockValue(desired.id,desired.meta,out.toByteArray(),mob);
    } catch(Exception failure) {throw new IllegalArgumentException("Could not prepare spawner mob at "+pos,failure);}
  }
}
