package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.pack.PackScreen;
import org.lwjgl.input.Keyboard;

public final class PreviewPickerChecks {
  private static AudioSettings audio;
  private static float music;
  private static String vanilla, background;
  public static void audio(Minecraft mc, String action) throws Exception {
    var system=SoundManagerAccessor.power$system();
    switch(action) {
      case "setup" -> {
        failures=0; audio=AudioConfig.copy(); music=mc.options.musicVolume;
        var draft=AudioConfig.copy();draft.musicDirectories=List.of("preview-check-music");draft.master=100;
        AudioConfig.preview(draft);mc.options.musicVolume=.2f;
      }
      case "vanilla" -> {
        test("custom tracks appear in the open menu after scanning", () -> {
          check(mc.currentScreen instanceof PowerOptionsScreen, "options screen closed");
          check(((PowerOptionsScreen)mc.currentScreen).session().settings().stream()
              .anyMatch(s -> s.label.equals("preview-test.wav")), "custom row needs menu reopen");
        });
        vanilla=AudioController.music(mc).stream().filter(id->!id.endsWith("preview-test.wav")).findFirst().orElseThrow();
        AudioController.next();
        var current=AudioController.class.getDeclaredField("currentMusic");current.setAccessible(true);
        background=((String)current.get(null)).substring(6);AudioController.previewSound(vanilla);
      }
      case "check-vanilla" -> {
        test("individual music streams immediately while options remain open",()->check(system.playing("PowerBetaMusicPreview"),"stream not playing"));
        test("preview pauses existing music without replacing playlist track",()->{
          check(!system.playing("BgMusic"),"background overlaps");
          check(AudioController.blockBackground(),"autoplay is not blocked");
          check(AudioController.status().contains(vanilla.substring(6)),"preview title missing");
        });
        AudioController.previewSound(vanilla);
      }
      case "resumed" -> {
        test("clicking same preview resumes original background music",()->{
          check(!system.playing("PowerBetaMusicPreview"),"preview not stopped");
          check(system.playing("BgMusic")&&AudioController.nowPlaying().equals(background),"original track not resumed: expected="+background+", actual="+AudioController.nowPlaying()+", playing="+system.playing("BgMusic"));
        });
        AudioController.previewSound(AudioController.customTracks().stream().filter(t -> t.name().equals("preview-test.wav")).findFirst().orElseThrow().id());
      }
      case "check-custom" -> {
        test("custom track preview streams immediately",()->check(system.playing("PowerBetaMusicPreview")&&AudioController.status().contains("preview-test.wav"),"custom preview missing"));
        AudioController.togglePause();
      }
      case "check-paused" -> {
        test("Play/pause during preview pauses the original playlist instead of advancing", () -> {
          check(!system.playing("PowerBetaMusicPreview") && !system.playing("BgMusic"), "preview pause did not stop audio: preview="+system.playing("PowerBetaMusicPreview")+", background="+system.playing("BgMusic")+", status="+AudioController.status());
          check(AudioController.status().equals("Music paused"), "pause changed playlist state");
        });
        AudioController.togglePause();
      }
      case "check-play" -> {
        test("Play resumes the same background track after preview pause", () -> {
          check(system.playing("BgMusic") && AudioController.nowPlaying().equals(background), "pause skipped the original track: expected="+background+", actual="+AudioController.nowPlaying()+", status="+AudioController.status());
        });
        AudioController.previewSound(AudioController.customTracks().stream().filter(t -> t.name().equals("preview-test.wav")).findFirst().orElseThrow().id());
      }
      case "next" -> AudioController.next();
      case "finish" -> {
        test("Next exits preview and normal playback continues",()->check(!system.playing("PowerBetaMusicPreview")&&system.playing("BgMusic"),"next playback broken"));
        AudioController.previewSound("music:missing-preview-test.wav");
        test("missing preview file does not stop active song",()->check(system.playing("BgMusic"),"missing track stopped music"));
        AudioConfig.preview(audio);mc.options.musicVolume=music;AudioController.refresh();
        log("AUDIO PREVIEW FAILURES "+failures);
      }
    }
  }

  public static void picker(Minecraft mc) throws Exception {
    failures=0;
    var parent=mc.currentScreen;
    PowerOptionsScreen s=new PowerOptionsScreen(parent);mc.setScreen(s);
    Setting setting=find(s.session(),"native.texturePack");
    String original=mc.field_2768.field_1175.field_1137;
    byte[] config=Files.readAllBytes(PowerConfig.path());
    Path custom=Path.of("texturepacks/Custom picker test.zip");
    Files.copy(Path.of("texturepacks/Alpha.zip"),custom,StandardCopyOption.REPLACE_EXISTING);
    try {
      test("texture-pack button opens native picker in place",()->{
        var activate=PowerOptionsScreen.class.getDeclaredMethod("activate",Setting.class,int.class);activate.setAccessible(true);
        activate.invoke(s,setting,1);
        check(mc.currentScreen instanceof PackScreen,"not native picker");
        mc.currentScreen.render(-1,-1,0);
      });
      test("native picker discovers custom ZIPs added after options opened",()->{
        var picker=mc.currentScreen;
        List<?> packs=mc.field_2768.method_1000();int index=-1;
        for(int i=0;i<packs.size();i++) if(((net.minecraft.class_285)packs.get(i)).field_1137.equals(custom.getFileName().toString())) index=i;
        check(index>=0,"custom pack missing");
        var field=PackScreen.class.getDeclaredField("field_706");field.setAccessible(true);Object list=field.get(picker);
        var select=Arrays.stream(list.getClass().getDeclaredMethods()).filter(m->Arrays.equals(m.getParameterTypes(),new Class<?>[]{int.class,boolean.class})).findFirst().orElseThrow();select.setAccessible(true);select.invoke(list,index,false);
        check(mc.field_2768.field_1175.field_1137.equals(custom.getFileName().toString()),"not selected");
        check(Arrays.equals(config,Files.readAllBytes(PowerConfig.path())),"selection prematurely saved drafts");
        ((ScreenInput)(Object)picker).power$key('\0',Keyboard.KEY_ESCAPE);
        check(mc.currentScreen==s&&setting.value.getAsString().equals(custom.getFileName().toString()),"return lost draft");
        s.render(-1,-1,0);
      });
      test("Cancel restores prior pack without losing original configuration",()->{
        s.session().discard();
        check(mc.field_2768.field_1175.field_1137.equals(original),"original pack not restored");
        check(Arrays.equals(config,Files.readAllBytes(PowerConfig.path())),"config modified");
      });
    } finally {
      s.session().discard();mc.setScreen(parent);Files.deleteIfExists(custom);mc.field_2768.method_998();
    }
    log("PACK PICKER FAILURES "+failures);
  }
}
