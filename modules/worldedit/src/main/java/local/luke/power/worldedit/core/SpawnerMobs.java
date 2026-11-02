package local.luke.power.worldedit.core;

import java.util.*;
import local.luke.power.input.EntityNames;

/** Concrete living mobs supported by Beta's native spawner, which casts to LivingEntity. */
public final class SpawnerMobs {
  private static final Map<String,String> NAMES;
  static {
    Map<String,String> names=new TreeMap<>();
    for(String id:List.of("Creeper","Skeleton","Spider","Giant","Zombie","Slime","Ghast","PigZombie",
        "Pig","Sheep","Cow","Chicken","Squid","Wolf"))names.put(EntityNames.modern(id),id);
    NAMES=Map.copyOf(names);
  }
  private SpawnerMobs() {}
  public static Set<String> names() { return NAMES.keySet(); }
  public static String registryName(String input) {
    String id=NAMES.get(EntityNames.modern(input));
    if(id==null)throw new IllegalArgumentException("Unsupported Beta spawner mob: "+input);
    return id;
  }
}
