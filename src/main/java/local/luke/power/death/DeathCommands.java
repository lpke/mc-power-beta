package local.luke.power.death;

import java.lang.reflect.*;
import java.util.*;
import local.luke.power.chat.HelpOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.language.TranslationStorage;

/** Bridge to the shared registry without coupling the root build to its mappings. */
public final class DeathCommands {
  private DeathCommands() {}
  public static void register() {
    try {
      Class<?> api = Class.forName("local.luke.power.commands.api.Command");
      Object command = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
        Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
        return switch (method.getName()) {
          case "name" -> "death";
          case "needsPermissions", "disableInSingleplayer" -> false;
          case "helpGroup" -> "";
          case "available" -> DeathTracker.available(mc);
          case "command" -> { execute(mc, (String[])args[1]); yield null; }
          case "manual" -> { help(mc); yield null; }
          case "suggestion" -> {
            int index = (Integer)args[1]; String prefix = (String)args[2];
            List<String> choices = new ArrayList<>();
            if (index == 1) {
              choices.addAll(List.of("help", "items", "log"));
              if (DeathTracker.recoveryDenial(mc).isEmpty()) choices.add("recover");
            }
            if (index == 1 || index == 2 && !((String)args[3]).startsWith("/death log"))
              DeathTracker.journal(mc).records().forEach(r -> choices.add(r.id));
            yield choices.stream().filter(v -> v.startsWith(prefix)).map(v -> v.substring(prefix.length())).toArray(String[]::new);
          }
          case "toString" -> "Death command";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> throw new UnsupportedOperationException(method.getName());
        };
      });
      Class.forName("local.luke.power.commands.api.CommandRegistry").getMethod("add", api).invoke(null, command);
    } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not register /death", e); }
  }

  public static void execute(Minecraft mc, String[] args) throws Exception {
    if (!DeathTracker.available(mc)) throw new IllegalArgumentException("Enable Death records in Options > Commands.");
    if (args.length > 3) { help(mc); return; }
    String action = args.length == 1 ? "" : args[1].toLowerCase(Locale.ROOT);
    if (action.equals("help")) { help(mc); return; }
    if (action.equals("log")) {
      List<DeathRecord> records = new ArrayList<>(DeathTracker.journal(mc).records());
      Collections.reverse(records);
      var date = java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(java.time.ZoneId.systemDefault());
      List<String> lines = records.stream().map(r -> describe(r) + " §8| §7" + date.format(java.time.Instant.ofEpochMilli(r.date)) + state(mc, r)).toList();
      int pages = Math.max(1, (lines.size() + 5) / 6);
      int page = args.length < 3 ? 1 : page(args[2], pages);
      HelpOutput.print(s -> chat(mc, s), "Deaths", lines, page, 6, args.length < 3 && HelpOutput.scrolling());
      if (lines.isEmpty()) chat(mc, "§7No deaths recorded.");
      return;
    }
    String id;
    if (Set.of("items", "recover").contains(action)) id = args.length == 3 ? args[2] : "";
    else {
      if (args.length > 2) { help(mc); return; }
      id = action;
    }
    DeathRecord record = find(mc, id);
    switch (action) {
      case "items" -> mc.setScreen(new DeathInventoryScreen(mc, record));
      case "recover" -> { DeathTracker.recover(mc, record); chat(mc, "§aRecovered items from death " + record.id + ". Dropped items remain in the world."); }
      default -> location(mc, record);
    }
  }
  private static int page(String text, int max) {
    try { int value = Integer.parseInt(text); if (value >= 1 && value <= max) return value; }
    catch (NumberFormatException ignored) {}
    throw new IllegalArgumentException("Choose a death log page from 1 to " + max + ".");
  }
  private static DeathRecord find(Minecraft mc, String id) throws Exception {
    List<DeathRecord> records = DeathTracker.journal(mc).records();
    if (records.isEmpty()) throw new IllegalArgumentException("No deaths recorded.");
    if (id.isEmpty()) return records.get(records.size() - 1);
    return records.stream().filter(r -> r.id.equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown death ID: " + id + ". Use /death log."));
  }
  private static String describe(DeathRecord record) {
    String dimension = switch (record.dimension) { case -1 -> "Nether"; case 0 -> "Overworld"; default -> "Dimension " + record.dimension; };
    return "§e" + record.id + " §7| §b" + record.x + ", " + record.y + ", " + record.z + " §7| " + dimension + " | " + record.cause;
  }
  private static String state(Minecraft mc, DeathRecord record) {
    try {
      return switch (DeathTracker.journal(mc).state(record.id)) {
        case "recovered" -> " §8| §aRecovered";
        case "partial" -> " §8| §ePartly recovered";
        case "pending" -> " §8| §cRecovery interrupted";
        default -> "";
      };
    } catch (Exception e) { throw new IllegalStateException(e); }
  }
  public static void location(Minecraft mc, DeathRecord record) { chat(mc, "§6Death " + describe(record) + state(mc, record)); }
  public static void items(Minecraft mc, DeathRecord record, boolean detailed) throws Exception {
    List<String> lines = new ArrayList<>();
    for (String bytes : record.items) {
      var item = DeathItems.decode(bytes);
      String name = TranslationStorage.getInstance().get(item.getTranslationKey() + ".name");
      if (name.endsWith(".name")) name = item.getTranslationKey();
      lines.add(item.count + "× " + name + (detailed ? " §8[id " + item.itemId + ", damage " + item.getDamage() + "]" : ""));
    }
    chat(mc, "§6Death " + record.id + " §7| Lost items");
    if (lines.isEmpty()) chat(mc, "§7No items.");
    else for (String line : lines) chat(mc, "§7" + line);
  }
  private static void help(Minecraft mc) {
    chat(mc, "§b/death [id] §7Location and cause; latest death by default.");
    chat(mc, "§b/death items [id] §7View saved inventory positions. Cheats allow stack recovery.");
    chat(mc, "§b/death log [page] §7Past deaths and their IDs.");
    if (DeathTracker.recoveryDenial(mc).isEmpty()) chat(mc, "§b/death recover [id] §7Recover remaining stacks, preferring their original slots. Cancels if they do not all fit.");
    else chat(mc, "§7Recovery requires cheats and /death recover access.");
    chat(mc, "§7Records belong to this world and player. Recovery keeps existing items and leaves dropped items in the world.");
  }
  private static void chat(Minecraft mc, String message) { mc.inGameHud.addChatMessage(message + "§r"); }
}
