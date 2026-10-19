package local.luke.power.map;

import static local.luke.power.map.MapMath.*;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/** Fixed-size decoded payload and CRC reject truncated, corrupt and oversized map files. */
public final class MapCodec {
  private static final int MAGIC = 0x50424d50, VERSION = 1, RAW_SIZE = PIXELS * 15;

  private MapCodec() {}

  public static byte[] encode(MapTile tile) throws IOException {
    var raw = new ByteArrayOutputStream(RAW_SIZE);
    try (var out = new DataOutputStream(raw)) {
      for (int i = 0; i < PIXELS; i++) {
        out.writeInt(tile.accurate[i]);
        out.writeInt(tile.vanilla[i]);
        out.writeInt(tile.blocks[i]);
        out.writeByte(tile.heights[i]);
        out.writeByte(tile.lights[i]);
        out.writeByte(tile.biomes[i]);
      }
    }
    byte[] data = raw.toByteArray();
    CRC32 crc = new CRC32();
    crc.update(data);
    var result = new ByteArrayOutputStream();
    var header = new DataOutputStream(result);
    header.writeInt(MAGIC);
    header.writeInt(VERSION);
    Key k = tile.key;
    header.writeInt(k.dimension());
    header.writeInt(k.layer());
    header.writeInt(k.level());
    header.writeInt(k.x());
    header.writeInt(k.z());
    header.writeInt((int) crc.getValue());
    try (var compressed = new DeflaterOutputStream(result)) {
      compressed.write(data);
    }
    return result.toByteArray();
  }

  public static MapTile decode(byte[] data, Key expected) throws IOException {
    if (data.length < 36 || data.length > RAW_SIZE + 4096)
      throw new IOException("Invalid map file size");
    var input = new ByteArrayInputStream(data);
    var header = new DataInputStream(input);
    if (header.readInt() != MAGIC) throw new IOException("Unknown map file");
    int version = header.readInt();
    if (version != VERSION)
      throw new IOException("Unsupported map format " + version + "; original retained");
    Key key;
    try {
      key =
          new Key(
              header.readInt(),
              header.readInt(),
              header.readInt(),
              header.readInt(),
              header.readInt());
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid map coordinates", e);
    }
    if (!key.equals(expected)) throw new IOException("Map tile belongs to another location");
    int checksum = header.readInt();
    byte[] raw;
    try (var inflater = new InflaterInputStream(input)) {
      raw = inflater.readNBytes(RAW_SIZE + 1);
      if (raw.length != RAW_SIZE || inflater.read() != -1)
        throw new IOException("Invalid decoded map size");
    }
    CRC32 crc = new CRC32();
    crc.update(raw);
    if ((int) crc.getValue() != checksum) throw new IOException("Map checksum failed");
    MapTile tile = new MapTile(key);
    try (var in = new DataInputStream(new ByteArrayInputStream(raw))) {
      for (int i = 0; i < PIXELS; i++) {
        tile.accurate[i] = in.readInt();
        tile.vanilla[i] = in.readInt();
        tile.blocks[i] = in.readInt();
        tile.heights[i] = in.readByte();
        tile.lights[i] = in.readByte();
        tile.biomes[i] = in.readByte();
        if ((tile.heights[i] & 255) > 127 && tile.heights[i] != (byte) 255
            || (tile.lights[i] & 255) > 15
            || tile.blocks[i] < 0) throw new IOException("Invalid map pixel");
      }
    }
    return tile;
  }

  public static MapTile read(Path path, Key key) throws IOException {
    if (!Files.exists(path)) return new MapTile(key);
    if (Files.size(path) > RAW_SIZE + 4096) throw new IOException("Map file too large");
    return decode(Files.readAllBytes(path), key);
  }

  public static void write(Path path, MapTile tile) throws IOException {
    byte[] bytes = encode(tile);
    decode(bytes, tile.key);
    MapFiles.replace(path, bytes);
  }
}
