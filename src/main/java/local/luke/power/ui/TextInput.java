package local.luke.power.ui;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
/** Single-line editor with selection, navigation and unrestricted clipboard pasting up to a cap. */
public final class TextInput {
  public String text;
  public boolean focused;
  private int cursor,anchor;
  private final int limit;
  public TextInput(String text,int limit){this.text=text;this.limit=limit;cursor=anchor=text.length();}
  public void selectAll(){anchor=0;cursor=text.length();}
  public int cursor(){return cursor;}
  public int start(){return Math.min(anchor,cursor);}
  public int end(){return Math.max(anchor,cursor);}
  public void key(char c,int key){
    if(!focused)return;boolean ctrl=Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)||Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);boolean shift=Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    if(ctrl&&key==Keyboard.KEY_A){selectAll();return;}
    if(ctrl&&(key==Keyboard.KEY_C||key==Keyboard.KEY_X)){
      if(start()!=end())try{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.substring(start(),end())),null);}catch(RuntimeException ignored){}
      if(key==Keyboard.KEY_X)replace("");return;
    }
    if(ctrl&&key==Keyboard.KEY_V){String clip=Screen.getClipboard();if(clip!=null)replace(clip.replace('\n',' ').replace('\r',' '));return;}
    if(key==Keyboard.KEY_LEFT||key==Keyboard.KEY_RIGHT||key==Keyboard.KEY_HOME||key==Keyboard.KEY_END){
      cursor=switch(key){case Keyboard.KEY_HOME->0;case Keyboard.KEY_END->text.length();case Keyboard.KEY_LEFT->Math.max(0,cursor-1);default->Math.min(text.length(),cursor+1);};if(!shift)anchor=cursor;return;
    }
    if(key==Keyboard.KEY_BACK){if(start()!=end())replace("");else if(cursor>0){anchor=cursor-1;replace("");}return;}
    if(key==Keyboard.KEY_DELETE){if(start()!=end())replace("");else if(cursor<text.length()){anchor=cursor+1;replace("");}return;}
    if(!ctrl&&c>=32&&c!=127)replace(String.valueOf(c));
  }
  private void replace(String value){int a=start(),b=end();int available=limit-(text.length()-(b-a));if(available<0)return;value=value.substring(0,Math.min(value.length(),available));text=text.substring(0,a)+value+text.substring(b);cursor=anchor=a+value.length();}
}
