package local.luke.power.map;

import static local.luke.power.map.MapMath.*;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/** Fixed-size decoded payload and CRC reject truncated, corrupt and oversized map files. */
public final class MapCodec {
  private static final int MAGIC = 0x50424d50, VERSION = 2, RAW_SIZE = PIXELS * 23;

  private MapCodec() {}

  public static byte[] encode(MapTile tile) throws IOException {
    tile.refreshRelief(null);
    var raw = new ByteArrayOutputStream(RAW_SIZE);
    try (var out = new DataOutputStream(raw)) {
      for (int i = 0; i < PIXELS; i++) {
        out.writeInt(tile.accurate[i]);
        out.writeInt(tile.vanilla[i]);
        out.writeInt(tile.reliefAccurate[i]);
        out.writeInt(tile.reliefVanilla[i]);
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
    if (version != 1 && version != VERSION)
      throw new IOException("Unsupported map format " + version + "; original retained");
    Key key;
    try {
      key =
          new Key(
              header.readInt(),
              header.readInt(),
              header.readInt(),
              header.readInt(),
              header.readInt(),
              expected.context());
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid map coordinates", e);
    }
    if (!key.equals(expected)) throw new IOException("Map tile belongs to another location");
    int checksum = header.readInt();
    int rawSize = PIXELS * (version == 1 ? 15 : 23);
    byte[] raw;
    try (var inflater = new InflaterInputStream(input)) {
      raw = inflater.readNBytes(rawSize + 1);
      if (raw.length != rawSize || inflater.read() != -1)
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
        tile.reliefAccurate[i] = version == 1 ? tile.accurate[i] : in.readInt();
        tile.reliefVanilla[i] = version == 1 ? tile.vanilla[i] : in.readInt();
        tile.blocks[i] = in.readInt();
        tile.heights[i] = in.readByte();
        tile.lights[i] = in.readByte();
        tile.biomes[i] = in.readByte();
        if ((tile.heights[i] & 255) > 127 && tile.heights[i] != (byte) 255
            || (tile.lights[i] & 255) > 15
            || tile.blocks[i] < 0) throw new IOException("Invalid map pixel");
      }
    }
    if (version == 1) {
      tile.invalidateAllRelief();
      tile.refreshRelief(null);
      tile.savedRevision = tile.revision;
      // Existing coarser files predate stored relief. Rebuild their quadrant even
      // when revisiting the leaf finds no changes in native terrain.
      tile.propagateRelief = tile.key.level() == 0;
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
    if (Files.exists(path)) {
      if (Files.size(path) > RAW_SIZE + 4096)
        throw new IOException("Existing map file too large; retained");
      byte[] previous = Files.readAllBytes(path);
      decode(previous, tile.key); // Future and corrupt files must never be overwritten.
      if (java.nio.ByteBuffer.wrap(previous).getInt(4) == 1) {
        Path backup = path.resolveSibling(path.getFileName() + ".v1.bak");
        if (!Files.exists(backup)) MapFiles.replace(backup, previous);
        if (Files.size(backup) != previous.length
            || !java.util.Arrays.equals(previous, Files.readAllBytes(backup)))
          throw new IOException("Map upgrade backup could not be verified; original retained");
      }
    }
    MapFiles.replace(path, bytes);
  }
}
