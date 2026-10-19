package local.luke.power.map;

import java.util.UUID;

public final class Waypoint {
  public String id, name = "Waypoint", set = "Default";

  public Waypoint() {
    this(UUID.randomUUID().toString());
  }

  private Waypoint(String id) {
    this.id = id;
  }

  public int x, y = 64, z, dimension, colour = 0xffcc33;
  public boolean enabled = true, global, death;
  public long created = System.currentTimeMillis();

  public void validate() {
    if (id == null || !id.matches("[a-zA-Z0-9-]{1,64}"))
      throw new IllegalArgumentException("Invalid waypoint ID");
    text(name, 64);
    text(set, 48);
    if (Math.abs((long) x) > MapMath.BORDER
        || Math.abs((long) z) > MapMath.BORDER
        || y < 0
        || y > 127
        || dimension < -1024
        || dimension > 1024
        || colour < 0
        || colour > 0xffffff
        || created < 0) throw new IllegalArgumentException("Invalid waypoint position or color");
  }

  public static void text(String value, int limit) {
    if (value == null
        || value.isBlank()
        || value.length() > limit
        || value.chars().anyMatch(c -> c < 32 || c == 127 || c == 167))
      throw new IllegalArgumentException("Use 1–" + limit + " plain text characters");
  }

  public Waypoint copy() {
    Waypoint result = new Waypoint(id);
    result.id = id;
    result.name = name;
    result.set = set;
    result.x = x;
    result.y = y;
    result.z = z;
    result.dimension = dimension;
    result.colour = colour;
    result.enabled = enabled;
    result.global = global;
    result.death = death;
    result.created = created;
    return result;
  }

  public double distance(double px, double pz) {
    return Math.hypot(x + .5 - px, z + .5 - pz);
  }

  public boolean visible(
      int dim, String activeSet, boolean allSets, double px, double pz, MapSettings settings) {
    return enabled
        && dimension == dim
        && (allSets || set.equals(activeSet))
        && (global
            || settings.waypointDistance == 0
            || distance(px, pz) <= settings.waypointDistance);
  }

  public String coordinates() {
    return x + " " + y + " " + z;
  }
}
