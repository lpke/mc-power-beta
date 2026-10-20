package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ZoomStepsFooterTest {
  @Test void allFiveActionsKeepTextPaddingAndDoNotOverlapAtNarrowOrWideSizes() {
    for (int width : new int[]{320,400,640,960,1920}) {
      int span=Math.min(420,width-24),left=(width-span)/2;
      var footer=ZoomStepsFooter.at(left,span,s->s.length()*6);
      assertEquals(left,footer.x()[0]);
      assertEquals(left+span,footer.x()[4]+footer.width()[4]);
      for(int i=0;i<5;i++) {
        assertTrue(footer.width()[i]>=ZoomStepsFooter.LABELS[i].length()*6+8);
        if(i>0)assertTrue(footer.x()[i]>=footer.x()[i-1]+footer.width()[i-1]+4);
      }
    }
  }
}
