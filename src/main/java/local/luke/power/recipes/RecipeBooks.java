package local.luke.power.recipes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/** Aggregate registration and pause-menu integration use this small public boundary. */
public final class RecipeBooks {
  private RecipeBooks() {}
  public static RecipeBookOverlay book(Object screen){return screen instanceof RecipeBookHost host?host.power$recipeBook():null;}
  public static boolean owns(Object screen,int x,int y){var book=book(screen);return book!=null&&book.owns(x,y);}
  public static boolean pauseAvailable(Minecraft mc){var state=RecipeBookConfig.current();return state.enabled&&state.pauseMenu&&mc.world!=null&&mc.player!=null;}
  public static void openViewer(Minecraft mc,Screen parent){if(pauseAvailable(mc))mc.setScreen(new RecipeBookViewerScreen(parent));}
}
