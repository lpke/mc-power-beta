package local.luke.power.input;

import java.io.*;
import java.util.ArrayList;

/** Bounded conversion of native binary NBT for clipboard inspection. */
public final class DebugNbt {
  private DebugNbt() {}
  public static String text(byte[] bytes) throws IOException {
    if(bytes.length>1024*1024)throw new IOException("Target data is too large to copy");
    try(var in=new DataInputStream(new ByteArrayInputStream(bytes))) {
      int type=in.readUnsignedByte();in.readUTF();String result=value(in,type,0);
      if(in.available()!=0)throw new IOException("Trailing NBT data");return result;
    }
  }
  private static String quoted(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\""; }
  private static int size(DataInputStream in) throws IOException {int n=in.readInt();if(n<0||n>65536)throw new IOException("Target list is too large");return n;}
  private static String value(DataInputStream in,int type,int depth) throws IOException {
    if(depth>32)throw new IOException("Target data is too deeply nested");
    return switch(type) {
      case 1 -> in.readByte()+"b";
      case 2 -> in.readShort()+"s";
      case 3 -> Integer.toString(in.readInt());
      case 4 -> in.readLong()+"L";
      case 5 -> in.readFloat()+"f";
      case 6 -> in.readDouble()+"d";
      case 7 -> {var parts=new ArrayList<String>();int n=size(in);for(int i=0;i<n;i++)parts.add(in.readByte()+"b");yield "[B;"+String.join(",",parts)+"]";}
      case 8 -> quoted(in.readUTF());
      case 9 -> {int child=in.readUnsignedByte(),n=size(in);var parts=new ArrayList<String>();for(int i=0;i<n;i++)parts.add(value(in,child,depth+1));yield "["+String.join(",",parts)+"]";}
      case 10 -> {var parts=new ArrayList<String>();int child;while((child=in.readUnsignedByte())!=0){String name=in.readUTF();parts.add(quoted(name)+":"+value(in,child,depth+1));}yield "{"+String.join(",",parts)+"}";}
      case 11 -> {int n=size(in);var parts=new ArrayList<String>();for(int i=0;i<n;i++)parts.add(Integer.toString(in.readInt()));yield "[I;"+String.join(",",parts)+"]";}
      default -> throw new IOException("Unsupported NBT type "+type);
    };
  }
}
