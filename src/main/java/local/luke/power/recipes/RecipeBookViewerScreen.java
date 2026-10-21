package local.luke.power.recipes;

import net.minecraft.client.gui.screen.Screen;
import local.luke.power.ui.UiScreen;
import org.lwjgl.input.Keyboard;

/** Pause viewer reads the current inventory without creating or opening a native handler. */
public final class RecipeBookViewerScreen extends UiScreen implements RecipeBookHost {
  private final Screen parent;
  private RecipeBookOverlay book;
  private RecipeDefinition.Kind kind=RecipeDefinition.Kind.CRAFTING;
  public RecipeBookViewerScreen(Screen parent){this.parent=parent;}
  @Override public void init(){if(book==null)book=new RecipeBookOverlay(this,minecraft,RecipeContext.viewer(minecraft,kind),true);book.tick();}
  @Override public RecipeBookOverlay power$recipeBook(){return book;}
  @Override public void tick(){if(book!=null)book.tick();}
  @Override public void render(int x,int y,float delta){renderBackground();titleBar("Recipe book");
    int left=Math.min(width-72,book.layout().x()+153),top=book.layout().y();
    tabButton("Crafting",left,top,68,x,y,kind==RecipeDefinition.Kind.CRAFTING);
    tabButton("Smelting",left,top+22,68,x,y,kind==RecipeDefinition.Kind.FURNACE);
    button("Done",left,top+144,68,20,x,y,true);
    book.render(x,y);
  }
  @Override protected void mouseClicked(int x,int y,int button){
    if(local.luke.power.input.InventoryPanels.modalOther(this,book))return;
    if(local.luke.power.input.InventoryPanels.modal(this)){book.mouse(x,y,button);return;}
    int left=Math.min(width-72,book.layout().x()+153),top=book.layout().y();
    if(button==0&&inside(x,y,left,top,68,20)){click();kind=RecipeDefinition.Kind.CRAFTING;book.kind(kind);return;}
    if(button==0&&inside(x,y,left,top+22,68,20)){click();kind=RecipeDefinition.Kind.FURNACE;book.kind(kind);return;}
    if(button==0&&inside(x,y,left,top+144,68,20)){click();minecraft.setScreen(parent);return;}
    book.mouse(x,y,button);
  }
  @Override protected void keyPressed(char c,int key){if(book.key(c,key))return;if(key==Keyboard.KEY_ESCAPE||local.luke.power.input.Bindings.matches("powerbeta.recipeBook",key))minecraft.setScreen(parent);}
  @Override public void removed(){if(book!=null)book.removed();local.luke.power.input.InventoryPanels.remove(this);}
}
