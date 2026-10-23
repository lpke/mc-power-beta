package local.luke.power.subtitles;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SubtitleCatalogTest {
  @Test void unknownMusicRecordsAndInterfaceClicksDoNotGainGuessedCaptions() {
    for (String sound : new String[]{"a.mod.custom.Sound", "music.calm1", "records.cat", "records.13", "random.breath", "random.click"}) assertNull(SubtitleCatalog.resolve(sound, null));
    assertNull(SubtitleCatalog.resolve("a.mod.custom.Sound", "subtitles.block.generic.place"));
    assertTrue(SubtitleCatalog.captions().size() <= SubtitleState.MAX_CAPTIONS);
  }
  @Test void actualCallContextsDistinguishReusedBlockAndClickSounds() {
    assertEquals("Footsteps", SubtitleCatalog.resolve("step.stone", null).label());
    assertEquals("Block placed", SubtitleCatalog.resolve("step.stone", "subtitles.block.generic.place").label());
    assertEquals("Block breaking", SubtitleCatalog.resolve("step.stone", "subtitles.block.generic.hit").label());
    assertEquals("Something falls on a block", SubtitleCatalog.resolve("step.stone", "subtitles.block.generic.fall").label());
    assertEquals("Lever clicks", SubtitleCatalog.resolve("random.click", "subtitles.block.lever.click").label());
    assertEquals("Chest opens", SubtitleCatalog.resolve("random.door_open", "subtitles.block.chest.open").label());
  }
  @Test void actualDamagePhaseCorrectlyLabelsSharedSpeciesAndNetworkDeathBeforeHealthChanges() {
    for (String sound : new String[]{"mob.cowhurt", "mob.chickenhurt", "mob.sheep", "mob.slime", "mob.spider"}) {
      assertTrue(SubtitleCatalog.resolve(sound, "hurt").label().endsWith("hurts"));
      assertTrue(SubtitleCatalog.resolve(sound, "death").label().endsWith("dies"));
    }
    assertEquals("Player dies", SubtitleCatalog.resolve("random.hurt", "player-death").label());
    assertEquals("Zombie Pigman dies", SubtitleCatalog.resolve("mob.zombiepig.zpighurt", "death").label());
  }
  @Test void projectilesLavaBurnoutAndPrimingUseExplicitSemanticLabels() {
    assertEquals("Egg flies", SubtitleCatalog.resolve("random.bow", "subtitles.entity.egg.throw").label());
    assertEquals("Bobber thrown", SubtitleCatalog.resolve("random.bow", "subtitles.entity.fishing_bobber.throw").label());
    assertEquals("Creeper hisses", SubtitleCatalog.resolve("random.fuse", "subtitles.entity.creeper.primed").label());
    assertEquals("TNT fizzes", SubtitleCatalog.resolve("random.fuse", null).label());
    assertEquals("Lava hisses", SubtitleCatalog.resolve("random.fizz", "subtitles.block.lava.extinguish").label());
    assertEquals("Torch fizzes", SubtitleCatalog.resolve("random.fizz", "subtitles.block.redstone_torch.burnout").label());
    assertEquals("Bucket fills", SubtitleCatalog.resolve("power_beta:item.bucket.fill_lava", null).label());
    assertEquals("Lava pops", SubtitleCatalog.resolve("power_beta:block.lava.pop", null).label());
    assertEquals("Lava pops", SubtitleCatalog.resolve("power_beta:block.lava.ambient", null).label());
    assertEquals("Burning", SubtitleCatalog.resolve("power_beta:entity.generic.burn", null).label());
    assertEquals("Splashing", SubtitleCatalog.resolve("power_beta:entity.generic.splash", null).label());
    assertEquals("Splashing", SubtitleCatalog.resolve("power_beta:entity.hostile.splash", null).label());
    assertEquals("Swimming", SubtitleCatalog.resolve("power_beta:entity.player.swim", null).label());
    assertEquals("Splashing", SubtitleCatalog.resolve("power_beta:entity.player.splash", null).label());
    assertEquals("Splashing", SubtitleCatalog.resolve("power_beta:entity.player.splash.high_speed", null).label());
  }
}
