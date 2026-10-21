package local.luke.power.recipes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

/** Aggregate registration and pause-menu integration use this small public boundary. */
public final class RecipeBooks {
  private RecipeBooks() {}
  public static RecipeBookOverlay book(Object screen){return screen instanceof RecipeBookHost host?host.power$recipeBook():null;}
  public static boolean owns(Object screen,int x,int y){var book=book(screen);return book!=null&&book.owns(x,y);}
  public static boolean press(Minecraft mc,int code) {
    if(!local.luke.power.input.Bindings.matches("powerbeta.recipeBook",code)||org.lwjgl.input.Keyboard.isRepeatEvent()
        ||mc.world==null||mc.player==null||mc.currentScreen!=null||mc.player.dead||mc.player.health<=0
        ||!org.lwjgl.opengl.Display.isActive()||local.luke.power.input.DetachedCamera.isActive()
        ||local.luke.power.input.InteractionState.carryingContainer)return false;
    var state=RecipeBookConfig.current();if(!state.enabled)return false;
    boolean creative=mc.player instanceof local.luke.power.input.PlayerMode mode&&mode.power$creative();
    if(state.inventory&&!creative){RecipeBookConfig.state(false,true,state.craftingFilter);mc.setScreen(new net.minecraft.class_585(mc.player));return true;}
    if(pauseAvailable(mc)){openViewer(mc,null);return true;}return false;
  }
  public static boolean pauseAvailable(Minecraft mc){var state=RecipeBookConfig.current();return state.enabled&&state.pauseMenu&&mc.world!=null&&mc.player!=null;}
  public static void openViewer(Minecraft mc,Screen parent){if(pauseAvailable(mc))mc.setScreen(new RecipeBookViewerScreen(parent));}
}
