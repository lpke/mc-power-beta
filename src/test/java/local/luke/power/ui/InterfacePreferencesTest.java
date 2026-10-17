package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class InterfacePreferencesTest {
  @Test void oldDocumentsReceivePresentationDefaultsWithGson289() {
    var values=new Gson().fromJson("{\"pauseToOptions\":true}",MenuPreferences.Values.class);
    values.validate();
    assertTrue(values.pauseToOptions);
    assertTrue(values.blinkingChatCursor);
    assertEquals("#FFFFFF",values.debugTextColor);
  }
  @Test void bothColumnsRoundTripWithNativeGsonAndRejectDuplicateOwnership() {
    Gson gson=new Gson();var values=new MenuPreferences.Values();
    values.debugOrder=java.util.List.of("usedMemory","","future.provider","x");
    values.debugRightOrder=java.util.List.of("version","","allocatedMemory");
    var saved=gson.fromJson(gson.toJson(values),MenuPreferences.Values.class);saved.validate();
    assertEquals(values.debugOrder,saved.debugOrder);assertEquals(values.debugRightOrder,saved.debugRightOrder);
    saved.debugRightOrder=java.util.List.of("x");assertThrows(IllegalArgumentException.class,saved::validate);
  }
  @Test void coloursAndBlinkingRoundTripAndInvalidColoursFail() {
    Gson gson=new Gson();var values=new MenuPreferences.Values();
    values.blinkingChatCursor=true;values.debugTextColor="#12AbEf";
    var saved=gson.fromJson(gson.toJson(values),MenuPreferences.Values.class);saved.validate();
    assertTrue(saved.blinkingChatCursor);assertEquals("#12AbEf",saved.debugTextColor);
    saved.debugTextColor="invalid";assertThrows(IllegalArgumentException.class,saved::validate);
  }
}
