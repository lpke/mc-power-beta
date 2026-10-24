package local.luke.power.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import net.minecraft.client.gui.screen.Screen;
import local.luke.power.map.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/** Full map navigation only changes the view. Panning never loads native terrain. */
public final class WorldMapScreen extends UiScreen {
  private Screen parent;
  private final MapMath.View view = new MapMath.View(Maps.x(), Maps.z());
  private final MapZoom zoom = new MapZoom();
  private final TextInput serverMap = new TextInput(Maps.submap(), 48);
  private final MapCoordinatesDialog coordinates = new MapCoordinatesDialog();
  private int dimension = Maps.dimension(), layer = Maps.layer();
  private boolean dragging, context, scaleDragging, screenshot;
  private double dragX, dragZ, scaleDx, scaleDy;
  private long deleteUntil;
  private final WorldMapBuffer buffer = new WorldMapBuffer();
  private int lastX, lastY, contextX, contextY;
  private Waypoint target;
  private String message = "";
  private MapSettings styleSource, worldStyle;
  private MapSettings minimapStyleSource, minimapStyle;
  private int minimapWidth, minimapHeight, minimapEntryCorner, minimapEntryX, minimapEntryY;
  private PowerOptionsScreen settingsPanel;
  private PowerOptionsScreen optionsOwner;
  private boolean openSettings, openMinimapSettings;
  private String initialSetting = "";
  private MinimapGeometry.Layout minimapLayout;
  private MinimapGeometry.Box minimapHit;
  private boolean panelPress;
  private WorldMapControls controls;
  private MapSettings controlsStyle;
  private int controlsLeft = -1, controlsWidth, controlsHeight, controlsLayer, controlsDimension;
  private boolean controlsSettings;
  private int mapLeft() { return settingsPanel == null ? 0 : MapSettingsScope.panelWidth(width, height); }
  private int mapWidth() { return Math.max(1, width - mapLeft()); }
  private double mapCenterX() { return mapLeft() + mapWidth() / 2d; }
  void closeSettings() {
    if (settingsPanel != null) settingsPanel.removed();
    settingsPanel = null; panelPress = false; minimapHit = null; minimapStyleSource = null;
    if (!MapConfig.current().minimap) MinimapBuffer.clear();
    if (optionsOwner != null || !MapConfig.current().worldMap) closeMap();
  }
  private void toggleSettings() {
    if (settingsPanel != null) { settingsPanel.requestPanelClose(this::closeSettings); return; }
    if (optionsOwner == null && MapSettingsScope.panelWidth(width, height) == width) { PowerOptionsScreen.openMap(minecraft, this); return; }
    var entryStyle = MapConfig.current();
    minimapEntryCorner = entryStyle.corner; minimapEntryX = entryStyle.offsetX; minimapEntryY = entryStyle.offsetY;
    settingsPanel = optionsOwner == null ? PowerOptionsScreen.mapPanel(this) : PowerOptionsScreen.mapPanel(this, optionsOwner);
    settingsPanel.panelSize(MapSettingsScope.panelWidth(width, height));
    settingsPanel.init(minecraft, width, height);
    dragging = context = scaleDragging = false;
  }
  private void requestClose() {
    if (settingsPanel == null) closeMap();
    else settingsPanel.requestPanelClose(() -> { closeSettings(); if (minecraft.currentScreen == this) closeMap(); });
  }
  public void tick() { if (settingsPanel != null) settingsPanel.tick(); }
  public boolean capturingBinding() { return settingsPanel != null && settingsPanel.capturingBinding(); }
  public void requestScreenshot() { screenshot = true; }
  public boolean minimapRadarActive() { return settingsPanel != null && mapWidth() >= 200; }
  public boolean radarIconsAllowed() {
    return !coordinates.active() && !serverMap.focused && !context
        && (settingsPanel == null || settingsPanel.radarIconsAllowed());
  }
  private boolean panelOwns(int x) { return settingsPanel != null && (settingsPanel.panelModal() || panelPress || !dragging && !scaleDragging && x < mapLeft()); }
  private void drawMinimap(float delta) {
    minimapHit = null;
    if (settingsPanel == null || mapWidth() < 200) return;
    if (minimapStyleSource != MapConfig.current() || minimapWidth != mapWidth() || minimapHeight != height) {
      minimapStyleSource = MapConfig.current(); minimapWidth = mapWidth(); minimapHeight = height;
      minimapStyle = MapConfig.copy();
      if (minimapStyle.corner == minimapEntryCorner && minimapStyle.offsetX == minimapEntryX && minimapStyle.offsetY == minimapEntryY) {
        minimapStyle.corner = 1; minimapStyle.offsetX = minimapStyle.offsetY = 0;
      }
      minimapStyle.hideWithDebug = false;
      minimapStyle.size = Math.min(minimapStyle.size, Math.max(48, Math.min(mapWidth() - 40, height - 100)));
    }
    var style = minimapStyle;
    var frame = Minimap.frame(minecraft, style, mapWidth(), height, false, false).translated(mapLeft(), 0);
    Minimap.drawFrame(minecraft, style, frame, true, delta);
    minimapHit = frame.bounds(); minimapLayout = frame.layout();
  }

