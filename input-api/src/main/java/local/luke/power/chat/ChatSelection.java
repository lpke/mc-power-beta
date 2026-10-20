package local.luke.power.chat;

import java.util.*;
import java.util.function.ToIntFunction;

/** Selection endpoints belong to original messages, independently of wrapping and scroll. */
public final class ChatSelection {
  public static final ChatSelection history = new ChatSelection();
  private final Row[] rows = new Row[64];
  private List<ChatTranscript.Message> messages = List.of();
  private final IdentityHashMap<ChatTranscript.Message,Integer> indexes = new IdentityHashMap<>();
  private Object screen;
  private int count;
  private int viewportLeft, viewportRight;
  private Point anchor, cursor;
  private boolean dragging, document;
  private record Point(ChatTranscript.Message message, int offset) {}
  public ChatSelection() { for (int i=0;i<rows.length;i++) rows[i]=new Row(); }
  public void context(Object current, boolean focused) {
    if (!focused || current != screen) { clear();count=0;screen=null; }
  }
  public void begin(Object current) {
    if (current != screen) clear();screen=current;count=0;document=false;
  }
  public void viewport(int left,int right) { viewportLeft=left;viewportRight=right; }
  public void messages(List<ChatTranscript.Message> source) {
    document=true;
    if (messages==source) return;
    messages=source;indexes.clear();for(int i=0;i<source.size();i++)indexes.put(source.get(i),i);
    if(anchor!=null && (!indexes.containsKey(anchor.message)||!indexes.containsKey(cursor.message)))clear();
  }
  /** Legacy callers describe one source message per row. */
  public int line(String text,Object font,int x,int y,ToIntFunction<String> measure) {
    if(screen==null||count==rows.length)return -1;
    Row row=rows[count];
    ChatTranscript.Message message=row.message!=null && row.raw.equals(text)?row.message:new ChatTranscript.Message(text);
    return line(message,0,message.plain.length(),text,font,x,y,measure);
  }
  public int line(ChatTranscript.Message message,int from,int to,String text,Object font,int x,int y,ToIntFunction<String> measure) {
    if(screen==null||count==rows.length)return -1;
    int index=count++;Row row=rows[index];
    if(row.message!=message||row.from!=from||row.to!=to||!text.equals(row.raw)||row.font!=font)row.set(message,from,to,text,font,measure);
    row.x=x;row.y=y;return index;
  }
  public void end() {
    if(!document) {
      var source=new ArrayList<ChatTranscript.Message>();for(int i=0;i<count;i++)source.add(rows[i].message);
      messages(List.copyOf(source));document=false;
    }
  }
  public boolean click(Object current,int x,int y) {
    if(current!=screen)return false;
    for(int i=0;i<count;i++) {
      Row row=rows[i];
      int left=document?viewportLeft:row.x,right=document?Math.max(row.x+row.width(),viewportRight):row.x+row.width();
      if(y>=row.y-1&&y<row.y+8&&x>=left&&x<=right) {
        anchor=cursor=new Point(row.message,row.from+row.hit(x));dragging=true;return true;
      }
    }
    clear();return false;
  }
  public void move(Object current,int x,int y,boolean release) {
    if(current!=screen||!dragging||count==0)return;
    Row nearest=rows[0];int distance=Integer.MAX_VALUE;
    for(int i=0;i<count;i++) { int d=Math.abs(y-(rows[i].y+3));if(d<distance){nearest=rows[i];distance=d;} }
    cursor=new Point(nearest.message,nearest.from+nearest.hit(x));
    if(release)dragging=false;
  }
  public int edge(int y) {
    if(!dragging||count==0)return 0;
    return y<rows[count-1].y-1?1:y>=rows[0].y+8?-1:0;
  }
  public boolean dragging(){return dragging;}
  public boolean selected(){return anchor!=null&&(!anchor.message.equals(cursor.message)||anchor.offset!=cursor.offset);}
  public void clear(){anchor=cursor=null;dragging=false;}
  private boolean before() {
    int a=indexes.getOrDefault(anchor.message,-1),b=indexes.getOrDefault(cursor.message,-1);
    return a>b||a==b&&anchor.offset<=cursor.offset;
  }
  public int start(int row) {
    if(!selected())return -1;
    Row r=rows[row];Point first=before()?anchor:cursor,last=before()?cursor:anchor;
    int index=indexes.getOrDefault(r.message,-1);
    if(index<indexes.get(last.message)||index>indexes.get(first.message))return -1;
    int start=r.message==first.message?first.offset:0,end=r.message==last.message?last.offset:r.message.plain.length();
    return end<r.from||start>r.to?-1:Math.max(0,Math.min(r.to-r.from,start-r.from));
  }
  public int end(int row) {
    if(start(row)<0)return -1;
    Row r=rows[row];Point last=before()?cursor:anchor;
    return r.message==last.message?Math.max(0,Math.min(r.to-r.from,last.offset-r.from)):r.to-r.from;
  }
  public int x(int row,int offset){return rows[row].x+rows[row].positions[offset];}
  public String text() {
    if(!selected())return "";
    Point first=before()?anchor:cursor,last=before()?cursor:anchor;
    StringBuilder text=new StringBuilder();
    for(int i=indexes.get(first.message);i>=indexes.get(last.message);i--) {
      var message=messages.get(i);if(i!=indexes.get(first.message))text.append('\n');
      text.append(message.plain,message==first.message?first.offset:0,message==last.message?last.offset:message.plain.length());
    }
    return text.toString();
  }
  private static final class Row {
    ChatTranscript.Message message;String raw="";Object font;int from,to,x,y;int[] positions={0};
    void set(ChatTranscript.Message message,int from,int to,String text,Object font,ToIntFunction<String> measure) {
      this.message=message;this.from=from;this.to=to;this.raw=text;this.font=font;
      int count=0;int[] widths=new int[to-from+1];
      for(int i=0;i<text.length();i++) {
        if(text.charAt(i)=='\u00a7'&&i+1<text.length()){i++;continue;}
        if(++count<widths.length)widths[count]=measure.applyAsInt(text.substring(0,i+1));
      }
      positions=widths;
    }
    int width(){return positions[positions.length-1];}
    int hit(int mouseX){int relative=mouseX-x;for(int i=0;i<positions.length-1;i++)if(relative<(positions[i]+positions[i+1])/2)return i;return positions.length-1;}
  }
}
