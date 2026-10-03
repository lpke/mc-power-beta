package local.luke.power.audio;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class AudioTest {
  @TempDir Path dir;
  @Test void volumeIsMultiplicativeAndMuteIsAbsolute(){AudioSettings s=new AudioSettings();s.master=50;s.categories.put("hostile",40);s.sounds.put("mob.zombie",25);assertEquals(.05,s.gain("mob.zombie",false),1e-9);s.master=0;assertEquals(0,s.gain("mob.zombie",false));}
  @Test void soundCategoriesCoverBetaFamilies(){assertEquals("weather",AudioSettings.category("ambient.weather.rain",false));assertEquals("passive",AudioSettings.category("mob.cow.say",false));assertEquals("hostile",AudioSettings.category("mob.ghast.moan",false));assertEquals("blocks",AudioSettings.category("step.stone",false));assertEquals("interface",AudioSettings.category("random.click",true));assertEquals("records",AudioSettings.category("records.cat",false));}
  @Test void invalidVolumesFailBeforeSaving(){AudioSettings s=new AudioSettings();s.master=101;assertThrows(IllegalArgumentException.class,s::validate);s.master=100;s.sounds.put("x",-1);assertThrows(IllegalArgumentException.class,s::validate);}
  @Test void scanDeduplicatesAndSkipsBadAndUnsupportedFiles()throws Exception{Files.write(dir.resolve("good.ogg"),new byte[]{'O','g','g','S',0,0,0,0,0,0,0,0});Files.writeString(dir.resolve("broken.ogg"),"not an audio stream");Files.writeString(dir.resolve("track.mp3"),"not supported");var scan=MusicLibrary.scan(dir,List.of(".",dir.toString()),false);assertEquals(1,scan.tracks().size());assertEquals(2,scan.warnings().size());assertTrue(Files.exists(dir.resolve("broken.ogg")));}
  @Test void recursionIsOptInAndSymlinksAreNotFollowed()throws Exception{Path sub=Files.createDirectory(dir.resolve("sub"));Files.write(sub.resolve("song.ogg"),new byte[]{'O','g','g','S',0,0,0,0,0,0,0,0});Files.createSymbolicLink(dir.resolve("loop"),dir);assertTrue(MusicLibrary.scan(dir,List.of("."),false).tracks().isEmpty());assertEquals(1,MusicLibrary.scan(dir,List.of("."),true).tracks().size());}
  @Test void missingDirectoriesDoNotEraseExistingFiles(){var scan=MusicLibrary.scan(dir,List.of("missing"),true);assertTrue(scan.tracks().isEmpty());assertEquals(1,scan.warnings().size());assertFalse(Files.exists(dir.resolve("missing")));}
  @Test void selectorAvoidsRepeatsAndHandlesEmptyOrSingletonLibraries(){TrackSelector s=new TrackSelector();Random r=new Random(1);assertNull(s.choose(List.<String>of(),true,true,r,x->x));assertEquals("only",s.choose(List.of("only"),true,true,r,x->x));String last="";for(int i=0;i<10000;i++){String next=s.choose(List.of("a","b","c"),true,true,r,x->x);assertNotEquals(last,next);last=next;}}
  @Test void sequentialSelectionIsStable(){TrackSelector s=new TrackSelector();Random r=new Random(1);for(int i=0;i<10;i++)assertEquals(List.of("a","b","c").get(i%3),s.choose(List.of("a","b","c"),false,true,r,x->x));}
}
