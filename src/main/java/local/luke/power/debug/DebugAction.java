package local.luke.power.debug;

import org.lwjgl.input.Keyboard;

/** Public modern debug actions, plus the traditional help chord. */
public enum DebugAction {
  RELOAD("reload", "Reload chunks", Keyboard.KEY_A),
  HITBOXES("hitboxes", "Entity hitboxes", Keyboard.KEY_B),
  LOCATION("location", "Copy location", Keyboard.KEY_C),
  CRASH("crash", "Manual crash (hold 10 seconds)", Keyboard.KEY_C),
  CLEAR_CHAT("clearChat", "Clear chat", Keyboard.KEY_D),
  DISTANCE("distance", "Cycle render distance", Keyboard.KEY_F),
  BORDERS("borders", "Chunk borders", Keyboard.KEY_G),
  TOOLTIPS("tooltips", "Advanced item tooltips", Keyboard.KEY_H),
  TARGET("target", "Copy target data", Keyboard.KEY_I),
  PROFILE("profile", "Record performance", Keyboard.KEY_L),
  SPECTATE("spectate", "Switch Spectator / previous mode", Keyboard.KEY_N),
  FOCUS("focus", "Pause on lost focus", Keyboard.KEY_P),
  HELP("help", "Debug shortcut help", Keyboard.KEY_Q),
  TEXTURE_DUMP("textureDump", "Dump textures", Keyboard.KEY_S),
  RESOURCES("resources", "Reload resources", Keyboard.KEY_T),
  VERSION("version", "Version information", Keyboard.KEY_V),
  OPTIONS("options", "Debug settings", Keyboard.KEY_F6),
  PAUSE("pause", "Pause without menu", Keyboard.KEY_ESCAPE),
  PROFILER("profiler", "Profiler chart", Keyboard.KEY_1),
  FRAMES("frames", "Frame / tick charts", Keyboard.KEY_2),
  NETWORK("network", "Network chart", Keyboard.KEY_3),
  LIGHT("light", "Light brightness table", Keyboard.KEY_4);

  public final String id, label;
  public final int key;
  DebugAction(String id, String label, int key) { this.id = "powerbeta.debug." + id; this.label = label; this.key = key; }
  public static DebugAction find(String id) { for (var action : values()) if (action.id.equals(id)) return action; return null; }
}
