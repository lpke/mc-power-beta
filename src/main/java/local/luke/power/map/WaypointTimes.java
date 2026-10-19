package local.luke.power.map;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Death timestamps use local wall time; cache only the bounded set of rendered labels. */
public final class WaypointTimes {
  private static final DateTimeFormatter COMPACT = DateTimeFormatter.ofPattern("d/M hh:mm", Locale.ROOT);
  private static final DateTimeFormatter DETAIL = DateTimeFormatter.ofPattern("d/M/yyyy hh:mm a", Locale.ROOT);
  private record Labels(ZoneId zone, String compact, String detail) {}
  private static final Map<Long, Labels> CACHE = new LinkedHashMap<>();
  private WaypointTimes() {}
  private static Labels labels(long timestamp) {
    ZoneId zone = ZoneId.systemDefault();
    Labels labels = CACHE.get(timestamp);
    if (labels == null || !labels.zone.equals(zone)) {
      var time = Instant.ofEpochMilli(timestamp).atZone(zone);
      labels = new Labels(zone, COMPACT.format(time), DETAIL.format(time));
      if (CACHE.size() >= 1024) CACHE.remove(CACHE.keySet().iterator().next());
      CACHE.put(timestamp, labels);
    }
    return labels;
  }
  public static String compact(long timestamp) { return labels(timestamp).compact; }
  public static String detail(long timestamp) { return labels(timestamp).detail; }
}
