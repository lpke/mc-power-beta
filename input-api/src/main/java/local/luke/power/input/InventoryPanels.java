package local.luke.power.input;

import java.util.*;

/** Shared inventory panel geometry across mapping-specific renderers and input owners. */
public final class InventoryPanels {
  public record Rect(int x,int y,int width,int height) {
    public boolean contains(int px,int py){return px>=x&&py>=y&&px<x+width&&py<y+height;}
  }
  private record Layout(int offset,List<Rect> panels) {}
  private static final Map<Object,Layout> LAYOUTS=new WeakHashMap<>();
  private static final Map<Object,Map<Object,List<Rect>>> EXTRAS=new WeakHashMap<>();
  private static final Map<Object,Set<Object>> MODALS=new WeakHashMap<>();
  private InventoryPanels() {}
  public static void update(Object screen,int offset,List<Rect> panels){LAYOUTS.put(Objects.requireNonNull(screen),new Layout(offset,List.copyOf(panels)));}
  public static int offset(Object screen){Layout value=LAYOUTS.get(screen);return value==null?0:value.offset;}
  public static boolean contains(Object screen,int x,int y){Layout value=LAYOUTS.get(screen);return value!=null&&value.panels.stream().anyMatch(rect->rect.contains(x,y))
      ||EXTRAS.getOrDefault(screen,Map.of()).values().stream().flatMap(List::stream).anyMatch(rect->rect.contains(x,y));}
  public static void extra(Object screen,Object owner,List<Rect> bounds){EXTRAS.computeIfAbsent(screen,key->new HashMap<>()).put(owner,List.copyOf(bounds));}
  public static void removeExtra(Object screen,Object owner){var owners=EXTRAS.get(screen);if(owners!=null)owners.remove(owner);}
  public static void modal(Object screen,Object owner,boolean active){if(active)MODALS.computeIfAbsent(screen,key->new HashSet<>()).add(owner);else{var owners=MODALS.get(screen);if(owners!=null)owners.remove(owner);}}
  public static boolean modal(Object screen){return !MODALS.getOrDefault(screen,Set.of()).isEmpty();}
  public static boolean modalOther(Object screen,Object owner){return MODALS.getOrDefault(screen,Set.of()).stream().anyMatch(value->!value.equals(owner));}
  public static void remove(Object screen){LAYOUTS.remove(screen);EXTRAS.remove(screen);MODALS.remove(screen);}
  public static void clear(){LAYOUTS.clear();EXTRAS.clear();MODALS.clear();}
}
