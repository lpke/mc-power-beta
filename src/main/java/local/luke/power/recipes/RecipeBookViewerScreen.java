package local.luke.power.recipes;

import net.minecraft.client.gui.screen.Screen;
import local.luke.power.ui.UiScreen;
import local.luke.power.ui.ConnectedTabs;
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
    var crafting=book.layout().kindTab(0);var smelting=book.layout().kindTab(1);var done=book.layout().done();
    var selected=kind==RecipeDefinition.Kind.CRAFTING?crafting:smelting;
    var detail=book.layout().detail();
    ConnectedTabs.baseline(this,book.layout().x()-30,detail.x()+detail.width(),crafting.y(),crafting.height(),selected.x(),selected.width());
    ConnectedTabs.tab(this,"Crafting",crafting.x(),crafting.y(),crafting.width(),crafting.height(),x,y,kind==RecipeDefinition.Kind.CRAFTING);
    ConnectedTabs.tab(this,"Smelting",smelting.x(),smelting.y(),smelting.width(),smelting.height(),x,y,kind==RecipeDefinition.Kind.FURNACE);
    button("Done",done.x(),done.y(),done.width(),done.height(),x,y,true);
    book.render(x,y);
  }
  @Override protected void mouseClicked(int x,int y,int button){
    if(local.luke.power.input.InventoryPanels.modalOther(this,book))return;
    if(local.luke.power.input.InventoryPanels.modal(this)){book.mouse(x,y,button);return;}
    var layout=book.layout();
    if(button==0&&layout.kindTab(0).contains(x,y)){click();kind=RecipeDefinition.Kind.CRAFTING;book.kind(kind);return;}
    if(button==0&&layout.kindTab(1).contains(x,y)){click();kind=RecipeDefinition.Kind.FURNACE;book.kind(kind);return;}
    if(button==0&&layout.done().contains(x,y)){click();minecraft.setScreen(parent);return;}
    book.mouse(x,y,button);
  }
  @Override protected void keyPressed(char c,int key){if(book.key(c,key))return;if(key==Keyboard.KEY_ESCAPE||local.luke.power.input.Bindings.matches("powerbeta.recipeBook",key))minecraft.setScreen(parent);}
  @Override public void removed(){if(book!=null)book.removed();local.luke.power.input.InventoryPanels.remove(this);}
}
