package local.luke.power.chat;

import java.util.*;
import java.util.function.ToIntFunction;

/** Cached wrapping shares exact source offsets with drawing, hit testing and copying. */
public final class ChatLayout {
  public record Row(ChatTranscript.Message message,int from,int to,String text) {}
  private final IdentityHashMap<ChatTranscript.Message,List<Row>> cache = new IdentityHashMap<>();
  private List<ChatTranscript.Message> source = List.of();
  private List<Row> rows = List.of();
  private Object font;
  private int width;
  private final Map<Character,Integer> advances = new HashMap<>();
  public List<Row> rows(){return rows;}
  public boolean update(List<ChatTranscript.Message> messages,Object font,int width,ToIntFunction<String> measure) {
    width=Math.max(1,width);
    if(source==messages&&this.font==font&&this.width==width)return false;
    if(this.font!=font||this.width!=width){cache.clear();advances.clear();}
    this.font=font;this.width=width;source=messages;
    var retained=Collections.newSetFromMap(new IdentityHashMap<ChatTranscript.Message,Boolean>());retained.addAll(messages);cache.keySet().retainAll(retained);
    List<Row> result=new ArrayList<>();
    for(var message:messages) {
      List<Row> pieces=cache.computeIfAbsent(message,key->wrap(key,measure));
      for(int i=pieces.size()-1;i>=0;i--)result.add(pieces.get(i));
    }
    rows=List.copyOf(result);return true;
  }
  private List<Row> wrap(ChatTranscript.Message message,ToIntFunction<String> measure) {
    List<Row> result=new ArrayList<>();int start=0,pixels=0;
    for(int i=0;i<message.plain.length();i++) {
      char c=message.plain.charAt(i);int advance=c=='\n'?0:advances.computeIfAbsent(c,key->measure.applyAsInt(String.valueOf(key)));
      if(c=='\n'||pixels+advance>width&&i>start) {
        result.add(new Row(message,start,i,message.slice(start,i)));start=c=='\n'?i+1:i;pixels=0;
      }
      if(c!='\n')pixels+=advance;
    }
    result.add(new Row(message,start,message.plain.length(),message.slice(start,message.plain.length())));return List.copyOf(result);
  }
  public int anchor(Row previous,int fallback) {
    if(previous!=null)for(int i=0;i<rows.size();i++) {
      Row row=rows.get(i);
      if(row.message==previous.message&&row.from<=previous.from&&row.to>=previous.from)return i;
    }
    return Math.max(0,Math.min(Math.max(0,rows.size()-1),fallback));
  }
}
