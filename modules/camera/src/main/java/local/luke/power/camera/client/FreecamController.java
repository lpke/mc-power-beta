package local.luke.power.camera.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import local.luke.power.camera.Freecam;
import local.luke.power.camera.mixin.WorldAccessor;
import local.luke.power.camera.util.CameraPosition;
import local.luke.power.camera.util.SavedCameraPosition;
import local.luke.power.input.CameraView;

import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class FreecamController {
    public FreecamController() {
        local.luke.power.input.TweakLifecycle.register("camera", () -> {
            setActive(false);
            cameraPositionSet = false;
            clearRenderedPosition();
        });
        local.luke.power.input.DetachedCamera.register(this::isActive,
                () -> cameraPosition.x, () -> cameraPosition.y, () -> cameraPosition.z,
                () -> cameraPosition.pitch, () -> cameraPosition.yaw);
        local.luke.power.input.MovementOwnership.registerCamera(() -> isActive() && !allowPlayerMovement);
        local.luke.power.input.TweakIndicators.register(
            local.luke.power.input.TweakIndicators.Tweak.FREECAM_PLAYER_MOVEMENT,
            () -> active && local.luke.power.camera.FreecamConfig.config.enabled && allowPlayerMovement ? "" : null,
            () -> local.luke.power.camera.FreecamConfig.config.playerMovementMessages);
    }
    private boolean active;
    private Boolean previousHideHud;
    private CameraPosition cameraPosition = new CameraPosition();
    private CameraPosition renderedPosition;
    private LivingEntity renderedEntity;
    private World renderedWorld;
    private boolean firstFramePending;
    private TreeMap<String, CameraPosition> savedCameraPositions = new TreeMap<>();
    public float move = 0f;
    public float strafe = 0f;
    public double velocityX;
    public double velocityY;
    public double velocityZ;
    public double accelerationX;
    public double accelerationY;
    public double accelerationZ;
    public boolean cameraPositionSet = false;
    public boolean jumping = false;
    public boolean sneaking = false;
    public boolean allowPlayerMovement = false;
    public boolean hidePlayer = false;
    public boolean updateSpeed = false;
    public boolean savePosition = false;
    public boolean loadPosition = false;
    public String cameraPositionName = "";


    private boolean sprintDown, sprintLatched;
    private static java.lang.reflect.Method flightSettings;
    private static java.lang.reflect.Field sprintToggle, sprintMultiplier;
    static {
        try {
            Class<?> api = Class.forName("local.luke.power.creative.config.Config");
            flightSettings = api.getMethod("current");
            Class<?> settings = flightSettings.getReturnType();
            sprintToggle = settings.getField("sprintToggle");
            sprintMultiplier = settings.getField("sprintMultiplier");
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    public float movementSpeed() {
        Minecraft minecraft = (Minecraft)FabricLoader.getInstance().getGameInstance();
        boolean pressed = local.luke.power.input.Bindings.down("power_creative.sprint");
        boolean eligible = minecraft != null && active && (move != 0 || strafe != 0 || jumping != sneaking) && minecraft.currentScreen == null && org.lwjgl.opengl.Display.isActive()
            && local.luke.power.camera.FreecamConfig.config.sprint;
        boolean toggle = true;
        double multiplier = 3;
        try {
            Object settings = flightSettings.invoke(null);
            toggle = sprintToggle.getBoolean(settings);
            multiplier = sprintMultiplier.getInt(settings) / 100.0;
        } catch (ReflectiveOperationException ignored) { }
        if (!eligible) sprintLatched = false;
        else if (toggle && pressed && !sprintDown) sprintLatched = !sprintLatched;
        sprintDown = pressed;
        return (float)(local.luke.power.camera.FreecamConfig.config.speed * (eligible && (toggle ? sprintLatched : pressed) ? multiplier : 1));
    }

    // Return if the freecam is active or not
    public boolean isActive(){
        if (active && !local.luke.power.camera.FreecamConfig.config.enabled) setActive(false);
        return active;
    }

    // Set the freecam active state
    public void setActive(boolean active){
        if (this.active != active) {
            Minecraft minecraft = (Minecraft)FabricLoader.getInstance().getGameInstance();
            if (minecraft != null && minecraft.options != null) {
                if (active) {
                    previousHideHud = minecraft.options.hideHud;
                    minecraft.options.hideHud = true;
                } else if (previousHideHud != null) {
                    minecraft.options.hideHud = previousHideHud;
                }
            }
            if (!active) previousHideHud = null;
        }
        this.active = active;
        firstFramePending = active;
        velocityX = velocityY = velocityZ = 0;
        accelerationX = accelerationY = accelerationZ = 0;
        sprintLatched = false;
        sprintDown = local.luke.power.input.Bindings.down("power_creative.sprint");
        this.allowPlayerMovement = false;
        clearMovementInput();
    }

    public void clearMovementInput() {
        move = strafe = 0;
        jumping = sneaking = false;
    }

    public void recordRenderedPosition(CameraPosition position, LivingEntity entity, World world) {
        renderedPosition = position;
        renderedEntity = entity;
        renderedWorld = world;
    }

    public void startFromView(Minecraft minecraft) {
        LivingEntity entity = minecraft.camera;
        if (renderedPosition != null && renderedEntity == entity && renderedWorld == minecraft.world) {
            cameraPosition = renderedPosition.clone();
            if (minecraft.options.thirdPerson) {
                // The saved transform is the viewpoint. Store its orbit anchor so adding
                // the third-person offset does not move the first detached frame again.
                double yaw = Math.toRadians(cameraPosition.yaw), pitch = Math.toRadians(cameraPosition.pitch);
                double distance = CameraView.distance();
                cameraPosition.x += Math.sin(yaw) * Math.cos(pitch) * distance;
                cameraPosition.y -= Math.sin(pitch) * distance;
                cameraPosition.z -= Math.cos(yaw) * Math.cos(pitch) * distance;
                if (CameraView.front()) { cameraPosition.yaw -= 180; cameraPosition.pitch = -cameraPosition.pitch; }
            }
        } else {
            setCameraPositionAndRotation(entity.x, entity.y - (entity.standingEyeHeight - 1.62F),
                    entity.z, entity.pitch, entity.yaw + 180, 0);
        }
        cameraPositionSet = true;
    }

    public boolean firstFramePending() { return firstFramePending; }
    public void frameRendered() { firstFramePending = false; }

    public void updatePlayerVisibility(LivingEntity player, float delta) {
        // Entity rendering uses the interpolated player position, too. Calculate
        // this before the first detached frame, even when the camera has not moved.
        double x = player.prevX + (player.x - player.prevX) * delta;
        double y = player.prevY + (player.y - player.prevY) * delta;
        double z = player.prevZ + (player.z - player.prevZ) * delta;
        double dx = cameraPosition.x - x, dy = cameraPosition.y - y, dz = cameraPosition.z - z;
        hidePlayer = dx * dx + dy * dy + dz * dz < 1;
    }

    public void clearRenderedPosition() {
        renderedPosition = null;
        renderedEntity = null;
        renderedWorld = null;
    }

    // Return freecam position
    public CameraPosition getCameraPosition(){
        return cameraPosition;
    }

    // Save cameraposition
    public void saveCameraPosition(String name){
        savedCameraPositions.put(name, cameraPosition.clone());
        saveCameraPositionsToFile();
    }

    // Save camerapositions to json file
    private void saveCameraPositionsToFile(){
        Minecraft minecraft = Minecraft.class.cast(FabricLoader.getInstance().getGameInstance());
        TreeMap<String, CameraPosition> cameraPositionTreeMap = getSavedCameraPositions();
        String[] keys = cameraPositionTreeMap.keySet().toArray(new String[0]);
        SavedCameraPosition[] cameraPositions = new SavedCameraPosition[keys.length];
        int index = 0;
        for(String key : keys){
            cameraPositions[index] = new SavedCameraPosition();
            cameraPositions[index].cameraPosition = cameraPositionTreeMap.get(key);
            cameraPositions[index].name = key;
            index++;
        }
        Freecam.saveManager.save(minecraft.world.getSeed(), ((WorldAccessor)minecraft.world).getProperties().getName(), cameraPositions);
    }

    // Load cameraposition from map
    public void loadCameraPosition(String name){
        if(!savedCameraPositions.containsKey(name)){
            return;
        }
        cameraPosition = savedCameraPositions.get(name).clone();
    }

    // Remove cameraposition from map
    public void removeCameraPosition(String key){
        savedCameraPositions.remove(key);
        saveCameraPositionsToFile();
    }

    // Load camerapositions from json file
    public void loadSavedCameraPositions(World world){
        if(world == null || !Freecam.saveManager.hasSavedCameraPositions(world.getSeed(), ((WorldAccessor)world).getProperties().getName())){
            setSavedCameraPositions(new SavedCameraPosition[0]);
            return;
        }
        SavedCameraPosition[] savedCameraPositions = Freecam.saveManager.load(world.getSeed(), ((WorldAccessor)world).getProperties().getName());
        setSavedCameraPositions(savedCameraPositions);
    }

    // Clear saved camerapositions map and fill with savedcamerapositions array
    private void setSavedCameraPositions(SavedCameraPosition[] savedCameraPositions){
        this.savedCameraPositions.clear();
        for(SavedCameraPosition savedCameraPosition : savedCameraPositions){
            this.savedCameraPositions.put(savedCameraPosition.name, savedCameraPosition.cameraPosition);
        }
    }

    // Get saved camerapositions count
    public int getSavedCameraPositionCount(){
        return savedCameraPositions.size();
    }

    // Get saved camerapositions in a sorted treemap
    public TreeMap<String, CameraPosition> getSavedCameraPositions(){
        TreeMap<String, CameraPosition> cameraPositions = new TreeMap<>();
        SortedSet<String> keys = new TreeSet<>(savedCameraPositions.keySet());
        for (String key : keys){
            cameraPositions.put(key, savedCameraPositions.get(key));
        }
        return cameraPositions;
    }

    // Set camera position and rotation
    public void setCameraPositionAndRotation(double x, double y, double z, float pitch, float yaw, float roll) {
        this.cameraPosition.x = x;
        this.cameraPosition.y = y;
        this.cameraPosition.z = z;
        this.cameraPosition.pitch = pitch;
        this.cameraPosition.yaw = yaw;
        this.cameraPosition.roll = roll;
    }

    // Set camera position
    public void setCameraPosition(double x, double y, double z) {
        this.cameraPosition.x = x;
        this.cameraPosition.y = y;
        this.cameraPosition.z = z;
    }

    // Set camera rotation
    public void setCameraRotation(float pitch, float yaw, float roll) {
        this.cameraPosition.pitch = pitch;
        this.cameraPosition.yaw = yaw;
        this.cameraPosition.roll = roll;
    }

    // Get camera position relative from player
    public CameraPosition getRelativeCameraPosition(CameraPosition cameraPosition, LivingEntity player, float f1){
        double d1 = player.prevX + (player.x - player.prevX) * (double)f1;
        double d2 = player.prevY + (player.y - player.prevY) * (double)f1;
        double d3 = player.prevZ + (player.z - player.prevZ) * (double)f1;


        CameraPosition relativeCameraPosition = new CameraPosition((cameraPosition.x - d1), (cameraPosition.y - d2), (cameraPosition.z - d3), cameraPosition.pitch, cameraPosition.yaw , cameraPosition.roll);

        return relativeCameraPosition;
    }

    // Update cameraposition
    public CameraPosition updateCameraPosition(LivingEntity player, float f1){
        CameraPosition view = getRelativeCameraPosition(this.cameraPosition, player, f1);
        view.yaw = CameraView.yaw(view.yaw);
        view.pitch = CameraView.pitch(view.pitch);
        Minecraft mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
        if (mc.options.thirdPerson && CameraView.front()) { view.yaw += 180; view.pitch = -view.pitch; }
        return view;
    }
}
