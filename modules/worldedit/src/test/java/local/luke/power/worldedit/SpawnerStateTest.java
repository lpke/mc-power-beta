package local.luke.power.worldedit;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.class_104;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;

class SpawnerStateTest {
  private static byte[] bytes(NbtCompound tag)throws IOException {
    var out=new ByteArrayOutputStream();NbtIo.write(tag,new DataOutputStream(out));return out.toByteArray();
  }
  private static NbtCompound tag(BlockValue value)throws IOException {
    return NbtIo.read(new DataInputStream(new ByteArrayInputStream(value.nbt())));
  }
  @Test void requestedMobPreservesNativeDelayAndSerializesTheWholePreparedPayload()throws Exception {
    var nativeSpawner=new class_104();nativeSpawner.field_2681=321;nativeSpawner.method_2035("Cow");
    nativeSpawner.x=4;nativeSpawner.y=65;nativeSpawner.z=8;
    var original=new NbtCompound();nativeSpawner.writeNbt(original);byte[] before=bytes(original);
    var existing=new BlockValue(52,0,before,"Cow");
    var desired=new BlockParser(id->id<=96).block("spawner[mob=zombified_piglin]");
    var prepared=SpawnerState.prepare(new Pos(4,65,8),desired,existing);
    var data=tag(prepared);
    assertEquals("MobSpawner",data.getString("id"));assertEquals("PigZombie",data.getString("EntityId"));
    assertEquals(321,data.getShort("Delay"));assertEquals(4,data.getInt("x"));assertEquals(65,data.getInt("y"));assertEquals(8,data.getInt("z"));
    var restored=new class_104();restored.readNbt(data);
    assertEquals(321,restored.field_2681);assertEquals("PigZombie",restored.method_2037());
    assertArrayEquals(before,existing.nbt());assertEquals("Cow",tag(existing).getString("EntityId"));
    assertSame(prepared,SpawnerState.prepare(new Pos(4,65,8),prepared,existing));
  }

  @Test void newSpawnerUsesNativeDefaultsAndInvalidMobsFailBeforeEditing()throws Exception {
    var desired=new BlockParser(id->id<=96).block("spawner[mob=skeleton]");
    var prepared=SpawnerState.prepare(new Pos(1,64,2),desired,BlockValue.AIR);
    assertEquals("Skeleton",tag(prepared).getString("EntityId"));assertEquals(20,tag(prepared).getShort("Delay"));
    assertThrows(IllegalArgumentException.class,()->SpawnerState.prepare(new Pos(1,64,2),new BlockValue(52,0,null,"Boat"),BlockValue.AIR));
    assertTrue(prepared.satisfiedBy(new BlockValue(52,0,prepared.nbt(),"Skeleton")));
  }

  @Test void explicitMobOverrideUsesDesiredPayloadAndKeepsConsistentClipboardBytesUntouched()throws Exception {
    var spawner=new class_104();spawner.field_2681=777;spawner.method_2035("Pig");
    var data=new NbtCompound();spawner.writeNbt(data);
    var copied=new BlockValue(52,0,bytes(data),"Pig");
    var pos=new Pos(7,66,9);
    assertSame(copied,SpawnerState.prepare(pos,copied,BlockValue.AIR));
    var changed=BlockStates.apply(copied,java.util.Map.of("mob","zombie"));
    assertEquals("Zombie",changed.spawnerMob);assertEquals("Pig",tag(changed).getString("EntityId"));
    spawner.field_2681=123;spawner.method_2035("Cow");spawner.writeNbt(data);
    var existing=new BlockValue(52,0,bytes(data),"Cow");
    var prepared=SpawnerState.prepare(pos,changed,existing);
    assertEquals("Zombie",tag(prepared).getString("EntityId"));
    assertEquals(777,tag(prepared).getShort("Delay"));
    assertEquals(7,tag(prepared).getInt("x"));assertEquals(66,tag(prepared).getInt("y"));assertEquals(9,tag(prepared).getInt("z"));
    assertEquals("Pig",tag(copied).getString("EntityId"));assertEquals("Cow",tag(existing).getString("EntityId"));
    assertSame(prepared,SpawnerState.prepare(pos,prepared,existing));
  }
}
