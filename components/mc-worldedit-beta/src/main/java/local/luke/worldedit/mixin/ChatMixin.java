package local.luke.worldedit.mixin;

import local.luke.worldedit.chat.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatScreen.class, priority = 1200)
public abstract class ChatMixin extends Screen {
  @Shadow protected String text;
  @Unique private final ChatHistory worldedit$history = new ChatHistory();
  @Unique private final CompletionCycle worldedit$completion = new CompletionCycle();

  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void worldedit$input(char c, int key, CallbackInfo ci) {
    String input = ChatAccess.text(text);
    // Leave ambiguous single-slash Tab suggestions to RetroCommands, but avoid its
    // selection handler swallowing the keystrokes needed to finish our aliases.
    boolean prefix =
        input.startsWith("/")
            && !input.contains(" ")
            && CommandCatalog.SINGLE.stream().anyMatch(n -> n.startsWith(input.substring(1)));
    if (CommandCatalog.owns(input)
        || prefix
            && (key != Keyboard.KEY_TAB
                || !FabricLoader.getInstance().isModLoaded("retrocommands"))) {
      ChatAccess.clearSuggestions((ChatScreen) (Object) this);
      if (key == Keyboard.KEY_TAB) {
        text =
            worldedit$completion.next(
                input,
                Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        ChatAccess.setText(text);
        ci.cancel();
        return;
      }
    }
    worldedit$completion.reset();
    if (!ChatAccess.nativeHistory() && (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN)) {
      text = worldedit$history.move(input, key == Keyboard.KEY_UP);
      ci.cancel();
    }
  }
}
