package local.luke.power.input;

import java.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DebugNbtTest {
  @Test void preservesNativeTypesAndEscapesNames() throws Exception {
    var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
    out.writeByte(10);out.writeUTF("");out.writeByte(8);out.writeUTF("Name");out.writeUTF("a\"b\\c");
    out.writeByte(1);out.writeUTF("flag");out.writeByte(1);out.writeByte(9);out.writeUTF("Pos");out.writeByte(6);out.writeInt(2);out.writeDouble(1.5);out.writeDouble(-2);out.writeByte(0);
    assertEquals("{\"Name\":\"a\\\"b\\\\c\",\"flag\":1b,\"Pos\":[1.5d,-2.0d]}",DebugNbt.text(bytes.toByteArray()));
  }
  @Test void rejectsTruncatedAndOversizeData(){assertThrows(IOException.class,()->DebugNbt.text(new byte[0]));assertThrows(IOException.class,()->DebugNbt.text(new byte[1024*1024+1]));}
  @Test void rejectsNegativeListSizes() throws Exception {
    var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(9);out.writeUTF("");out.writeByte(1);out.writeInt(-1);
    assertThrows(IOException.class,()->DebugNbt.text(bytes.toByteArray()));
  }
}
