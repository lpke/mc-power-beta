package local.luke.power.client_fixes.client.gui.multiplayer;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class ServerData {
    @NonNull
    private String name;

    @NonNull
    private String ip;

    public NbtCompound save() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", name);
        nbt.putString("ip", ip);
        return nbt;
    }

    public ServerData(NbtCompound nbt) {
        this(nbt.getString("name"), nbt.getString("ip"));
    }

    public static NbtList save(List<ServerData> servers) {
        NbtList nbt = new NbtList();
        for (ServerData server : servers) {
            nbt.add(server.save());
        }
        return nbt;
    }

    public static List<ServerData> load(NbtList nbt) {
        ArrayList<ServerData> servers = new ArrayList<>();
        for (int i = 0; i < nbt.size(); i++) {
            servers.add(new ServerData((NbtCompound) nbt.get(i)));
        }
        return servers;
    }
}
