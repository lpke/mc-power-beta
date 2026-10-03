package local.luke.power.ui;
import local.luke.power.audio.MusicLibrary;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.*;
public final class DirectoryScreen extends UiScreen {
  private final Screen parent;private final Consumer<String> chosen;
  private Path directory=Path.of(System.getProperty("user.home"));private List<Path> children=List.of();private String error="";private int scroll;
  private final TextInput path=new TextInput(directory.toString(),4096);
  public DirectoryScreen(Screen parent,Consumer<String> chosen){this.parent=parent;this.chosen=chosen;read();}
  public void init(){Keyboard.enableRepeatEvents(true);}public void removed(){Keyboard.enableRepeatEvents(false);}
  private void read(){try(var stream=Files.list(directory)){children=stream.filter(p->Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS)).limit(2048).sorted(Comparator.comparing(p->p.getFileName().toString().toLowerCase(Locale.ROOT))).toList();path.text=directory.toString();path.selectAll();scroll=0;error="";}catch(Exception e){children=List.of();error="Could not read this folder.";}}
  private void go(Path next){if(Files.isDirectory(next)){directory=next.toAbsolutePath().normalize();read();}else error="Folder unavailable.";}
  public void onMouseEvent(){super.onMouseEvent();int d=Mouse.getEventDWheel();if(d!=0)scroll=Math.max(0,Math.min(Math.max(0,children.size()*20-(height-124)),scroll-(int)Math.signum(d)*40));}
  protected void keyPressed(char c,int key){if(key==Keyboard.KEY_ESCAPE){minecraft.setScreen(parent);return;}if(key==Keyboard.KEY_RETURN&&path.focused){try{go(MusicLibrary.resolve(FabricLoader.getInstance().getGameDir(),path.text));}catch(RuntimeException e){error="Invalid folder path.";}return;}path.key(c,key);}
  protected void mouseClicked(int x,int y,int b){if(b!=0)return;if(inside(x,y,14,38,width-76,18)){path.focused=true;return;}path.focused=false;
    if(inside(x,y,width-54,37,40,20)){try{go(MusicLibrary.resolve(FabricLoader.getInstance().getGameDir(),path.text));}catch(RuntimeException e){error="Invalid folder path.";}return;}
    if(inside(x,y,14,62,48,20)){if(directory.getParent()!=null)go(directory.getParent());return;}
    if(inside(x,y,66,62,52,20)){go(Path.of(System.getProperty("user.home")));return;}
    if(inside(x,y,122,62,76,20)){go(FabricLoader.getInstance().getGameDir());return;}
    if(y>=90&&y<height-34){int i=(y-90+scroll)/20;if(i>=0&&i<children.size())go(children.get(i));return;}
    if(inside(x,y,width/2-112,height-28,108,20)){chosen.accept(directory.toString());minecraft.setScreen(parent);}
    if(inside(x,y,width/2+4,height-28,108,20))minecraft.setScreen(parent);
  }
  public void render(int x,int y,float delta){renderBackground();drawCenteredTextWithShadow(textRenderer,"Choose a music folder",width/2,14,0xffffff);input(path,14,38,width-76,x,y,"");button("Go",width-54,37,40,20,x,y,true);button("Up",14,62,48,20,x,y,directory.getParent()!=null);button("Home",66,62,52,20,x,y,true);button("Game folder",122,62,76,20,x,y,true);
    clip(14,90,width-28,height-124);for(int i=0;i<children.size();i++){int yy=90+i*20-scroll;boolean hover=inside(x,y,14,yy,width-28,20);if(hover)fill(14,yy,width-14,yy+20,0x50555555);text(fit("> "+children.get(i).getFileName(),width-44),20,yy+6,hover?0xffffa0:0xdddddd);}if(children.isEmpty())text(error.isEmpty()?"No subfolders.":error,20,96,0xaaaaaa);unclip();button("Use this folder",width/2-112,height-28,108,20,x,y,true);button("Cancel",width/2+4,height-28,108,20,x,y,true);}
}
