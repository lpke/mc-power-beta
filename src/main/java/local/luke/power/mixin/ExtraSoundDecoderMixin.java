package local.luke.power.mixin;

import com.jcraft.jogg.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.io.*;
import java.net.URL;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import org.spongepowered.asm.mixin.*;
import paulscode.sound.codecs.CodecJOrbis;

/** A partial buffered Ogg page is not EOF. Keep the native decoder for other resources. */
@Mixin(value = CodecJOrbis.class, remap = false)
public class ExtraSoundDecoderMixin {
  @Shadow protected URL url;
  @Shadow private InputStream inputStream;
  @Shadow private int bufferSize;
  @Shadow private Page joggPage;
  @Shadow private Packet joggPacket;
  @Shadow private SyncState joggSyncState;
  @Shadow private StreamState joggStreamState;
  @Shadow private AudioFormat audioFormat;
  @Shadow private byte[] decodeCurrentPacket() { throw new AssertionError(); }
  @Shadow private boolean initialized(boolean action, boolean value) { throw new AssertionError(); }
  @Shadow private boolean endOfStream(boolean action, boolean value) { throw new AssertionError(); }
  @Unique private long power$decodedFrames;
  @Unique private boolean power$extra;

  @WrapMethod(method = "initialize")
  private boolean power$reset(URL resource, Operation<Boolean> original) {
    power$decodedFrames = 0;
    power$extra = local.luke.power.audio.ExtraSoundResources.owns(resource);
    return original.call(resource);
  }

  @WrapMethod(method = "readBytes")
  private byte[] power$completePage(Operation<byte[]> original) {
    if (!power$extra) return original.call();
    if (!initialized(false, false) || endOfStream(false, false)) return null;
    try {
      // Refill only when pageout needs data. Beta treats that ordinary condition
      // as EOF and also reads past the input before draining already buffered pages.
      int result;
      while ((result = joggSyncState.pageout(joggPage)) == 0) {
        int offset = joggSyncState.buffer(bufferSize);
        if (offset < 0) { endOfStream(true, true); return null; }
        int count = inputStream.read(joggSyncState.data, offset, bufferSize);
        if (count <= 0) { endOfStream(true, true); return null; }
        joggSyncState.wrote(count);
      }
      if (result < 0 || joggStreamState.pagein(joggPage) < 0) { endOfStream(true, true); return null; }
      var output = new ByteArrayOutputStream();
      while ((result = joggStreamState.packetout(joggPacket)) != 0) {
        if (result < 0) { endOfStream(true, true); return null; }
        byte[] packet = decodeCurrentPacket();
        if (packet != null) output.writeBytes(packet);
      }
      if (joggPage.eos() != 0) endOfStream(true, true);
      return power$trimPadding(output.toByteArray());
    } catch (IOException failed) { endOfStream(true, true); return null; }
  }

  @Unique private byte[] power$trimPadding(byte[] bytes) {
    int frameSize = audioFormat.getFrameSize();
    if (joggPage.eos() != 0 && joggPage.granulepos() >= 0) {
      long remaining = Math.max(0, joggPage.granulepos() - power$decodedFrames);
      if (remaining < bytes.length / frameSize) bytes = Arrays.copyOf(bytes, (int)remaining * frameSize);
    }
    power$decodedFrames += bytes.length / frameSize;
    return bytes;
  }
}
