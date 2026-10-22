package local.luke.power.debug;

import java.io.*;
import java.util.Locale;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.rules.GameRule;
import net.minecraft.class_212;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.modificationstation.stationapi.api.registry.BlockRegistry;

final class DebugTarget {
  private DebugTarget() {}
  static void copy(Minecraft mc) throws Exception {
    if(mc.world.isRemote || !CommandPermissions.allowed(GameRule.ALLOW_WORLD_EDITING)) {
      DebugActions.message(mc,"Copying target data requires local World editing permission.");return;
    }
    var hit=mc.field_2823;if(hit==null)return;
    boolean data=!org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
        && !org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
    if(hit.field_1983==class_212.TILE) {
      int x=hit.field_1984,y=hit.field_1985,z=hit.field_1986;
      if(!mc.world.method_239(x,y,z))return;
      var id=BlockRegistry.INSTANCE.getId(net.minecraft.block.Block.BLOCKS[mc.world.getBlockId(x,y,z)]);
      int meta=mc.world.method_1778(x,y,z);String text="/setblock "+x+" "+y+" "+z+" "+id+" "+meta;
      var blockEntity=mc.world.method_1777(x,y,z);
      if(data && blockEntity!=null) {var tag=new NbtCompound();blockEntity.writeNbt(tag);text+=" "+snbt(tag);}
      DebugActions.copy(mc,text);
    } else if(hit.field_1983==class_212.ENTITY && hit.field_1989!=null) {
      var entity=hit.field_1989;var tag=new NbtCompound();
      if(!entity.method_1343(tag))return;
      String text=String.format(Locale.ROOT,"/summon %s %.2f %.2f %.2f",tag.getString("id"),entity.x,entity.y,entity.z);
      if(data)text+=" "+snbt(tag);
      DebugActions.copy(mc,text);
    }
  }
  private static String snbt(NbtCompound tag) throws IOException {
    var bytes=new ByteArrayOutputStream();NbtIo.write(tag,new DataOutputStream(bytes));
    return local.luke.power.input.DebugNbt.text(bytes.toByteArray());
  }
}
