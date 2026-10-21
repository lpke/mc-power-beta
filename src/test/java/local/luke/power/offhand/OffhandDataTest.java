package local.luke.power.offhand;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import local.luke.power.death.KeptInventory;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

class OffhandDataTest {
  private NbtCompound empty() {
    NbtCompound player=new NbtCompound(),hand=new NbtCompound();hand.putInt("format",1);player.put(OffhandData.TAG,hand);return player;
  }
  @Test void historicalMissingSlotStaysAbsentUntilItHasOwnedData() throws Exception {
    NbtCompound player=new NbtCompound();assertNull(OffhandData.read(player));
    OffhandData.write(player,null,null);assertFalse(player.contains(OffhandData.TAG));
  }
  @Test void emptySupportedDocumentAndExtensionsSurviveSaveAndCopy() throws Exception {
    NbtCompound player=empty(),original=player.getCompound(OffhandData.TAG);
    original.putString("CustomExtension","retained");
    OffhandData.write(player,null,OffhandData.copy(original));
    assertNull(OffhandData.read(player));assertEquals("retained",player.getCompound(OffhandData.TAG).getString("CustomExtension"));
    assertEquals(1,player.getCompound(OffhandData.TAG).getInt("format"));
  }
  @Test void futureAndMalformedDocumentsStopBeforePublication() {
    NbtCompound future=empty();future.getCompound(OffhandData.TAG).putInt("format",2);
    assertThrows(IOException.class,()->OffhandData.validatePlayer(future));
    NbtCompound wrongType=new NbtCompound();wrongType.putString(OffhandData.TAG,"unsupported");
    assertThrows(IOException.class,()->OffhandData.read(wrongType));
    NbtCompound orphan=empty();orphan.getCompound(OffhandData.TAG).putInt("count",20);
    assertThrows(IOException.class,()->OffhandData.read(orphan));
    NbtCompound versionType=empty();versionType.getCompound(OffhandData.TAG).putByte("format",(byte)1);
    assertThrows(IOException.class,()->OffhandData.read(versionType));
  }
  @Test void legacyAndCurrentEmptyKeptSnapshotsRemainReadableButFutureVersionsFail() throws Exception {
    String items=java.util.Collections.nCopies(41,"null").toString(),craft="[null,null,null,null]";
    String old="{\"items\":"+items+",\"crafting\":"+craft+",\"selected\":8}";
    assertNotNull(KeptInventory.read(old.getBytes(StandardCharsets.UTF_8)));
    assertNotNull(KeptInventory.read(("{\"format\":1,"+old.substring(1)).getBytes(StandardCharsets.UTF_8)));
    for(String format:java.util.List.of("2","-1","1.25","\"1\"")) {
      String invalid=old.replace("{","{\"format\":"+format+",");
      assertThrows(IOException.class,()->KeptInventory.read(invalid.getBytes(StandardCharsets.UTF_8)));
    }
  }
}
