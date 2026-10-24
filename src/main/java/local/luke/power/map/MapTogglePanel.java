package local.luke.power.map;

import local.luke.power.ui.UiScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Checkbox rows use the same bounded viewport for drawing, clicks and wheel input. */
public final class MapTogglePanel extends UiScreen {
  public static final int ROW_HEIGHT = 20, PADDING = 4, HORIZONTAL_ALLOWANCE = 32;
  private static final MapTogglePanel VIEW = new MapTogglePanel();
  private static final String[] TICK = {"      ##", "     ## ", "    ##  ", "## ##   ", " ###    ", "  #     "};
  public record Layout(MinimapQuickMenuLayout.Rect bounds, int first, int visible) {
    public MinimapQuickMenuLayout.Rect row(int index) {
      return new MinimapQuickMenuLayout.Rect(bounds.x()+PADDING,
          bounds.y()+PADDING+(index-first)*ROW_HEIGHT,bounds.width()-PADDING*2,ROW_HEIGHT);
    }
    public int rowAt(int x, int y) {
      if (!bounds.contains(x,y)) return -1;
      for (int i=first; i<first+visible; i++) if (row(i).contains(x,y)) return i;
      return -1;
    }
    public int scroll(int wheel) {
      return Math.max(0,Math.min(MapToggle.ALL.size()-visible,first-Integer.signum(wheel)));
    }
  }
  private MapTogglePanel() {}
  public static int labelWidth(Minecraft mc) {
    return MapToggle.ALL.stream().mapToInt(toggle->mc.textRenderer.getWidth(toggle.label)).max().orElse(120);
  }
  public static Layout at(int x,int bottom,int maxWidth,int maxHeight,int labelWidth,int first) {
    int visible=Math.max(0,Math.min(MapToggle.ALL.size(),(maxHeight-PADDING*2)/ROW_HEIGHT));
    int height=PADDING*2+visible*ROW_HEIGHT;
    return new Layout(new MinimapQuickMenuLayout.Rect(x,bottom-height,Math.max(1,Math.min(maxWidth,labelWidth+HORIZONTAL_ALLOWANCE)),height),
        Math.max(0,Math.min(MapToggle.ALL.size()-visible,first)),visible);
  }
  public static void draw(Minecraft mc,Layout layout,int mouseX,int mouseY,boolean minimap) {
    VIEW.init(mc,mc.currentScreen.width,mc.currentScreen.height);
    var box=layout.bounds();var settings=MapConfig.current();
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);
      VIEW.clip(box.x(),box.y(),box.width(),box.height());
      VIEW.fill(box.x(),box.y(),box.x()+box.width(),box.y()+box.height(),0xf0202020);
      for(int index=layout.first();index<layout.first()+layout.visible();index++) {
        var row=layout.row(index);var toggle=MapToggle.ALL.get(index);
        boolean hovered=row.contains(mouseX,mouseY);
        if(hovered)VIEW.fill(row.x(),row.y(),row.x()+row.width(),row.y()+row.height(),0xff444444);
        VIEW.fill(row.x()+3,row.y()+5,row.x()+13,row.y()+15,0xffa0a0a0);
        VIEW.fill(row.x()+4,row.y()+6,row.x()+12,row.y()+14,0xff101010);
        if(toggle.enabled(settings,minimap)) {
          int left=row.x()+4,top=row.y()+7;
          for(int y=0;y<TICK.length;y++)for(int x=0;x<TICK[y].length();x++)
            if(TICK[y].charAt(x)=='#')VIEW.fill(left+x,top+y,left+x+1,top+y+1,0xffffffff);
        }
        VIEW.text(VIEW.fit(toggle.label,row.width()-23),row.x()+19,row.y()+6,hovered?0xffffff:0xdddddd);
      }
      if(layout.visible()<MapToggle.ALL.size()) {
        int track=box.height()-8,thumb=Math.max(4,track*layout.visible()/MapToggle.ALL.size());
        int y=box.y()+4+(track-thumb)*layout.first()/(MapToggle.ALL.size()-layout.visible());
        VIEW.fill(box.x()+box.width()-3,y,box.x()+box.width()-2,y+thumb,0xff999999);
      }
    }finally{GL11.glPopAttrib();}
  }
}
