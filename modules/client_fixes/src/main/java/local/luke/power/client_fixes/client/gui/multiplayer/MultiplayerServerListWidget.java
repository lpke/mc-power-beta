package local.luke.power.client_fixes.client.gui.multiplayer;

import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.resource.language.TranslationStorage;
import local.luke.power.client_fixes.Config;

public class MultiplayerServerListWidget extends EntryListWidget {
    private final MultiplayerScreen parent;

    public MultiplayerServerListWidget(MultiplayerScreen parent) {
        super(parent.getMinecraft(), parent.width, parent.height, 32, parent.height - 64, 36);
        this.parent = parent;
    }

    @Override
    protected int getEntryCount() {
        return this.parent.getServersList().size();
    }

    @Override
    protected void entryClicked(int slot, boolean doubleClick) {
        this.parent.selectServer(slot, doubleClick);
    }

    @Override
    protected boolean isSelectedEntry(int i) {
        return i == this.parent.getServersList().indexOf(this.parent.getSelectedServer());
    }

    @Override
    protected int getEntriesHeight() {
        return this.parent.getServersList().size() * 36;
    }

    @Override
    protected void renderBackground() {
        this.parent.renderBackground();
    }

    @Override
    protected void renderEntry(int index, int x, int y, int l, Tessellator arg) {
        ServerData server = this.parent.getServersList().get(index);
        this.parent.drawTextWithShadow(this.parent.getFontRenderer(), server.getName(), x + 2, y + 1, 0xffffff);
        if (Config.config.disableServerListIpAddresses) {
            TranslationStorage translations = TranslationStorage.getInstance();
            String hideIp = "(" + translations.get("multiplayer.power_client_fixes.hidden") + ")";
            this.parent.drawTextWithShadow(this.parent.getFontRenderer(), hideIp, x + 2, y + 12, 0x808080);
        } else {
            this.parent.drawTextWithShadow(this.parent.getFontRenderer(), server.getIp(), x + 2, y + 12, 0x808080);
        }
    }
}
