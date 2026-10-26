package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.AudioController;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Reorders an isolated copy; Done returns it to the existing playlist editor. */
public final class MusicPlaylistOrderScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final MusicPresetDraft editor;
  private final List<String> tracks;
  private final ScrollBar scrollbar = new ScrollBar();
  private int scroll;

  public MusicPlaylistOrderScreen(PowerOptionsScreen parent, MusicPresetDraft editor) {
    this.parent=parent; this.editor=editor; tracks=new ArrayList<>(editor.tracks());
  }
  private int span() { return Math.min(540,width-24); }
  private int left() { return (width-span())/2; }
  private int right() { return left()+span(); }
  private int bottom() { return height-42; }
  private ScrollBar.Track track() {
    return new ScrollBar.Track(right()+3,54,Math.max(0,bottom()-54),tracks.size()*26,scroll);
  }
  public void removed() { scrollbar.release(); }
  protected void keyPressed(char c,int key) { if (key==Keyboard.KEY_ESCAPE) minecraft.setScreen(parent); }
  public void onMouseEvent() {
    super.onMouseEvent(); int wheel=Mouse.getEventDWheel();
    if(wheel!=0) scroll=(int)Math.max(0,Math.min(track().maximum(),scroll-Math.signum(wheel)*26));
  }
  protected void mouseClicked(int x,int y,int button) {
    if(button!=0) return;
    if(scrollbar.press(track(),x,y)) { scroll=(int)scrollbar.drag(track(),y,true);return; }
    if(inside(x,y,width/2-102,height-28,100,20)) { click();editor.order(tracks);minecraft.setScreen(parent);return; }
    if(inside(x,y,width/2+2,height-28,100,20)) { click();minecraft.setScreen(parent);return; }
    if(y<54||y>=bottom()) return;
    int index=(y-54+scroll)/26;
    if(index<0||index>=tracks.size()) return;
    int rowY=54+index*26-scroll;
    int offset=inside(x,y,right()-44,rowY,20,18)?-1:inside(x,y,right()-22,rowY,20,18)?1:0;
    if(offset!=0&&index+offset>=0&&index+offset<tracks.size()) { click();Collections.swap(tracks,index,index+offset); }
  }
  protected void mouseReleased(int x,int y,int button) {
    super.mouseReleased(x,y,button);
    if(button==0) {scroll=(int)scrollbar.drag(track(),y,false);scrollbar.release();}
  }
  public void render(int mx,int my,float delta) {
    renderBackground();titleBar("Playlist order");
    text("Used when Shuffle tracks is Off",left(),33,0xaaaaaa);
    scroll=(int)Math.max(0,Math.min(track().maximum(),scrollbar.drag(track(),my,Mouse.isButtonDown(0))));
    clip(left(),54,span(),Math.max(0,bottom()-54));
    for(int i=0;i<tracks.size();i++) {
      int y=54+i*26-scroll;if(y+24<54||y>=bottom())continue;
      text(fit((i+1)+". "+AudioController.musicLabel(tracks.get(i)),span()-54),left()+2,y+5,0xdddddd);
      iconButton("up",right()-44,y,20,mx,my,i>0);
      iconButton("down",right()-22,y,20,mx,my,i+1<tracks.size());
    }
    unclip();scrollbar.render(this,track());
    button("Done",width/2-102,height-28,100,20,mx,my,true);
    button("Cancel",width/2+2,height-28,100,20,mx,my,true);
  }
}
