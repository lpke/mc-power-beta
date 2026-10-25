package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.power.world.WorldRuleFiles;
import net.minecraft.client.Minecraft;

/** Validate disposable metadata copies with the client's complete native item codecs. */
final class WorldMetadata450Checks {
  static void run(Minecraft mc) throws Exception {
    check(mc.world==null,"close the disposable world before validating copied metadata");
    check(mc.options.soundVolume==0&&mc.options.musicVolume==0,"mute the disposable client");
    int worlds=0,files=0;
    try(var directories=Files.list(Path.of("saves"))){
      for(Path world:directories.filter(Files::isDirectory).sorted().toList()){
        var upgrade=WorldRuleFiles.planUpgrade(world);
        check(upgrade.planned().isEmpty(),"copied metadata requires a backed-up migration before use");
        for(var original:upgrade.originals().entrySet())
          check(Arrays.equals(original.getValue(),Files.readAllBytes(original.getKey())),"metadata validation changed its input");
        worlds++;files+=upgrade.originals().size();
      }
    }
    check(worlds>0&&files>0,"copy current-format metadata into the disposable client's saves first");
    log("PASS WORLD450 native rules, player and full item data; worlds="+worlds+" metadataFiles="+files+" writes=0");
  }
}