  /** Borrow the Options session so manual drafts and the return position remain owned by Options. */
  public static boolean openMinimapSettings(net.minecraft.client.Minecraft mc, PowerOptionsScreen owner, boolean enabled) {
    return openSettings(mc, owner, enabled, true, "");
  }
  public static boolean canOpenSettings(net.minecraft.client.Minecraft mc, PowerOptionsScreen owner, boolean enabled) {
    return enabled && mc.world != null && mc.player != null
        && MapSettingsScope.panelWidth(owner.width, owner.height) != owner.width;
  }
  public static boolean openSettings(net.minecraft.client.Minecraft mc, PowerOptionsScreen owner,
      boolean enabled, boolean minimap, String setting) {
    if (!canOpenSettings(mc, owner, enabled)) return false;
    var screen = new WorldMapScreen(); screen.optionsOwner = owner;
    screen.openSettings = true; screen.openMinimapSettings = minimap; screen.initialSetting = setting;
    mc.setScreen(screen); return true;
  }
  private void closeMap() { minecraft.setScreen(optionsOwner == null ? MinimapMapVisit.returning(minecraft,parent) : optionsOwner.returnScreen()); }
  public WorldMapScreen() {}
  public WorldMapScreen(Screen parent) { this.parent = parent; }
  public WorldMapScreen(Waypoint point) { this(null, point); }
  public WorldMapScreen(Screen parent, Waypoint point) {
    this.parent = parent;
    view.x = point.x + .5; view.z = point.z + .5;
    dimension = point.dimension;
  }

  private MapSettings worldStyle() {
    if (styleSource != MapConfig.current()) {
      styleSource = MapConfig.current();
      worldStyle = MapConfig.copy();
      worldStyle.opacity = 1;
    }
    return worldStyle;
  }

  private int top() {
    return 0;
  }

  private int bottom() {
    return height;
  }

  /** Read-only interest for tick-owned sampling; only the currently loaded dimension/layer qualifies. */
  public MapProjection samplingView(int dimension, int layer) {
    return this.dimension == dimension && this.layer == layer ? projection() : null;
  }

  private MapProjection projection() {
    return new MapProjection(
        view.x, view.z, view.scale, mapLeft(), top(), mapWidth(), Math.max(1, bottom() - top()), 0);
  }

  private static MapProjection.Point bounded(MapProjection.Point p) {
    return new MapProjection.Point(
        MapMath.bounded(p.x(), -MapMath.BORDER, MapMath.BORDER),
        MapMath.bounded(p.y(), -MapMath.BORDER, MapMath.BORDER),
        0,
        0);
  }

