package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.ExtraSoundResources;
import local.luke.power.mixin.*;
import net.minecraft.client.Minecraft;
import paulscode.sound.*;

/** Compares repaired bundled effects with Beta's unchanged decoder on the same Ogg bytes. */
final class ExtraSoundDecoder432Checks {
  private static URL legacy(URL resource) throws Exception {
    return new URL(null, "legacy:effect.ogg", new URLStreamHandler() {
      @Override protected URLConnection openConnection(URL url) {
        return new URLConnection(url) {
          @Override public void connect() {}
          @Override public InputStream getInputStream() throws IOException { return resource.openStream(); }
        };
      }
    });
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "use a muted disposable world");
    SilentAudioChecks.ready(mc); failures = 0;
    var manifest = JsonParser.parseReader(new InputStreamReader(ExtraSoundDecoder432Checks.class.getResourceAsStream("/assets/powerbeta/extra-sounds.json"))).getAsJsonObject();
    var groups = ((SoundPoolAccessor)((SoundManagerAccessor)mc.soundManager).power$sounds()).power$groups();
    Path output = Path.of("power-beta-validation-pcm"); Files.createDirectories(output);
    var assets = new ArrayList<JsonObject>();
    for (var value : manifest.getAsJsonArray("assets")) assets.add(value.getAsJsonObject());
    for (var value : manifest.getAsJsonArray("existingAssets")) assets.add(value.getAsJsonObject());
    var report = new JsonArray();
    int truncated = 0, padded = 0, unchanged = 0;
    for (var asset : assets) {
      String full = asset.get("file").getAsString();
      String file = full.contains("src/main/resources/") ? full.substring(full.indexOf("src/main/resources/") + "src/main/resources/".length()) : full;
      boolean controls = file.startsWith("assets/power_controls/");
      String suffix = file.substring(file.indexOf("/sounds/sound/") + "/sounds/sound/".length());
      String id = controls ? "power_controls:" + suffix.replaceFirst("[0-9]*\\.ogg$", "").replace('/', '.')
          : "power_beta:" + asset.get("event").getAsString();
      var track = groups.get(id).stream().filter(t -> t.field_2126.replace('\\', '/').endsWith(suffix)).findFirst().orElseThrow();
      check(ExtraSoundResources.owns(track.field_2127), "registered extra escaped the decoder scope " + id);
      long expected = asset.get("frames").getAsLong();
      var codec = SoundSystemConfig.getCodec(file); var original = SoundSystemConfig.getCodec(file);
      var streaming = SoundSystemConfig.getCodec(file);
      long before; long streamed;
      try {
        check(codec.initialize(track.field_2127) && original.initialize(legacy(track.field_2127)), "codec initialization " + file);
        var repaired = codec.readAll(); var baseline = original.readAll();
        check(codec.endOfStream() && repaired != null && repaired.audioData != null, "incomplete repair " + file);
        check(repaired.audioData.length / repaired.audioFormat.getFrameSize() == expected, "lost source samples " + file);
        before = baseline == null || baseline.audioData == null ? 0 : baseline.audioData.length / baseline.audioFormat.getFrameSize();
        Files.write(output.resolve(file.replace('/', '_') + ".pcm"), repaired.audioData);
        check(streaming.initialize(track.field_2127), "streaming initialization " + file);
        var chunks = new ByteArrayOutputStream();
        int reads = 0;
        while (!streaming.endOfStream() && reads++ < 100_000) {
          var buffer = streaming.read();
          if (buffer != null && buffer.audioData != null) {
            check(buffer.audioFormat.matches(repaired.audioFormat), "streaming format changed " + file);
            chunks.write(buffer.audioData);
          }
        }
        check(streaming.endOfStream(), "streaming never reached EOS " + file);
        byte[] streamedBytes = chunks.toByteArray();
        streamed = streamedBytes.length / repaired.audioFormat.getFrameSize();
        check(streamed == expected && Arrays.equals(repaired.audioData, streamedBytes),
            "streaming lost source frames, retained padding or changed PCM " + file);

        // The foreign resource takes Beta's original path, including its original result.
        var foreign = legacy(track.field_2127); check(!ExtraSoundResources.owns(foreign), "foreign decoder was widened");
      } finally { codec.cleanup(); original.cleanup(); streaming.cleanup(); }
      if (before < expected) truncated++; else if (before > expected) padded++; else unchanged++;
      var row = asset.deepCopy(); row.addProperty("resource", file); row.addProperty("legacyFrames", before); row.addProperty("decodedFrames", expected); row.addProperty("streamedFrames", streamed); report.add(row);
      log("DECODER432 " + id + " " + suffix + " sourceFrames=" + expected + " legacyFrames=" + before + " repairedFrames=" + expected + " streamedFrames=" + streamed);
    }
    Files.writeString(output.resolve("decoded.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
    check(assets.size() == 88, "not every bundled extra was audited");
    log("DECODER432 PASS assets=" + assets.size() + " truncated=" + truncated + " padded=" + padded + " unchanged=" + unchanged);
  }
}
