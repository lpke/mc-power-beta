package local.luke.power.camera.client;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import local.luke.power.storage.PowerConfig;
import local.luke.power.camera.util.SavedCameraPosition;
import local.luke.power.camera.util.SavedCameraPositions;

public final class SaveManager {
    private static final Gson JSON = new Gson();
    private static String key(long seed, String world) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((seed + ":" + world).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public SavedCameraPosition[] load(long seed, String world) {
        JsonElement data = PowerConfig.section("cameraPositions").get(key(seed,world));
        if (data == null) return new SavedCameraPosition[0];
        SavedCameraPositions stored = JSON.fromJson(data,SavedCameraPositions.class);
        validate(stored.cameraPositions);
        return stored.cameraPositions;
    }
    public boolean hasSavedCameraPositions(long seed, String world) { return PowerConfig.section("cameraPositions").has(key(seed,world)); }
    public void save(long seed, String world, SavedCameraPosition[] positions) {
        validate(positions);
        // Validate the existing record before changing it.
        load(seed,world);
        SavedCameraPositions stored = new SavedCameraPositions();
        stored.seed=seed; stored.worldName=world; stored.cameraPositions=positions;
        JsonObject all = PowerConfig.section("cameraPositions"); all.add(key(seed,world),JSON.toJsonTree(stored));
        try { PowerConfig.put("cameraPositions",all); }
        catch (java.io.IOException e) { throw new IllegalStateException("Camera positions could not be saved; original data preserved",e); }
    }
    private static void validate(SavedCameraPosition[] positions) {
        if (positions == null || positions.length > 1024) throw new IllegalArgumentException("Invalid camera positions");
        java.util.Set<String> names=new java.util.HashSet<>();
        for (var p:positions) {
            if (p==null || p.name==null || p.name.length()>256 || !names.add(p.name) || p.cameraPosition==null)
                throw new IllegalArgumentException("Invalid camera position");
            var c=p.cameraPosition;
            if (!Double.isFinite(c.x) || !Double.isFinite(c.y) || !Double.isFinite(c.z)
                || !Float.isFinite(c.pitch) || !Float.isFinite(c.yaw) || !Float.isFinite(c.roll))
                throw new IllegalArgumentException("Camera coordinates must be finite");
        }
    }
}