  public int lightLayer(int dimension) { return this.dimension == dimension ? layer : Integer.MIN_VALUE; }

  private WorldMapControls controls() {
    var style = MapConfig.current();
    boolean panel = settingsPanel != null;
    if (controls == null || controlsLeft != mapLeft() || controlsWidth != mapWidth() || controlsHeight != height
        || controlsLayer != layer || controlsDimension != dimension || controlsSettings != panel || controlsStyle != style) {
      String[] labels = {style.lightOverlay ? "Spawnable: On" : "Spawnable: Off",
          style.worldMapRadarIcons ? "Mobs: On" : "Mobs: Off", style.slimeChunks ? "Slime: On" : "Slime: Off",
          style.chunkGrid ? "Chunks: On" : "Chunks: Off", style.footsteps ? "Path: On" : "Path: Off", "Waypoints", panel ? "Close settings" : "Settings",
          layer < 0 ? "Caves" : "Surface", MapDimensions.name(dimension), "Center", "Go to", "Close"};
      int bottomWidth = 12;
      for (int i = 5; i < 9; i++) bottomWidth += textRenderer.getWidth(labels[i]) + 12;
      if (bottomWidth > mapWidth() - 16) labels[6] = "Settings";
      controls = WorldMapControls.at(mapLeft(), mapWidth(), height, labels, textRenderer::getWidth);
      controlsLeft = mapLeft(); controlsWidth = mapWidth(); controlsHeight = height;
      controlsLayer = layer; controlsDimension = dimension; controlsSettings = panel; controlsStyle = style;
    }
    return controls;
  }
  private int scaleY() { return controls().scale().y() + 16; }
  private WorldMapControls.Box scaleBounds() { return controls().scale().translated((int)scaleDx, (int)scaleDy); }
  private boolean overControls(int x, int y) {
    if (controls().at(x,y) != null) return true;
    if (settingsPanel != null && minimapHit != null
        && inside(x, y, (int)minimapHit.x(), (int)minimapHit.y(), (int)minimapHit.width(), (int)minimapHit.height())) return true;
    if (minecraft.world != null && minecraft.world.isRemote
        && inside(x, y, mapLeft() + 8, 4, Math.min(180, mapWidth() / 3), 18)) return true;
    return scaleBounds().contains(x,y);
  }

  private List<String> contextActions() {
    var actions = new ArrayList<String>();
    if (target.id.equals("temporary-destination")) actions.add("Clear destination");
    else {
      boolean saved = Maps.waypoints() != null && Maps.waypoints().contains(target.id);
      actions.add(saved ? "Edit waypoint" : "Create waypoint");
      if (saved) actions.add(System.nanoTime() < deleteUntil ? "Confirm delete?" : "Delete waypoint");
    }
    actions.add("Copy coordinates");
    if (Maps.canTeleport(minecraft, target)) actions.add("Teleport");
    actions.add("Set destination");
    return actions;
  }

  private Waypoint hoveredWaypoint(int x, int y) {
    var settings = MapConfig.current();
    var store = Maps.waypoints();
    if (!settings.waypoints || store == null) return null;
    var projection = projection();
    Waypoint closest = null;
    double limit = Math.max(7, settings.worldMapWaypointSize * .063);
    int count = 0;
    for (Waypoint point : Maps.points()) {
      if (count >= 512) break;
      if (point.dimension != dimension || point.death && !settings.deathpoints) continue;
      if (MinimapArrows.destination(point)) {
        if (!point.enabled) continue;
      } else if (!point.visible(dimension, store.selectedSet(), store.allSets(), Maps.x(), Maps.z(), settings)) continue;
      count++;
      if (WaypointSymbols.opacity(point, settings) <= 0) continue;
      var marker = projection.screen(point.x + .5, point.z + .5);
      if (!projection.contains(marker.x(), marker.y(), false, Math.max(6, settings.worldMapWaypointSize * .06))) continue;
      double distance = Math.hypot(marker.x() - x, marker.y() - y);
      if (distance < limit || closest == null && distance <= limit) { limit = distance; closest = point; }
    }
    return closest;
  }

