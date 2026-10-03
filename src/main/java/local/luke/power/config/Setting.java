package local.luke.power.config;

import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;

/** A typed value in a draft. Reading and editing this object never touches live game state. */
public final class Setting {
  public enum Kind { BOOLEAN, INTEGER, DECIMAL, CHOICE, TEXT, LIST, KEY }
  public final String id, backend, page, group, label, description;
  public final Kind kind;
  public final List<String> choices;
  public final JsonElement defaultValue;
  public final double min, max, step;
  public final boolean restart;
  public JsonElement value;
  private JsonElement original;

  public Setting(String id, String backend, String page, String group, String label, String description,
      Kind kind, JsonElement value, JsonElement defaultValue, double min, double max, double step,
      List<String> choices, boolean restart) {
    this.id=id;this.backend=backend;this.page=page;this.group=group;this.label=label;
    this.description=description;this.kind=kind;this.value=value.deepCopy();this.original=value.deepCopy();
    this.defaultValue=defaultValue.deepCopy();this.min=min;this.max=max;this.step=step;
    this.choices=List.copyOf(choices);this.restart=restart;
  }
  public boolean changed(){return !value.equals(original);}
  public JsonElement original(){return original.deepCopy();}
  public void accept(){original=value.deepCopy();}
  public void reset(){value=defaultValue.deepCopy();}
  public void cycle(int direction) {
    if(direction!=1 && direction!=-1)throw new IllegalArgumentException("Direction must be -1 or 1");
    switch(kind) {
      case BOOLEAN -> value=new JsonPrimitive(!value.getAsBoolean());
      case CHOICE -> value=new JsonPrimitive(Math.floorMod(value.getAsInt()+direction, choices.size()));
      case INTEGER, DECIMAL -> {
        BigDecimal next=value.getAsBigDecimal().add(BigDecimal.valueOf(step*direction));
        if(next.doubleValue()>max+1e-8)next=BigDecimal.valueOf(min);
        if(next.doubleValue()<min-1e-8)next=BigDecimal.valueOf(max);
        value=kind==Kind.INTEGER?new JsonPrimitive(next.intValueExact()):new JsonPrimitive(next.stripTrailingZeros());
      }
      default -> {}
    }
  }
  public void parse(String text) {
    JsonElement next=switch(kind) {
      case TEXT -> new JsonPrimitive(text);
      case LIST -> JsonParser.parseString(text);
      case BOOLEAN -> {if(!text.equals("true")&&!text.equals("false"))throw new IllegalArgumentException("Use true or false");yield new JsonPrimitive(Boolean.parseBoolean(text));}
      default -> new JsonPrimitive(new BigDecimal(text.trim()));
    };
    validate(next);value=next;
  }
  public void validate(JsonElement next) {
    if(next==null||next.isJsonNull())throw new IllegalArgumentException(label+": a value is required");
    if(kind==Kind.LIST){if(!next.isJsonArray()&&!next.isJsonObject())throw new IllegalArgumentException("Enter a list or object");if(next.toString().length()>16384)throw new IllegalArgumentException("Value exceeds 16 KiB");return;}
    if(!next.isJsonPrimitive())throw new IllegalArgumentException(label+": invalid value type");
    JsonPrimitive p=next.getAsJsonPrimitive();
    if(kind==Kind.BOOLEAN){if(!p.isBoolean())throw new IllegalArgumentException("Choose On or Off");return;}
    if(kind==Kind.TEXT){if(!p.isString()||next.getAsString().length()>4096)throw new IllegalArgumentException("Text exceeds 4096 characters");return;}
    if(!p.isNumber())throw new IllegalArgumentException("Enter a number");
    double n=next.getAsDouble();
    if(!Double.isFinite(n))throw new IllegalArgumentException("Enter a finite number");
    if(kind==Kind.CHOICE){if(n!=Math.rint(n)||n<0||n>=choices.size())throw new IllegalArgumentException("Choose a listed option");return;}
    if(kind==Kind.INTEGER||kind==Kind.KEY)next.getAsBigDecimal().intValueExact();
    if(n<min||n>max)throw new IllegalArgumentException("Use "+number(min)+" to "+number(max));
  }
  public String display(){return display(value);}
  public String display(JsonElement v) {
    return switch(kind){
      case BOOLEAN -> choices.size()==2?choices.get(v.getAsBoolean()?1:0):(v.getAsBoolean()?"On":"Off");
      case CHOICE -> {int n=v.getAsInt();yield n>=0&&n<choices.size()?choices.get(n):"Invalid selection";}
      case LIST -> v.isJsonArray()?v.getAsJsonArray().size()+" entries":v.getAsJsonObject().size()+" entries";
      case INTEGER, DECIMAL -> number(v.getAsDouble());
      default -> v.getAsString();
    };
  }
  public String editText(){return kind==Kind.LIST?value.toString():value.getAsString();}
  public static String number(double v){return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();}
}
