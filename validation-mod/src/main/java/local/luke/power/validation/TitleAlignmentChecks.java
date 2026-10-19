package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import javax.imageio.ImageIO;
import local.luke.power.config.SettingsRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;

/** Measure actual logo pixels separately from the splash text and other title decorations. */
final class TitleAlignmentChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var session = SettingsRegistry.open(mc);
    var style = find(session, "logo.style");
    var original = style.value.deepCopy();
    var titleConfig = local.luke.power.storage.PowerConfig.section("title"); titleConfig.remove("style");
    Path directory = Path.of("power-beta-data/reports/title-alignment"); Files.createDirectories(directory);
    var render = Class.forName("local.luke.power.title.TitleLogos").getMethod("render", Minecraft.class, int.class);
    try {
      for (int selected = 0; selected < style.choices.size(); selected++) {
        final int current = selected;
        test("title style " + style.choices.get(selected) + " renders at narrow, wide and odd GUI widths", () -> {
          style.value = new JsonPrimitive(current); session.save(Path.of("."));
          for (int[] size : new int[][]{{320,240},{640,420},{427,300}}) {
            var title = new TitleScreen(); title.init(mc,size[0],size[1]);
            SettingsSnapshot.render(mc,title,size[0],size[1],directory.resolve("title-"+current+"-"+size[0]+".png"));
            if (current == 0) {
              for (int tick = 0; tick < 160; tick++) title.tick();
              SettingsSnapshot.render(mc,title,size[0],size[1],directory.resolve("title-"+current+"-"+size[0]+".png"));
            }
            if (current == 0) continue;
            Screen isolated = new Screen() {
              public void render(int x, int y, float delta) {
                fill(0,0,width,height,0xff00ff00);
                try { render.invoke(null,mc,width); }
                catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
              }
            };
            isolated.init(mc,size[0],size[1]);
            Path file = directory.resolve("isolated-"+current+"-"+size[0]+".png");
            SettingsSnapshot.render(mc,isolated,size[0],size[1],file);
            var image = ImageIO.read(file.toFile());
            int left = image.getWidth(), right = -1;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
              if ((image.getRGB(x,y)&0xffffff)==0x00ff00) continue;
              left=Math.min(left,x);right=Math.max(right,x);
            }
            check(right>=left,"logo pixels missing");
            check(Math.abs(left+right+1-image.getWidth())<=2,
                "visible lettering is off center: "+left+".."+right+" / "+image.getWidth());
            log("LOGO PIXELS style "+current+" GUI "+size[0]+" bounds "+left+".."+right);
          }
        });
      }
    } finally {
      style.value=original;session.save(Path.of("."));
      var after=local.luke.power.storage.PowerConfig.section("title");after.remove("style");
      check(after.equals(titleConfig),"title checks changed retro customization");
    }
    log("TITLE ALIGNMENT FAILURES "+failures);
  }
}