  private void contextAction(String action) {
    if (action.equals("Delete waypoint")) { deleteUntil = System.nanoTime() + 4_000_000_000L; return; }
    context = false;
    switch (action) {
      case "Edit waypoint", "Create waypoint" -> minecraft.setScreen(new WaypointEditorScreen(this, target));
      case "Confirm delete?" -> Maps.waypoints().remove(target.id);
      case "Clear destination" -> Maps.destination(null);
      case "Copy coordinates" -> {
        try {
          Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(target.coordinates()), null);
          message = "Coordinates copied";
        } catch (RuntimeException e) { message = "Clipboard is unavailable"; }
      }
      case "Teleport" -> {
        if (Maps.canTeleport(minecraft, target)) { Maps.teleport(minecraft, target); closeMap(); }
      }
      case "Set destination" -> { Maps.destination(target); closeMap(); }
    }
  }

  private void overlay(String label, int x, int y, int color) {
    fill(x - 3, y - 3, x + textRenderer.getWidth(label) + 3, y + 11, 0x90000000);
    text(label, x, y, color);
  }

  private void center() {
    zoom.cancel();
    view.x = Maps.x();
    view.z = Maps.z();
    dimension = Maps.dimension();
    layer = Maps.layer();
  }

  public void init() {
    Keyboard.enableRepeatEvents(true);
    if (openSettings) {
      openSettings = false;
      var entry = MapConfig.current(); minimapEntryCorner = entry.corner; minimapEntryX = entry.offsetX; minimapEntryY = entry.offsetY;
      settingsPanel = PowerOptionsScreen.mapPanel(this, optionsOwner);
      settingsPanel.panelEntry(openMinimapSettings, initialSetting);
    }
    if (settingsPanel != null) {
      settingsPanel.panelCancelGesture();
      settingsPanel.panelSize(MapSettingsScope.panelWidth(width, height));
      settingsPanel.init(minecraft, width, height);
    }
    MapScreenshot.clearStatus();
    message = "";
    context = dragging = scaleDragging = panelPress = false;
    scaleDx = scaleDy = 0;
    controls = null; TextDrag.cancel();
  }

  public void removed() {
    dragging = scaleDragging = panelPress = false;
    if (settingsPanel != null) settingsPanel.panelCancelGesture();
    buffer.clear();
    zoom.cancel();
    Keyboard.enableRepeatEvents(false);
    TextDrag.cancel();
  }

  protected void keyPressed(char c, int key) {
    if (coordinates.key(this,c,key)) return;
    if (radarIconsAllowed() && local.luke.power.input.Bindings.matches(MapKeys.RADAR_ICONS, key)) return;
    if (settingsPanel != null) { settingsPanel.keyPressed(c, key); return; }
    if (context) {
      if (key == Keyboard.KEY_ESCAPE) context = false;
      return;
    }
    if (serverMap.focused) {
      if (key == Keyboard.KEY_RETURN) {
        try {
          Maps.submap(serverMap.text().trim());
          message = "Map selected";
        } catch (RuntimeException e) {
          message = e.getMessage();
        }
        serverMap.focused = false;
        return;
      }
      if (key == Keyboard.KEY_ESCAPE) {
        serverMap.focused = false;
        serverMap.setText(Maps.submap());
        return;
      }
      serverMap.key(c, key);
      return;
    }
    if (key == Keyboard.KEY_ESCAPE || local.luke.power.input.Bindings.matches(MapKeys.MAP, key)) {
      closeMap();
      return;
    }
    if (key == Keyboard.KEY_HOME || key == Keyboard.KEY_SPACE) center();
    if (key == Keyboard.KEY_UP) view.pan(0, 32);
    if (key == Keyboard.KEY_DOWN) view.pan(0, -32);
    if (key == Keyboard.KEY_LEFT) view.pan(32, 0);
    if (key == Keyboard.KEY_RIGHT) view.pan(-32, 0);
    if (key == Keyboard.KEY_EQUALS || key == Keyboard.KEY_ADD)
      zoom.wheel(view, 1, 0, 0, System.nanoTime());
    if (key == Keyboard.KEY_MINUS || key == Keyboard.KEY_SUBTRACT)
      zoom.wheel(view, -1, 0, 0, System.nanoTime());
    if (local.luke.power.input.Bindings.matches(MapKeys.WAYPOINT, key)) createAt(view.x, view.z);
    if (key == Keyboard.KEY_G) goTo();
  }

  private void goTo() {
    zoom.cancel(); context = dragging = scaleDragging = panelPress = false;
    scaleDx = scaleDy = 0; serverMap.focused = false;
    if (settingsPanel != null) settingsPanel.panelCancelGesture();
    coordinates.open(view.x,view.z,(x,z)->{view.x=x;view.z=z;});
  }

  private void toggleOverlay(WorldMapControls.Action action) {
    String id = switch(action) {
      case LIGHT -> "maps.lightOverlay";
      case SLIME -> "maps.slimeChunks";
      case CHUNKS -> "maps.chunkGrid";
      case MOBS -> "maps.worldMapRadarIcons";
      case PATH -> "maps.footsteps";
      default -> throw new IllegalArgumentException("Unknown map overlay");
    };
    if (settingsPanel != null) { settingsPanel.toggleMapSetting(id); return; }
    if (optionsOwner != null) { optionsOwner.toggleMapSetting(id); return; }
    var settings = MapConfig.copy();
    switch(action) {
      case LIGHT -> settings.lightOverlay = !settings.lightOverlay;
      case SLIME -> settings.slimeChunks = !settings.slimeChunks;
      case CHUNKS -> settings.chunkGrid = !settings.chunkGrid;
      case MOBS -> settings.worldMapRadarIcons = !settings.worldMapRadarIcons;
      case PATH -> settings.footsteps = !settings.footsteps;
      default -> throw new IllegalArgumentException("Unknown map overlay");
    }
    MapConfig.queued(settings);
  }

  private void controlAction(WorldMapControls.Action action) {
    click();
    switch(action) {
      case LIGHT, SLIME, MOBS, CHUNKS, PATH -> toggleOverlay(action);
      case WAYPOINTS -> minecraft.setScreen(new WaypointsScreen(this));
      case SETTINGS -> toggleSettings();
      case LAYER -> layer = layer < 0 ? MapSampler.window(MapConfig.current(),Maps.y()).layer() : -1;
      case DIMENSION -> dimension = dimension == 0 ? -1 : 0;
      case CENTER -> center();
      case GO_TO -> goTo();
      case CLOSE -> requestClose();
    }
  }

  private void createAt(double x, double z) {
    Waypoint p = MapKeys.newWaypoint();
    p.x = (int) Math.floor(x);
    p.z = (int) Math.floor(z);
    p.dimension = dimension;
    MapTile tile =
        Maps.cache() == null ? null : Maps.cache().peek(MapMath.key(dimension, layer, 0, p.x, p.z));
    if (tile != null && tile.known(MapMath.index(p.x, p.z)))
      p.y = Math.min(127, tile.height(MapMath.index(p.x, p.z)) + 1);
    minecraft.setScreen(new WaypointEditorScreen(this, p));
  }

  protected void mouseClicked(int x, int y, int button) {
    if (coordinates.press(this,x,y,button)) return;
    if (panelOwns(x)) { panelPress = true; settingsPanel.mouseClicked(x, y, button); return; }
    if (settingsPanel != null && minimapHit != null && button == 1 && settingsPanel.minimapWheelAllowed()
        && !context && !dragging && !scaleDragging
        && inside(x, y, (int)minimapHit.x(), (int)minimapHit.y(), (int)minimapHit.width(), (int)minimapHit.height())) {
      click(); settingsPanel.resetMinimapZoom(); return;
    }
    if (settingsPanel != null && minimapHit != null && button == 0
        && inside(x, y, (int)minimapHit.x(), (int)minimapHit.y(), (int)minimapHit.width(), (int)minimapHit.height())) {
      click(); settingsPanel.minimapAppearance(""); return;
    }
    zoom.cancel();
    if (context) {
      var actions = contextActions();
      if (button == 0 && inside(x, y, contextX, contextY, 142, actions.size() * 18)) {
        click();
        contextAction(actions.get((y - contextY) / 18));
      } else context = false;
      return;
    }
    if ((button == 0 || button == 1) && minecraft.world.isRemote
        && inside(x, y, mapLeft() + 8, 4, Math.min(180, mapWidth() / 3), 18)) {
      serverMap.focused = true; serverMap.click(x, button); return;
    }
    if (button == 0) {
      var control = controls().at(x,y);
      if (control != null) { controlAction(control.action()); return; }
      if (scaleBounds().contains(x,y)) {
        scaleDragging = true; lastX = x; lastY = y; return;
      }
      serverMap.focused = false;
    }
    if (x < mapLeft() || y < top() || y >= bottom() || overControls(x, y)) return;
    if (button == 0) {
      dragging = true;
      lastX = x;
      lastY = y;
      var anchor = projection().world(x, y); dragX = anchor.x(); dragZ = anchor.y();
    }
    if (button == 1) {
      var position = bounded(projection().world(x, y));
      target = MapKeys.newWaypoint();
      target.x = (int) Math.floor(position.x());
      target.z = (int) Math.floor(position.y());
      target.dimension = dimension;
      MapTile tile = Maps.cache().peek(MapMath.key(dimension, layer, 0, target.x, target.z));
      if (tile != null && tile.known(MapMath.index(target.x, target.z)))
        target.y = Math.min(127, tile.height(MapMath.index(target.x, target.z)) + 1);
      Waypoint hovered = hoveredWaypoint(x, y);
      if (hovered != null) target = hovered.copy();
      dragging = false;
      context = true;
      contextX = Math.max(0, Math.min(width - 142, x));
      deleteUntil = 0;
      contextY = Math.max(top(), Math.min(bottom() - contextActions().size() * 18, y));
    }
  }

  protected void mouseReleased(int x, int y, int button) {
    // Beta also dispatches pointer motion here with no button transition.
    if (button < 0) return;
    if (coordinates.active()) return;
    if (panelPress) {
      if (settingsPanel != null) settingsPanel.panelRelease();
      panelPress = false; return;
    }
    if (button == 0) {
      if (dragging) panTo(x, y);
      dragging = scaleDragging = false;
      scaleDx = scaleDy = 0;
    }
  }

  public void onMouseEvent() {
    int x = Mouse.getEventX() * width / minecraft.displayWidth,
        y = height - Mouse.getEventY() * height / minecraft.displayHeight - 1;
    if (!Display.isActive()) {
      dragging = scaleDragging = panelPress = false; scaleDx = scaleDy = 0;
      if (settingsPanel != null) settingsPanel.panelCancelGesture();
    }
    if (coordinates.active()) { super.onMouseEvent(); return; }
    if (panelOwns(x)) {
      settingsPanel.panelWheel(Mouse.getEventDWheel());
      super.onMouseEvent(); return;
    }
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0 && Display.isActive() && !Mouse.isGrabbed() && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1) && !context && !dragging && !scaleDragging && settingsPanel != null
        && settingsPanel.minimapWheelAllowed() && minimapLayout != null
        && MinimapGeometry.contains(minimapLayout, minimapStyle.circular, x, y)) {
      settingsPanel.zoomMinimap(wheel); return;
    }
    if (!context
        && (scaleDragging || !overControls(x, y))
        && Mouse.getEventDWheel() != 0
        && y >= top()
        && y < bottom())
      zoom.wheel(
          view,
          Mouse.getEventDWheel(),
          x - mapCenterX(),
          y - (top() + bottom()) / 2d,
          System.nanoTime());
    super.onMouseEvent();
  }

  private void panTo(double x, double y) {
    view.x = MapMath.bounded(dragX - (x - mapCenterX()) / view.scale, -MapMath.BORDER, MapMath.BORDER);
    view.z = MapMath.bounded(dragZ - (y - height / 2d) / view.scale, -MapMath.BORDER, MapMath.BORDER);
  }

  public void render(int mx, int my, float delta) {
    if (!MapConfig.current().worldMap && settingsPanel == null) { closeMap(); return; }
    zoom.update(view, System.nanoTime());
    if (!Display.isActive()) {
      dragging = scaleDragging = panelPress = false; scaleDx = scaleDy = 0;
      if (settingsPanel != null) settingsPanel.panelCancelGesture();
    }
    double pointerX = Mouse.getX() * (double) width / minecraft.displayWidth;
    double pointerY = height - Mouse.getY() * (double) height / minecraft.displayHeight - 1;
    if (dragging && !context && !coordinates.active()) panTo(pointerX, pointerY);
    if (scaleDragging) { scaleDx = pointerX - lastX; scaleDy = pointerY - lastY; }
    fill(0, 0, width, height, 0xff101015);
    MapRenderer.beginFrame(delta);
    if (mapLeft() < width && Maps.cache() != null && minecraft.world != null) {
      var projection = projection();
      var style = worldStyle();
      MapRenderer.uploadBudget(MapConfig.current().mapOpenUploads);
      GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
      try {
        clip(mapLeft(), top(), mapWidth(), bottom() - top());
        if (!buffer.draw(minecraft, projection, dimension, layer, style, width, height)) MapRenderer.terrain(
            Maps.cache(),
            projection,
            dimension,
            layer,
            false,
            style,
            !minecraft.world.isRemote,
            minecraft.world.getSeed());
        MapRenderer.markers(minecraft, projection, dimension, false, false);
      } finally { GL11.glPopAttrib(); }
    }
    if (mapLeft() == width) {
      settingsPanel.render(coordinates.active()?-1000:mx, coordinates.active()?-1000:my, delta);
      coordinates.render(this,mx,my); return;
    }
    drawMinimap(delta);
    if (screenshot) { screenshot = false; MapScreenshot.capture(minecraft); }
    int hoverX = coordinates.active() || context ? -1000 : mx, hoverY = coordinates.active() || context ? -1000 : my;
    for (var control : controls().buttons()) {
      var b = control.bounds();
      String label = fit(control.label(),b.width()-12);
      if (control.action() == WorldMapControls.Action.SETTINGS && settingsPanel != null)
        tabButton(label,b.x(),b.y(),b.width(),18,hoverX,hoverY,true,true);
      else button(label,b.x(),b.y(),b.width(),18,hoverX,hoverY,true);
    }
    if (minecraft.world != null && minecraft.world.isRemote)
      input(serverMap, mapLeft() + 8, 4, Math.min(180, mapWidth() / 3), hoverX, hoverY, "Map name");
    var cursor = bounded(projection().world(mx, my));
    String cursorHeight = "?", cursorBiome = "";
    if (Maps.cache() != null) {
      MapTile tile =
          Maps.cache()
              .peek(
                  MapMath.key(
                      dimension,
                      layer,
                      0,
                      (int) Math.floor(cursor.x()),
                      (int) Math.floor(cursor.y())));
      if (tile != null) {
        int i = MapMath.index((int) Math.floor(cursor.x()), (int) Math.floor(cursor.y()));
        if (tile.known(i)) {
          cursorHeight = Integer.toString(tile.height(i));
          cursorBiome = " · " + MapPalette.biome(tile.biome(i));
        }
      }
    }
    String readout = (int)Math.floor(cursor.x()) + ", " + cursorHeight + ", "
        + (int)Math.floor(cursor.y()) + cursorBiome;
    int blocks = (int) Math.max(1, Math.round(80 / view.scale));
    boolean separateReadout = mapWidth() < 640 || minecraft.world != null && minecraft.world.isRemote;
    int readoutY = separateReadout ? 29 : 9;
    int readoutWidth = separateReadout ? mapWidth() - 16 : Math.max(40, mapWidth() - 160);
    readout = fit(readout, readoutWidth);
    overlay(readout, mapLeft() + (mapWidth() - textRenderer.getWidth(readout)) / 2, readoutY, 0xffffff);
    var scale = scaleBounds();
    int barX = scale.x()+18, barY = scale.y()+16;
    fill(scale.x(),scale.y(),scale.x()+scale.width(),scale.y()+scale.height(),0x90000000);
    fill(barX, barY, barX + 80, barY + 1, 0xffcccccc);
    fill(barX, barY - 2, barX + 1, barY + 2, 0xffcccccc);
    fill(barX + 79, barY - 2, barX + 80, barY + 2, 0xffcccccc);
    String scaleLabel = blocks + " blocks";
    text(scaleLabel, barX + (80 - textRenderer.getWidth(scaleLabel)) / 2, barY - 13, 0xcccccc);
    if (context) {
      var actions = contextActions();
      fill(contextX, contextY, contextX + 142, contextY + actions.size() * 18, 0xf0202020);
      for (int i = 0; i < actions.size(); i++) {
        if (inside(mx, my, contextX, contextY + i * 18, 142, 18))
          fill(contextX, contextY + i * 18, contextX + 142, contextY + (i + 1) * 18, 0xff444444);
        text(actions.get(i), contextX + 5, contextY + i * 18 + 5,
            actions.get(i).equals("Teleport") && !Maps.canTeleport(minecraft, target) ? 0x666666 : 0xffffff);
      }
    }
    String status = MapScreenshot.status();
    if (status.isEmpty()) status = message.isEmpty() ? Maps.error() : message;
    if (!status.isEmpty()) overlay(fit(status, width - 16), 8, readoutY + 20, 0xffdd99);
    if (!context && !coordinates.active() && !dragging && !scaleDragging && mx >= mapLeft() && !overControls(mx, my)) {
      Waypoint hovered = hoveredWaypoint(mx, my);
      if (hovered != null) {
        String caption = hovered.death ? WaypointTimes.detail(hovered.created) : hovered.name;
        String cause = MapDeathCauses.caption(hovered);
        if (!cause.isEmpty()) caption = cause + "\n" + caption;
        caption += "\n" + hovered.x + ", " + hovered.y + ", " + hovered.z;
        if (minecraft.player != null && dimension == Maps.dimension()) {
          double dx = hovered.x + .5 - minecraft.player.x, dy = hovered.y - minecraft.player.boundingBox.minY,
              dz = hovered.z + .5 - minecraft.player.z;
          caption += "\n" + Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)) + "m";
        }
        tooltip(caption, mx, my);
      }
    }
    if (!coordinates.active() && !context) {
      var control=controls().at(mx,my);
      if (control != null && control.action()==WorldMapControls.Action.SETTINGS && settingsPanel!=null)
        tooltip("Close settings",mx,my);
    }
    if (settingsPanel != null) settingsPanel.render(coordinates.active()?-1000:mx, coordinates.active()?-1000:my, delta);
    coordinates.render(this,mx,my);
  }

  public boolean shouldPause() {
    return MapConfig.current().pauseMap || parent != null && parent.shouldPause();
  }
}
