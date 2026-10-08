package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class InterfacePreferencesTest {
  @Test void oldDocumentsReceivePresentationDefaultsWithGson289() {
    var values=new Gson().fromJson("{\"pauseToOptions\":true}",MenuPreferences.Values.class);
    values.validate();
    assertTrue(values.pauseToOptions);
    assertFalse(values.blinkingChatCursor);
    assertEquals("#FFFFFF",values.debugTextColor);
  }
  @Test void coloursAndBlinkingRoundTripAndInvalidColoursFail() {
    Gson gson=new Gson();var values=new MenuPreferences.Values();
    values.blinkingChatCursor=true;values.debugTextColor="#12AbEf";
    var saved=gson.fromJson(gson.toJson(values),MenuPreferences.Values.class);saved.validate();
    assertTrue(saved.blinkingChatCursor);assertEquals("#12AbEf",saved.debugTextColor);
    saved.debugTextColor="invalid";assertThrows(IllegalArgumentException.class,saved::validate);
  }
}
